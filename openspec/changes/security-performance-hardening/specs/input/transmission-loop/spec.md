## MODIFIED Requirements

### Requirement: Zero-allocation hot path for HID report packing
The system SHALL pack HID input reports at 125Hz without allocating new ByteArray objects on each call.

#### Scenario: No GC pressure from report packing
- **WHEN** `HidReportPacker.pack()` is called 125 times per second continuously
- **THEN** zero `ByteArray` allocations occur (measured via Android Studio profiler or allocation tracker)

#### Scenario: Packed report matches descriptor exactly
- **WHEN** an `InputState` is packed
- **THEN** the 13-byte output matches the HID report descriptor layout (left stick X/Y, right stick X/Y, hat, buttons, triggers)

### Requirement: Real-time priority transmission loop
The system SHALL run the 125Hz HID transmission loop on a dedicated high-priority thread to minimize jitter and prevent thread pool contention.

#### Scenario: Loop runs on HandlerThread with URGENT_AUDIO priority
- **WHEN** `GamepadService.startSession()` starts the transmission loop
- **THEN** the loop executes on a `HandlerThread` with `THREAD_PRIORITY_URGENT_AUDIO` (or equivalent real-time priority)

#### Scenario: Loop maintains 8ms interval under load
- **WHEN** the system is under CPU load (other apps running, GC cycles)
- **THEN** the average interval between report sends stays within 8ms ± 1ms

#### Scenario: Loop stops cleanly on session end
- **WHEN** `GamepadService.stopSession()` is called
- **THEN** the transmission thread stops within 50ms and releases all resources

### Requirement: Gyroscope accumulators reset on Bluetooth disconnect
The system SHALL reset gyroscope accumulation variables when the Bluetooth connection drops to prevent drift on reconnect.

#### Scenario: Gyro accumulators cleared on disconnect
- **WHEN** `BluetoothGamepadService.ConnectionState` changes to `DISCONNECTED` or `REGISTERED_WAITING`
- **THEN** `gyroAccumX` and `gyroAccumY` are reset to 0.0f

#### Scenario: No extreme mouse movement on reconnect
- **WHEN** device reconnects after gyro was enabled during disconnect
- **THEN** first mouse/gamepad report shows centered sticks (no accumulated drift)

### Requirement: Reused ByteBuffers for keyboard/mouse reports
The system SHALL reuse pre-allocated ByteBuffers for keyboard and mouse HID reports instead of allocating new buffers per transmission.

#### Scenario: No ByteBuffer allocations in KEYBOARD mode loop
- **WHEN** `GamepadService` runs in KEYBOARD mode at 125Hz
- **THEN** `sendKeyboardReport` and `sendMouseReportDirect` reuse buffers created once in the loop

#### Scenario: Mouse movement API accepts reusable buffer
- **WHEN** `GamepadViewModel.onMouseMoved()` calls `sendMouseMovement`
- **THEN** an optional reusable `ByteBuffer` parameter is used (backward compatible default)