## Why

The Bluetooth HID Device logic is locked inside a concrete `BluetoothGamepadService` class (330 lines) with zero testability. `GamepadService`, `GamepadViewModel`, and UI screens all depend directly on this concrete class, blocking:
- Unit tests for the core phone→Mac gamepad path (zero automated tests exist — P2 concern)
- The God Service refactor (P1) — `GamepadService` cannot be split until BT dependency is inverted
- Mac bridge simulation in CI — no way to mock HID Device profile

Extracting a `HidDeviceManager` interface enables mocking, isolates platform IPC, and unblocks both testability and architectural refactors.

## What Changes

- **Extract enums**: `ConnectionState` + `Mode` from `BluetoothGamepadService` → shared `bt/ConnectionState.kt` + `bt/Mode.kt`
- **New interface**: `HidDeviceManager` in `bt/` with complete surface (connection state, device name, error message, bonded devices, mode switching, report sending, notification channel management)
- **Implementation**: `BluetoothGamepadService` implements `HidDeviceManager`
- **Dependency inversion**: `GamepadService` receives `HidDeviceManager` via constructor injection (removes direct `BluetoothGamepadService` instantiation)
- **UI layer update**: `GamepadViewModel`, `ConnectionScreen`, `ProfileScreen` depend on interface types
- **Testability**: Robolectric test with mocked `HidDeviceManager` verifying registration flow

**BREAKING**: Internal API only — `GamepadService` constructor signature changes, UI components receive interface instead of concrete class. No public API affected.

## Capabilities

### New Capabilities
- `bt/hid-device-manager`: Interface for Bluetooth HID Device profile management — connection lifecycle, report transmission, bonded device access, mode/profile configuration

### Modified Capabilities
- `permission/bluetooth-progressive`: `GamepadViewModel` initialization flow now receives `HidDeviceManager` instead of concrete `BluetoothGamepadService`
- `ui/edge-to-edge`: `ConnectionScreen` receives interface-typed connection state and bonded devices

## Impact

**Affected code**:
- `android/app/src/main/java/com/macgamepad/controller/bt/BluetoothGamepadService.kt` (implements new interface)
- `android/app/src/main/java/com/macgamepad/controller/bt/GamepadService.kt` (constructor injection, removes concrete dependency)
- `android/app/src/main/java/com/macgamepad/controller/GamepadViewModel.kt` (interface types)
- `android/app/src/main/java/com/macgamepad/controller/ui/screens/ConnectionScreen.kt` (interface params)
- `android/app/src/main/java/com/macgamepad/controller/ui/screens/ProfileScreen.kt` (interface params)
- New files: `bt/ConnectionState.kt`, `bt/Mode.kt`, `bt/HidDeviceManager.kt`

**Dependencies**: None new — uses existing coroutines, Bluetooth APIs (minSdk 28)

**Systems**: Enables Robolectric unit tests for HID registration; unblocks P1 God Service refactor