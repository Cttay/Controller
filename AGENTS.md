# Agent Notes: MacGamepad Project

## Build & Install

Android app (Gradle 8.11.1, AGP 8.7.3, Kotlin 2.1.0, compileSdk 35, minSdk 28):

```bash
cd android
./gradlew assembleDebug    # APK → android/app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug     # build + install on the attached device/emulator via adb
```

If `./gradlew` can't find the SDK, pass it inline (no `local.properties` is committed):

```bash
ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew installDebug
```

Launch/relaunch the app after install:

```bash
ADB=$HOME/Library/Android/sdk/platform-tools/adb
$ADB devices -l
$ADB shell am force-stop com.macgamepad.controller
$ADB shell am start -n com.macgamepad.controller/.MainActivity
```

Manual APK install: `$ADB install -r android/app/build/outputs/apk/debug/app-debug.apk`

### Fresh Install Test (Automated)

```bash
# Run the automated test script (handles build, install, launch, permission taps, log monitoring)
./.slim/scripts/fresh-install-test.sh
# Or with custom device serial:
./.slim/scripts/fresh-install-test.sh 192.168.68.61:5555
```

The script will:
1. Clean build + install
2. Launch app
3. Auto-tap "Initialize Controller" and "Allow" permission dialog
4. Monitor logs for HID registration success
5. Exit when app shows "Discoverable — waiting for Mac"

See `docs/FRESH_INSTALL_TESTING.md` for full manual flow and troubleshooting.

Tests and lint (unit tests live in `android/app/src/test/`; there are no instrumented tests):

```bash
cd android
./gradlew test          # JVM unit tests
./gradlew lint          # Android lint report → app/build/reports/lint-results-debug.html
```

Runtime permissions the app requests (`AndroidManifest.xml`): `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_CONNECTED_DEVICE`, `POST_NOTIFICATIONS` (plus legacy `BLUETOOTH`/`BLUETOOTH_ADMIN` below API 31). HID registration will silently fail without the Bluetooth grants.

macOS helper tools (optional, not needed to build the app; needs Xcode command line tools for IOKit/CoreFoundation):

```bash
make -C tools      # builds gamepad_bridge and print_reports
make -C tools clean
```

## Architectural Concerns (from Security & Performance Audit)

### 1. God Service Anti-Pattern
**File:** `GamepadService.kt` (430 lines)

**Concerns:**
- Single class manages: Bluetooth HID lifecycle, 125Hz transmission loop, gyroscope sensor fusion, keyboard/mouse input mapping, notification management, foreground service lifecycle
- Violates Single Responsibility Principle
- Hard to unit test in isolation (requires Android Context, Bluetooth stack, SensorManager)
- Changes to one domain (e.g., gyro logic) risk breaking unrelated domains (e.g., notification)

**Recommended Split:**
```
GamepadService (orchestrator only)
├── TransmissionEngine    — 125Hz loop, report scheduling, thread management
├── InputMapper           — GAMEPAD/KEYBOARD/MOUSE mapping logic
├── SensorFusion          — Gyroscope accumulation, drift correction
├── HidDeviceManager      — BluetoothGamepadService wrapper, connection state
└── NotificationManager   — Foreground service notification
```

---

### 2. macOS Bridge Runs as Root
**File:** `tools/gamepad_bridge.c` (requires `sudo`)

**Concerns:**
- `IOHIDUserDeviceCreateWithProperties` requires root privileges
- Bridge runs with full kernel-level HID injection capability
- Any vulnerability in bridge = kernel-level compromise on Mac
- No privilege dropping after device activation

**Mitigation Options:**
1. **LaunchAgent with entitlement** (macOS 13+): `com.apple.developer.iokit.hid` entitlement allows non-root `IOHIDUserDevice`
2. **SMAppService** (macOS 13+): System-managed privileged helper
3. **Drop privileges after activate**: Call `setuid(getuid())` after `IOHIDUserDeviceActivate()` — but may break subsequent IOKit calls
4. **Sandboxed helper**: Separate minimal helper binary with only IOKit access

**Priority:** High — this is the largest attack surface in the project.

---

### 3. No Integration Tests for Critical Path
**Affected:** Bluetooth HID registration → report transmission → macOS bridge → virtual device

**Concerns:**
- Zero automated tests for the core value proposition (phone → Mac gamepad)
- Manual testing only via `gamepad_tester.c` and `print_reports.c`
- Regression risk high for BT stack changes, Android version updates, macOS version updates
- No CI/CD pipeline for hardware-dependent tests

**Recommended:**
- Unit tests: `HidReportPacker` round-trip, `InputModel` bitmask/hat logic
- Robolectric tests: Mock `BluetoothHidDevice` for `BluetoothGamepadService` registration flow
- Instrumented tests: Local BT loopback (requires two devices or emulator with BT support)
- Mac bridge: Unit test HID descriptor parsing, virtual device creation (requires Mac runner)

---

### 4. StateFlow Propagation Duplication
**Pattern:** `BluetoothGamepadService` → `GamepadService` → `GamepadViewModel` → UI

**Current Flow:**
```
BluetoothGamepadService.connectionState (StateFlow)
    → GamepadService (observes, exposes via btService)
    → GamepadViewModel._connectionState (MutableStateFlow mirror)
    → MainActivity collects as State
    → Compose screens observe
```

