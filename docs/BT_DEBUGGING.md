# Bluetooth HID Debugging Playbook

This document captures the hard-won knowledge from debugging the MacGamepad Bluetooth HID registration flow.

---

## HID Registration Flow

```
User clicks "Initialize Controller"
       │
       ▼
Permissions granted (BLUETOOTH_CONNECT, SCAN, ADVERTISE)
       │
       ▼
GamepadViewModel.proceedAfterPermissionsGranted()
       │
       ▼
Shows ShowingDiscoverabilityScreen (NO service bind yet!)
       │
       ▼
User clicks "Allow Discovery" → System dialog
       │
       ▼
onDiscoverabilityResult(300) → bindGamepadService()  ◄── FIRST & ONLY bind
       │
       ▼
MainActivity.onServiceConnected()
       │
       ▼
Creates BluetoothGamepadService → injects into GamepadService
       │
       ▼
GamepadViewModel.initializeBluetooth()
       │
       ▼
GamepadService.startSession()
       │
       ▼
hidManager.initialize()  (BluetoothGamepadService)
       │
       ├─▶ bluetoothAdapter.name = "MacGamepad"  ◄── Adapter renamed HERE
       ├─▶ getProfileProxy(HID_DEVICE) → serviceListener.onServiceConnected()
       │                                      │
       │                                      ▼
       │                              registerHidApp()
       │                                      │
       │                                      ▼
       │                              hid.registerApp() async
       │                                      │
       │                                      ▼
       │                              hidCallback.onAppStatusChanged(registered=true)
       │                                      │
       │                                      ▼
       │                              _registrationComplete.value = true
       │                                      │
       │                                      ▼
       │                              _btServiceReady.value = true (in GamepadService)
       │                                      │
       │                                      ▼
       │                              startTransmissionLoop()
       │
       ▼
ConnectionState: REGISTERING → REGISTERED_WAITING → CONNECTING → CONNECTED
```

---

## Key Log Markers

| Stage | Log Tag | Expected Message |
|-------|---------|------------------|
| Discoverability granted | GamepadVM | `Discoverability granted (resultCode=300), proceeding to HID initialization` |
| Adapter renamed | BTGamepad | `Renamed BT adapter from '...' to 'MacGamepad'` |
| HID profile proxy | BTGamepad | `HID Device profile proxy obtained` |
| RegisterApp called | BTGamepad | `✅ registerApp() succeeded — waiting for callback...` |
| Registration complete | BTGamepad | `✅ HID Device app registered. Waiting for host connection...` |
| **RegistrationComplete signal** | GamepadService | *(internal StateFlow)* |
| Transmission loop starts | GamepadService | `Starting high-frequency report loop...` |
| Mac connects | BTGamepad | `🔄 Connecting to: <Mac hostname>` → `✅ Connected to host: <Mac hostname>` |
| Auto-unregister (timeout) | BTGamepad | `⚠️ HID Device app unregistered` |

---

## Common Failure Modes

### ⚠️ CRITICAL: Discoverability is Mandatory for HID Device Profile
The Android Bluetooth HID Device profile **requires** the device to be in discoverable mode (`ACTION_REQUEST_DISCOVERABLE`) for the host to see it. This is not optional — the Bluetooth stack only advertises the HID SDP record when discoverable. The 300-second window is a hard Android limit (`EXTRA_DISCOVERABLE_DURATION` max = 300).

**Never** call `bindGamepadService()` or `initializeBluetooth()` before the user grants discoverability. The flow must be:
1. Permissions granted → `ShowingDiscoverabilityScreen` (NO service bind)
2. User clicks "Allow Discovery" → `onDiscoverabilityResult(300)`
3. **Then** `bindGamepadService()` → `initializeBluetooth()` → `registerApp()`

---

### Common Failure Modes

### 1. Device visible in macOS BEFORE "Allow Discovery"
**Symptom**: "MacGamepad" appears in macOS Bluetooth list immediately after permissions granted
**Cause**: `bindGamepadService()` called too early (before `onDiscoverabilityResult`)
**Fix**: Verify `proceedAfterPermissionsGranted()` does NOT call `bindGamepadService()` (GamepadViewModel.kt:656)

### 2. HID Registration Succeeds but Mac Can't Pair
**Symptom**: Logs show "HID Device app registered. Waiting for host connection..." but macOS connection fails
**Causes**:
- Stale pairing on Mac: System Settings → Bluetooth → Remove "MacGamepad" → Turn BT off/on
- Discoverability expired: 300s window passed before Mac connected
- Adapter rename race: Device briefly visible under old name

### 3. Auto-Disconnect After Mac Connects
**Symptom**: Mac connects → disconnects within seconds
**Causes**:
- Transmission loop started before `registrationComplete` (fixed: GamepadService.kt:134 waits for `registrationComplete.first { it }`)
- `hid.sendReport()` called when not fully connected

### 4. UI Stays on DiscoverabilityScreen
**Symptom**: Mac connects but phone doesn't navigate to GamepadScreen
**Causes**:
- StateFlow race conditions (fixed: MainActivity.kt:136 uses `combine()`)
- Missing auto-navigation (GamepadViewModel.kt:250 handles CONNECTED → DASHBOARD)

### 5. `lateinit property hidManager has not been initialized`
**Symptom**: App crashes on service bind
**Cause**: Race between `LaunchedEffect(gamepadService)` and `hidManager` injection (fixed: removed LaunchedEffect)

---

## Debugging Checklist

When Bluetooth pairing fails:

1. **On Mac**: Remove ALL "MacGamepad" entries from Bluetooth settings → Turn Bluetooth off/on
2. **Run fresh install test**: `./scripts/fresh-install-test.sh --wait-for-connect`
3. **Watch for these log sequence**:
   ```
   ✅ Discoverability granted (resultCode=300)
   ✅ registerApp() succeeded — waiting for callback...
   ✅ HID Device app registered. Waiting for host connection...
   ✅ Starting high-frequency report loop...
   🔄 Connecting to: <Mac hostname>
   ✅ Connected to host: <Mac hostname>
   ```
4. **If missing "HID Device app registered"**: Check `registerApp()` return value (BTGamepad.kt:319)
5. **If missing "Connected to host"**: Verify Mac can see device, try manual connect
6. **If "HID Device app unregistered" appears**: Discoverability window expired (300s) or Mac disconnected

---

## Testing Commands

```bash
# Full test with connection verification
./scripts/fresh-install-test.sh --wait-for-connect

# With specific device
./scripts/fresh-install-test.sh 344c73ad --wait-for-connect

# Manual log monitoring
adb logcat -s BTGamepad:V GamepadService:V GamepadVM:V

# Check bonded devices
adb shell dumpsys bluetooth_manager | grep -A 5 "MacGamepad"
```

---

## Architecture Notes

- **HidDeviceManager interface** (bt/HidDeviceManager.kt) enables testability
- **BluetoothGamepadService** implements the interface (330 lines)
- **GamepadService** orchestrates; uses `registrationComplete` StateFlow to gate transmission loop
- **MainActivity** collects combined StateFlow via `combine()` to avoid races
- **GamepadViewModel** auto-navigates on CONNECTED state

---

*Last updated: 2026-11-08 — based on session debugging race conditions in BT lifecycle*