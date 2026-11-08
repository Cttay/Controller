## MODIFIED Requirements

### Requirement: Composable content handles system bar insets
The system SHALL ensure all screen composables apply appropriate padding for system bar insets to prevent content overlap.

**CHANGED**: `ConnectionScreen` now receives `HidDeviceManager` interface for connection state and bonded devices instead of concrete `BluetoothGamepadService`. The screen observes interface-typed `StateFlow<ConnectionState>` and `List<BluetoothDevice>`.

#### Scenario: ConnectionScreen shows bonded devices
- **WHEN** `ConnectionScreen` is displayed with `HidDeviceManager` providing bonded devices
- **THEN** "Previously Paired Macs" section shows list from `HidDeviceManager.getBondedDevices()`
- **THEN** tapping a device calls `HidDeviceManager.connectToDevice(device)`

#### Scenario: ConnectionScreen shows connection state
- **WHEN** `HidDeviceManager.connectionState` emits `REGISTERED_WAITING`
- **THEN** `ConnectionScreen` shows "Discoverable — waiting for Mac" state
- **THEN** user can proceed to gamepad screen on host connection

#### Scenario: Gamepad screen controls accessible
- **WHEN** GamepadScreen displays virtual controller
- **THEN** controller buttons/sticks SHALL not overlap with navigation bar
- **AND** bottom controls SHALL have navigation bar padding