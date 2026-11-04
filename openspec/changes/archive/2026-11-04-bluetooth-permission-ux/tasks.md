## 1. Core Permission State Machine (GamepadViewModel)

- [x] 1.1 Add `PermissionStatus` sealed interface to `GamepadViewModel.kt` with:
  - `Granted`
  - `Denied(shouldShowRationale: Boolean, missingPermissions: List<String>)`
  - `PermanentlyDenied(missingPermissions: List<String>)`
  - `Unknown`
  — verify compiles and unit tests cover all variants
- [x] 1.2 Add `permissionState: MutableStateFlow<PermissionStatus>` initialized to `Unknown` — verify state observable in Compose
- [x] 1.3 Add `hasEverRequestedBluetoothPermissions: Boolean` persisted in `SharedPreferences` (key: `pref_bt_perm_requested`) — verify persists across app restart
- [x] 1.4 Implement `checkBluetoothPermissions(context: Context): PermissionStatus` helper that reads actual system grants and `shouldShowRequestPermissionRationale` for each Bluetooth permission — verify returns correct state for each scenario: full grant, partial grant (Denied with missing list), denied with rationale, permanently denied, unknown. Add unit tests.
- [x] 1.5 Add `ON_RESUME` lifecycle observer in `GamepadViewModel.init { }` that calls `checkBluetoothPermissions()` and updates `permissionState` — verify triggers on app resume after Settings permission change. Add `hasDoneInitialCheck` flag to avoid double-check on cold start.
- [x] 1.6 Add `requestBluetoothPermissions(missingPermissions: List<String> = bluetoothPermissions)` method that launches `permissionLauncher` (callback from MainActivity) and sets `hasEverRequestedBluetoothPermissions = true` — verify launches system dialog for correct permission subset
- [x] 1.7 Add `onBluetoothPermissionsResult(results: Map<String, Boolean>)` callback handler that updates `permissionState` based on each permission's result and `shouldShowRequestPermissionRationale` — verify transitions: full grant → Granted; partial grant with rationale → Denied(missing=...); partial grant without rationale → PermanentlyDenied(missing=...)
- [x] 1.8 Add `openAppSettings()` method that launches `ACTION_APPLICATION_DETAILS_SETTINGS` intent — verify opens correct Settings page
- [x] 1.9 Add `isRequestingPermissions: MutableStateFlow<Boolean>` guard flag to prevent duplicate launcher launches — verify rapid taps don't launch multiple dialogs

## 2. MainActivity Integration

- [x] 2.1 Remove `checkPermissionsAndConnect()` call from `onCreate()` AND `onStart()` — verify app launches without permission prompt and service not auto-bound
- [x] 2.2 Keep `permissionLauncher` and `discoverableLauncher` but wire callbacks to `GamepadViewModel` via interface:
  - `PermissionCallback.onBluetoothPermissionsResult(results: Map<String, Boolean>)`
  - `PermissionCallback.onDiscoverabilityResult(resultCode: Int)`
  — verify callbacks invoked
- [x] 2.3 Remove `onPermissionsGranted()` (discoverability now separate step) — verify no auto-discoverability on launch
- [x] 2.4 Implement `PermissionCallback` in `GamepadViewModel`; `MainActivity` calls `vm.setPermissionCallback(this)` — verify ViewModel can trigger system dialogs via callback, no direct launcher reference
- [x] 2.5 Update `MainActivity.onStart()` to only bind service if `permissionState == Granted` AND discoverability completed — verify service not bound prematurely

## 3. Permission Rationale Screen (Compose)

- [x] 3.1 Create `PermissionRationaleScreen.kt` in `ui/screens/` with title, grouped explanation for Bluetooth permissions (CONNECT, SCAN, ADVERTISE) with bullet points, "Continue" and "Cancel" buttons — verify renders correctly in `@Preview`
- [x] 3.2 "Continue" calls `GamepadViewModel.requestBluetoothPermissions(missingPermissions)` passing only permissions needing rationale — verify launches system dialog for correct subset
- [x] 3.3 "Cancel" / back gesture disabled (use `BackHandler(enabled = true) { }` to consume back) — verify user must explicitly choose Continue or Cancel
- [x] 3.4 **MERGED INTO 6.1** — ConnectionScreen refactor handles wiring

## 4. Permanent Denial Recovery Screen (Compose)

- [x] 4.1 Create `PermissionRecoveryScreen.kt` in `ui/screens/` with message, "Open Settings" button — verify renders correctly in `@Preview`
- [x] 4.2 "Open Settings" calls `GamepadViewModel.openAppSettings()` — verify opens app Settings page
- [x] 4.3 **MERGED INTO 6.1** — ConnectionScreen refactor handles wiring

