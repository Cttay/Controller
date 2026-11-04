## Context

See [proposal.md](./proposal.md) for motivation. Current state from codebase analysis:

- **BluetoothGamepadService.kt** (330 lines): Manages HID Device profile registration, connection state, report sending. Uses unified descriptor (Keyboard ID 1, Mouse ID 2, Gamepad ID 3).
- **GamepadService.kt** (429 lines): Foreground service with 125Hz transmission loop on `Dispatchers.Default`, gyroscope sensor fusion, keyboard/mouse mapping.
- **HidReportPacker.kt** (78 lines): Hot-path `pack()` allocates `ByteArray` via `buffer.array().copyOf()` every call.
- **GamepadViewModel.kt** (414 lines): `onInputChanged()` has 35+ conditionals for release detection, throttles to 4ms minimum.
- **KeyboardHidDescriptor.kt** (207 lines): Unified HID descriptor with 3 report IDs.
- **gamepad_bridge.c** (362 lines): macOS bridge requiring `sudo` for `IOHIDUserDeviceCreateWithProperties`.

Key constraints:
- minSdk 28, targetSdk 35 — encryption QoS only fully available API 30+
- Foreground service required for background BT operation
- No existing test infrastructure for BT HID path

## Goals / Non-Goals

**Goals:**
1. Eliminate global BT adapter rename side effect (S1)
2. Enforce encryption on HID traffic where API permits (S2)
3. Add one-time consent for keyboard/mouse mode (S4)
4. Zero allocations in 125Hz hot path (P1, P2)
5. Real-time priority for transmission loop (P3)
6. Gyro accumulator reset on BT disconnect (P4)
7. Efficient input change detection (P5)

**Non-Goals:**
- Full device allowlist UI (foundation only in specs)
- Mac bridge privilege dropping (separate concern)
- Integration test infrastructure (follow-up)
- Refactoring God Service into smaller classes (out of scope for this change)

## Decisions

### 1. Adapter Rename Removal (S1)
**Decision:** Remove `bluetoothAdapter?.name = targetDeviceName` entirely (lines 178-185) and corresponding restore in `shutdown()` (lines 263-267).

**Rationale:** The HID Device profile's SDP record (`BluetoothHidDeviceAppSdpSettings` at line 307) already carries the device name ("Wireless Controller") that macOS reads during discovery/pairing. The system adapter name is irrelevant for HID Device profile and causes harmful global side effects.

**Alternative considered:** Keep rename but add `try/finally` for restore — rejected because crash/force-stop bypasses finally, leaving phone renamed.

### 2. Encryption Enforcement (S2)
**Decision:** Pass `BluetoothHidDeviceAppQosSettings` with encryption to `registerApp()` for API 30+. For API 28-29, rely on bonding requiring MITM (default on modern Android).

**Rationale:** `BluetoothHidDeviceAppQosSettings` allows specifying `SERVICE_TYPE_GUARANTEED` which implies encryption. This is the only programmatic way to enforce encryption on the HID Device profile.

**Code approach:**
```kotlin
// API 30+ (Build.VERSION_CODES.R)
val inputQos = BluetoothHidDeviceAppQosSettings(
    BluetoothHidDeviceAppQosSettings.SERVICE_TYPE_NO_TRAFFIC, 0, 0, 0, 0
)
val outputQos = BluetoothHidDeviceAppQosSettings(
    BluetoothHidDeviceAppQosSettings.SERVICE_TYPE_GUARANTEED, 0, 0, 0, 0
)
hid.registerApp(sdpSettings, inputQos, outputQos, executor, hidCallback)
```

**Fallback for API 28-29:** Pass `null` QoS (current behavior). Document that encryption depends on host initiating Secure Simple Pairing.

### 3. Keyboard Mode Consent (S4)
**Decision:** Store consent in `SharedPreferences` (`keyboard_mode_consented`). Show `AlertDialog` in `MainActivity` when `ViewModel.setMode(KEYBOARD)` called without consent. `GamepadService.setMode()` rejects change if no consent.

