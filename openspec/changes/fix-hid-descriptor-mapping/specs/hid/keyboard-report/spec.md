## MODIFIED Requirements

### Requirement: Keyboard report functional via unified descriptor

**FROM (Broken):**
- Keyboard collection exists in `KeyboardHidDescriptor` but never registered
- `sendKeyboardReport()` calls `sendReport(KEYBOARD_REPORT_ID, ...)` but Android HID Device profile only registered Gamepad collection
- Mac receives no keyboard input

**TO (Functional):**
- Keyboard collection (Report ID 2) registered as part of unified descriptor
- `sendKeyboardReport()` works correctly on existing HID Device registration

The system SHALL send functional Keyboard reports (Report ID 2) via the unified HID Device registration.

#### Scenario: Keyboard input works in macOS Notes.app
- **WHEN** in Keyboard mode and user presses virtual face button mapped to keyboard key
- **THEN** a Keyboard report (Report ID 2) SHALL be sent via `sendReport(2, ...)`
- **AND** macOS SHALL receive and process the keyboard input
- **AND** characters SHALL appear in focused text field (e.g., Notes.app)

#### Scenario: Keyboard report format matches descriptor
- **WHEN** `sendKeyboardReport()` is called
- **THEN** report SHALL be 8 bytes: Modifier (1) + Reserved (1) + Keycodes (6)
- **AND** keycodes SHALL use USB HID Keyboard/Keypad usage page (0x07) values
- **AND** up to 6 simultaneous keys SHALL be supported

### Requirement: Keyboard mode switch preserves connection

The system SHALL switch to Keyboard mode without Bluetooth reconnection.

#### Scenario: Mode switch to Keyboard
- **WHEN** user selects "Keyboard" Output Protocol
- **THEN** `GamepadService._mode` SHALL update to KEYBOARD
- **AND** `hidManager` connection state SHALL remain CONNECTED
- **AND** subsequent input SHALL use Report ID 2