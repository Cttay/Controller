## Purpose

Enforces Bluetooth HID encryption, removes global adapter rename side effects, and provides a foundation for device allowlisting to prevent unauthorized HID connections.

## ADDED Requirements

### Requirement: Encrypted HID traffic
The system SHALL enforce encryption for all Bluetooth HID reports sent from the Android device to the host. Keystrokes, mouse movements, and gamepad inputs MUST NOT be transmitted in cleartext over the air.

#### Scenario: Encryption negotiated on connection
- **WHEN** the Android device registers the HID Device profile and a host connects
- **THEN** the Bluetooth link uses encryption (AES-CCM or equivalent) for all HID report traffic

#### Scenario: Connection refused if encryption unavailable
- **WHEN** a host attempts to connect but encryption cannot be negotiated (e.g., legacy host without Secure Simple Pairing)
- **THEN** the connection SHALL be rejected and the user notified

### Requirement: No global Bluetooth adapter rename
The system SHALL NOT modify the system-wide Bluetooth adapter name when registering the HID Device profile. The phone's discoverable name MUST remain under user control.

#### Scenario: Adapter name unchanged after HID registration
- **WHEN** the app calls `BluetoothGamepadService.initialize()` and registers the HID app
- **THEN** `BluetoothAdapter.getName()` returns the original user-set name, not "Wireless Controller" or any app-defined name

#### Scenario: Adapter name unchanged after crash/force-stop
- **WHEN** the app crashes or is force-stopped while the HID service is active
- **THEN** the system Bluetooth adapter name is unchanged (no orphaned rename)

### Requirement: Mac discovers device via SDP record name
The system SHALL advertise the controller identity (e.g., "Wireless Controller") exclusively through the HID Device SDP record, not the system adapter name.

#### Scenario: Mac sees correct device name in Bluetooth settings
- **WHEN** a Mac scans for Bluetooth devices while the app is advertising
- **THEN** the discovered device shows the SDP record name ("Wireless Controller" or profile-specific name) and pairs correctly

### Requirement: Device allowlist foundation (future-proofing)
The system SHALL provide an internal API to restrict HID connections to a user-approved list of bonded devices.

#### Scenario: Allowlist blocks unknown device
- **WHEN** a bonded but unapproved device attempts to connect
- **THEN** the connection SHALL be rejected (foundation only; UI for allowlist management is out of scope)

## REMOVED Requirements

### Requirement: Global adapter rename for HID discovery
**Reason**: Changing the system Bluetooth adapter name is a global side effect that persists across crashes, affects other apps, and violates user expectations. The HID Device profile's SDP record provides the correct discovery mechanism.

**Migration**: Remove `bluetoothAdapter?.name = targetDeviceName` and the corresponding restore logic in `shutdown()`. The `BluetoothHidDeviceAppSdpSettings` name parameter (already implemented) handles Mac-side identification.