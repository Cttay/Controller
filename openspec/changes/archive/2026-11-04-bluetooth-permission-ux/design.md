## Context

See proposal.md - Why. Current code requests all Bluetooth permissions at launch in `MainActivity.checkPermissionsAndConnect()` (line 323), with no rationale dialog, no lifecycle re-check, no permanent denial recovery, and discoverability bundled immediately after. The `GamepadViewModel` has no permission state awareness.

Key existing files:
- `MainActivity.kt` — Permission launcher, `checkPermissionsAndConnect()`, `onPermissionsGranted()`
- `GamepadViewModel.kt` — Service connection, `initializeBluetooth()`, `connectToDevice()`
- `BluetoothGamepadService.kt` — `initialize()` called from `GamepadService.startSession()`
- `AndroidManifest.xml` — All permissions correctly declared

## Goals / Non-Goals

**Goals:**
- Implement progressive JIT permission flow triggered by "Initialize Controller" button
- Add sealed-class permission state machine in `GamepadViewModel`
- Add lifecycle-aware re-check on `ON_RESUME`
- Add rationale dialogs (Compose) before system prompts
- Add permanent denial detection + Settings deep-link recovery
- Separate discoverability into dedicated screen with skip option
- Decouple `POST_NOTIFICATIONS` to Settings toggle
- Gate `BluetoothGamepadService.initialize()` behind completed permission flow

**Non-Goals:**
- Refactor `GamepadService` / `BluetoothGamepadService` architecture (God Service, P1)
- Add integration tests (P2)
- Change HID descriptor or report format
- Modify macOS bridge
- Add device allowlist/trust model (P6)

## Decisions

### 1. Permission State in GamepadViewModel (not MainActivity)
**Decision:** Move permission state management to `GamepadViewModel` as a `MutableStateFlow<PermissionStatus>`, observed by Compose screens.

**Rationale:** 
- ViewModel survives configuration changes; Activity recreation loses state
- Single source of truth for UI (ConnectionScreen, SettingsScreen)
- `MainActivity` only hosts launchers (`permissionLauncher`, `discoverableLauncher`)
- Consistent with existing pattern: `GamepadViewModel` already mirrors BT connection state

**Alternatives considered:**
- Keep in `MainActivity` — rejected: loses state on rotation, harder to share with SettingsScreen
- New `PermissionViewModel` — rejected: over-fragmentation; permission state is tightly coupled to BT initialization flow

### 2. Rationale Dialogs as Compose Screens (not AlertDialog)
**Decision:** Full-screen Compose screens for rationale/education, not `AlertDialog`.

**Rationale:**
- Material Design 3 recommends full-screen education for high-risk permissions (Bluetooth + location-adjacent)
- Allows richer content: icons, step-by-step explanation, "Learn more" links
- Consistent with app's Compose-only UI
- Dialogs feel dismissible/ignorable; screens convey importance

**Alternatives considered:**
- `AlertDialog` — rejected: too lightweight for Bluetooth permissions; users dismiss without reading
- `BottomSheet` — rejected: same issue; doesn't command attention for critical permissions

### 3. Single Combined Bluetooth Permission Request with Partial-Grant Handling
**Decision:** Request all three Bluetooth permissions (`CONNECT`, `SCAN`, `ADVERTISE`) in one `RequestMultiplePermissions` call, but handle partial grants gracefully.

**Rationale:**
- All three are required for HID Device profile; partial grant breaks functionality
- Single system dialog reduces user fatigue
- Android 12+ groups them conceptually (all "Nearby devices" permission group)
- **Partial grants are possible** — system dialog can return mixed results. Handle by re-requesting only missing permissions.

**Alternatives considered:**
- Progressive individual requests — rejected: three dialogs worse UX; all-or-nothing for HID
- Request SCAN/ADVERTISE first, CONNECT later — rejected: CONNECT required for `getProfileProxy(HID_DEVICE)`

**Partial-Grant Handling:**
- After `RequestMultiplePermissions` callback, check each permission result individually
- If any denied: filter to only denied permissions, call `shouldShowRequestPermissionRationale` for each
- If all denied have rationale → `Denied(shouldShowRationale=true, missingPermissions=...)` → show rationale for missing only
- If any denied has no rationale → `PermanentlyDenied` for those → recovery screen for those permissions
- Re-request only missing permissions (not already-granted ones)

### 4. Discoverability as Separate Screen After Permissions
**Decision:** Dedicated `DiscoverabilityScreen` shown after `PermissionStatus.Granted`, before `BluetoothGamepadService.initialize()`.

**Rationale:**
- Spec requires separation: discoverability is a user action, not a permission
- Skip option supports manual pairing workflow (Mac initiates pairing)
- 300-second duration matches current behavior

**Alternatives considered:**
- Auto-trigger discoverability after grant — rejected: spec requires explicit user consent
- Merge into permission rationale screen — rejected: different mental model (permission vs. action)

### 5. POST_NOTIFICATIONS in SettingsScreen Toggle
**Decision:** `ConnectionAlerts` toggle in `SettingsScreen` requests `POST_NOTIFICATIONS` when enabled.

**Rationale:**
- Notifications are optional enhancement, not required for core gamepad function
- Settings is natural place for "alerts" preference
- Persisted preference survives app restart

**Alternatives considered:**
- Request on first connection — rejected: surprising; user not in Settings mindset
- Request after discoverability — rejected: conflates two unrelated permissions

