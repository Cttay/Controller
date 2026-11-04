package com.macgamepad.controller.bt

/**
 * Gamepad-Only HID Report Descriptor (Report ID 1).
 *
 * Designed to match W3C "standard" gamepad mapping for macOS/Chrome detection.
 * No Keyboard/Mouse collections - pure Gamepad to avoid macOS composite device confusion.
 *
 * Gamepad report (12 bytes, excluding Report ID):
 *   Byte 0-1:   Left Stick X   (int16 LE)
 *   Byte 2-3:   Left Stick Y   (int16 LE)
 *   Byte 4-5:   Right Stick X  (int16 LE)
 *   Byte 6-7:   Right Stick Y  (int16 LE)
 *   Byte 8:     Buttons 1-6 (A,B,X,Y,LB,RB) in bits 0-5, bits 6-7 = padding
 *   Byte 9:     Left Trigger  (uint8, 0-255) — Button 7
 *   Byte 10:    Right Trigger (uint8, 0-255) — Button 8
 *   Byte 11:    Buttons 9-17 (Back,Start,L3,R3,DPadU,D,D,L,R,Guide) in bits 0-8, bits 9-15 = padding
 *
 * Button mapping matches W3C "standard" gamepad layout:
 *   0: A, 1: B, 2: X, 3: Y, 4: LB, 5: RB, 6: LT, 7: RT,
 *   8: Back/Select, 9: Start, 10: L3, 11: R3,
 *   12: DPadUp, 13: DPadDown, 14: DPadLeft, 15: DPadRight, 16: Guide/Home
 */
object GamepadHidDescriptor {

    const val GAMEPAD_REPORT_ID: Byte = 0x01

    // Gamepad report size (excluding Report ID)
    const val GAMEPAD_REPORT_SIZE = 12

    val descriptor: ByteArray = byteArrayOf(
        // ============================================================
        // GAMEPAD (Report ID 1) — ONLY COLLECTION for macOS detection
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

        // Buttons 1-6: A, B, X, Y, LB, RB (1 bit each)
        0x05, 0x09,             //   USAGE_PAGE (Button)
        0x19, 0x01,             //   USAGE_MINIMUM (Button 1)
        0x29, 0x06,             //   USAGE_MAXIMUM (Button 6)
        0x15, 0x00,             //   LOGICAL_MINIMUM (0)
        0x25, 0x01,             //   LOGICAL_MAXIMUM (1)
        0x75, 0x01,             //   REPORT_SIZE (1)
        0x95.toByte(), 0x06,    //   REPORT_COUNT (6)
        0x81.toByte(), 0x02,    //   INPUT (Data, Variable, Absolute)

        // Padding (2 bits) to byte-align
        0x75, 0x02,             //   REPORT_SIZE (2)
        0x95.toByte(), 0x01,    //   REPORT_COUNT (1)
        0x81.toByte(), 0x03,    //   INPUT (Constant, Variable, Absolute)

        // Buttons 7-8: LT, RT as 8-bit analog values (0-255)
        0x05, 0x09,             //   USAGE_PAGE (Button)
        0x19, 0x07,             //   USAGE_MINIMUM (Button 7)
        0x29, 0x08,             //   USAGE_MAXIMUM (Button 8)
        0x15, 0x00,             //   LOGICAL_MINIMUM (0)
        0x26, 0xFF.toByte(), 0x00, //   LOGICAL_MAXIMUM (255)
        0x75, 0x08,             //   REPORT_SIZE (8)
        0x95.toByte(), 0x02,    //   REPORT_COUNT (2)
        0x81.toByte(), 0x02,    //   INPUT (Data, Variable, Absolute)

        // Buttons 9-17: Back, Start, L3, R3, DPadUp, DPadDown, DPadLeft, DPadRight, Guide (1 bit each)
        0x05, 0x09,             //   USAGE_PAGE (Button)
        0x19, 0x09,             //   USAGE_MINIMUM (Button 9)
        0x29, 0x11,             //   USAGE_MAXIMUM (Button 17)
        0x15, 0x00,             //   LOGICAL_MINIMUM (0)
        0x25, 0x01,             //   LOGICAL_MAXIMUM (1)
        0x75, 0x01,             //   REPORT_SIZE (1)
        0x95.toByte(), 0x09,    //   REPORT_COUNT (9)
        0x81.toByte(), 0x02,    //   INPUT (Data, Variable, Absolute)

        // Padding (7 bits) to byte-align
        0x75, 0x07,             //   REPORT_SIZE (7)
        0x95.toByte(), 0x01,    //   REPORT_COUNT (1)
        0x81.toByte(), 0x03,    //   INPUT (Constant, Variable, Absolute)

        0xC0.toByte(),           // END_COLLECTION
    )
}