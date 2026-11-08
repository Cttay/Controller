## MODIFIED Requirements

### Requirement: Mouse report functional via unified descriptor

**FROM (Broken):**
- Mouse collection exists in `KeyboardHidDescriptor` but never registered
- `sendMouseReportDirect()` calls `sendReport(MOUSE_REPORT_ID, ...)` but Android HID Device profile only registered Gamepad collection
- Mac receives no mouse input

**TO (Functional):**
- Mouse collection (Report ID 3) registered as part of unified descriptor
- `sendMouseReportDirect()` works correctly on existing HID Device registration

The system SHALL send functional Mouse reports (Report ID 3) via the unified HID Device registration.

#### Scenario: Mouse cursor moves in macOS
- **WHEN** in Mouse mode and user moves virtual right stick
- **THEN** a Mouse report (Report ID 3) SHALL be sent via `sendReport(3, ...)`
- **AND** macOS SHALL receive and process the mouse movement
- **AND** cursor SHALL move on screen

#### Scenario: Mouse buttons work
- **WHEN** in Mouse mode and user presses virtual face button mapped to mouse click
- **THEN** a Mouse report (Report ID 3) SHALL be sent with button bits set
- **AND** macOS SHALL register click events

#### Scenario: Mouse report format matches descriptor
- **WHEN** `sendMouseReportDirect()` is called
- **THEN** report SHALL be 4 bytes: Buttons (1) + Relative X (1) + Relative Y (1) + Wheel (1)
- **AND** X/Y SHALL be signed 8-bit relative movement (-127 to 127)
- **AND** Buttons: Bit 0=Left, Bit 1=Right, Bit 2=Middle

### Requirement: Mouse mode switch preserves connection

The system SHALL switch to Mouse mode without Bluetooth reconnection.

#### Scenario: Mode switch to Mouse
- **WHEN** user selects "Mouse" Output Protocol
- **THEN** `GamepadService._mode` SHALL update to MOUSE
- **AND** `hidManager` connection state SHALL remain CONNECTED
- **AND** subsequent input SHALL use Report ID 3