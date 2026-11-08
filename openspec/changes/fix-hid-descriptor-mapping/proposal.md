## Why

Physical device testing (12 scenarios on PJX110/Android 16) revealed two critical blockers in Scenario 7 (Profile Switch Mid-Session):
1. **Keyboard mode broken** — HID keyboard reports (Report ID 2) sent but Mac receives no input
2. **Gamepad button mapping wrong** — Most buttons report incorrect values in gamepad-tester.com

Root cause: **Android registers a Gamepad-only HID descriptor** (`GamepadHidDescriptor.descriptor`, Report ID 1 only), but **the macOS bridge (`tools/gamepad_bridge.c`) uses a unified descriptor** with Gamepad (ID 1) + Keyboard (ID 2) + Mouse (ID 3). The unified descriptor already exists in `KeyboardHidDescriptor.kt` but is never registered. Additionally, the Gamepad collection's button layout doesn't match the Xbox 360 VID/PID (0x045E/0x028E) expected by macOS GameController.framework.

This change fixes the HID descriptor registration and aligns button mapping with Xbox 360 layout (using Hat Switch only for D-Pad, per standard gamepad practice).

## What Changes

1. **Register unified descriptor** — Change `BluetoothGamepadService.registerHidApp()` to use `KeyboardHidDescriptor.descriptor` instead of `GamepadHidDescriptor.descriptor`. Enables Keyboard (Report ID 2) and Mouse (Report ID 3) modes immediately.

2. **Fix Gamepad button mapping to Xbox 360 layout** — Update `KeyboardHidDescriptor` gamepad collection button order and `HidReportPacker.pack()` bit assignments to match Xbox 360 HID layout expected by macOS GameController.framework for VID 0x045E/PID 0x028E.

3. **Use Hat Switch only for D-Pad** — Remove redundant D-Pad button mappings (bits 13-16) from gamepad collection; rely solely on Hat Switch (4-bit, byte 8) for 8-direction D-Pad input. Frees 4 bits, adds DPadRight support, matches standard gamepad practice.

4. **Fix `onGetReport()` return size** — Change `ByteArray(13)` to `ByteArray(14)` in `BluetoothGamepadService.onGetReport()`.

5. **Delete `GamepadHidDescriptor.kt`** — Eliminate dual-descriptor anti-pattern; single source of truth in `KeyboardHidDescriptor.kt`.

6. **Unit tests** — Add Robolectric tests for `HidReportPacker` round-trip, `InputState.buttonBitmask()`, descriptor parsing, and `BluetoothGamepadService` registration flow with mocked `BluetoothHidDevice`.

## Capabilities

### New Capabilities
- `hid/unified-descriptor`: Registers unified HID descriptor (Gamepad + Keyboard + Mouse) with Android Bluetooth HID Device profile; enables all three report IDs
- `hid/xbox360-button-mapping`: Gamepad button layout matches Xbox 360 HID specification for native macOS GameController.framework recognition

### Modified Capabilities
- `hid/gamepad-report`: Button mapping changed from W3C "standard" layout to Xbox 360 layout; D-Pad now uses Hat Switch only (no button bits); DPadRight added
- `hid/keyboard-report`: Now functional (was registered but never worked due to missing Keyboard collection in descriptor)
- `hid/mouse-report`: Now functional (was registered but never worked due to missing Mouse collection in descriptor)

## Impact

**Android Code:**
- `BluetoothGamepadService.kt`: `registerHidApp()` descriptor change; `onGetReport()` size fix; `sendGamepadReport()` constant update
- `KeyboardHidDescriptor.kt`: Gamepad collection button order & Hat Switch-only D-Pad; remove redundant button bits
- `HidReportPacker.kt`: Button bit assignments aligned with new descriptor order
- `GamepadHidDescriptor.kt`: **Deleted**
- `GamepadService.kt`: Import updates for constants

**Mac Bridge:**
- `tools/gamepad_bridge.c`: `hid_descriptor` byte array updated to match new Android descriptor (Gamepad collection only; Keyboard/Mouse unchanged)

**Tests:**
- New: `android/app/src/test/java/com/macgamepad/controller/bt/HidReportPackerTest.kt`
- New: `android/app/src/test/java/com/macgamepad/controller/bt/KeyboardHidDescriptorTest.kt`
- New: `android/app/src/test/java/com/macgamepad/controller/bt/BluetoothGamepadServiceRegistrationTest.kt`

**Documentation:**
- `docs/ARCHITECTURE_REVIEW.md`: Update with implemented fixes
- `README.md`: Document platform limitations (no background HID, no auto-reconnect)