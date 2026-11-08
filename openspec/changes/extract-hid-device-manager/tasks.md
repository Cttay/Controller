## 1. Extract Shared Enums

- [ ] 1.1 Create `bt/ConnectionState.kt` with enum `ConnectionState { DISCONNECTED, REGISTERING, REGISTERED_WAITING, CONNECTING, CONNECTED, ERROR }` — verify: file exists, compiles
- [ ] 1.2 Create `bt/Mode.kt` with enum `Mode { GAMEPAD, KEYBOARD, MOUSE }` — verify: file exists, compiles
- [ ] 1.3 Update `BluetoothGamepadService` to import and use shared enums (remove nested declarations) — verify: `compileDebugKotlin` passes

## 2. Define HidDeviceManager Interface

- [ ] 2.1 Create `bt/HidDeviceManager.kt` with complete interface (connectionState, connectedDeviceName, errorMessage, initialize, shutdown, sendGamepadReport, sendReport, connectToDevice, getBondedDevices, setMode, setTargetDeviceName, recreateNotificationChannel, removeNotificationChannel) — verify: file exists, compiles
- [ ] 2.2 Add KDoc comments to each method matching spec requirements — verify: documentation renders in IDE

## 3. Implement Interface in BluetoothGamepadService

- [ ] 3.1 Make `BluetoothGamepadService` implement `HidDeviceManager` — verify: `compileDebugKotlin` passes, all interface methods implemented
- [ ] 3.2 Remove nested `ConnectionState` and `Mode` enums from `BluetoothGamepadService` — verify: no compilation errors
- [ ] 3.3 Ensure existing behavior preserved: registration flow, state transitions, report sending, notification channel management — verify: existing tests pass

## 4. Update GamepadService for Dependency Injection

- [ ] 4.1 Modify `GamepadService` constructor to accept `HidDeviceManager` parameter — verify: compiles
- [ ] 4.2 Remove direct instantiation `btService = BluetoothGamepadService(applicationContext)` — verify: no references to concrete `BluetoothGamepadService` in `GamepadService`
- [ ] 4.3 Update all `btService` method calls to use interface methods — verify: `compileDebugKotlin` passes
- [ ] 4.4 Update `GamepadService` to call `hidManager.setMode()` before `initialize()` — verify: mode configuration works

## 5. Update MainActivity for Composition Root

- [ ] 5.1 In `MainActivity.onServiceConnected`, create `BluetoothGamepadService` and pass as `HidDeviceManager` to `GamepadService` — verify: app launches, service binds correctly
- [ ] 5.2 Ensure `GamepadViewModel` receives `HidDeviceManager` via `GamepadService` — verify: no compilation errors

## 6. Update GamepadViewModel

- [ ] 6.1 Change `GamepadViewModel` to use `HidDeviceManager` interface types instead of concrete `BluetoothGamepadService` — verify: imports updated, compiles
- [ ] 6.2 On permission grant: call `hidManager.initialize()` instead of `service.startSession()` — verify: HID registration starts
- [ ] 6.3 On permission revocation: call `hidManager.shutdown()` — verify: HID app unregistered
- [ ] 6.4 On mode/profile change: call `hidManager.setMode()`, `hidManager.setTargetDeviceName()`, `hidManager.recreateNotificationChannel()` — verify: mode switching works

## 7. Update UI Screens

- [ ] 7.1 Update `ConnectionScreen` params: `connectionState: StateFlow<ConnectionState>`, `pairedDevices: List<BluetoothDevice>`, `onDeviceConnectClick: (BluetoothDevice) -> Unit` from `HidDeviceManager` — verify: compiles, shows bonded devices
- [ ] 7.2 Update `ConnectionScreen` to call `hidManager.connectToDevice(device)` on device tap — verify: manual connection works
- [ ] 7.3 Update `ProfileScreen` to use interface types — verify: compiles
- [ ] 7.4 Verify `DisconnectedState` shows "Previously Paired Macs" from `hidManager.getBondedDevices()` — verify: UI displays correctly

## 8. Add Robolectric Test

- [ ] 8.1 Create `HidDeviceManagerFake` implementing interface with realistic state transitions (REGISTERING → REGISTERED_WAITING → CONNECTED) — verify: fake compiles
- [ ] 8.2 Add Robolectric test: `HidDeviceManagerRegistrationTest` verifying `initialize()` emits correct state sequence — verify: test passes
- [ ] 8.3 Add test for `shutdown()` emitting `DISCONNECTED` — verify: test passes
- [ ] 8.4 Add test for `sendGamepadReport()` returning `false` when not connected — verify: test passes
- [ ] 8.5 Ensure test runs in `test` source set (JVM, no device) — verify: `./gradlew test` passes

## 9. Verification & Cleanup

- [ ] 9.1 Run `./gradlew compileDebugKotlin` — verify: BUILD SUCCESSFUL
- [ ] 9.2 Run `./gradlew test` — verify: all tests pass (including new Robolectric tests)
- [ ] 9.3 Run `./gradlew lint` — verify: no new warnings
- [ ] 9.4 Manual smoke test: fresh install → Initialize Controller → permissions → discoverable → connect Mac → gamepad screen — verify: end-to-end flow works
- [ ] 9.5 Verify `GamepadService` has zero references to `BluetoothGamepadService` concrete class — verify: grep returns empty