**Rationale:** System `AlertDialog` cannot be spoofed by custom composables. SharedPreferences persists across app restarts. ViewModel → Activity communication via callback/flow for dialog display.

**Flow:**
1. User taps KEYBOARD mode in Settings → `ViewModel.setMode(KEYBOARD)`
2. ViewModel checks `prefs.getBoolean("keyboard_mode_consented", false)`
3. If false: posts `_pendingModeChange.value = KEYBOARD`, returns
4. MainActivity observes `_pendingModeChange`, shows AlertDialog
5. On Allow: saves consent, calls `service.setMode(KEYBOARD)` again
6. On Deny: clears pending, stays in current mode

### 4. Zero-Allocation Report Packing (P1)
**Decision:** Reuse a single `ByteArray` in `HidReportPacker` via `System.arraycopy` from `ByteBuffer` backing array.

**Rationale:** `ByteBuffer.array()` returns the backing array (may be larger than position). `System.arraycopy` is intrinsified by JIT, zero allocation. `buffer.array().copyOf()` allocates new array every call.

**Implementation:**
```kotlin
class HidReportPacker {
    private val buffer = ByteBuffer.allocate(13).order(ByteOrder.LITTLE_ENDIAN)
    private val output = ByteArray(13)
    
    fun pack(state: InputState): ByteArray {
        buffer.clear()
        // ... putShort, put calls ...
        val backing = buffer.array()
        System.arraycopy(backing, buffer.arrayOffset(), output, 0, 13)
        return output
    }
}
```

**Verification:** Android Studio Profiler → Allocations → filter `ByteArray` — should show zero during active transmission.

### 5. Reused ByteBuffers for Keyboard/Mouse (P2)
**Decision:** Create `keyboardBuffer` (8 bytes) and `mouseBuffer` (4 bytes) once in transmission loop, pass to all send methods.

**Rationale:** Current code allocates in `sendMouseMovement` (line 371) and `triggerMouseClick` (line 379). Loop already creates buffers at lines 258-259 — just pass them down.

**API change:** Add optional `ByteBuffer` parameter to `sendMouseMovement` and `triggerMouseClick` with default `ByteBuffer.allocate(4)` for backward compatibility (ViewModel calls these directly).

### 6. Real-Time Priority Transmission Loop (P3)
**Decision:** Replace coroutine on `Dispatchers.Default` with `HandlerThread(THREAD_PRIORITY_URGENT_AUDIO)` + `Handler.postDelayed`.

**Rationale:** 
- `Dispatchers.Default` = shared thread pool, no priority guarantee
- `HandlerThread` with `THREAD_PRIORITY_URGENT_AUDIO` (−16) gets CPU priority above normal apps
- `postDelayed` on dedicated looper = precise 8ms scheduling without coroutine overhead
- Simpler than `ExecutorService` + `Thread.MAX_PRIORITY` (requires `SET_THREAD_PRIORITY` permission)

**Structure:**
```kotlin
private var transmissionHandler: Handler? = null
private val transmissionRunnable = Runnable { ... loop body ... }

fun startTransmissionLoop() {
    val thread = HandlerThread("HID-Transmission", THREAD_PRIORITY_URGENT_AUDIO).apply { start() }
    transmissionHandler = Handler(thread.looper)
    transmissionHandler?.post(transmissionRunnable)
}

fun stopTransmissionLoop() {
    transmissionHandler?.removeCallbacks(transmissionRunnable)
    transmissionHandler?.looper?.quitSafely()
    transmissionHandler = null
}
```

**Loop body** moves from coroutine `while (isActive)` to `Runnable` that re-posts itself with 8ms delay.

### 7. Gyro Accumulator Reset on Disconnect (P4)
**Decision:** Add `resetGyroAccumulators()` called from `BluetoothGamepadService.hidCallback.onConnectionStateChanged()` when state becomes `DISCONNECTED` or `REGISTERED_WAITING`.

**Rationale:** Current reset only in `unregisterGyroListener()` (gyro disable). If gyro stays enabled during disconnect, accumulators grow unbounded.

