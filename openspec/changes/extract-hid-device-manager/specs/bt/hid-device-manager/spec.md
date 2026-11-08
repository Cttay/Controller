## Purpose

Provides a platform-agnostic interface for Bluetooth HID Device profile management, enabling testable connection lifecycle, report transmission, bonded device access, and mode/profile configuration without coupling to Android framework implementation.

## ADDED Requirements

### Requirement: Connection state observation
The system SHALL expose a connection state stream that emits the current HID Device profile connection status.

#### Scenario: Initial state
- **WHEN** the interface is created before initialization
- **THEN** the connection state stream emits `DISCONNECTED`

#### Scenario: State transitions during registration
- **WHEN** `initialize()` is called
- **THEN** the connection state stream emits `REGISTERING`, then `REGISTERED_WAITING`

#### Scenario: State transitions during connection
- **WHEN** a host connects to the HID Device profile
- **THEN** the connection state stream emits `CONNECTING`, then `CONNECTED`

#### Scenario: State transitions on disconnect
- **WHEN** the host disconnects
- **THEN** the connection state stream emits `REGISTERED_WAITING`

#### Scenario: Error state
- **WHEN** an unrecoverable error occurs (Bluetooth unavailable, permission denied)
- **THEN** the connection state stream emits `ERROR`

### Requirement: Connected device name observation
The system SHALL expose the name of the currently connected host device.

#### Scenario: No device connected
- **WHEN** connection state is not `CONNECTED`
- **THEN** the connected device name stream emits `null`

#### Scenario: Device connects
- **WHEN** a host connects and connection state becomes `CONNECTED`
- **THEN** the connected device name stream emits the host device name

### Requirement: Error message observation
The system SHALL expose human-readable error messages for UI display.

#### Scenario: No error
- **WHEN** no error condition exists
- **THEN** the error message stream emits `null`

#### Scenario: Bluetooth unavailable
- **WHEN** `initialize()` is called but Bluetooth is not available on the device
- **THEN** the error message stream emits "Bluetooth not available on this device"

#### Scenario: Bluetooth disabled
- **WHEN** `initialize()` is called but Bluetooth is disabled
- **THEN** the error message stream emits "Bluetooth is not enabled"

### Requirement: Initialization and shutdown
The system SHALL provide explicit initialization and shutdown control.

#### Scenario: Successful initialization
- **WHEN** `initialize()` is called with Bluetooth available and enabled
- **THEN** the HID Device profile proxy is acquired and registration begins

#### Scenario: Initialization fails when Bluetooth unavailable
- **WHEN** `initialize()` is called but Bluetooth is unavailable
- **THEN** connection state emits `ERROR` and error message is set

#### Scenario: Clean shutdown
- **WHEN** `shutdown()` is called
- **THEN** the HID Device app is unregistered, profile proxy released, and connection state emits `DISCONNECTED`

### Requirement: Mode configuration
The system SHALL support configuring the HID mode before initialization.

#### Scenario: Set mode before initialization
- **WHEN** `setMode(Mode.GAMEPAD)` is called before `initialize()`
- **THEN** the HID Device registers with gamepad SDP configuration

#### Scenario: Set mode after initialization
- **WHEN** `setMode(Mode.KEYBOARD)` is called after `initialize()`
- **THEN** the mode change takes effect on next re-registration

### Requirement: Target device name configuration
The system SHALL allow setting the Bluetooth adapter name for host recognition.

#### Scenario: Set target device name
- **WHEN** `setTargetDeviceName("MacGamepad")` is called
- **THEN** the Bluetooth adapter is renamed to the target name before registration

### Requirement: Bonded device access
The system SHALL provide a list of currently bonded (paired) Bluetooth devices.

#### Scenario: Get bonded devices
- **WHEN** `getBondedDevices()` is called
- **THEN** returns a list of paired Bluetooth devices suitable for manual connection
- **NOTE**: This is a synchronous call; UI observes bonded devices via `GamepadViewModel._pairedDevices` StateFlow which snapshots the list on service binding

### Requirement: Manual device connection
The system SHALL support initiating connection to a specific bonded device.

#### Scenario: Connect to paired device
- **WHEN** `connectToDevice(device)` is called with a bonded device
- **THEN** initiates connection to that device and connection state transitions accordingly
- **THEN** returns `true` if the connection command was successfully sent, `false` if HID Device proxy is unavailable

### Requirement: Gamepad report transmission
The system SHALL transmit packed gamepad input reports at high frequency.

#### Scenario: Send gamepad report
- **WHEN** `sendGamepadReport(state)` is called with valid `InputState`
- **THEN** the report is formatted per HID descriptor and sent via `BluetoothHidDevice.sendReport()`
- **THEN** returns `true` if the command was successfully sent

#### Scenario: Send report when not connected
- **WHEN** `sendGamepadReport(state)` is called but connection state is not `CONNECTED`
- **THEN** returns `false` without attempting transmission

### Requirement: Raw report transmission (keyboard/mouse)
The system SHALL transmit raw HID reports for keyboard and mouse modes.

#### Scenario: Send raw report
- **WHEN** `sendReport(reportId, data)` is called with valid report ID and byte array
- **THEN** the report is sent via `BluetoothHidDevice.sendReport()`
- **THEN** returns `true` if the command was successfully sent

### Requirement: Notification channel management
The system SHALL manage the foreground service notification channel for connection persistence.

#### Scenario: Recreate notification channel
- **WHEN** `recreateNotificationChannel()` is called
- **THEN** the notification channel is created with appropriate importance for foreground service

#### Scenario: Remove notification channel
- **WHEN** `removeNotificationChannel()` is called
- **THEN** the notification channel is deleted