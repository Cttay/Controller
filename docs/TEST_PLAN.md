# MacGamepad Physical Device Test Plan

**Device**: PJX110 (Android 16)  
**Test Script**: `./scripts/fresh-install-test.sh <serial> --wait-for-connect`  
**Log Capture**: `adb logcat -s BTGamepad:V GamepadService:V GamepadVM:V MainActivity:V`

---

## Test Matrix: 12 Scenarios

| # | Scenario | Android State | macOS State | Purpose |
|---|----------|---------------|-------------|---------|
| 1 | **Clean Fresh Install** | `pm clear`, BT off | No "MacGamepad" in BT list | Baseline: full permission → discoverability → HID flow |
| 2 | **Clean, BT Already On** | `pm clear`, BT **on** | No "MacGamepad" in BT list | Verify BT enable step is skipped correctly |
| 3 | **Reopen App (Cached Pairing)** | App installed, BT on, **previously paired** | **"MacGamepad" exists** (stale) | Test cache/state handling on app restart |
| 4 | **Reopen After Mac Forgot** | App installed, BT on, previously paired | **Mac forgot** "MacGamepad" | Test reconnect without re-pairing |
| 5 | **Discoverability Timeout** | Start discoverability, **wait 300s** | Don't connect | Verify auto-unregister + UI handling |
| 6 | **Mid-Game BT Off (Mac)** | Connected, in GamepadScreen | Turn macOS BT off | Test broadcast receiver → UI returns to Connection |
| 7 | **Profile Switch Mid-Session** | Connected, GAMEPAD mode | Connected | Test `setProfile()` → re-registration |
| 8 | **Permission Revoked** | Connected, BT on | Connected | Revoke BT permissions in Settings → test recovery |
| 9 | **Phone BT Off Mid-Session** | Connected, in GamepadScreen | Connected | Phone-side BT off triggers different broadcast path |
| 10 | **App Background/Foreground** | Connected, in GamepadScreen | Connected | Foreground service survival, state recovery |
| 11 | **Rapid Profile Switch Stress** | Connected, GAMEPAD mode | Connected | `stopSession`/`startSession` race handling |
| 12 | **Config Change Survival** | Connected, in GamepadScreen | Connected | Compose recomposition, ViewModel survival |

---

## Per-Test Log Capture

```bash
# Terminal 1: Continuous logcat (save to file)
adb -s <serial> logcat -s BTGamepad:V GamepadService:V GamepadVM:V MainActivity:V > test_<N>_<scenario>.log 2>&1

# Terminal 2: Run test script
./scripts/fresh-install-test.sh <serial> --wait-for-connect 2>&1 | tee test_<N>_output.log
```

**Key log markers to verify per scenario:**

| Marker | Expected in Scenario |
|--------|---------------------|
| `Discoverability granted (resultCode=300), proceeding to HID initialization` | 1, 2, 3, 4, 5, 6, 7, 9, 10, 11 |
| `Renamed BT adapter from '...' to 'MacGamepad'` | 1, 2, 3, 4, 5, 6, 7, 9, 10, 11 |
| `HID Device profile proxy obtained` | 1, 2, 3, 4, 5, 6, 7, 9, 10, 11 |
| `registerApp() succeeded — waiting for callback...` | 1, 2, 3, 4, 5, 6, 7, 9, 10, 11 |
| `HID Device app registered. Waiting for host connection...` | 1, 2, 3, 4, 5, 6, 7, 9, 10, 11 |
| `Starting high-frequency report loop...` | 1, 2, 3, 4, 6, 7, 9, 10, 11 |
| `Connected to host: <Mac hostname>` | 1, 2, 3, 4, 6, 7, 9, 10, 11 |
| `HID Device app unregistered` | 5 (timeout), 6 (Mac BT off), 9 (Phone BT off) |
| `Bluetooth turned off - forcing UI back to Connection screen` | 6, 9 |
| `Restarting discoverable session for profile` | 7, 11 |
| `Permissions re-granted, reinitializing...` | 8 |
| `Foreground service started` / `Foreground service stopped` | 10 |
| `SecurityException` / `FATAL EXCEPTION` | **Never** (any = bug) |

