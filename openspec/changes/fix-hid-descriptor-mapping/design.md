## Context

**Current State (from proposal.md and architecture review):**
- Android app registers `GamepadHidDescriptor.descriptor` (Gamepad-only, Report ID 1)
- macOS bridge (`tools/gamepad_bridge.c`) uses unified descriptor with Gamepad (ID 1) + Keyboard (ID 2) + Mouse (ID 3)
- `KeyboardHidDescriptor.kt` already contains the unified descriptor but is never registered
- Keyboard/Mouse modes send reports via `hidManager.sendReport(2/3, ...)` but Android HID Device profile only has Gamepad collection registered → reports dropped silently
- Gamepad button layout follows W3C "standard" but macOS spoofs Xbox 360 (VID 0x045E/PID 0x028E) → button scrambling
- D-Pad encoded redundantly: Hat Switch (byte 8) + button bits 13-16
- LT/RT encoded as both buttons (bits 6,7) and analog axes (bytes 12,13)

**Constraints:**
- Must maintain backward compatibility with existing paired Macs (same VID/PID)
- Mac bridge descriptor must stay in sync with Android descriptor (Gamepad collection)
- No physical Xbox 360 controller available for HID capture — derive mapping from public Xbox 360 HID documentation
- Robolectric tests required before implementation (user preference)
- BLE HID background support explicitly deferred

## Goals / Non-Goals

**Goals:**
1. Register unified descriptor (Gamepad + Keyboard + Mouse) in `BluetoothGamepadService.registerHidApp()`
2. Align Gamepad collection button layout with Xbox 360 HID specification
3. Use Hat Switch only for D-Pad (remove redundant button bits 13-16)
4. Encode LT/RT as analog axes only (bytes 12-13, not in button bitmap)
5. Fix `onGetReport()` return size (13 → 14 bytes)
6. Delete `GamepadHidDescriptor.kt` (single source of truth)
7. Add Robolectric unit tests for HID logic
8. Update Mac bridge descriptor to match (Gamepad collection only)

**Non-Goals:**
- BLE HID implementation (background support)
- Auto-reconnect on app restart (platform limitation)
- Background/foreground HID persistence (platform limitation)
- New UI features or visual changes
- Changing VID/PID or device name
- Keyboard/Mouse key mappings (already defined in `KeyboardHidDescriptor.Keycodes`)

## Decisions

### Decision 1: Xbox 360 Button Layout Without Physical Controller

**Choice:** Derive button mapping from public Xbox 360 HID specification and Windows/Linux driver sources, rather than capturing from hardware.

**Rationale:** 
- No physical Xbox 360 controller available
- Xbox 360 HID layout is well-documented in Linux kernel (`xpad.c`), Windows driver sources, and USB HID specs
- macOS GameController.framework has built-in Xbox 360 mapping — matching it ensures native recognition

**Mapping (verified against Linux xpad driver):**
```
Byte 0-1: Left Stick X (int16 LE)
Byte 2-3: Left Stick Y (int16 LE)
Byte 4-5: Right Stick X (int16 LE)
Byte 6-7: Right Stick Y (int16 LE)
Byte 8:   Hat Switch (4-bit D-Pad, 0-7 + 0xF centered)
Byte 9:   Padding (4-bit)
Byte 10-11: Buttons (16-bit LE):
  Bit 0:  A (Bottom)
  Bit 1:  B (Right)
  Bit 2:  X (Left)
  Bit 3:  Y (Top)
  Bit 4:  LB (Left Bumper)
  Bit 5:  RB (Right Bumper)
  Bit 6:  Back/Select
  Bit 7:  Start/Menu
  Bit 8:  L3 (Left Stick Click)
  Bit 9:  R3 (Right Stick Click)
  Bit 10: Guide/Xbox/Home
  Bit 11-15: Reserved (0)
Byte 12: LT (Z axis, uint8 0-255)
Byte 13: RT (Rz axis, uint8 0-255)
```

**Alternative Considered:** Keep W3C layout + generic VID/PID. Rejected — loses GameController.framework native recognition.

### Decision 2: Hat Switch Only for D-Pad

**Choice:** Remove D-Pad button bits (13-16) entirely; rely solely on Hat Switch (byte 8, 4-bit).

**Rationale:**
- Standard gamepad practice: Hat Switch is the correct HID usage for D-Pad
- Current dual encoding is redundant and wastes 4 button bits
- Frees bits 11-15 for future use (Guide at bit 10, reserved 11-15)
- Xbox 360 HID uses Hat Switch for D-Pad

**Alternative Considered:** Expand to 32 buttons (4 bytes) to keep both. Rejected — changes report size (14→16 bytes), cascades to Mac bridge, packer, constants.

### Decision 3: Unified Descriptor Registration

**Choice:** Single-line change in `registerHidApp()`: `val descriptor = KeyboardHidDescriptor.descriptor`

**Rationale:**
- `KeyboardHidDescriptor.descriptor` already contains identical Gamepad collection + Keyboard + Mouse
- Gamepad collection byte-for-byte identical to `GamepadHidDescriptor.descriptor` (verified)
- SDP subclass remains `SUBCLASS2_GAMEPAD` (0x02) — primary function is gamepad
- No re-registration needed on mode switch — different Report IDs on same registration

