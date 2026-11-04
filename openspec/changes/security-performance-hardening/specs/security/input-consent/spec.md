## Purpose

Requires explicit user consent before enabling keyboard/mouse mode, which can inject arbitrary keystrokes and mouse movements on the connected host.

## ADDED Requirements

### Requirement: Keyboard mode requires one-time consent
The system SHALL require explicit user confirmation before switching to KEYBOARD mode for the first time. The consent dialog MUST explain that this mode can type and click on the connected Mac.

#### Scenario: First switch to KEYBOARD mode shows consent dialog
- **WHEN** user selects KEYBOARD mode from the UI and no prior consent exists
- **THEN** a confirmation dialog appears explaining: "Keyboard mode sends keystrokes and mouse clicks to your Mac. Allow?"

#### Scenario: Consent persists across sessions
- **WHEN** user confirms the consent dialog
- **THEN** the preference is stored and KEYBOARD mode can be enabled without re-prompting

#### Scenario: Consent can be revoked in settings
- **WHEN** user visits Settings and revokes keyboard mode consent
- **THEN** next attempt to enable KEYBOARD mode shows the consent dialog again

### Requirement: Consent dialog is unspoofable
The system SHALL display the consent dialog as a system-level AlertDialog (not a custom composable) to prevent UI spoofing.

#### Scenario: Dialog shows app name and clear action
- **WHEN** consent dialog appears
- **THEN** dialog title includes "MacGamepad" and buttons are "Allow" / "Deny" (not generic "OK"/"Cancel")

### Requirement: KEYBOARD mode cannot activate without consent
The system SHALL block KEYBOARD mode activation if consent is not granted, keeping the current mode active.

#### Scenario: Deny keeps GAMEPAD mode
- **WHEN** user selects KEYBOARD mode but taps "Deny" on consent dialog
- **THEN** mode remains GAMEPAD and no HID keyboard reports are sent

#### Scenario: Programmatic setMode respects consent
- **WHEN** `GamepadService.setMode(KEYBOARD)` is called without consent
- **THEN** mode change is rejected, current mode preserved, error logged