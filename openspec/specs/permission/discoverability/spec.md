# permission/discoverability Specification

## Purpose
Provides a dedicated discoverability request screen that appears after Bluetooth permissions are granted, educating the user on why discoverability is needed and offering a skip option for users who prefer manual pairing.

## Requirements

### Requirement: Separate discoverability screen after permissions
The system SHALL display a dedicated discoverability screen only after all Bluetooth permissions are granted, separate from the permission request flow.

#### Scenario: Discoverability screen shown after permission grant
- **WHEN** Bluetooth permission state transitions to `Granted`
- **THEN** UI navigates to discoverability screen (not directly to HID initialization)

#### Scenario: Discoverability screen not shown if permissions denied
- **WHEN** Bluetooth permission state is `Denied` or `PermanentlyDenied`
- **THEN** UI shows permission screen, not discoverability screen

### Requirement: Discoverability education content
The system SHALL display clear messaging explaining what discoverability does, how long it lasts, and why it helps the Mac find the phone.

#### Scenario: Discoverability screen content visible
- **WHEN** discoverability screen is displayed
- **THEN** screen shows: "Make your phone discoverable to your Mac"
- **THEN** screen shows: "Your phone will be visible for 5 minutes"
- **THEN** screen shows: "This helps your Mac find and pair with MacGamepad"

### Requirement: Allow discoverability with system dialog
The system SHALL launch the system Bluetooth discoverability dialog (`ACTION_REQUEST_DISCOVERABLE`) when user taps "Allow Discoverability", with a duration of 300 seconds (5 minutes).

#### Scenario: User allows discoverability
- **WHEN** user taps "Allow Discoverability" button
- **THEN** system launches `Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)` with `EXTRA_DISCOVERABLE_DURATION=300`
- **THEN** after system dialog completes with `RESULT_OK`, UI proceeds to HID initialization

#### Scenario: User skips discoverability via button
- **WHEN** user taps "Skip for now" button
- **THEN** system does NOT launch discoverability dialog
- **THEN** UI proceeds directly to HID initialization (user can pair manually from Mac)

#### Scenario: Discoverability dialog dismissed or timed out
- **WHEN** user allows discoverability but system dialog returns `RESULT_CANCELED` (back press, "Cancel", or timeout)
- **THEN** system treats as "skip" — does NOT retry discoverability
- **THEN** UI proceeds directly to HID initialization (user can pair manually from Mac)
- **THEN** system logs discoverability cancellation reason for diagnostics

### Requirement: Skip option for manual pairing workflow
The system SHALL allow users to skip discoverability and proceed to HID initialization, supporting manual pairing from the Mac's Bluetooth settings.

#### Scenario: Skip leads to connection screen
- **WHEN** user taps "Skip for now"
- **THEN** `BluetoothGamepadService.initialize()` is called
- **THEN** UI shows connection screen with paired device list
- **THEN** user can pair manually from Mac's Bluetooth settings