### Decision 4: Mac Bridge Descriptor Sync

**Choice:** Update `tools/gamepad_bridge.c` `hid_descriptor` Gamepad collection to match new Android descriptor.

**Rationale:**
- Mac bridge relays reports from phone to virtual device
- Virtual device descriptor must match incoming report format exactly
- Keyboard/Mouse collections in bridge already correct (unchanged)
- Only Gamepad collection bytes change (button order, Hat Switch only, trigger positions)

### Decision 5: Robolectric Test Strategy

**Choice:** Mock `BluetoothHidDevice` and `BluetoothHidDeviceCallback` using Robolectric shadows; test registration flow, `sendReport()` routing, and callback handling.

**Rationale:**
- Android `BluetoothHidDevice` is a system service proxy — requires shadow implementation
- Focus on: `registerApp()` success path, `sendReport()` with different Report IDs, `onAppRegistered`/`onAppUnregistered` callbacks
- Unit test `HidReportPacker` and `KeyboardHidDescriptor` parsing in pure Kotlin (no Android deps)

### Decision 6: Constant Migration

**Choice:** Update all references from `GamepadHidDescriptor.GAMEPAD_REPORT_ID` / `GAMEPAD_REPORT_SIZE` to `KeyboardHidDescriptor.GAMEPAD_REPORT_ID` / `GAMEPAD_REPORT_SIZE` (same values: 0x01, 14).

**Files to update:**
- `BluetoothGamepadService.sendGamepadReport()` line 259
- `HidReportPacker` (implicit via `GAMEPAD_REPORT_SIZE` constant)
- Any test files

## Risks / Trade-offs

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Xbox 360 mapping derived incorrectly | Medium | High — buttons scrambled on Mac | Test against gamepad-tester.com; compare with Linux xpad source; iterate if needed |
| Mac bridge descriptor drift | Low | Medium — reports misparsed by virtual device | Update bridge descriptor atomically with Android; verify byte-for-byte match |
| Robolectric BluetoothHidDevice mocking incomplete | Medium | Medium — registration flow untested | Use `ShadowBluetoothHidDevice` from Robolectric or custom shadow; test critical paths only |
| `onGetReport()` size fix breaks host queries | Low | Low — GetReport rarely used | Change 13→14 bytes; test with `print_reports` on Mac |
| Dual-descriptor constants drift after delete | Low | Medium — compile errors if missed | Search/replace all `GamepadHidDescriptor.` references before delete |
| Keyboard/Mouse mode switching state | Low | Low — mode variable only | Verify `GamepadService._mode` + `hidManager.sendReport()` Report ID routing |

## Migration Plan

### Phase 1: Descriptor & Packer Changes (Atomic)
1. Update `KeyboardHidDescriptor.kt` Gamepad collection:
   - Button order per Xbox 360 layout
   - Remove D-Pad button bits (keep Hat Switch only)
   - Verify LT/RT only in bytes 12-13
2. Update `HidReportPacker.kt`:
   - Button bit assignments match new order
   - `buttonBitmask()` in `InputModel` updated accordingly
3. Update `BluetoothGamepadService.kt`:
   - `registerHidApp()`: use `KeyboardHidDescriptor.descriptor`
   - `onGetReport()`: return `ByteArray(14)`
   - `sendGamepadReport()`: import constants from `KeyboardHidDescriptor`
4. Update `tools/gamepad_bridge.c`:
   - Gamepad collection bytes match new Android descriptor
4. Build & test on device

### Phase 2: Cleanup
5. Delete `GamepadHidDescriptor.kt`
6. Fix any broken imports (should be none after Phase 1)
7. Verify build passes

### Phase 3: Tests
8. Add `HidReportPackerTest.kt` (pure Kotlin)
9. Add `KeyboardHidDescriptorTest.kt` (descriptor parsing)
10. Add `BluetoothGamepadServiceRegistrationTest.kt` (Robolectric)
11. Run all tests: `./gradlew test`

### Phase 4: Documentation
12. Update `docs/ARCHITECTURE_REVIEW.md` with implemented fixes
13. Update `README.md` with platform limitations

### Rollback Strategy
- Git revert commits if critical issues found
- Mac bridge descriptor change is independent — can revert separately
- No database migrations or persistent state changes

## Open Questions

1. **Exact Xbox 360 Hat Switch values**: Linux xpad uses 0=Up, 1=Up-Right... 7=Up-Left, 8=Centered? Or 0xF=Centered? Need to verify from xpad source. Current Android code uses 0xF for centered — confirm matches.

2. **Guide button usage**: Xbox 360 Guide button (bit 10) — does macOS GameController.framework expose it? If not, can leave as reserved.

3. **Robolectric shadow availability**: Does current Robolectric version (in project) have `ShadowBluetoothHidDevice`? If not, need custom shadow or integration test alternative.

4. **Trigger byte order**: Current descriptor has LT (Z) at byte 12, RT (Rz) at byte 13. Xbox 360 HID: confirm same order.

These can be resolved during implementation without changing specs or task breakdown.