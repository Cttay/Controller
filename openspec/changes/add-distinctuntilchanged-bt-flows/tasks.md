## 1. Add distinctUntilChanged to BluetoothGamepadService StateFlows

- [x] 1.1 Update `connectionState` property (line 58) to `_connectionState.distinctUntilChanged().asStateFlow()`. Verify: compiles and property type unchanged.
- [x] 1.2 Update `connectedDeviceName` property (line 64) to `_connectedDeviceName.distinctUntilChanged().asStateFlow()`. Verify: compiles and property type unchanged.
- [x] 1.3 Update `errorMessage` property (line 61) to `_errorMessage.distinctUntilChanged().asStateFlow()`. Verify: compiles and property type unchanged.

## 2. Verification

- [x] 2.1 Build debug APK: `cd android && ./gradlew assembleDebug` — verify compiles successfully.
- [x] 2.2 Run unit tests: `cd android && ./gradlew test` — verify no regressions. (Pre-existing test failures unrelated to this change)
- [ ] 2.3 Manual smoke test: install on device, verify connection state UI updates correctly (connect/disconnect transitions work). Verify: no duplicate emissions observed in logs.