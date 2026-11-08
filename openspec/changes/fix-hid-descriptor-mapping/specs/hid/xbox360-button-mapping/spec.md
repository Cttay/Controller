## Purpose

Defines the Gamepad collection button layout and axis mapping to match the Xbox 360 Controller HID specification (Vendor ID 0x045E, Product ID 0x028E) so that macOS GameController.framework recognizes and maps buttons correctly without custom configuration.

## ADDED Requirements

### Requirement: Gamepad button layout matches Xbox 360 HID specification

The system SHALL use the Xbox 360 Controller HID report layout for the Gamepad collection buttons, ensuring native macOS GameController.framework recognition.

#### Scenario: Button order matches Xbox 360 layout
- **WHEN** the Gamepad report is sent (Report ID 1)
- **THEN** button bits SHALL follow Xbox 360 HID order:
  - Bit 0: A (Bottom face button)
  - Bit 1: B (Right face button)
  - Bit 2: X (Left face button)
  - Bit 3: Y (Top face button)
  - Bit 4: LB (Left Bumper)
  - Bit 5: RB (Right Bumper)
  - Bit 6: Back/View/Select
  - Bit 7: Start/Menu
  - Bit 8: L3 (Left Stick Click)
  - Bit 9: R3 (Right Stick Click)
  - Bit 10: Guide/Xbox/Home
  - Bit 11-15: Reserved (0)

#### Scenario: D-Pad uses Hat Switch only
- **WHEN** the Gamepad report is sent (Report ID 1)
- **THEN** D-Pad input SHALL be encoded ONLY in the Hat Switch (byte 8, 4-bit value 0-7 for directions, 0xF for centered)
- **AND** no button bits SHALL be used for D-Pad directions
- **AND** Hat Switch value mapping:
  - 0: Up
  - 1: Up-Right
  - 2: Right
  - 3: Down-Right
  - 4: Down
  - 5: Down-Left
  - 6: Left
  - 7: Up-Left
  - 0xF: Centered (neutral)

### Requirement: Triggers as analog axes only

The system SHALL encode LT (Left Trigger) and RT (Right Trigger) as analog axes (bytes 12-13, uint8 0-255) only, NOT as buttons.

#### Scenario: Triggers not in button bitmap
- **WHEN** the Gamepad report is sent (Report ID 1)
- **THEN** LT/RT SHALL NOT appear in the 16-bit button bitmap (bits 0-15)
- **AND** LT SHALL be byte 12 (Z axis, 0-255)
- **AND** RT SHALL be byte 13 (Rz axis, 0-255)

### Requirement: Stick axes as 16-bit signed integers

The system SHALL encode Left Stick (X, Y) and Right Stick (Rx, Ry) as 16-bit signed little-endian integers (-32767 to 32767).

#### Scenario: Stick values in correct byte positions
- **WHEN** the Gamepad report is sent (Report ID 1)
- **THEN** Byte 0-1: Left Stick X (int16 LE)
- **AND** Byte 2-3: Left Stick Y (int16 LE)
- **AND** Byte 4-5: Right Stick X (int16 LE)
- **AND** Byte 6-7: Right Stick Y (int16 LE)

### Requirement: Report size is 14 bytes (excluding Report ID)

The Gamepad collection SHALL produce exactly 14 bytes of report data (excluding the 1-byte Report ID prefix).

#### Scenario: Report size verification
- **WHEN** `HidReportPacker.pack()` is called with any InputState
- **THEN** the returned ByteArray SHALL have length 14
- **AND** the descriptor SHALL define GAMEPAD_REPORT_SIZE = 14