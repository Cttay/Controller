## Context

See proposal.md - Why. `BluetoothGamepadService` exposes three `StateFlow` properties (lines 58, 61, 64) that are collected by `GamepadService`, mirrored by `GamepadViewModel`, and observed by Compose UI. Each layer adds coroutines. Redundant emissions (setting same value) propagate through all layers.

## Goals / Non-Goals

**Goals:**
- Add `.distinctUntilChanged()` to `connectionState`, `connectedDeviceName`, `errorMessage` StateFlow exposures
- Eliminate redundant emissions at the source
- No behavior change — same values emitted, just deduplicated

**Non-Goals:**
- Change StateFlow to SharedFlow
- Modify downstream collectors (GamepadService, ViewModel, UI)
- Add replay or buffering

## Decisions

### 1. Apply at exposure point (StateFlow property getter)
**Current:** `val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()`
**Change:** `val connectionState: StateFlow<ConnectionState> = _connectionState.distinctUntilChanged().asStateFlow()`
**Rationale:** Single change at source affects all downstream. `distinctUntilChanged` uses structural equality (correct for enums, strings, null).

### 2. Use structural equality (default)
**Rationale:** `ConnectionState` is enum, `String?` and `String` use value equality. No custom comparator needed.

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| First emission might be dropped if same as initial | Initial values are distinct (DISCONNECTED, null, null) — first real change will emit |
| Downstream expects every setValue call to emit | Only redundant sets are suppressed; all actual changes emit |