## 1. Descriptor & Packer Updates (Atomic Core Changes)

- [ ] 1.1 Update `KeyboardHidDescriptor.kt` Gamepad collection to Xbox 360 layout — verify descriptor byte array matches design spec (button order, Hat Switch only D-Pad, triggers at bytes 12-13)
- [ ] 1.2 Update `HidReportPacker.kt` button bit assignments to match new Xbox 360 button order — verify `pack()` produces correct 14-byte report
- [ ] 1.3 Update `InputModel.kt` `buttonBitmask()` to use new button bit positions — verify unit tests pass
- [ ] 1.4 Update `BluetoothGamepadService.kt` `registerHidApp()` to use `KeyboardHidDescriptor.descriptor` — verify build compiles
- [ ] 1.5 Update `BluetoothGamepadService.kt` `onGetReport()` return size from 13 to 14 bytes — verify build compiles
- [ ] 1.6 Update `BluetoothGamepadService.kt` `sendGamepadReport()` to import constants from `KeyboardHidDescriptor` — verify build compiles

## 2. Mac Bridge Descriptor Sync

- [ ] 2.1 Update `tools/gamepad_bridge.c` `hid_descriptor` Gamepad collection (bytes 0-97) to match new Android descriptor exactly — verify byte-for-byte match with `KeyboardHidDescriptor.kt` Gamepad collection
- [ ] 2.2 Build Mac bridge (`make -C tools`) — verify compiles without errors
- [ ] 2.3 Verify bridge descriptor size unchanged (Keyboard/Mouse collections unchanged)

## 3. Cleanup

- [ ] 3.1 Delete `GamepadHidDescriptor.kt` — verify no remaining references in codebase (grep for "GamepadHidDescriptor")
- [ ] 3.2 Fix any broken imports after deletion — verify build passes (`./gradlew assembleDebug`)

## 4. Unit Tests (Robolectric & Pure Kotlin)

- [ ] 4.1 Create `HidReportPackerTest.kt` — test `pack()` round-trip, button bit positions, stick encoding, trigger encoding, Hat Switch encoding — verify `./gradlew test` passes
- [ ] 4.2 Create `KeyboardHidDescriptorTest.kt` — test descriptor parsing, GAMEPAD_REPORT_SIZE=14, GAMEPAD_REPORT_ID=1, KEYBOARD_REPORT_ID=2, MOUSE_REPORT_ID=3 — verify `./gradlew test` passes
- [ ] 4.3 Create `BluetoothGamepadServiceRegistrationTest.kt` (Robolectric) — mock `BluetoothHidDevice`, test `registerApp()` success path, `sendReport()` with Report IDs 1/2/3, callback handling — verify `./gradlew test` passes
- [ ] 4.4 Run all unit tests — verify `./gradlew test` passes with 100% success

## 5. Integration Verification (Physical Device)

- [ ] 5.1 Build & install debug APK (`ANDROID_HOME=... ./gradlew installDebug`) — verify installs on PJX110
- [ ] 5.2 Run Mac bridge (`sudo make -C tools && sudo ./tools/gamepad_bridge`) — verify bridge starts and listens
- [ ] 5.3 Test Gamepad mode: connect, verify gamepad-tester.com shows correct button labels (A/B/X/Y/LB/RB/Back/Start/L3/R3/Guide) and D-Pad via Hat Switch — verify all 16 buttons + D-Pad work
- [ ] 5.4 Test Keyboard mode: switch to Keyboard, open Notes.app, press virtual face buttons — verify characters appear
- [ ] 5.5 Test Mouse mode: switch to Mouse, move right stick — verify cursor moves; press face button — verify click
- [ ] 5.6 Test mode switching: rapid Gamepad↔Keyboard↔Mouse switches — verify no disconnect, connection state remains CONNECTED

## 6. Documentation

- [ ] 6.1 Update `docs/ARCHITECTURE_REVIEW.md` — mark P0-1 through P0-4 as implemented, update status
- [ ] 6.2 Update `README.md` — add "Platform Limitations" section documenting: no background HID persistence, no auto-reconnect on app restart, requires foreground app (Classic BT HID Device profile limitation)