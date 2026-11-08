## 1. Remove dataSync from Foreground Service Type

- [x] 1.1 Update `AndroidManifest.xml` line 36: change `android:foregroundServiceType="connectedDevice|dataSync"` to `android:foregroundServiceType="connectedDevice"`. Verify: manifest compiles, only connectedDevice declared.

## 2. Verification

- [x] 2.1 Build debug APK: `cd android && ./gradlew assembleDebug` — verify compiles successfully.
- [x] 2.2 Verify manifest: `cd android && ./gradlew :app:processDebugManifest` and check output contains only `connectedDevice`. (Verified: merged manifest shows `android:foregroundServiceType="connectedDevice"`)
- [ ] 2.3 Manual test: install on device, verify foreground service starts correctly with only connectedDevice type (notification shows, BT connection works).