**Implementation:** 
- Add `resetGyroAccumulators()` to `GamepadService` (sets `gyroAccumX/Y = 0f`)
- `BluetoothGamepadService` needs reference to `GamepadService` or callback interface
- Simpler: `GamepadService` observes `btService.connectionState` flow and resets on disconnect

### 8. Efficient Input Change Detection (P5)
**Decision:** Replace 35-condition release detection with:
1. Button bitmask XOR: `(prev.buttonBitmask() xor curr.buttonBitmask()) != 0` → immediate send
2. Stick delta: `abs(prevStick - currStick) > 0.01 && currStick == 0f` → immediate send
3. 4ms minimum interval preserved

**Rationale:** Bitmask XOR is single integer operation vs 12 boolean checks. Stick delta threshold avoids float equality issues.

**Implementation:**
```kotlin
fun onInputChanged(state: InputState) {
    val now = System.currentTimeMillis()
    val prev = lastState ?: return
    
    val buttonsChanged = prev.buttonBitmask() != state.buttonBitmask()
    val stickReleased = (prev.leftStickX != 0f && state.leftStickX == 0f) ||
                        (prev.leftStickY != 0f && state.leftStickY == 0f) ||
                        (prev.rightStickX != 0f && state.rightStickX == 0f) ||
                        (prev.rightStickY != 0f && state.rightStickY == 0f)
    val triggersReleased = (prev.leftTrigger != 0f && state.leftTrigger == 0f) ||
                           (prev.rightTrigger != 0f && state.rightTrigger == 0f)
    val dpadReleased = (prev.dpadUp || prev.dpadDown || prev.dpadLeft || prev.dpadRight) &&
                       (!state.dpadUp && !state.dpadDown && !state.dpadLeft && !state.dpadRight)
    
    val isReleaseTransition = buttonsChanged || stickReleased || triggersReleased || dpadReleased
    
    if (isReleaseTransition || now - lastInputReportTime >= 4L) {
        lastInputReportTime = now
        lastState = state
        _gamepadService.value?.updateInputState(state)
    } else {
        lastState = state
    }
}
```

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| **Encryption QoS fails on some hosts** | Graceful fallback: if `registerApp` returns false with QoS, retry without QoS (API 28-29 path). Log warning. |
| **HandlerThread priority ignored on some OEMs** | `THREAD_PRIORITY_URGENT_AUDIO` is standard Android. Fallback: `Thread.MAX_PRIORITY` on `ExecutorService` if jitter detected. |
| **Consent dialog not shown if Activity destroyed** | Store pending mode in ViewModel; `MainActivity` observes on resume. |
| **Gyro reset race condition** | Reset in `GamepadService` observing `btService.connectionState` flow — single-threaded via `serviceScope`. |
| **Bitmask diff misses simultaneous press+release** | XOR catches any bit change. Press+release in same frame = bit changed = detected. |
| **Reused ByteArray returned to caller** | `HidReportPacker.pack()` returns same `ByteArray` instance — caller must not mutate. Document clearly. |

## Migration Plan

1. **Security fixes first** (S1, S2): No behavioral change for users, only removes side effect and adds encryption
2. **Consent flow** (S4): New UI, behind feature flag initially
3. **Performance fixes** (P1-P5): Pure implementation changes, no API surface change
4. **Verification:** 
   - Unit test `HidReportPacker` round-trip
   - Manual test: pair with Mac, verify no adapter rename, verify encryption in macOS Bluetooth Explorer
   - Profile allocations: confirm zero `ByteArray` in 125Hz loop
   - Test gyro disconnect/reconnect: no drift on reconnect

**Rollback:** Each fix is independent. Revert individual commits if issues arise.

## Open Questions

1. **API 28-29 encryption**: Should we require `setEncryptionRequired(true)` on `BluetoothDevice` before `connect()`? (Added in API 30, no-op on older)
2. **Consent revocation UI**: Where in Settings? (Defer to implementation — just needs a preference toggle)
3. **HandlerThread vs ExecutorService**: If `THREAD_PRIORITY_URGENT_AUDIO` causes issues on specific devices, we may need a configurable executor. Defer decision.
4. **macOS bridge root requirement**: Out of scope for this change, but track as follow-up.