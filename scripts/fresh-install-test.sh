#!/bin/bash
# fresh-install-test.sh — MacGamepad end-to-end fresh install test
# Usage: ./fresh-install-test.sh [DEVICE_SERIAL] [--wait-for-connect]
# Default device: first connected device via adb

set -euo pipefail

# Parse arguments
WAIT_FOR_CONNECT=false
if [ $# -eq 0 ]; then
    DEVICE=$($HOME/Library/Android/sdk/platform-tools/adb devices | grep -v "List" | grep -v "^$" | head -1 | awk '{print $1}')
    if [ -z "$DEVICE" ]; then
        echo "❌ No device connected. Please connect a device or specify serial: ./fresh-install-test.sh <serial>"
        exit 1
    fi
else
    DEVICE="$1"
    if [ $# -ge 2 ] && [ "$2" = "--wait-for-connect" ]; then
        WAIT_FOR_CONNECT=true
    fi
fi

ADB="$HOME/Library/Android/sdk/platform-tools/adb"
PROJECT_ROOT="/Users/stephen/Developer/projects/android/Controller"
ANDROID_DIR="$PROJECT_ROOT/android"
PACKAGE="com.macgamepad.controller"

echo "=== MacGamepad Fresh Install Test ==="
echo "Device: $DEVICE"
echo "Wait for connect: $WAIT_FOR_CONNECT"
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

# 2. Clear app data for truly fresh state (removes permissions, settings, etc.)
echo "[2/6] Clearing app data..."
$ADB -s "$DEVICE" shell pm clear "$PACKAGE" 2>/dev/null || true
echo "✅ App data cleared"
echo ""

# 3. Force stop any existing instance
echo "[3/6] Force stopping app..."
$ADB -s "$DEVICE" shell am force-stop "$PACKAGE" 2>/dev/null || true
echo "✅ App stopped"
echo ""

# 4. Launch app
echo "[4/6] Launching app..."
$ADB -s "$DEVICE" shell am start -n "$PACKAGE/.MainActivity"
echo "✅ App launched"
echo ""

# Keep screen on during test to prevent app from being backgrounded
echo "Keeping screen on..."
$ADB -s "$DEVICE" shell svc power stayon true 2>/dev/null || true
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
if [ "$WAIT_FOR_CONNECT" = true ]; then
    echo "Waiting for 'registerApp() succeeded', 'Waiting for host connection', AND 'Connected to host' state..."
else
    echo "Waiting for 'registerApp() succeeded' and 'Waiting for host connection' state..."
fi
echo ""

# Track test results
REGISTER_APP_SUCCEEDED=false
DISCOVERABILITY_REQUESTED=false
WAITING_FOR_HOST=false
CONNECTED_TO_HOST=false
SECURITY_EXCEPTION=false
APP_CRASHED=false

# Capture logs to file for summary
LOG_FILE=$(mktemp)
trap "rm -f $LOG_FILE" EXIT

$ADB -s "$DEVICE" logcat -s BTGamepad:V GamepadService:V GamepadVM:V 2>&1 | tee "$LOG_FILE" | while IFS= read -r line; do
    echo "$line"
    
    if echo "$line" | grep -q "registerApp() succeeded"; then
        REGISTER_APP_SUCCEEDED=true
        echo "✅ HID registration succeeded"
    fi
    if echo "$line" | grep -q "Requested Bluetooth discoverability for 300 seconds"; then
        DISCOVERABILITY_REQUESTED=true
        echo "✅ Discoverability requested"
    fi
    if echo "$line" | grep -q "Waiting for host connection"; then
        WAITING_FOR_HOST=true
        echo "✅ App in 'Waiting for host connection' state"
    fi
    if echo "$line" | grep -q "Connected to host"; then
        CONNECTED_TO_HOST=true
        echo "✅ Mac connected successfully!"
    fi
    if echo "$line" | grep -q "SecurityException"; then
        SECURITY_EXCEPTION=true
        echo "❌ SecurityException detected - permission flow broken"
        break
    fi
    if echo "$line" | grep -q "FATAL EXCEPTION"; then
        APP_CRASHED=true
        echo "❌ App crashed"
        break
    fi
    
    # Exit conditions
    if [ "$WAIT_FOR_CONNECT" = true ]; then
        if [ "$CONNECTED_TO_HOST" = true ]; then
            echo ""
            echo "=== FRESH INSTALL TEST PASSED (FULL CONNECTION) ==="
            break
        fi
    else
        if [ "$WAITING_FOR_HOST" = true ]; then
            echo ""
            echo "=== FRESH INSTALL TEST PASSED (DISCOVERABLE) ==="
            break
        fi
    fi
done

# Test summary
echo ""
echo "=== TEST SUMMARY ==="
echo "Device: $DEVICE"
echo "RegisterApp succeeded: $REGISTER_APP_SUCCEEDED"
echo "Discoverability requested: $DISCOVERABILITY_REQUESTED"
echo "Waiting for host: $WAITING_FOR_HOST"
echo "Connected to host: $CONNECTED_TO_HOST"
echo "SecurityException: $SECURITY_EXCEPTION"
echo "App crashed: $APP_CRASHED"

if [ "$SECURITY_EXCEPTION" = true ] || [ "$APP_CRASHED" = true ]; then
    echo "❌ TEST FAILED"
    exit 1
elif [ "$WAIT_FOR_CONNECT" = true ]; then
    if [ "$CONNECTED_TO_HOST" = true ]; then
        echo "✅ TEST PASSED - Full connection verified"
    else
        echo "⚠️  TEST INCOMPLETE - Mac did not connect within timeout"
        echo "   Next manual steps:"
        echo "   1. Mac: System Settings → Bluetooth → Connect 'MacGamepad'"
        echo "   2. Phone: Auto-flips to Gamepad screen (Xbox profile)"
        echo "   3. Mac Chrome: https://gadgetester.com/gamepad-tester/"
        echo "      Verify W3C mapping: Buttons 0-11, Axes 0-5, Hat Switch for D-pad"
    fi
else
    if [ "$WAITING_FOR_HOST" = true ]; then
        echo "✅ TEST PASSED - App in discoverable state"
        echo ""
        echo "Next manual steps:"
        echo "  1. Mac: System Settings → Bluetooth → Connect 'MacGamepad'"
        echo "  2. Phone: Auto-flips to Gamepad screen (Xbox profile)"
        echo "  3. Mac Chrome: https://gadgetester.com/gamepad-tester/"
        echo "     Verify W3C mapping: Buttons 0-11, Axes 0-5, Hat Switch for D-pad"
    fi
fi

# Restore screen timeout
echo "Restoring screen timeout..."
$ADB -s "$DEVICE" shell svc power stayon false 2>/dev/null || true