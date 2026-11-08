## MODIFIED Requirements

### Requirement: Minimal foreground service type declaration

The system SHALL declare only the foreground service types strictly required for Bluetooth HID device functionality.

#### Scenario: Only connectedDevice type declared
- **WHEN** examining the `GamepadService` manifest declaration
- **THEN** `android:foregroundServiceType` SHALL be exactly `"connectedDevice"`
- **AND** `dataSync` SHALL NOT be present
- **AND** no other foreground service types SHALL be declared

#### Scenario: Runtime foreground service type matches manifest
- **WHEN** `GamepadService.startForeground()` is called
- **THEN** the foreground service type passed SHALL be `ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE`
- **AND** no other types SHALL be included in the bitmask