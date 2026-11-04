# permission/notification-opt-in Specification

## Purpose
Provides an optional notification permission flow accessible from Settings, allowing users to enable foreground service notifications for connection status alerts without blocking core Bluetooth/gamepad functionality.

## Requirements

### Requirement: Notification permission requested in Settings, not on launch
The system SHALL request `POST_NOTIFICATIONS` permission only when user explicitly enables "Connection Alerts" toggle in Settings screen, not during initial app launch or Bluetooth permission flow.

#### Scenario: User enables Connection Alerts in Settings
- **WHEN** user navigates to Settings and toggles "Connection Alerts" ON
- **THEN** system launches system permission dialog for `POST_NOTIFICATIONS`
- **THEN** if granted, foreground service shows connection status notifications
- **THEN** if denied, toggle reverts to OFF and user can retry later

#### Scenario: User disables Connection Alerts in Settings
- **WHEN** user toggles "Connection Alerts" OFF
- **THEN** foreground service stops showing connection status notifications
- **THEN** no system permission dialog is shown

### Requirement: Core functionality works without notification permission
The system SHALL maintain full Bluetooth HID gamepad functionality regardless of `POST_NOTIFICATIONS` grant state.

#### Scenario: Gamepad works without notifications
- **WHEN** `POST_NOTIFICATIONS` is denied or not requested
- **THEN** Bluetooth HID registration succeeds
- **THEN** gamepad input transmission works
- **THEN** foreground service runs (without notification channel for connection alerts)

### Requirement: Notification permission state persisted
The system SHALL persist the user's "Connection Alerts" preference and restore it on app restart.

#### Scenario: Preference restored on restart
- **WHEN** user enabled "Connection Alerts", closes app, reopens app
- **THEN** toggle shows ON state
- **THEN** if permission granted, notifications resume automatically

#### Scenario: Pre-granted permission reflected in toggle
- **WHEN** `POST_NOTIFICATIONS` is already granted (Android 13+ auto-grant or user granted via Settings)
- **THEN** toggle shows ON state on first Settings open
- **THEN** no system dialog launched when toggle already reflects grant state
- **THEN** user can still toggle OFF to disable alerts
