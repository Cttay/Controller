## Context

See proposal.md - Why. The current throttling logic lives inline in `GamepadViewModel.onInputChanged()` (lines 465-505) and is duplicated in `GamepadViewModelThrottleTest`. Both implementations use `System.currentTimeMillis()` for timing and maintain mutable state (`lastInputReportTime`, `lastState`, `pendingState`).

**Current throttle interval:** 4ms (line 501) — note: comment says ~120 Hz (8ms) but code uses 4ms (~250 Hz).

## Goals / Non-Goals

**Goals:**
- Extract throttling logic to a pure `InputThrottler` class in `com.macgamepad.controller.input` package
- Provide clean API: `process(state: InputState): ThrottleResult` where result indicates transmit/skip/pending
- Remove all throttling state from `GamepadViewModel`
- Update `GamepadViewModelThrottleTest` to test `InputThrottler` directly
- Maintain identical behavior (same release detection, same throttle semantics)

**Non-Goals:**
- Change throttle interval (keep 4ms as currently implemented)
- Add new throttling algorithms (exponential backoff, adaptive, etc.)
- Modify `InputState` data class
- Change `GamepadService.updateInputState()` API

## Decisions

### 1. Package placement: `com.macgamepad.controller.input`
**Rationale:** `InputState` already lives here. Throttling is input-domain logic, not ViewModel logic.
**Alternative:** `com.macgamepad.controller.throttle` — rejected: over-fragmentation for single class.

### 2. API shape: `process(state: InputState): ThrottleResult` (sealed class)
```kotlin
sealed interface ThrottleResult {
    data class Transmit(val state: InputState, val alsoTransmitPending: Boolean) : ThrottleResult
    object Skip : ThrottleResult
    data class Pending(val state: InputState) : ThrottleResult
}
```
**Rationale:** Explicit result makes caller intent clear. `Transmit.alsoTransmitPending` handles trailing-edge delivery (pending + current both sent).
**Alternative:** Callback-based `onTransmit(state)` — rejected: harder to test, couples to side effects.

### 3. Release detection: bitmask XOR + stick delta threshold (future-proof)
**Current:** 35+ individual field comparisons.
**Design:** Keep explicit comparisons for now (matches test expectations exactly). Add `InputState.toButtonBitmask()` and `InputState.stickDelta(other)` methods in follow-up (P5 task 8) to simplify.
**Why not now:** Spec doesn't require algorithm change; avoid scope creep.

### 4. Time source: `System.currentTimeMillis()` passed as parameter
**Rationale:** Testability. `InputThrottler.process(state, nowMs)` allows tests to control time without mocking `System`.
**Current code:** Uses `System.currentTimeMillis()` directly — hard to test timing edge cases.

### 5. Throttle interval: configurable via constructor (default 4ms)
**Rationale:** Current code uses 4ms (not 8ms as comment claims). Make it explicit and configurable for future tuning.
**Default:** `DEFAULT_THROTTLE_MS = 4L`

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| Behavioral divergence from current implementation | Port tests first (TDD), verify identical outputs for all test cases |
| `GamepadViewModel` still needs `lastState` for release detection across calls | `InputThrottler` encapsulates all state — ViewModel just holds throttler instance |
| Tests currently replicate logic inline | Rewrite test to instantiate `InputThrottler` and call `process()` — delete replicated logic |
| 4ms vs 8ms discrepancy | Document actual interval (4ms) in throttler; update comment in ViewModel |