## 5. Discoverability Screen (Compose)

- [x] 5.1 Create `DiscoverabilityScreen.kt` in `ui/screens/` with education content, "Allow Discoverability" and "Skip for now" buttons — verify renders correctly in `@Preview`
- [x] 5.2 "Allow Discoverability" calls `GamepadViewModel.requestDiscoverability()` → callback to Activity launches `discoverableLauncher` with 300s intent — verify system discoverability dialog appears
- [x] 5.3 "Skip for now" calls `GamepadViewModel.onDiscoverabilitySkipped()` → calls `initializeBluetooth()` — verify proceeds to HID init without discoverability
- [x] 5.4 **MERGED INTO 6.1** — ConnectionScreen refactor handles wiring

## 6. ConnectionScreen Flow Integration

- [x] 6.1 Refactor `ConnectionScreen.kt` to observe `GamepadViewModel.permissionState` and render appropriate child screen:
  - `Unknown` → show "Initialize Controller" button that calls `vm.requestBluetoothPermissions()` → shows `PermissionRationaleScreen`
  - `Denied(shouldShowRationale=true, missingPermissions)` → `PermissionRationaleScreen` with missing permissions context
  - `PermanentlyDenied(missingPermissions)` → `PermissionRecoveryScreen`
  - `Granted` → `DiscoverabilityScreen`
  - Add `BackHandler(enabled = true) { }` on rationale/recovery screens to prevent accidental back navigation
  — verify all state transitions render correctly in `@Preview` with mocked ViewModel
- [x] 6.2 "Initialize Controller" button (for `Unknown` state) triggers rationale flow — verify first-time flow
- [x] 6.3 After discoverability (allowed or skipped), `GamepadViewModel.initializeBluetooth()` called — verify HID registration starts
- [x] 6.4 Add `onDiscoverabilityResult(resultCode: Int)` handler in ViewModel: `RESULT_OK` or `RESULT_CANCELED` both call `initializeBluetooth()` — verify both paths work

## 7. SettingsScreen: Notification Opt-In

- [x] 7.1 Add `connectionAlertsEnabled: MutableStateFlow<Boolean>` to `GamepadViewModel`, persisted in `SharedPreferences` (key: `pref_connection_alerts`) — verify persists
- [x] 7.2 Add `requestNotificationPermission()` method that launches `ActivityResultContracts.RequestPermission()` for `POST_NOTIFICATIONS` — verify launches system dialog
- [x] 7.3 Add `onNotificationPermissionResult(granted: Boolean)` callback handler — verify updates preference
- [x] 7.4 Update `SettingsScreen.kt` to show "Connection Alerts" toggle bound to `connectionAlertsEnabled` — verify toggle reflects persisted state
- [x] 7.5 Toggle ON → calls `requestNotificationPermission()`; if denied, revert toggle to OFF — verify UX (use `onCheckedChange` not `checked` binding)
- [x] 7.6 Toggle OFF → no system dialog, just updates preference — verify
- [x] 7.7 On SettingsScreen compose, read actual `POST_NOTIFICATIONS` grant state and sync toggle on first composition — verify pre-granted permission reflected

## 8. BluetoothGamepadService Gate

- [x] 8.1 Ensure `GamepadService.startSession()` → `BluetoothGamepadService.initialize()` only called after:
  - `permissionState == Granted` AND
  - Discoverability step completed (allowed or skipped)
- [x] 8.2 Verify fresh install flow: launch → "Initialize Controller" → rationale → permissions → discoverability → HID init → "Discoverable — waiting for Mac"

## 9. Cleanup & Verification

- [x] 9.1 Remove unused `checkPermissionsAndConnect()`, `onPermissionsGranted()` from `MainActivity.kt` — verify no dead code
- [x] 9.2 Remove legacy `requiredPermissions` val from `MainActivity` if no longer used — verify
- [x] 9.3 Run fresh install test script: `./.slim/scripts/fresh-install-test.sh` — verify completes to "Discoverable — waiting for Mac"
- [x] 9.4 Test permission revocation: grant → background → revoke in Settings → resume → shows permission screen — verify lifecycle re-check works
- [x] 9.5 Test permanent denial: deny + "Don't ask again" → shows recovery screen → Settings → grant → resume → proceeds — verify recovery flow
- [x] 9.6 Test notification toggle: Settings → enable alerts → grant → notifications appear on connect/disconnect — verify opt-in works
- [x] 9.7 Run `./gradlew lint` and `./gradlew test` — verify no regressions