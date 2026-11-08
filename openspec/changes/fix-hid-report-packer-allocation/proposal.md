## Why

`HidReportPacker.pack()` at line 58 (in the security-performance-hardening worktree) and line 81 (in main branch) returns `buffer.array().copyOf()` which allocates a new 12-byte `ByteArray` on every call. At 125 Hz transmission rate, this creates ~16 KB/s of garbage collection pressure on constrained Android devices. The main branch version already has a pre-allocated `output` array and uses `System.arraycopy` but it's not in the main branch yet.

## What Changes

- Ensure `HidReportPacker` reuses a pre-allocated `ByteArray` output buffer instead of allocating on each `pack()` call
- Use `System.arraycopy` from the `ByteBuffer` backing array to the pre-allocated output
- Add unit test verifying zero-allocation behavior (same array instance returned)
- No externally observable behavior change — same report bytes, just no allocation

## Capabilities

### New Capabilities
- None

### Modified Capabilities
- `bt/hid-report-packing`: HID report packing for Bluetooth transmission — allocation behavior changed from allocating to zero-allocation

## Impact

**Affected Files:**
- `android/app/src/main/java/com/macgamepad/controller/bt/HidReportPacker.kt` — modify `pack()` to reuse output array
- `android/app/src/test/java/com/macgamepad/controller/bt/HidReportPackerTest.kt` — add test for zero-allocation behavior (or create if missing)

**Dependencies:** None. Pure Kotlin/JVM.