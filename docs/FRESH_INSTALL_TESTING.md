# Fresh Install Test Flow — MacGamepad

## Prerequisites
- Android device connected via adb (`adb devices` shows `192.168.68.61:5555`)
- Mac ready for Bluetooth pairing (A4:CF:99:93:37:37 "Stephen's MacBook Pro")
- Chrome on Mac for gadgetester.com verification

---

## Quick Start (Automated)

```bash
# Run the automated test script (handles build, install, launch, permission taps, log monitoring)
./.slim/scripts/fresh-install-test.sh
# Or with custom device serial:
./.slim/scripts/fresh-install-test.sh 192.168.68.61:5555
```

The script will:
1. Clean build + install
2. Launch app
3. Auto-tap "Initialize Controller" and "Allow" permission dialog
4. Monitor logs for HID registration success
5. Exit when app shows "Discoverable — waiting for Mac"

---

## Manual Commands

```bash
# 1. Clean build + install + launch (chained — only launches if build succeeds)
cd /Users/stephen/Developer/projects/android/Controller/.slim/worktrees/security-performance-hardening/android
ADB=~/Library/Android/sdk/platform-tools/adb
./gradlew clean assembleDebug installDebug && $ADB -s 192.168.68.61:5555 shell am start -n com.macgamepad.controller/.MainActivity

# Monitor logs (run in separate terminal)
$ADB -s 192.168.68.61:5555 logcat -s BTGamepad:V GamepadService:V GamepadVM:V HidReportPacker:V
```

---

## On Device (Permission Dialog)

1. **"Initialize Controller"** button → tap center (~540, 1512)
2. **Permission dialog** → tap **"Allow"** (~540, 2087)
3. App shows **"Discoverable — waiting for Mac"**

---

## Expected Log Markers

| Stage | Log Line |
|-------|----------|
| Service created | `I GamepadService: Creating GamepadService...` |
| Service bound | `I MainActivity: Connected to GamepadService` |
| HID proxy | `I BTGamepad: HID Device profile proxy obtained` |
| Register success | `I BTGamepad: ✅ registerApp() succeeded — waiting for callback...` |
| Registered | `I BTGamepad: ✅ HID Device app registered. Waiting for host connection...` |
| Discoverability | `I BTGamepad: Requested Bluetooth discoverability for 300 seconds` |
| Focus loss (normal) | `W BTGamepad: ⚠️ HID Device app temporarily unregistered (likely focus loss), keeping registration active` |

---

## On Device (Permission Dialog)

1. **"Initialize Controller"** button → tap center (~540, 1512)
2. **Permission dialog** → tap **"Allow"** (~540, 2087)
3. App shows **"Discoverable — waiting for Mac"**

---

## On Mac

1. **System Settings → Bluetooth**
2. Find **"MacGamepad"** → **Connect**
3. Phone shows **"Connected · MacBook Pro"**
4. Tap **"Launch Controller"** → Select **Xbox** profile

---

## Verification (Mac Chrome)

Open: https://gadgetester.com/gamepad-tester/

| W3C Index | Xbox Label | Expected |
|-----------|------------|----------|
| 0 | A (bottom) | Button 0 |
| 1 | B (right) | Button 1 |
| 2 | X (left) | Button 2 |
| 3 | Y (top) | Button 3 |
| 4 | LB | Button 4 |
| 5 | RB | Button 5 |
| 6 | View/Select | Button 6 |
| 7 | Menu/Start | Button 7 |
| 8 | Xbox/Home | Button 8 |
| 9 | L3 | Button 9 |
| 10 | R3 | Button 10 |
| 11 | Capture/Guide | Button 11 |
| Axis 0 | LX | Left stick X |
| Axis 1 | LY | Left stick Y |
| Axis 2 | RX | Right stick X |
| Axis 3 | RY | Right stick Y |
| Axis 4 | Z | Left trigger (0-255) |
| Axis 5 | Rz | Right trigger (0-255) |
| Hat Switch | D-pad | 1=UP, 3=RIGHT, 5=DOWN, 7=LEFT |

---

## Troubleshooting

| Issue | Fix |
|-------|-----|
| App crashes on launch | Check `logcat` for `SecurityException: BLUETOOTH_CONNECT` — permission flow broken |
| Foreground service doesn't start | Android 14+ delays ~3.6 min (`startForegroundDelayMs:217622`) — wait |
| Mac doesn't see device | Verify discoverability: `I BTGamepad: Requested Bluetooth discoverability for 300 seconds` |
| Buttons wrong on gadgetester.com | Verify W3C descriptor: `GamepadHidDescriptor.kt` Report ID 1, 12 buttons, Hat Switch |

---

## Quick Re-test (skip clean build)

```bash
# After code changes only
cd android
./gradlew installDebug
$ADB -s 192.168.68.61:5555 shell am force-stop com.macgamepad.controller
$ADB -s 192.168.68.61:5555 shell am start -n com.macgamepad.controller/.MainActivity
```