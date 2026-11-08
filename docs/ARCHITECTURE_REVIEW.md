# MacGamepad Architecture Review — Bluetooth HID Implementation

**Date**: 2026-11-09  
**Reviewer**: @advisor (via orchestrator)  
**Source**: Physical device test results (docs/TEST_RESULTS.md, 12 scenarios on PJX110/Android 16)

---

## Executive Summary

The MacGamepad Android app implements a **Classic Bluetooth HID Device profile** to present as a gamepad/keyboard/mouse to macOS. Physical device testing revealed **two critical blockers** (Scenario 7) rooted in a single architectural mismatch: **Android registers a Gamepad-only HID descriptor, but the macOS bridge uses a unified descriptor with Gamepad + Keyboard + Mouse collections**.

All other test scenarios pass or reflect genuine platform limitations of the Classic BT HID Device profile.

---

## Critical Findings

### 1. Root Cause: Wrong HID Descriptor Registered

**File**: `BluetoothGamepadService.kt` line 309
```kotlin
val descriptor = GamepadHidDescriptor.descriptor  // GAMEPAD-ONLY (Report ID 1 only)
```

**MacOS Bridge** (`tools/gamepad_bridge.c`) uses **unified descriptor** with 3 collections:
- Report ID 1: Gamepad
- Report ID 2: Keyboard
- Report ID 3: Mouse

**Impact**:
- **Keyboard mode broken** (Scenario 7): Report ID 2 sent but never registered on Android → Mac receives nothing
- **Gamepad button mapping wrong** (Scenario 7): Even Gamepad collection (ID 1) has different button layout than Xbox 360 VID/PID (0x045E/0x028E) expected by macOS GameController.framework

### 2. Dual Descriptor Anti-Pattern

Two nearly-identical gamepad descriptors exist:
- `GamepadHidDescriptor.kt` — used for registration (Gamepad-only)
- `KeyboardHidDescriptor.kt` — contains unified descriptor (Gamepad + Keyboard + Mouse) but **never registered**

The Gamepad collection in both descriptors is **byte-for-byte identical** — verified by comparing descriptor byte arrays.

---

## Platform Limitations (Confirmed)

| Scenario | Limitation | Verdict | Mitigation |
|----------|------------|---------|------------|
| 3 | No auto-reconnect on app restart | **True platform limitation** | HID Device registration tied to process lifetime; implicit `unregisterApp()` on process death |
| 10 | Background/foreground loses HID | **True platform limitation** | `FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE` keeps process alive but **does not maintain HID registration** in background |
| — | Requires foreground app | **True for Classic BT HID** | **BLE HID** (GATT-based) supports background — major architectural change (P2+) |

---

## Priority Fix Plan

### P0 — Critical (Week 1)

| ID | Fix | Risk | Effort |
|----|-----|------|--------|
| **P0-1** | Register unified descriptor: `KeyboardHidDescriptor.descriptor` in `registerHidApp()` | **Low** — Gamepad collection identical | 1 day |
| **P0-2** | Fix Gamepad button mapping to match Xbox 360 HID layout expected by macOS | **Medium** — requires Mac testing | 2-3 days |
| **P0-3** | Add DPadRight (17th button) — expand to 32-bit or remap | **Low** | 1 day |
| **P0-4** | Fix `onGetReport()` return size: 13 → 14 bytes | **Low** | <1 day |

### P1 — Quality (Week 2)

| ID | Fix | Risk | Effort |
|----|-----|------|--------|
| P1-1 | Unit tests: `HidReportPacker` round-trip, `InputState.buttonBitmask()`, descriptor parsing | Low | 2-3 days |
| P1-2 | Robolectric test: Mock `BluetoothHidDevice` for registration flow | Low | 2-3 days |
| P1-3 | Delete `GamepadHidDescriptor.kt` (dead code) | Low | <1 day |

### P2/P3 — Polish (Week 3)

| ID | Fix | Risk | Effort |
|----|-----|------|--------|
| P2-1 | Landscape UI truncation (edge-to-edge insets with side nav bar) | Low | 1-2 days |
| P3-1 | README documentation of platform limitations | None | <1 day |

---

## Additional Architectural Concerns

### A. Misleading `setMode()` Implementation
**File**: `BluetoothGamepadService.kt` line 226
```kotlin
override fun setMode(mode: Mode) { activeMode = mode }  // No-op!
```
Mode change only updates a variable. With unified descriptor, this works (different Report IDs on same registration), but the method name implies re-registration capability that doesn't exist.

### B. Foreground Service Notification Misleading
**File**: `GamepadService.kt` line 118
```kotlin
startForeground(..., ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
```
Notification claims "Keeps Bluetooth controller connection alive in background" — but Scenario 10 proves HID connection drops. Should either document limitation in notification text or use different foreground service type.

### C. Zero Integration Test Coverage
Scenario 11 (rapid profile switch) passes UI stress test but **functional correctness not verified** — Keyboard mode broken, Gamepad mapping wrong. Future HID logic changes have no automated validation.

---

## Verification Steps for P0 Fixes

| Step | Action | Expected if Fixed |
|------|--------|-------------------|
| 1 | Change `registerHidApp()` to use `KeyboardHidDescriptor.descriptor` | Keyboard mode works in Notes.app; Mouse mode works |
| 2 | Test Keyboard input on Mac | Characters appear in Notes.app |
| 3 | Test gamepad-tester.com | All 16 buttons + DPad correct; DPadRight works (P0-3) |
| 4 | Verify `onGetReport()` returns 14 bytes | Host doesn't reject GetReport requests |

---

## Risk Assessment

| Change | Risk | Mitigation |
|--------|------|------------|
| P0-1 (unified descriptor) | **Very Low** — Gamepad collection identical; only adds Keyboard/Mouse collections | Verified by byte-array comparison of both descriptors |
| P0-2 (button mapping) | **Medium** — Requires matching macOS GameController.framework Xbox 360 expectations | Capture real Xbox 360 HID report on Mac for reference mapping |
| P0-3 (DPadRight) | **Low** — Descriptor change + packer update | 32-bit button mask or remap unused bit |

---

## Recommendation

**Start with P0-1 immediately** — single-line change in `BluetoothGamepadService.registerHidApp()` that unblocks Keyboard/Mouse modes entirely with minimal risk. The unified descriptor's Gamepad collection is verified identical to the current gamepad-only descriptor.

Then proceed with P0-2/P0-3 for Gamepad correctness, followed by P1 test infrastructure to prevent regressions.