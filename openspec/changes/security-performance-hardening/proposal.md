## Why

The MacGamepad Android app turns a phone into a Bluetooth HID gamepad/keyboard/mouse for macOS. A security and performance audit revealed critical vulnerabilities: the app globally renames the phone's Bluetooth adapter (persisting on crash), sends HID reports (keystrokes, mouse movements) without encryption enforcement, and allocates heavily on the 125Hz hot path causing GC pressure. These issues must be fixed before any production use.

## What Changes

### Security Fixes (Critical)
- **Remove global BT adapter rename** — Delete lines 178-185 in `BluetoothGamepadService.kt`; SDP record name already handles Mac-side discovery. **BREAKING**: Phone name no longer changes globally.
- **Enforce encryption on HID traffic** — Add `BluetoothHidDeviceAppQosSettings` with encryption flags to `registerApp()` call. Requires API 30+ for full enforcement; older APIs rely on bonding MITM.
- **Add keyboard/mouse mode consent** — One-time confirmation dialog before enabling KEYBOARD mode (arbitrary keystroke injection capability).

### Performance Fixes (High Impact)
- **Eliminate `copyOf()` allocation in hot path** — `HidReportPacker.pack()` reuses a pre-allocated `ByteArray` via `System.arraycopy` instead of allocating 125×/sec.
- **Reuse ByteBuffers in keyboard/mouse path** — Pass pre-allocated buffers from transmission loop to `sendMouseMovement`/`triggerMouseClick` instead of allocating per-call.
- **Move 125Hz loop to high-priority HandlerThread** — Replace `Dispatchers.Default` coroutine with `HandlerThread(THREAD_PRIORITY_URGENT_AUDIO)` for real-time scheduling and reduced jitter.
- **Reset gyro accumulators on BT disconnect** — Clear `gyroAccumX/Y` when connection drops to prevent drift on reconnect.
- **Simplify input throttling logic** — Replace 35-condition release detection in `GamepadViewModel.onInputChanged()` with bitmask diff + stick delta threshold.

## Capabilities

### New Capabilities
- `security/bluetooth-hardening`: Encryption enforcement, adapter rename removal, device allowlist foundation
- `security/input-consent`: Per-session consent for keyboard/mouse input injection modes

### Modified Capabilities
- `input/transmission-loop`: Hot-path allocation elimination, real-time thread scheduling, gyro drift fix
- `input/throttling`: Simplified release detection algorithm

## Impact

### Affected Code
- `android/app/src/main/java/com/macgamepad/controller/bt/BluetoothGamepadService.kt` — Security fixes (S1, S2)
- `android/app/src/main/java/com/macgamepad/controller/bt/HidReportPacker.kt` — Performance fix P1
- `android/app/src/main/java/com/macgamepad/controller/bt/GamepadService.kt` — Performance fixes P2, P3, P4
- `android/app/src/main/java/com/macgamepad/controller/GamepadViewModel.kt` — Performance fix P5, security S4
- `android/app/src/main/java/com/macgamepad/controller/MainActivity.kt` — Consent dialog integration

### Dependencies
- Android API 30+ for full encryption QoS (graceful degradation to API 28)
- No new external dependencies

### Systems
- macOS bridge (`tools/gamepad_bridge.c`) unaffected by Android changes
- Existing paired devices will re-pair after adapter rename removal (SDP record name unchanged)