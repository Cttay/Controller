## Purpose

Registers a unified HID report descriptor with the Android Bluetooth HID Device profile that includes Gamepad (Report ID 1), Keyboard (Report ID 2), and Mouse (Report ID 3) collections in a single registration. This enables all three input modes to function over a single Bluetooth HID connection without re-registration.

## ADDED Requirements

### Requirement: Unified HID descriptor registration

The system SHALL register a single HID descriptor containing Gamepad, Keyboard, and Mouse collections with the Android Bluetooth HID Device profile during initialization.

#### Scenario: Successful unified descriptor registration
- **WHEN** the Bluetooth HID Device profile initializes
- **THEN** `registerApp()` SHALL be called with a descriptor containing three collections: Gamepad (Report ID 1), Keyboard (Report ID 2), Mouse (Report ID 3)
- **AND** the registration SHALL succeed (callback `onAppRegistered` invoked)
- **AND** all three report IDs SHALL be available for `sendReport()` calls

#### Scenario: Gamepad collection is first in descriptor
- **WHEN** the unified descriptor is registered
- **THEN** the Gamepad collection SHALL appear first (Report ID 1)
- **AND** macOS GameController.framework SHALL recognize the device as a native gamepad

### Requirement: Single registration serves all modes

The system SHALL use the same HID Device registration for all three output modes (Gamepad, Keyboard, Mouse) without requiring re-registration when switching modes.

#### Scenario: Mode switch uses different Report IDs on same registration
- **WHEN** user switches Output Protocol from Gamepad to Keyboard
- **THEN** the system SHALL send reports with Report ID 2 (Keyboard) on the existing HID Device registration
- **AND** no Bluetooth reconnection SHALL occur
- **AND** the connection state SHALL remain CONNECTED

#### Scenario: Keyboard input works after mode switch
- **WHEN** in Keyboard mode and user presses a key on the virtual controller
- **THEN** a Keyboard report (Report ID 2) SHALL be sent via `sendReport()`
- **AND** macOS SHALL receive and process the keyboard input (e.g., characters appear in Notes.app)

#### Scenario: Mouse input works after mode switch
- **WHEN** in Mouse mode and user moves the virtual right stick
- **THEN** a Mouse report (Report ID 3) SHALL be sent via `sendReport()`
- **AND** macOS SHALL receive and process the mouse movement (cursor moves)

### Requirement: SDP settings reflect primary gamepad function

The system SHALL configure SDP settings with Gamepad subclass (0x02) since Gamepad is the primary collection in the unified descriptor.

#### Scenario: SDP subclass is GAMEPAD
- **WHEN** registering the unified descriptor
- **THEN** `BluetoothHidDeviceAppSdpSettings` SHALL use `BluetoothHidDevice.SUBCLASS2_GAMEPAD` (0x02)
- **AND** the device SHALL be discoverable as a gamepad primarily