---

## Scenario-Specific Steps & Expected Results

### 1. Clean Fresh Install (Baseline)
```bash
# On phone: Settings → Apps → MacGamepad → Storage → Clear Data
# On phone: BT off
# On Mac: BT settings → Remove "MacGamepad" if exists → BT off/on
./scripts/fresh-install-test.sh <serial> --wait-for-connect
```
**Expected**: Full flow completes → Mac connects → GamepadScreen shows

---

### 2. Clean, BT Already On
```bash
# On phone: pm clear, then Settings → BT → ON
# On Mac: Remove "MacGamepad" → BT off/on
./scripts/fresh-install-test.sh <serial> --wait-for-connect
```
**Expected**: Skips `RequestingBluetoothEnable` → goes straight to `ShowingDiscoverabilityScreen`

---

### 3. Reopen App (Stale Pairing on Mac)
```bash
# Precondition: Completed test 1 successfully, Mac still has "MacGamepad" paired
# On phone: Force stop app, DO NOT clear data
adb shell am force-stop com.macgamepad.controller
adb shell am start -n com.macgamepad.controller/.MainActivity
# Manually tap "Initialize Controller" (script doesn't handle reopen)
```
**Expected**: 
- App shows ConnectionScreen (not DiscoverabilityScreen)
- "Previously Paired Macs" shows the Mac (filtered by HID-capable name)
- Tapping it → `connectToDevice()` → CONNECTED → GamepadScreen
- **No** discoverability dialog
- **If stale device fails**: Error shown → then "Initialize Controller" → fresh discoverability

---

### 4. Reopen After Mac Forgot
```bash
# Precondition: Test 3 completed
# On Mac: BT settings → Forget "MacGamepad" → BT off/on
# On phone: App still installed, data intact
adb shell am start -n com.macgamepad.controller/.MainActivity
# Tap "Initialize Controller" → allow permissions if needed → allow discoverability
```
**Expected**: 
- Shows DiscoverabilityScreen (not cached pairing)
- Mac sees "MacGamepad" → connect → GamepadScreen

---

### 5. Discoverability Timeout (300s)
```bash
# Start test 1, but DON'T connect on Mac
# Wait 5+ minutes (script: sleep 300)
# Observe phone UI
```
**Expected**:
- Logs: `HID Device app unregistered` after ~300s
- `ConnectionState`: `REGISTERED_WAITING` → `DISCONNECTED` (not `ERROR`)
- UI: Returns to ConnectionScreen with error "Discoverability timed out"
- Can retry by tapping "Initialize Controller" again

---

### 6. Mid-Game BT Off (Mac)
```bash
# Complete test 1 → Mac connected → GamepadScreen visible
# On Mac: Turn Bluetooth OFF (manual or `blueutil -p 0`)
# Observe phone within 3s
```
**Expected**:
- Logs: `Bluetooth turned off - forcing UI back to Connection screen`
- UI: Auto-navigates to ConnectionScreen with "Bluetooth was turned off" error
- Foreground service notification updates

---

### 7. Profile Switch Mid-Session
```bash
# Complete test 1 → GamepadScreen
# Tap profile button → switch to Xbox profile
# Observe reconnection
```
**Expected**:
- Logs: `Restarting discoverable session for profile: xbox`
- `stopSession()` → `startSession()` → re-registration
- Mac may briefly disconnect/reconnect
- New profile layout loads

---

### 8. Permission Revoked
```bash
# Complete test 1 → GamepadScreen
# On phone: Settings → Apps → MacGamepad → Permissions → Revoke "Nearby devices"
# Observe app (or use: adb shell am start -a android.settings.APPLICATION_DETAILS_SETTINGS -d package:com.macgamepad.controller)
```
**Expected**:
- App detects revocation (onResume lifecycle OR runtime callback)
- Shows PermissionRecoveryScreen or requests permissions again
- After re-granting → `Permissions re-granted, reinitializing...` → reconnects automatically

