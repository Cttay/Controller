## Why

The input throttling logic in `GamepadViewModel.onInputChanged()` (lines 365-405) contains 35+ conditionals comparing every button, stick axis, trigger, and dpad field to detect release transitions. This same logic is duplicated in `GamepadViewModelThrottleTest` because the test comment explicitly states "cannot modify production code to extract a testable helper." This duplication creates maintenance burden, risk of divergence, and makes the ViewModel harder to test in isolation.

## What Changes

- Extract throttling logic into a pure `InputThrottler` class with a clean API
- Replace the inline throttling in `GamepadViewModel` with delegation to `InputThrottler`
- Update `GamepadViewModelThrottleTest` to test `InputThrottler` directly (removing duplicated logic)
- No externally observable behavior change — same throttling semantics, just better factored

## Capabilities

### New Capabilities
- `input/throttling`: Pure input throttling logic — deduplication of consecutive identical states, release detection via bitmask XOR, stick delta threshold comparison

### Modified Capabilities
- None — this is a pure refactor with no spec-level behavior change

## Impact

**Affected Files:**
- `android/app/src/main/java/com/macgamepad/controller/GamepadViewModel.kt` — remove inline throttling, delegate to `InputThrottler`
- `android/app/src/main/java/com/macgamepad/controller/InputThrottler.kt` — new file
- `android/app/src/test/java/com/macgamepad/controller/GamepadViewModelThrottleTest.kt` — rewrite to test `InputThrottler` directly

**Dependencies:** None new. Pure Kotlin, no Android framework dependencies in the throttler itself.