#!/bin/bash
# fresh-install-test.sh — MacGamepad end-to-end fresh install test
# Usage: ./fresh-install-test.sh [DEVICE_SERIAL]
# Default device: first connected device via adb

set -euo pipefail

# Resolve device serial - use first connected device if not specified
if [ $# -eq 0 ]; then
    DEVICE=$($HOME/Library/Android/sdk/platform-tools/adb devices | grep -v "List" | grep -v "^$" | head -1 | awk '{print $1}')
    if [ -z "$DEVICE" ]; then
        echo "❌ No device connected. Please connect a device or specify serial: ./fresh-install-test.sh <serial>"
        exit 1
    fi
else
    DEVICE="$1"
fi

ADB="$HOME/Library/Android/sdk/platform-tools/adb"
PROJECT_ROOT="/Users/stephen/Developer/projects/android/Controller"
ANDROID_DIR="$PROJECT_ROOT/android"
PACKAGE="com.macgamepad.controller"

echo "=== MacGamepad Fresh Install Test ==="
echo "Device: $DEVICE"
echo "Project: $PROJECT_ROOT"
echo ""

# 1. Clean build + install
echo "[1/6] Clean build + install..."
cd "$ANDROID_DIR"
if ! ./gradlew clean assembleDebug installDebug; then
    echo "❌ Build/install failed"
    exit 1
fi
echo "✅ Build/install complete"
echo ""

# 2. Force stop any existing instance
echo "[2/6] Force stopping app..."
$ADB -s "$DEVICE" shell am force-stop "$PACKAGE" 2>/dev/null || true
echo "✅ App stopped"
echo ""

# 3. Launch app
echo "[3/6] Launching app..."
$ADB -s "$DEVICE" shell am start -n "$PACKAGE/.MainActivity"
echo "✅ App launched"
echo ""

# 4. Wait for UI to settle, then tap "Initialize Controller"
echo "[4/6] Waiting for UI... (3s)"
sleep 3
echo "Tapping 'Initialize Controller' (540, 1512)..."
$ADB -s "$DEVICE" shell input tap 540 1512
echo "✅ Tapped Initialize Controller"
echo ""

# 5. Wait for permission dialog, tap "Allow"
echo "[5/6] Waiting for permission dialog... (5s)"
sleep 5
echo "Tapping 'Allow' (540, 2087)..."
$ADB -s "$DEVICE" shell input tap 540 2087
echo "✅ Tapped Allow"
echo ""

# 5b. Wait for Bluetooth discoverability dialog, tap "Allow"
echo "[5b/6] Waiting for Bluetooth discoverability dialog... (3s)"
sleep 3
echo "Tapping 'Allow' on discoverability dialog (540, 1188)..."
$ADB -s "$DEVICE" shell input tap 540 1188
echo "✅ Tapped Allow on discoverability dialog"
echo ""

# 6. Monitor logs for key markers
echo "[6/6] Monitoring logs for HID registration..."
echo "Waiting for 'registerApp() succeeded' and 'Discoverable' state..."
echo ""

$ADB -s "$DEVICE" logcat -s BTGamepad:V GamepadService:V GamepadVM:V | while IFS= read -r line; do
    echo "$line"
    if echo "$line" | grep -q "registerApp() succeeded"; then
        echo "✅ HID registration succeeded"
    fi
    if echo "$line" | grep -q "Requested Bluetooth discoverability for 300 seconds"; then
        echo "✅ Discoverability requested"
    fi
    if echo "$line" | grep -q "Discoverable"; then
        echo ""
        echo "=== FRESH INSTALL TEST PASSED ==="
        echo "App is now in 'Discoverable — waiting for Mac' state"
        echo ""
        echo "Next manual steps:"
        echo "  1. Mac: System Settings → Bluetooth → Connect 'MacGamepad'"
        echo "  2. Phone: Auto-flips to Gamepad screen (Xbox profile)"
        echo "  3. Mac Chrome: https://gadgetester.com/gamepad-tester/"
        echo "     Verify W3C mapping: Buttons 0-11, Axes 0-5, Hat Switch for D-pad"
        break
    fi
    if echo "$line" | grep -q "SecurityException"; then
        echo "❌ SecurityException detected - permission flow broken"
        break
    fi
    if echo "$line" | grep -q "FATAL EXCEPTION"; then
        echo "❌ App crashed"
        break
    fi
done