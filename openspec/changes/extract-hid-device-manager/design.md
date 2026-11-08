## Context

See proposal.md for motivation.

**Current state** (from codebase inspection):
- `BluetoothGamepadService` (330 lines) is a concrete class handling all HID Device profile logic
- `GamepadService` directly instantiates `BluetoothGamepadService(applicationContext)` (line 92)
- `GamepadViewModel` mirrors `BluetoothGamepadService.ConnectionState` and calls concrete methods
- `ConnectionScreen` and `ProfileScreen` import `BluetoothGamepadService` directly for state/enums
- Zero automated tests for HID registration/transmission path

**Constraints**:
- minSdk 28, compileSdk 35, Kotlin 2.1.0
- Must maintain existing Bluetooth HID protocol (14-byte report, 125Hz)
- No new dependencies
- Interface must support both gamepad (packed) and keyboard/mouse (raw) report types

## Goals / Non-Goals

**Goals:**
- Extract `HidDeviceManager` interface with complete surface for HID Device profile management
- Make `BluetoothGamepadService` implement the interface
- Invert dependency: `GamepadService` receives `HidDeviceManager` via constructor injection
- Update UI layer to depend on interface types
- Enable Robolectric unit tests with mocked `HidDeviceManager`

**Non-Goals:**
- Refactor `GamepadService` internals (transmission loop, gyro, input mapping) — that's P1 God Service refactor
- Change Bluetooth HID protocol or report format
- Add new Bluetooth features (allowlist, encryption)
- Modify macOS bridge (`tools/gamepad_bridge.c`)

## Decisions

### 1. Interface location: `bt/HidDeviceManager.kt`
**Rationale**: Co-located with `BluetoothGamepadService`, `ConnectionState`, `Mode`. Keeps Bluetooth domain together.
**Alternative considered**: Separate `bt/manager/` package — rejected as over-engineering for single interface.

### 2. Enum extraction: `bt/ConnectionState.kt` + `bt/Mode.kt`
**Rationale**: `ConnectionState` and `Mode` are currently nested in `BluetoothGamepadService`. Moving to shared files allows both interface and implementation to reference them without circular dependency.
**Alternative considered**: Keep enums in interface file — rejected; separate files allow UI to import enums without interface.

### 3. Two report methods: `sendGamepadReport()` + `sendReport()`
**Rationale**: 
- Gamepad mode uses packed `InputState` → 14-byte report via `HidReportPacker`
- Keyboard/mouse modes use raw `ByteArray` with specific `reportId`
- Merging would require `when` on mode inside interface, leaking implementation detail
**Alternative considered**: Single `sendReport(reportId: Int, data: ByteArray)` for all modes — rejected; gamepad packing logic belongs in implementation, not callers.

### 4. StateFlow for connection state, device name, error message
**Rationale**: Matches current `BluetoothGamepadService` implementation. UI already observes these as `StateFlow`. No behavior change.
**Alternative considered**: `SharedFlow` with replay=1 — rejected; `StateFlow` is current pattern, `distinctUntilChanged()` in UI layer handles duplication.

### 5. Constructor injection in `GamepadService`
**Rationale**: Removes direct `BluetoothGamepadService` instantiation (line 92). `MainActivity` or DI framework provides implementation.
**Alternative considered**: Service locator / manual factory — rejected; constructor injection is simplest, testable, and explicit.

### 6. `HidDeviceManager` fake for Robolectric tests
**Rationale**: Robolectric shadows for `BluetoothHidDevice` are incomplete. A fake implementing the interface enables fast, reliable unit tests for registration flow.
**Alternative considered**: Mockito mocks — rejected; fake provides realistic state transitions (REGISTERING → REGISTERED_WAITING → CONNECTED).

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| **Interface too large** — 12 methods/properties may violate ISP | All methods are used by at least one caller (GamepadService, GamepadViewModel, UI). Split later if needed. |
| **Breaking internal API** — `GamepadService` constructor changes | Single PR updates all callers (MainActivity, tests). No public API affected. |
| **StateFlow duplication persists** — UI still mirrors interface flows | P3 StateFlow cleanup is separate change. This change enables it by centralizing source of truth in interface impl. |
| **SDP/QoS config hidden in impl** — cannot customize per test | Tests use fake; integration tests use real impl. SDP is stable for gamepad/keyboard/mouse. |
| **Mode switch after init** — `setMode()` after `initialize()` requires re-registration | Document in interface: "Mode change takes effect on next re-registration." Current behavior preserved. |

## Migration Plan

1. **Add new files**: `bt/ConnectionState.kt`, `bt/Mode.kt`, `bt/HidDeviceManager.kt`
2. **Update `BluetoothGamepadService`**: Implement interface, remove nested enums
3. **Update `GamepadService`**: Constructor inject `HidDeviceManager`, remove direct instantiation
4. **Update `MainActivity`**: Provide `BluetoothGamepadService` as `HidDeviceManager` when creating `GamepadService`
5. **Update `GamepadViewModel`**: Use interface types, call `initialize()`/`shutdown()` on permission changes
6. **Update `ConnectionScreen`/`ProfileScreen`**: Accept interface-typed params
7. **Add Robolectric test**: Fake `HidDeviceManager` + test registration flow
8. **Verify**: `compileDebugKotlin`, `test` pass

**Rollback**: Revert all file changes in single commit. No database migrations or config changes.

## Open Questions

None — all technical decisions resolved. Specs define exact behavior.