### 6. Lifecycle Re-check via LifecycleEventObserver in ViewModel
**Decision:** `GamepadViewModel` registers `LifecycleEventObserver` on `ON_RESUME` to re-check permissions.

**Rationale:**
- ViewModel has `DefaultLifecycleObserver` capability via `lifecycleScope`
- Centralizes permission logic; Activity stays thin
- Works across configuration changes

**Alternatives considered:**
- `MainActivity.onResume()` callback to ViewModel — rejected: duplicates lifecycle handling
- `DisposableEffect(Lifecycle.Event.ON_RESUME)` in Compose — rejected: recomposition-dependent; ViewModel is more reliable

### 7. Permanent Denial Detection
**Decision:** Detect via `shouldShowRequestPermissionRationale == false` + permission denied after system dialog returns.

**Rationale:**
- Standard Android pattern; no heuristic needed
- Works across API levels

**Edge case:** First-time denial also returns `false` on some devices. Mitigation: track `hasEverRequested` in `SharedPreferences` to distinguish.

### 8. Settings Deep-Link Intent
**Decision:** `Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))`

**Rationale:**
- Opens directly to app's permission page (not generic Settings home)
- Works on all Android versions with runtime permissions

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| **State loss on process death** | ViewModel survives config changes but not process death. Permission state re-derived from actual system grants on `ON_RESUME` — source of truth is always `ContextCompat.checkSelfPermission()` |
| **Race: user grants in Settings, returns, ON_RESUME fires before ViewModel re-created** | `GamepadViewModel` re-checks in `init { }` block on creation + `ON_RESUME` observer — covers both cold start and resume |
| **Android 14+ foreground service type enforcement** | `FOREGROUND_SERVICE_CONNECTED_DEVICE` already declared; service only starts after permissions granted |
| **Discoverability skip → manual pairing confusion** | ConnectionScreen shows paired device list + "Pair from Mac" instruction; user can initiate from macOS Bluetooth settings |
| **POST_NOTIFICATIONS denied → foreground service shows no notification** | Service starts with minimal notification channel; connection alerts only appear if permission granted. Core BT unaffected. |
| **Rationale screen back navigation** | Disable back gesture on rationale screens (user must explicitly choose Continue/Cancel) to prevent accidental dismissal |
| **Multiple permission launcher instances** | Single `ActivityResultLauncher<String[]>` in `MainActivity` passed to ViewModel via callback; ViewModel triggers via `permissionLauncher.launch(array)` |
| **Partial grant handling** | System dialog can return mixed grant/deny results. ViewModel must check each permission individually, re-request only missing ones. If any missing permission has no rationale → recovery screen for that permission. |
| **Discoverability timeout handling** | `discoverableLauncher` returns `RESULT_CANCELED` for both user cancellation AND 300s timeout. Treat both as "skip" — proceed to HID init. Log distinction for analytics. |
| **Concurrent permission requests** | User may tap "Initialize" rapidly. Add `isRequestingPermissions` guard flag in ViewModel to prevent duplicate launcher launches. |
| **Settings deep-link on Android 11-** | `ACTION_APPLICATION_DETAILS_SETTINGS` works but legacy permissions (API 30) don't have runtime revocation. Low impact — runtime flow only runs on API 31+. |
| **ViewModel init + ON_RESUME double-check** | Both run on cold start → double permission check. Harmless but wasteful. Add `hasDoneInitialCheck` flag to skip duplicate. |

## Migration Plan

1. **Phase 1 (Core):** Add `PermissionStatus` sealed interface + `permissionState` StateFlow to `GamepadViewModel`; add `ON_RESUME` re-check; add `requestBluetoothPermissions()` + `openSettings()` methods
2. **Phase 2 (UI):** Create `PermissionRationaleScreen`, `DiscoverabilityScreen`, `PermissionRecoveryScreen` in `ui/screens/`; wire into `ConnectionScreen` flow
3. **Phase 3 (Integration):** Modify `MainActivity` to remove launch-time `checkPermissionsAndConnect()`; add `permissionLauncher` + `discoverableLauncher` callbacks to ViewModel; gate `initializeBluetooth()` behind permission flow completion
4. **Phase 4 (Settings):** Add `ConnectionAlerts` toggle to `SettingsScreen`; wire `POST_NOTIFICATIONS` request
5. **Phase 5 (Cleanup):** Remove legacy `checkPermissionsAndConnect()`, `onPermissionsGranted()` from `MainActivity`; verify fresh install flow

**Rollback:** Revert `MainActivity` + `GamepadViewModel` changes; old launch-time flow restored. No data migration needed.

## Open Questions

1. **Should rationale screen explain all three permissions individually or as a group?**
   - Current spec: grouped rationale. Decision: group as "Bluetooth access" with bullet points for each — simpler, less overwhelming.

2. **Discoverability duration: keep 300s or make configurable?**
   - Current: 300s hardcoded. Decision: keep 300s; not user-facing setting.

3. **Analytics/events for permission funnel?**
   - Not in scope. Can add later via existing analytics if needed.

4. **Test on Android 11 (API 30) where legacy BLUETOOTH/BLUETOOTH_ADMIN apply?**
   - Legacy permissions are install-time (pre-12). Runtime flow only runs on API 31+. `MainActivity` already handles version check. No additional work needed.