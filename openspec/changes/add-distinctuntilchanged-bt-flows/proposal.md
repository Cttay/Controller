## Why

The `BluetoothGamepadService` exposes three `StateFlow` properties (`connectionState`, `connectedDeviceName`, `errorMessage`) that are mirrored through 4 layers: BT Service → GamepadService → GamepadViewModel → UI. Each layer adds a `collectLatest`/`collectAsState` coroutine. Without `distinctUntilChanged()` at the source, redundant emissions (same value) propagate through all layers, causing unnecessary UI recompositions and potential stale data if any layer emits before downstream collects.

## What Changes

- Add `.distinctUntilChanged()` to the three `StateFlow` properties in `BluetoothGamepadService`:
  - `connectionState`
  - `connectedDeviceName`
  - `errorMessage`
- No externally observable behavior change — same values, just deduplicated at source

## Capabilities

### New Capabilities
- None

### Modified Capabilities
- `bt/connection-state-management`: Bluetooth HID connection state propagation — deduplication added at source

## Impact

**Affected Files:**
- `android/app/src/main/java/com/macgamepad/controller/bt/BluetoothGamepadService.kt` — add `.distinctUntilChanged()` to three StateFlow exposure points

**Dependencies:** None. Pure Kotlin Flow operator.