package com.macgamepad.controller.bt

/**
 * HID Device operating mode — determines SDP configuration and report format.
 */
enum class Mode {
    /** Gamepad mode: 14-byte report with buttons, sticks, triggers, D-pad */
    GAMEPAD,
    /** Keyboard mode: raw keyboard HID reports */
    KEYBOARD,
    /** Mouse mode: raw mouse HID reports */
    MOUSE
}