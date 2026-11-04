## Purpose

Provides a progressive, just-in-time Bluetooth permission request flow with custom rationale dialogs, a sealed-class state machine, and lifecycle-aware re-checking to align with Android 12+ best practices and Material Design 3 permission patterns.

## ADDED Requirements

### Requirement: Progressive permission request on user action
The system SHALL request Bluetooth permissions (`BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE`) only when the user explicitly taps "Initialize Controller" or equivalent connect action, not on app launch.

#### Scenario: First-time user taps Initialize Controller
- **WHEN** user opens app for first time and taps "Initialize Controller"
- **THEN** system shows rationale dialog explaining why Bluetooth permissions are needed
- **THEN** user confirms rationale dialog
- **THEN** system launches system permission dialog for all three Bluetooth permissions

#### Scenario: Returning user with permissions already granted
- **WHEN** user has previously granted all Bluetooth permissions and taps "Initialize Controller"
- **THEN** system proceeds directly to discoverability step without showing rationale or system dialog

### Requirement: Custom rationale dialog before system prompt
The system SHALL display a custom rationale dialog explaining the purpose of each Bluetooth permission before launching the system permission dialog, and SHALL only launch the system dialog after user confirms the rationale.

#### Scenario: User denies permission after rationale
- **WHEN** rationale dialog is shown and user taps "Cancel" or dismisses
- **THEN** system does NOT launch system permission dialog
- **THEN** UI remains on permission screen with option to retry

#### Scenario: User confirms rationale
- **WHEN** rationale dialog is shown and user taps "Continue"
- **THEN** system launches `ActivityResultContracts.RequestMultiplePermissions` for all three Bluetooth permissions

### Requirement: Permission state machine
The system SHALL track permission state using a sealed interface with four states: `Granted`, `Denied` (with rationale flag and missing permissions list), `PermanentlyDenied`, and `Unknown`.

#### Scenario: State transitions on full grant
- **WHEN** user grants all three Bluetooth permissions via system dialog
- **THEN** state transitions from `Unknown` or `Denied` to `Granted`

#### Scenario: State transitions on partial grant
- **WHEN** user grants a subset of the three Bluetooth permissions (1 or 2 of 3)
- **THEN** state transitions to `Denied(shouldShowRationale=true, missingPermissions=[...])` listing only denied permissions
- **THEN** UI shows rationale screen for missing permissions only
- **THEN** subsequent request asks only for missing permissions

#### Scenario: State transitions on denial with rationale
- **WHEN** user denies at least one permission and `shouldShowRequestPermissionRationale` returns true for any denied permission
- **THEN** state transitions to `Denied(shouldShowRationale=true, missingPermissions=[...])` listing denied permissions with rationale available

#### Scenario: State transitions on permanent denial
- **WHEN** user denies at least one permission and `shouldShowRequestPermissionRationale` returns false for all denied permissions (user selected "Don't ask again" for all)
- **THEN** state transitions to `PermanentlyDenied`

#### Scenario: Mixed rationale state
- **WHEN** user denies permissions where some have `shouldShowRequestPermissionRationale=true` and others `false`
- **THEN** state transitions to `Denied(shouldShowRationale=true, missingPermissions=[...])` — rationale screen shown for permissions that still allow it; permanently denied permissions require Settings recovery

### Requirement: Lifecycle-aware permission re-check
The system SHALL re-check permission state on `ON_RESUME` lifecycle event and update UI if permissions were revoked in Settings while app was backgrounded.

#### Scenario: Permissions revoked in Settings
- **WHEN** user grants permissions, backgrounds app, revokes Bluetooth permissions in Settings, returns to app
- **THEN** `ON_RESUME` triggers permission re-check
- **THEN** state transitions from `Granted` to `Denied` or `PermanentlyDenied`
- **THEN** UI updates to show permission screen instead of gamepad/dashboard

#### Scenario: Permissions granted in Settings
- **WHEN** user denied permissions, backgrounds app, grants Bluetooth permissions in Settings, returns to app
- **THEN** `ON_RESUME` triggers permission re-check
- **THEN** state transitions to `Granted`
- **THEN** UI proceeds to discoverability step

### Requirement: Permanent denial recovery via Settings deep-link
The system SHALL detect permanent denial and provide a button that opens app-specific Settings screen for manual permission grant.

#### Scenario: User taps "Open Settings" from permanent denial screen
- **WHEN** state is `PermanentlyDenied` and user taps "Open Settings" button
- **THEN** system launches `Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)` for app package
- **THEN** user can manually enable permissions
- **THEN** on return, `ON_RESUME` re-check detects grant and transitions to `Granted`