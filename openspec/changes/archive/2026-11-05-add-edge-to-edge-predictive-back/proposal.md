## Why

Android 15 (API 35) enforces edge-to-edge display by default. Apps that don't opt in will have system bars covering content. Additionally, predictive back gestures are a key UX feature in Android 14+. The app currently has neither, risking visual regression on API 35 devices and poor back navigation UX.

## What Changes

- Add `WindowCompat.setDecorFitsSystemWindows(window, false)` in `MainActivity.onCreate()` to opt into edge-to-edge
- Call `enablePredictiveBack()` in `MainActivity.onCreate()` for predictive back gestures
- Update Compose screens to handle system bar insets with `Modifier.statusBarsPadding()` and `Modifier.navigationBarsPadding()`
- No externally observable behavior change on pre-API 35; improved UX on API 34+

## Capabilities

### New Capabilities
- `ui/edge-to-edge`: Edge-to-edge display support for Android 15+
- `ui/predictive-back`: Predictive back gesture support for Android 14+

### Modified Capabilities
- None — this adds new UI behavior without changing existing requirements

## Impact

**Affected Files:**
- `android/app/src/main/java/com/macgamepad/controller/MainActivity.kt` — add edge-to-edge + predictive back setup
- `android/app/src/main/java/com/macgamepad/controller/ui/theme/Theme.kt` — may need inset handling
- Screen composables (`ConnectionScreen`, `DashboardScreen`, `GamepadScreen`, `ProfileScreen`, `SettingsScreen`) — add inset padding modifiers

**Dependencies:** `androidx.core:core-ktx` (already included), `androidx.activity:activity-compose` (already included)