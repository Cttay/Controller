## MODIFIED Requirements

### Requirement: Gamepad button layout matches Xbox 360 HID specification

**FROM (W3C "standard" layout):**
- Bit 0: A, Bit 1: B, Bit 2: X, Bit 3: Y
- Bit 4: LB, Bit 5: RB, Bit 6: LT, Bit 7: RT
- Bit 8: Back/Select, Bit 9: Start
- Bit 10: L3, Bit 11: R3, Bit 12: Guide/Home
- Bit 13: DPadUp, Bit 14: DPadDown, Bit 15: DPadLeft
- Bit 16: DPadRight (needed, not in 16-bit allocation)

**TO (Xbox 360 HID layout):**
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

The system SHALL use the Xbox 360 Controller HID report layout for the Gamepad collection buttons.

#### Scenario: Button order matches Xbox 360 layout
- **WHEN** the Gamepad report is sent (Report ID 1)
- **THEN** button bits SHALL follow Xbox 360 HID order as defined above
- **AND** macOS gamepad-tester.com SHALL show correct button labels for each press

### Requirement: D-Pad uses Hat Switch only

**FROM (Redundant: Hat Switch + Button bits):**
- Hat Switch (byte 8, 4-bit) for 8-direction D-Pad
- Button bits 13-16: DPadUp, DPadDown, DPadLeft, DPadRight (redundant)

**TO (Hat Switch only):**
- Hat Switch (byte 8, 4-bit) for 8-direction D-Pad
- No button bits used for D-Pad directions

The system SHALL encode D-Pad input ONLY in the Hat Switch.

#### Scenario: D-Pad uses Hat Switch only
- **WHEN** the Gamepad report is sent (Report ID 1)
- **THEN** D-Pad input SHALL be encoded ONLY in the Hat Switch (byte 8, 4-bit value 0-7 for directions, 0xF for centered)
- **AND** no button bits SHALL be used for D-Pad directions

### Requirement: Triggers as analog axes only

**FROM (Dual encoding):**
- LT/RT as buttons (bits 6, 7 in button bitmap)
- LT/RT as analog axes (bytes 12, 13)

**TO (Analog axes only):**
- LT/RT as analog axes only (bytes 12, 13, uint8 0-255)
- Not in button bitmap

The system SHALL encode LT (Left Trigger) and RT (Right Trigger) as analog axes only.

#### Scenario: Triggers not in button bitmap
- **WHEN** the Gamepad report is sent (Report ID 1)
- **THEN** LT/RT SHALL NOT appear in the 16-bit button bitmap (bits 0-15)
- **AND** LT SHALL be byte 12 (Z axis, 0-255)
- **AND** RT SHALL be byte 13 (Rz axis, 0-255)

### Requirement: Report size is 14 bytes (excluding Report ID)

The Gamepad collection SHALL produce exactly 14 bytes of report data.

#### Scenario: Report size verification
- **WHEN** `HidReportPacker.pack()` is called with any InputState
- **THEN** the returned ByteArray SHALL have length 14
- **AND** the descriptor SHALL define GAMEPAD_REPORT_SIZE = 14