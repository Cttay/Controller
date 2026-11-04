## Why

The current MacGamepad app requests all Bluetooth permissions (`BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE`) and `POST_NOTIFICATIONS` at app launch, before the user has any context for why they're needed. This violates Android 12+ best practices for permission UX, leading to:

- **High denial rates**: Users deny permissions they don't understand
- **No rationale flow**: No custom explanation before system dialog
- **No permanent denial handling**: Users who select "Don't ask again" get stuck with no recovery path
- **No lifecycle awareness**: Permissions revoked in Settings while app is backgrounded aren't detected until next launch
- **Discoverability bundled**: Bluetooth discoverability request fires immediately after permissions, confusing users

This change implements **just-in-time progressive disclosure** with proper rationale dialogs, lifecycle-aware re-checking, and graceful permanent denial recovery — aligning with Material Design 3 permission patterns and Android 14/15 requirements.

## What Changes

- **New progressive permission flow**: Request Bluetooth permissions only when user taps "Initialize Controller" (not on launch)
- **Rationale dialogs**: Custom education screens before each system permission prompt explaining *why* each permission is needed
- **Permission state machine**: Sealed class tracking `Granted`, `Denied`, `PermanentlyDenied`, `Unknown` states
- **Lifecycle-aware re-check**: `ON_RESUME` observer detects revoked permissions and updates UI
- **Permanent denial recovery**: Settings deep-link button when user selects "Don't ask again"
- **Separated discoverability step**: Dedicated screen after Bluetooth permissions granted, with skip option
- **POST_NOTIFICATIONS decoupled**: Moved to Settings screen as optional "Enable Alerts" flow
- **Foreground service ordering**: Service starts only after all Bluetooth permissions + discoverability confirmed

## Capabilities

### New Capabilities
- `permission/bluetooth-progressive`: Progressive Bluetooth permission request flow with rationale dialogs, state machine, and lifecycle-aware re-checking
- `permission/discoverability`: Separate discoverability request screen with user education and skip option
- `permission/notification-opt-in`: Optional notification permission flow in Settings (not blocking core functionality)
- `permission/settings-recovery`: Permanent denial detection and Settings deep-link recovery UX

### Modified Capabilities
- `bluetooth/hid-device`: Requirement change — HID Device initialization now **must** be gated behind progressive permission flow completion (was: auto-initialized on launch)
- `app/lifecycle`: Requirement change — `MainActivity` and `GamepadViewModel` must observe permission state and react to revocation (was: no permission state observation)

## Impact

**Affected Files:**
- `android/app/src/main/java/com/macgamepad/controller/MainActivity.kt` — Remove launch-time permission check; add permission launcher + rationale dialog integration
- `android/app/src/main/java/com/macgamepad/controller/GamepadViewModel.kt` — Add permission state sealed class, lifecycle observer, and public API for permission flow
- `android/app/src/main/java/com/macgamepad/controller/ui/screens/ConnectionScreen.kt` (new) — Compose screen for permission rationale, discoverability, and settings recovery
- `android/app/src/main/java/com/macgamepad/controller/ui/screens/SettingsScreen.kt` — Add notification opt-in toggle
- `android/app/src/main/java/com/macgamepad/controller/bt/BluetoothGamepadService.kt` — Initialize only after explicit permission flow completion
- `android/app/src/main/AndroidManifest.xml` — No changes needed (permissions already declared correctly)

**Dependencies:** 
- `androidx.activity:activity-compose` (already present) for `rememberMultiplePermissionsState`
- `androidx.lifecycle:lifecycle-runtime-compose` for lifecycle observation

**Breaking Changes:** None — existing permission grants persist; flow only changes for new/denied users