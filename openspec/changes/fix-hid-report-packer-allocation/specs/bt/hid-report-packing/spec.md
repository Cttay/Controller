## MODIFIED Requirements

### Requirement: Zero-allocation HID report packing for hot path

The system SHALL pack input state into HID reports without allocating new byte arrays on each call, to eliminate GC pressure at 125 Hz transmission rate.

#### Scenario: Repeated pack calls return same array instance
- **WHEN** calling `pack(state)` multiple times on the same `HidReportPacker` instance
- **THEN** the returned `ByteArray` SHALL be the same object instance (reference equality)
- **AND** the content SHALL reflect the latest input state

#### Scenario: Pack produces correct report bytes
- **WHEN** packing any valid `InputState`
- **THEN** the 12-byte report SHALL match the W3C Gamepad HID descriptor layout:
  - Bytes 0-1: Left Stick X (int16 LE)
  - Bytes 2-3: Left Stick Y (int16 LE)
  - Bytes 4-5: Right Stick X (int16 LE)
  - Bytes 6-7: Right Stick Y (int16 LE)
  - Byte 8: Left Trigger (uint8, 0-255)
  - Byte 9: Right Trigger (uint8, 0-255)
  - Byte 10: Hat switch (low 4 bits) | Buttons 0-3 (high 4 bits)
  - Byte 11: Buttons 4-11 (8 bits)

#### Scenario: No allocation on hot path
- **WHEN** `pack()` is called in a loop at 125 Hz
- **THEN** zero `ByteArray` allocations SHALL occur after initial construction
- **AND** GC pressure from report packing SHALL be eliminated