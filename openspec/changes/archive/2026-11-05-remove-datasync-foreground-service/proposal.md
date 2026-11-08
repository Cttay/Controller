## Why

The `GamepadService` declares `android:foregroundServiceType="connectedDevice|dataSync"` in the manifest. The `dataSync` type is unnecessary for a Bluetooth HID controller app — it's intended for background data synchronization tasks. Including it may trigger Play Store policy scrutiny since `dataSync` requires justification for background data usage. The app only needs `connectedDevice` for maintaining the Bluetooth HID connection.

## What Changes

- Remove `dataSync` from `android:foregroundServiceType` in `AndroidManifest.xml`
- Change from `connectedDevice|dataSync` to `connectedDevice` only
- No code changes needed — the service already uses `FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE` correctly

## Capabilities

### New Capabilities
- None

### Modified Capabilities
- `bt/foreground-service-configuration`: Foreground service type declaration — `dataSync` removed

## Impact

**Affected Files:**
- `android/app/src/main/AndroidManifest.xml` line 36 — single line change

**Dependencies:** None. Pure manifest change.