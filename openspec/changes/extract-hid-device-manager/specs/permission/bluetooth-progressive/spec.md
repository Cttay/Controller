## MODIFIED Requirements

### Requirement: Progressive permission request on user action
The system SHALL request Bluetooth permissions (`BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE`) only when the user explicitly taps "Initialize Controller" or equivalent connect action, not on app launch.

**CHANGED**: The initialization flow now receives a `HidDeviceManager` interface instead of concrete `BluetoothGamepadService`. The `GamepadViewModel` coordinates permission grants with `HidDeviceManager.initialize()`.

#### Scenario: First-time user taps Initialize Controller
- **WHEN** user opens app for first time and taps "Initialize Controller"
- **THEN** system shows rationale dialog explaining why Bluetooth permissions are needed
- **THEN** user confirms rationale dialog
- **THEN** system launches system permission dialog for all three Bluetooth permissions
- **THEN** on grant, `GamepadViewModel` calls `HidDeviceManager.initialize()` to start HID registration

#### Scenario: Returning user with permissions already granted
- **WHEN** user has previously granted all Bluetooth permissions and taps "Initialize Controller"
- **THEN** system proceeds directly to discoverability step without showing rationale or system dialog
- **THEN** `GamepadViewModel` calls `HidDeviceManager.initialize()` to start HID registration

### Requirement: Permission state machine
The system SHALL track permission state using a sealed interface with four states: `Granted`, `Denied` (with rationale flag and missing permissions list), `PermanentlyDenied`, and `Unknown`.

**CHANGED**: On full grant, `GamepadViewModel` transitions to `Granted` AND invokes `HidDeviceManager.initialize()` to begin HID Device registration.

#### Scenario: State transitions on full grant
- **WHEN** user grants all three Bluetooth permissions via system dialog
- **THEN** state transitions from `Unknown` or `Denied` to `Granted`
- **THEN** `GamepadViewModel` calls `HidDeviceManager.initialize()` to start HID registration flow

#### Scenario: State transitions on partial grant
- **WHEN** user grants a subset of the three Bluetooth permissions (1 or 2 of 3)
- **THEN** state transitions to `Denied(shouldShowRationale=true, missingPermissions=[...])` listing only denied permissions
- **THEN** UI shows rationale screen for missing permissions only
- **THEN** subsequent request asks only for missing permissions

### Requirement: Lifecycle-aware permission re-check
The system SHALL re-check permission state on `ON_RESUME` lifecycle event and update UI if permissions were revoked in Settings while app was backgrounded.

**CHANGED**: On permission revocation, `GamepadViewModel` calls `HidDeviceManager.shutdown()` to clean up HID registration before showing permission screen.

#### Scenario: Permissions revoked in Settings
- **WHEN** user grants permissions, backgrounds app, revokes Bluetooth permissions in Settings, returns to app
- **THEN** `ON_RESUME` triggers permission re-check
- **THEN** state transitions from `Granted` to `Denied` or `PermanentlyDenied`
- **THEN** `GamepadViewModel` calls `HidDeviceManager.shutdown()` to unregister HID app
- **THEN** UI updates to show permission screen instead of gamepad/dashboard

#### Scenario: Permissions granted in Settings
- **WHEN** user denied permissions, backgrounds app, grants Bluetooth permissions in Settings, returns to app
- **THEN** `ON_RESUME` triggers permission re-check
- **THEN** state transitions to `Granted`
- **THEN** `GamepadViewModel` calls `HidDeviceManager.initialize()` to start HID registration