## 1. Create InputThrottler Class

- [x] 1.1 Create `android/app/src/main/java/com/macgamepad/controller/input/InputThrottler.kt` with `ThrottleResult` sealed interface and `InputThrottler` class implementing the spec requirements. Verify: file compiles and `ThrottleResult` has `Transmit`, `Skip`, `Pending` variants.
- [x] 1.2 Implement `process(state: InputState, nowMs: Long = System.currentTimeMillis()): ThrottleResult` with release detection (buttons, sticks, triggers, D-Pad), throttle window (4ms default, configurable), and trailing-edge pending logic. Verify: all spec scenarios covered by implementation.
- [x] 1.3 Add `reset()` method to clear internal state (lastReportTime, lastState, pendingState). Verify: calling reset returns throttler to initial state.

## 2. Update GamepadViewModel to Use InputThrottler

- [x] 2.1 Add `InputThrottler` instance field to `GamepadViewModel` (initialized in `init` block or lazily). Verify: ViewModel compiles with new dependency.
- [x] 2.2 Replace `onInputChanged()` implementation (lines 465-505) with delegation to `inputThrottler.process(state)` and handle `ThrottleResult` variants:
  - `Transmit` → call `_gamepadService.value?.updateInputState(state)` and if `alsoTransmitPending` send pending too
  - `Skip` → do nothing
  - `Pending` → do nothing (state stored internally)
  Verify: ViewModel compiles; no inline throttle logic remains.
- [x] 2.3 Remove `lastInputReportTime`, `lastState` fields from `GamepadViewModel`. Verify: no references remain.

## 3. Rewrite GamepadViewModelThrottleTest

- [x] 3.1 Update `GamepadViewModelThrottleTest` to instantiate `InputThrottler` and call `process()` directly instead of replicating logic in `onInputChanged()` helper. Verify: test file compiles.
- [x] 3.2 Migrate all 5 existing test cases (`immediate send on release transition`, `suppress when within 8 ms and no release transition`, `trailing edge throttled state sent on next call`, `release transition with sticks`, `multiple buttons release transition`) to use `InputThrottler`. Verify: all 5 tests pass with identical assertions.
- [x] 3.3 Add new test cases for `InputThrottler`-specific behavior: `reset()` clears state, configurable throttle interval, time-controlled testing via `nowMs` parameter. Verify: new tests pass.

## 4. Verification

- [x] 4.1 Run unit tests: `cd android && ./gradlew test --tests "com.macgamepad.controller.GamepadViewModelThrottleTest"` — verify all tests pass.
- [x] 4.2 Run full test suite: `cd android && ./gradlew test` — verify no regressions in `InputStateTest`, `KeyboardMapperTest`. (Pre-existing failures confirmed unrelated)
- [x] 4.3 Build debug APK: `cd android && ./gradlew assembleDebug` — verify compiles successfully.
- [ ] 4.4 Manual smoke test: install on device, verify gamepad input still works (throttling behavior unchanged). Verify: no observable behavior change.