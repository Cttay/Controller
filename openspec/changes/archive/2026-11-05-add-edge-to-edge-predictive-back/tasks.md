## 1. Enable Predictive Back

- [x] 1.1 Add `enablePredictiveBack()` call in `MainActivity.onCreate()` after `super.onCreate(savedInstanceState)` and before `setContent()`. Verify: compiles and runs on API 34+ device with predictive back animation.

## 2. Update Theme for Inset Handling

- [x] 2.1 Update `Theme.kt` to wrap `MaterialTheme` content with a `Box` that applies `Modifier.statusBarsPadding().navigationBarsPadding()` for non-immersive screens. Verify: content not obscured by system bars on API 35.
- [x] 2.2 Add `ProvideWindowInsets` or use `WindowInsets` API to make insets available to descendant composables. Verify: `WindowInsets.statusBars` and `WindowInsets.navigationBars` accessible in screens.

## 3. Apply Inset Padding to Screen Composables

- [x] 3.1 Update `ConnectionScreen` — apply `Modifier.statusBarsPadding()` to top container. Verify: content not hidden behind status bar.
- [x] 3.2 Update `DashboardScreen` — apply `Modifier.statusBarsPadding()` to top, `Modifier.navigationBarsPadding()` to bottom button area. Verify: bottom buttons accessible.
- [x] 3.3 Update `ProfileScreen` — apply `Modifier.statusBarsPadding()` to top, `Modifier.navigationBarsPadding()` to bottom. Verify: content not obscured.
- [x] 3.4 Update `SettingsScreen` — apply `Modifier.statusBarsPadding()` to top, `Modifier.navigationBarsPadding()` to bottom. Verify: content not obscured.
- [x] 3.5 Update `GamepadScreen` — in immersive mode (default), NO padding (bars hidden); when bars shown by swipe, content should handle transient insets. Verify: controller controls not obscured when bars transiently shown.

## 4. Verification

- [x] 4.1 Build debug APK: `cd android && ./gradlew assembleDebug` — verify compiles successfully.
- [ ] 4.2 Manual test on API 34+ device/emulator: launch app, verify predictive back animation works when swiping back from Gamepad → Dashboard → Connection. (Deferred: requires API 34+ device)
- [ ] 4.3 Manual test on API 35 device/emulator: verify edge-to-edge — content extends behind status/navigation bars, no clipping. (Deferred: requires API 35 device)
- [ ] 4.4 Test GamepadScreen immersive mode: bars hidden, swipe from bottom shows bars transiently, controller still usable. (Deferred: requires device)