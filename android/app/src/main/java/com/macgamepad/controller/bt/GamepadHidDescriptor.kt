package com.macgamepad.controller.bt

/**
 * Gamepad-Only HID Report Descriptor (Report ID 1).
 *
 * Designed to match W3C "standard" gamepad mapping for macOS/Chrome detection.
 * No Keyboard/Mouse collections - pure Gamepad to avoid macOS composite device confusion.
 *
 * Gamepad report (14 bytes, excluding Report ID):
 *   Byte 0-1:   Left Stick X   (int16 LE)
 *   Byte 2-3:   Left Stick Y   (int16 LE)
 *   Byte 4-5:   Right Stick X  (int16 LE)
 *   Byte 6-7:   Right Stick Y  (int16 LE)
 *   Byte 8:     Hat Switch (4-bit, 0-7 for directions, 0xF for centered)
 *   Byte 9:     Padding (4 bits)
 *   Byte 10-11: Buttons 1-16 (A,B,X,Y,LB,RB,LT,RT,Back,Start,L3,R3,Guide,DPadU,D,L,R)
 *   Byte 12:    Left Trigger (Z) - uint8, 0-255
 *   Byte 13:    Right Trigger (Rz) - uint8, 0-255
 *
 * Button mapping matches W3C "standard" gamepad layout:
 *   0: A, 1: B, 2: X, 3: Y, 4: LB, 5: RB, 6: LT, 7: RT,
 *   8: Back/Select, 9: Start, 10: L3, 11: R3, 12: Guide/Home,
 *   13: DPadUp, 14: DPadDown, 15: DPadLeft, 16: DPadRight
 */
object GamepadHidDescriptor {

    const val GAMEPAD_REPORT_ID: Byte = 0x01

    // Gamepad report size (excluding Report ID) - 14 bytes to match HidReportPacker
    const val GAMEPAD_REPORT_SIZE = 14

    val descriptor: ByteArray = byteArrayOf(
        // ============================================================
        // GAMEPAD (Report ID 1) — ONLY COLLECTION for macOS detection
        // Matches the macOS bridge descriptor exactly
        // ============================================================
        0x05, 0x01,             // USAGE_PAGE (Generic Desktop)
        0x09, 0x05,             // USAGE (Game Pad)
        0xA1.toByte(), 0x01,    // COLLECTION (Application)
        0x85.toByte(), GAMEPAD_REPORT_ID, // REPORT_ID (1)

        // Left Stick (X, Y)
        0x05, 0x01,             //   USAGE_PAGE (Generic Desktop)
        0x09, 0x30,             //   USAGE (X)
        0x09, 0x31,             //   USAGE (Y)
        0x16, 0x01, 0x80.toByte(), //   LOGICAL_MINIMUM (-32767)
        0x26, 0xFF.toByte(), 0x7F, //   LOGICAL_MAXIMUM (32767)
        0x75, 0x10,             //   REPORT_SIZE (16)
        0x95.toByte(), 0x02,    //   REPORT_COUNT (2)
        0x81.toByte(), 0x02,    //   INPUT (Data, Variable, Absolute)

        // Right Stick (Rx, Ry)
        0x09, 0x33,             //   USAGE (Rx)
        0x09, 0x34,             //   USAGE (Ry)
        0x16, 0x01, 0x80.toByte(), //   LOGICAL_MINIMUM (-32767)
        0x26, 0xFF.toByte(), 0x7F, //   LOGICAL_MAXIMUM (32767)
        0x75, 0x10,             //   REPORT_SIZE (16)
        0x95.toByte(), 0x02,    //   REPORT_COUNT (2)
        0x81.toByte(), 0x02,    //   INPUT (Data, Variable, Absolute)

        // Hat Switch (D-Pad) — 4-bit, 8 directions + null
        0x05, 0x01,             //   USAGE_PAGE (Generic Desktop)
        0x09, 0x39,             //   USAGE (Hat Switch)
        0x15, 0x00,             //   LOGICAL_MINIMUM (0)
        0x25, 0x07,             //   LOGICAL_MAXIMUM (7)
        0x35, 0x00,             //   PHYSICAL_MINIMUM (0)
        0x46, 0x3B, 0x01,       //   PHYSICAL_MAXIMUM (315 degrees)
        0x65, 0x14,             //   UNIT (Degrees)
        0x75, 0x04,             //   REPORT_SIZE (4)
        0x95.toByte(), 0x01,    //   REPORT_COUNT (1)
        0x81.toByte(), 0x42,    //   INPUT (Data, Variable, Absolute, Null State)

        // 4-bit padding after D-pad
        0x75, 0x04,             //   REPORT_SIZE (4)
        0x95.toByte(), 0x01,    //   REPORT_COUNT (1)
        0x81.toByte(), 0x03,    //   INPUT (Const, Var, Abs)

        // Buttons — 16 buttons (2 bytes)
        0x05, 0x09,             //   USAGE_PAGE (Button)
        0x19, 0x01,             //   USAGE_MINIMUM (Button 1)
        0x29, 0x10,             //   USAGE_MAXIMUM (Button 16)
        0x15, 0x00,             //   LOGICAL_MINIMUM (0)
        0x25, 0x01,             //   LOGICAL_MAXIMUM (1)
        0x75, 0x01,             //   REPORT_SIZE (1)
        0x95.toByte(), 0x10,    //   REPORT_COUNT (16)
        0x81.toByte(), 0x02,    //   INPUT (Data, Var, Abs)

        // Triggers — Z (LT) and Rz (RT), 8-bit unsigned
        0x05, 0x01,             //   USAGE_PAGE (Generic Desktop)
        0x09, 0x32,             //   USAGE (Z)
        0x09, 0x35,             //   USAGE (Rz)
        0x15, 0x00,             //   LOGICAL_MINIMUM (0)
        0x26, 0xFF.toByte(), 0x00, //   LOGICAL_MAXIMUM (255)
        0x75, 0x08,             //   REPORT_SIZE (8)
        0x95.toByte(), 0x02,    //   REPORT_COUNT (2)
        0x81.toByte(), 0x02,    //   INPUT (Data, Var, Abs)

        0xC0.toByte(),           // END_COLLECTION
    )
}