---

### 9. Phone BT Off Mid-Session
```bash
# Complete test 1 → Mac connected → GamepadScreen visible
# On phone: Settings → Bluetooth → OFF
# Observe phone within 3s
```
**Expected**:
- Logs: `Bluetooth turned off - forcing UI back to Connection screen` (from `MainActivity.onReceive` for `ACTION_STATE_CHANGED`)
- UI: Auto-navigates to ConnectionScreen with "Bluetooth was turned off" error
- Foreground service stops, notification removed
- Different broadcast path than Scenario 6 (phone-side vs Mac-side)

---

### 10. App Background/Foreground During Connection
```bash
# Complete test 1 → GamepadScreen
# Press Home button → wait 10s → reopen app (tap icon or recent apps)
```
**Expected**:
- Foreground service survives (`FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE`)
- GamepadScreen resumes, connection maintained
- No re-pairing, no discoverability dialog
- Logs: `Foreground service started` (if restarted) / no disconnect logs
- InputState preserved (VirtualTrigger positions, button states)

---

### 11. Rapid Profile Switch Stress
```bash
# Complete test 1 → GamepadScreen
# Tap profile button 5x rapidly: GAMEPAD → KEYBOARD → MOUSE → GAMEPAD → XBOX
# (Each tap triggers setProfile() → stopSession/startSession)
```
**Expected**:
- No crashes, no ANR
- Final profile (XBOX) active
- Mac reconnects cleanly (may see brief disconnects)
- Logs show 5x `Restarting discoverable session for profile: ...`
- No `SecurityException` from concurrent HID operations

---

### 12. Config Change Survival
```bash
# Complete test 1 → GamepadScreen
# Rotate device: Portrait → Landscape → Portrait
# (Or use: adb shell settings put system accelerometer_rotation 0; adb shell settings put system user_rotation 1)
```
**Expected**:
- InputState preserved (VirtualTrigger depth, button states)
- VirtualTrigger layout adapts to new orientation
- Connection intact (no reconnection)
- Compose recomposition works, ViewModel survives

---

## Test Execution Order (Recommended)

```
Day 1: 1 → 2 → 3 → 4  (Core flows)
Day 2: 5 → 6 → 9 → 10 (System-level edge cases)
Day 3: 7 → 11 → 12 → 8 (Profile/App lifecycle edge cases)
```

---

## Pass/Fail Criteria

| Scenario | Pass If |
|----------|---------|
| 1, 2 | Mac connects, GamepadScreen shows, no crashes |
| 3 | Reconnects without discoverability, no "Initialize" tap needed |
| 4 | Discovers need for discoverability, pairs fresh |
| 5 | Auto-unregisters at 300s, UI recovers gracefully, `ConnectionState` = `DISCONNECTED` |
| 6 | UI returns to ConnectionScreen within 3s of BT off |
| 7 | Profile switches, reconnects, layout updates |
| 8 | Detects revocation, re-prompts, reconnects after grant |
| 9 | Phone BT off → UI returns to ConnectionScreen within 3s, foreground service stops |
| 10 | Background/foreground → connection maintained, InputState preserved |
| 11 | 5 rapid profile switches → no crashes, final profile active |
| 12 | Rotation → InputState preserved, connection intact, no reconnection |

---

## Bug Report Template (if any fail)

```
**Scenario**: #N - <name>
**Device**: PJX110 Android 16 (<serial>)
**Mac**: macOS <version>
**Log File**: test_N_<scenario>.log
**Expected**: <what should happen>
**Actual**: <what happened>
**Key Logs**: <relevant lines from log file>
**UI State**: <screenshot or description>
```

---

## Quick Reference: Expected Log Sequence (Success)

```
✅ Discoverability granted (resultCode=300)
✅ registerApp() succeeded — waiting for callback...
✅ HID Device app registered. Waiting for host connection...
✅ Starting high-frequency report loop...
🔄 Connecting to: <Mac hostname>
✅ Connected to host: <Mac hostname>
```

**Any deviation = investigate**