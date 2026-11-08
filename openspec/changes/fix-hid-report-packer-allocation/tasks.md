## 1. Fix HidReportPacker Allocation

- [ ] 1.1 Update `android/app/src/main/java/com/macgamepad/controller/bt/HidReportPacker.kt` `pack()` method to use `System.arraycopy` to pre-allocated `output` array and return `output` instead of `buffer.array().copyOf()`. Verify: method compiles and returns same array instance.
- [ ] 1.2 Verify the main branch already has `private val output = ByteArray(12)` field (line 29) and `System.arraycopy` logic (line 80) — if missing, add them. Verify: fields exist and are used correctly.

## 2. Add Zero-Allocation Test

- [ ] 2.1 Create `android/app/src/test/java/com/macgamepad/controller/bt/HidReportPackerTest.kt` if not exists. Verify: test file compiles.
- [ ] 2.2 Add test `pack returns same array instance (zero allocation)` that calls `packer.pack(state)` twice and uses `assertSame(report1, report2)`. Verify: test passes.
- [ ] 2.3 Add test `pack multiple times with different states returns same instance with updated content` that verifies reference equality AND content correctness. Verify: test passes.

## 3. Verification

- [ ] 3.1 Run unit tests: `cd android && ./gradlew test --tests "com.macgamepad.controller.bt.HidReportPackerTest"` — verify all tests pass.
- [ ] 3.2 Run full test suite: `cd android && ./gradlew test` — verify no regressions.
- [ ] 3.3 Build debug APK: `cd android && ./gradlew assembleDebug` — verify compiles successfully.