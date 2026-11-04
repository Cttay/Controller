## Purpose

Provides permanent denial detection and Settings deep-link recovery UX, allowing users who selected "Don't ask again" to manually grant permissions via Android Settings and resume the permission flow.

## ADDED Requirements

### Requirement: Permanent denial detection
The system SHALL detect when a user has permanently denied a Bluetooth permission (selected "Don't ask again") by checking `shouldShowRequestPermissionRationale == false` while permission remains denied.

#### Scenario: Permanent denial detected after system dialog
- **WHEN** system permission dialog returns denial for a Bluetooth permission
- **THEN** `ActivityCompat.shouldShowRequestPermissionRationale` returns false for that permission
- **THEN** permission state transitions to `PermanentlyDenied`

#### Scenario: Permanent denial not confused with first-time denial
- **WHEN** user denies permission for first time (rationale still available)
- **THEN** `shouldShowRequestPermissionRationale` returns true
- **THEN** state transitions to `Denied(shouldShowRationale=true)`, NOT `PermanentlyDenied`

### Requirement: Settings recovery screen
The system SHALL display a recovery screen when state is `PermanentlyDenied`, with a clear message and a button to open app Settings.

#### Scenario: Recovery screen content
- **WHEN** permission state is `PermanentlyDenied`
- **THEN** screen shows: "Bluetooth permissions are required for MacGamepad to work"
- **THEN** screen shows: "You previously chose 'Don't ask again'. Please enable permissions in Settings."
- **THEN** screen shows "Open Settings" button

#### Scenario: Settings deep-link opens correct screen
- **WHEN** user taps "Open Settings" button
- **THEN** system launches `Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))`
- **THEN** Android Settings opens directly to app's permission page

### Requirement: Automatic recovery detection on return
The system SHALL automatically detect when user grants permissions in Settings and resume the flow.

#### Scenario: User grants all permissions in Settings and returns
- **WHEN** user opens Settings via recovery button, grants all three Bluetooth permissions, returns to app
- **THEN** `ON_RESUME` triggers permission re-check
- **THEN** state transitions from `PermanentlyDenied` to `Granted`
- **THEN** UI proceeds to discoverability screen

#### Scenario: User grants partial permissions in Settings and returns
- **WHEN** user opens Settings via recovery button, grants only a subset of Bluetooth permissions, returns to app
- **THEN** `ON_RESUME` triggers permission re-check
- **THEN** state transitions to `Denied(shouldShowRationale=false, missingPermissions=[...])` — permanently denied for ungranted permissions
- **THEN** UI shows recovery screen for still-denied permissions (rationale not available, Settings only path)

#### Scenario: User returns without granting
- **WHEN** user opens Settings via recovery button, does NOT grant permissions, returns to app
- **THEN** `ON_RESUME` triggers permission re-check
- **THEN** state remains `PermanentlyDenied`
- **THEN** recovery screen remains visible