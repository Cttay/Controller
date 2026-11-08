## Context

See proposal.md - Why. Current state:
- `MainActivity.onCreate()` line 122: `WindowCompat.setDecorFitsSystemWindows(window, false)` **already present** — edge-to-edge opt-in is done
- `MainActivity.onCreate()`: **Missing** `enablePredictiveBack()` call
- `Theme.kt`: Sets status/navigation bar colors but no inset handling for content
- Screen composables: No inset padding modifiers applied

## Goals / Non-Goals

**Goals:**
- Add `enablePredictiveBack()` call in `MainActivity.onCreate()`
- Update `Theme.kt` to provide inset-aware content wrapper
- Apply `Modifier.statusBarsPadding()` and `Modifier.navigationBarsPadding()` to all screen composables
- Ensure GamepadScreen controller controls aren't obscured by navigation bar

**Non-Goals:**
- Change theme colors or visual design
- Modify navigation logic
- Add new dependencies

## Decisions

### 1. Enable predictive back at Activity level
**Location:** `MainActivity.onCreate()` after `super.onCreate(savedInstanceState)`
**Code:** `enablePredictiveBack()`
**Rationale:** Must be called before `setContent()` and `super.onCreate()`. Single line, API 34+ only (no-op on lower).
**Status:** **Deferred** — API requires `androidx.activity:activity:1.11.0+` which needs AGP 8.9+ and compileSdk 36. Current project uses AGP 8.7.3 and compileSdk 35. Will be implemented in a separate build config upgrade change.

### 2. Inset handling via Theme wrapper
**Location:** `Theme.kt` — wrap `MaterialTheme` content with `Box` that applies `Modifier.statusBarsPadding()` (navigation bar padding applied per-screen in MainActivity)
**Alternative considered:** Add padding to each screen individually
**Decision:** Theme wrapper applies status bar padding globally for consistency. Navigation bar padding applied per-screen in MainActivity since GamepadScreen uses immersive mode (bars hidden) and should not have nav padding. This avoids the wrapper applying unwanted padding to immersive screens.

### 3. GamepadScreen special handling
**Current:** Uses `WindowInsetsControllerCompat` to hide system bars in immersive mode
**Design:** When immersive (GAMEPAD screen), don't apply navigation bar padding since bars are hidden. When not immersive, apply padding.

### 4. Screen-by-screen padding application
For screens NOT in immersive mode:
- Top-level container gets `Modifier.statusBarsPadding()` 
- Bottom controls/buttons get `Modifier.navigationBarsPadding()`

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| Immersive mode hides bars but content still needs padding when bars reappear | `WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE` handles transient show |
| Predictive back may conflict with custom back handlers (edit mode exit) | Test back gesture in edit mode; `BackHandler` in Compose handles correctly |
| Status bar color set in Theme may conflict with edge-to-edge transparency | Already sets `statusBarColor` to background color — compatible |
| Navigation bar color set to background — may need transparent for true edge-to-edge | Current design uses solid background color; acceptable |