**Concerns:**
- 4 layers of StateFlow mirroring for same data
- Each layer adds `collectLatest`/`collectAsState` coroutines
- `MainActivity.startStateCollection()` launches 3 separate coroutines for connectionState, connectedDeviceName, errorMessage
- Risk of stale data if any layer's flow emits before downstream collects

**Simplification:** Single source of truth in `BluetoothGamepadService` + `distinctUntilChanged()` in UI layer. Or use `SharedFlow` with replay=1 for connection events.

---

### 5. Gyroscope Logic Coupled to Transmission Loop
**Files:** `GamepadService.kt` lines 55-60, 164-176, 183-195, 292-311

**Concerns:**
- Gyro accumulation (`gyroAccumX/Y`) lives in `GamepadService` but consumed in two different modes:
  - GAMEPAD: Applied in `updateInputState()` → right stick offset
  - KEYBOARD: Applied in transmission loop → mouse movement
- Reset logic split: `unregisterGyroListener()` clears, but disconnect doesn't (bug P4)
- No separation between sensor reading, fusion algorithm, and consumption

**Recommended:** Extract `GyroProcessor` class:
```kotlin
class GyroProcessor {
    fun onSensorEvent(event: SensorEvent): Pair<Float, Float>  // returns delta X,Y
    fun reset(): Unit
    fun setEnabled(enabled: Boolean): Unit
}
```

---

### 6. Single Point of Failure: BluetoothGamepadService
**File:** `BluetoothGamepadService.kt` (330 lines)

**Concerns:**
- All HID Device profile logic in one class: registration, connection callbacks, report sending, SDP configuration
- No interface for testing — concrete `BluetoothHidDevice` usage throughout
- `sendReport()` synchronous, may block coroutine thread (P6)

**Recommended:** Extract `HidDeviceManager` interface:
```kotlin
interface HidDeviceManager {
    val connectionState: StateFlow<ConnectionState>
    fun initialize()
    fun sendReport(reportId: Int, data: ByteArray): Boolean
    fun shutdown()
}
```

---

### 7. Input Throttling Complexity
**File:** `GamepadViewModel.kt` lines 365-405 (35+ conditionals)

**Concerns:**
- Release detection via exhaustive per-field comparison
- 12 button checks + 4 stick axes + 2 triggers + dpad = 18 conditionals minimum
- Hard to verify correctness (missed transitions possible)
- CPU overhead on UI thread for every input event

**Planned in P5** (bitmask XOR + stick delta threshold, tasks.md task 8) — **not yet implemented**; verified against source 2026-11-02.

---

### 8. Coroutine Dispatcher Mismatch for Real-Time Loop
**File:** `GamepadService.kt` line 64, 256

**Concerns:**
- `Dispatchers.Default` = shared thread pool (CPU count threads)
- 125Hz loop competes with other coroutines (image loading, network, etc.)
- No priority guarantee — Android may throttle background coroutines
- `delay(8)` not precise — depends on dispatcher scheduling

**Planned in P3** (`HandlerThread(THREAD_PRIORITY_URGENT_AUDIO)`, tasks.md task 6) — **not yet implemented**; the loop still runs on `Dispatchers.Default` (verified against source 2026-11-02).

---

### 9. HID Report Allocation Hot Path
**File:** `HidReportPacker.kt` line 56

**Concerns:**
- `buffer.array().copyOf()` allocates 13-byte `ByteArray` 125×/sec = ~16 KB/s
- GC pressure on constrained Android devices
- `ByteBuffer` backing array may be larger than 13 bytes (position/limit issues)

**Planned in P1** (reused `ByteArray` + `System.arraycopy`, tasks.md task 4) — **not yet implemented**; line 56 still returns `buffer.array().copyOf()` (verified against source 2026-11-02).

---

### 10. Missing Device Allowlist / Trust Model
**Current:** Accepts any paired device via `connectToDevice()`

**Concerns:**
- Previously paired malicious device can reconnect and inject input
- No user visibility into which device is connected beyond name
- No revocation mechanism

**Partial fix in spec:** `security/bluetooth-hardening` capability includes allowlist foundation. UI deferred.

---

## Priority Ranking for Follow-Up Work

| Priority | Concern | Effort | Risk if Unaddressed |
|----------|---------|--------|---------------------|
| P0 | macOS bridge root requirement | 2-4 weeks | Kernel exploit on Mac |
| P1 | God Service refactor | 1-2 weeks | Bugs, untestable code |
| P2 | Integration test infrastructure | 2-3 weeks | Regression in core feature |
| P3 | StateFlow duplication cleanup | 3-5 days | Stale UI, subtle bugs |
| P4 | Gyro logic extraction | 2-3 days | Drift bugs, hard to modify |
| P5 | HidDeviceManager interface | 2-3 days | Untestable BT logic |
| P6 | Device allowlist UI | 1 week | Unauthorized device access |

---

## Notes for Future Agents

- **Do not** implement architectural refactors in the current change (`security-performance-hardening`) — they are out of scope
- **Do** reference these concerns when planning future changes
- **Do** consider extracting interfaces before adding new features to `GamepadService` or `BluetoothGamepadService`
- The `openspec/changes/security-performance-hardening/` directory contains the current approved change with full specs, design, and tasks
- **`tasks.md` checkboxes are unreliable**: several tasks (4.1-4.2, 6.1-6.5, 8.1-8.4) are marked `[x]` but the corresponding code does not exist. Verify against source before trusting completion state.