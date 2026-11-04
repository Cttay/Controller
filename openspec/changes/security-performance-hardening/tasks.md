## 1. Security: Bluetooth Adapter Rename Removal (S1)

- [x] 1.1 Remove global BT adapter rename in `BluetoothGamepadService.initialize()` (lines 178-185) and verify `BluetoothAdapter.name` unchanged after `initialize()`
- [x] 1.2 Remove adapter name restore logic in `BluetoothGamepadService.shutdown()` (lines 263-267) and verify no crash leaves phone renamed
- [x] 1.3 Verify Mac still discovers "Wireless Controller" via SDP record name in `BluetoothHidDeviceAppSdpSettings` (line 307)

## 2. Security: Encryption Enforcement (S2)

- [x] 2.1 Add `BluetoothHidDeviceAppQosSettings` with encryption to `registerApp()` for API 30+ in `BluetoothGamepadService.registerHidApp()` (lines 314-320)
- [x] 2.2 Add API level guard: use QoS on API 30+, pass `null` on API 28-29 with comment explaining fallback
- [x] 2.3 Add `device.setEncryptionRequired(true)` call in `connectToDevice()` for API 30+ (graceful no-op on older)
- [x] 2.4 Fix QoS channel swap: Input=GUARANTEED (our reports), Output=NO_TRAFFIC (host→device)
- [ ] 2.5 Verify encryption in macOS Bluetooth Explorer: connected device shows "Encryption: On" for HID traffic

## 3. Security: Keyboard Mode Consent (S4)

- [x] 3.1 Add `keyboard_mode_consented` boolean to `SharedPreferences` in `GamepadViewModel` (default false)
- [x] 3.2 Modify `GamepadViewModel.setMode(KEYBOARD)` to check consent; if missing, set `_pendingModeChange.value = KEYBOARD` and return
- [x] 3.3 Add `_pendingModeChange` `MutableStateFlow<GamepadService.Companion.Mode?>` to `GamepadViewModel`
- [x] 3.4 In `MainActivity`, observe `_pendingModeChange` and show `AlertDialog` with "Keyboard mode sends keystrokes and mouse clicks to your Mac. Allow?" message
- [x] 3.5 On dialog Allow: save consent to prefs, call `service.setMode(KEYBOARD)` again
- [x] 3.6 On dialog Deny: clear `_pendingModeChange`, keep current mode
- [x] 3.7 Add consent revocation toggle in Settings screen (SettingsScreen.kt)
- [x] 3.8 Modify `GamepadService.setMode()` to reject KEYBOARD if consent not granted (log warning, keep current mode)

## 4. Performance: Zero-Allocation Report Packing (P1)

- [x] 4.1 Add `private val output = ByteArray(13)` field to `HidReportPacker`
- [x] 4.2 Replace `buffer.array().copyOf()` with `System.arraycopy(buffer.array(), buffer.arrayOffset(), output, 0, 13); return output`
- [ ] 4.3 Add unit test for `HidReportPacker.pack()` verifying output matches expected byte layout for known `InputState`
- [ ] 4.4 Profile allocations in Android Studio: run app, transmit at 125Hz for 10s, filter `ByteArray` allocations — verify zero allocations from `pack()`

## 5. Performance: Reused ByteBuffers for Keyboard/Mouse (P2)

- [x] 5.1 In `GamepadService.startTransmissionLoop()`, create `keyboardBuffer = ByteBuffer.allocate(8)` and `mouseBuffer = ByteBuffer.allocate(4)` once (already at lines 258-259)
- [x] 5.2 Modify `sendKeyboardReport(keys, buffer)` to use passed buffer (already accepts buffer parameter — verify no allocation inside)
- [x] 5.3 Modify `sendMouseReportDirect(dx, dy, buttons, buffer)` to use passed buffer (already accepts buffer parameter)
- [x] 5.4 Add `ByteBuffer` parameter to `sendMouseMovement(dx, dy, buffer: ByteBuffer = ByteBuffer.allocate(4))` with default for backward compatibility
- [x] 5.5 Add `ByteBuffer` parameter to `triggerMouseClick(buffer: ByteBuffer = ByteBuffer.allocate(4))` with default
- [x] 5.6 Update call sites in transmission loop to pass `mouseBuffer` to both methods
- [ ] 5.7 Verify no `ByteBuffer` allocations in KEYBOARD mode via profiler

## 6. Performance: Real-Time Priority Transmission Loop (P3)

- [x] 6.1 Replace `serviceScope.launch { while (isActive) { ... delay(REPORT_INTERVAL_MS) } }` with `HandlerThread` + `Handler.postDelayed` approach
- [x] 6.2 Create `HandlerThread("HID-Transmission", THREAD_PRIORITY_URGENT_AUDIO)` in `startTransmissionLoop()`
- [x] 6.3 Implement `transmissionRunnable` that executes loop body and re-posts itself with 8ms delay
- [x] 6.4 In `stopTransmissionLoop()`, remove callbacks and `looper.quitSafely()`
- [x] 6.5 Move gyro registration/unregistration to align with new thread lifecycle
- [ ] 6.6 Verify loop maintains 8ms interval under load: log timestamps, compute avg/stddev over 1000 iterations
- [ ] 6.7 Ensure `updateInputState()` (called from ViewModel on UI thread) safely communicates with transmission thread — use `@Volatile` state (already done) or `AtomicReference`

## 7. Performance: Gyro Accumulator Reset on Disconnect (P4)

- [x] 7.1 Add `resetGyroAccumulators()` method to `GamepadService` that sets `gyroAccumX = 0f; gyroAccumY = 0f`
- [x] 7.2 In `GamepadService`, observe `btService.connectionState` flow in `serviceScope` and call `resetGyroAccumulators()` when state == `DISCONNECTED` or `REGISTERED_WAITING`
- [ ] 7.3 Verify: enable gyro, disconnect BT, wait 5s, reconnect — first report shows centered sticks (no drift)

## 8. Performance: Efficient Input Change Detection (P5)

- [x] 8.1 Refactor `GamepadViewModel.onInputChanged()` to use bitmask XOR for button changes: `prev.buttonBitmask() != state.buttonBitmask()`
- [x] 8.2 Replace stick release conditionals with delta threshold: `abs(prevStick - currStick) > 0.01 && currStick == 0f` for each axis
- [x] 8.3 Combine triggers and dpad into same pattern
- [x] 8.4 Preserve 4ms minimum interval: `now - lastInputReportTime >= 4L`
- [ ] 8.5 Verify: rapid stick wiggle produces max ~250Hz reports; button press+release in same frame detected

## 9. Integration Verification

- [ ] 9.1 Full manual test: pair with Mac, verify gamepad mode works (sticks, buttons, triggers, dpad)
- [ ] 9.2 Switch to keyboard mode: verify consent dialog appears, Allow enables keyboard input on Mac
- [ ] 9.3 Verify adapter name unchanged in Android Settings > Bluetooth after pairing
- [ ] 9.4 Verify encryption shown in macOS Bluetooth Explorer
- [ ] 9.5 Profile allocations in both GAMEPAD and KEYBOARD modes for 30s each
- [ ] 9.6 Test gyro: enable, rotate phone, verify mouse movement on Mac; disconnect/reconnect, verify no drift
- [ ] 9.7 Run existing unit tests (if any) and verify no regressions