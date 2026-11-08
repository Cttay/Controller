## Context

See proposal.md - Why. The current main branch `HidReportPacker.pack()` (line 81) returns `buffer.array().copyOf()` which allocates a new 12-byte array each call. The security-performance-hardening worktree version (line 58) also uses `copyOf()`. However, the main branch already has a pre-allocated `output` array (line 29) and uses `System.arraycopy` (line 80) — but it's not in the main branch yet.

## Goals / Non-Goals

**Goals:**
- Modify `HidReportPacker.pack()` to reuse the pre-allocated `output` ByteArray
- Use `System.arraycopy` from buffer backing array to output array
- Return the same `output` array instance on every call (reference equality)
- Add test verifying zero-allocation behavior
- No change to report byte layout or content

**Non-Goals:**
- Change HID report format (W3C Gamepad descriptor)
- Modify `InputState` or conversion functions
- Add new dependencies

## Decisions

### 1. Keep existing buffer + output field structure
**Current main branch:** Already has `private val buffer = ByteBuffer.allocate(12)` and `private val output = ByteArray(12)`
**Action:** Use the existing pattern — `buffer.clear()`, write to buffer, `System.arraycopy(backing, offset, output, 0, 12)`, return `output`

### 2. Return the same `output` array instance
**Rationale:** Caller (GamepadService) passes this directly to `BluetoothHidDevice.sendReport()` which copies internally. Reference equality is safe and verifiable in tests.

### 3. Test strategy: `assertSame(packer.pack(state1), packer.pack(state2))`
**Rationale:** Directly verifies zero-allocation. Content verified by existing `assertArrayEquals`.

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| Caller mutates returned array | Document in KDoc: "caller must not mutate"; `BluetoothHidDevice.sendReport` copies anyway |
| `ByteBuffer.array()` returns larger array than 12 bytes | Use `buffer.arrayOffset()` and copy exactly 12 bytes (already done in main branch line 80) |
| Thread safety | `HidReportPacker` is single-threaded (called from transmission loop); no synchronization needed |