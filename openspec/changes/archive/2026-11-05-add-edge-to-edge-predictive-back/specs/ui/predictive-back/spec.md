## Purpose

Enables predictive back gesture animations on Android 14+ (API 34), allowing users to preview the destination screen during a back swipe gesture before committing to the navigation.

## ADDED Requirements

### Requirement: Predictive back gestures enabled

The system SHALL enable predictive back animations so that back swipe gestures show a preview of the previous screen.

#### Scenario: Predictive back works on API 34+
- **WHEN** user performs back swipe gesture on Android 14 (API 34) or higher
- **THEN** system SHALL show animated preview of previous screen during gesture
- **AND** gesture completion SHALL navigate back
- **AND** gesture cancellation SHALL return to current screen smoothly

#### Scenario: Graceful fallback on pre-API 34
- **WHEN** user performs back gesture on Android 13 (API 33) or lower
- **THEN** standard back navigation SHALL occur without predictive animation
- **AND** no crashes or errors SHALL occur

### Requirement: Back handlers compatible with predictive back

The system SHALL ensure all custom back handlers (e.g., exiting edit mode, dismissing dialogs) work correctly with predictive back.

#### Scenario: Exit layout edit mode on back gesture
- **WHEN** user is in layout edit mode and performs back gesture
- **THEN** predictive animation SHALL show Dashboard screen
- **AND** completing gesture SHALL exit edit mode and navigate to Dashboard

#### Scenario: Dismiss permission dialog on back gesture
- **WHEN** permission dialog is showing and user performs back gesture
- **THEN** dialog SHALL dismiss with predictive animation
- **AND** completing gesture SHALL return to previous screen