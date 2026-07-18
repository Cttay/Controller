package com.macgamepad.controller.input

/**
 * Universal input state — the canonical representation of all controller inputs.
 *
 * Every controller profile (PS5, Xbox, Switch, generic) maps its touch events
 * into this same model. The BT HID service reads from this to build HID reports.
 *
 * All values are normalized:
 * - Sticks: Float -1.0 to 1.0 (0.0 = centered)
 * - Triggers: Float 0.0 to 1.0 (0.0 = released, 1.0 = fully pressed)
 * - Buttons: Boolean
 * - D-pad: Enum direction
 */
data class InputState(
    // Left analog stick
    val leftStickX: Float = 0f,
    val leftStickY: Float = 0f,

    // Right analog stick
    val rightStickX: Float = 0f,
    val rightStickY: Float = 0f,

    // D-pad
    val dpadUp: Boolean = false,
    val dpadDown: Boolean = false,
    val dpadLeft: Boolean = false,
    val dpadRight: Boolean = false,

    // Face buttons
    val buttonA: Boolean = false,
    val buttonB: Boolean = false,
    val buttonX: Boolean = false,
    val buttonY: Boolean = false,

    // Shoulder buttons
    val buttonL1: Boolean = false,
    val buttonR1: Boolean = false,

    // Triggers (analog)
    val leftTrigger: Float = 0f,
    val rightTrigger: Float = 0f,

    // Stick clicks
    val buttonL3: Boolean = false,
    val buttonR3: Boolean = false,

    // System buttons
    val buttonStart: Boolean = false,
    val buttonSelect: Boolean = false,
    val buttonHome: Boolean = false,
    val buttonCapture: Boolean = false,
) {
    /**
     * Computes the hat switch value from D-pad booleans.
     * Returns the HID hat direction (0-7) or 0x0F for centered.
     */
    fun hatDirection(): Int {
        val up = dpadUp
        val down = dpadDown
        val left = dpadLeft
        val right = dpadRight

        return when {
            up && right -> 1    // UP_RIGHT
            up && left -> 7     // UP_LEFT
            down && right -> 3  // DOWN_RIGHT
            down && left -> 5   // DOWN_LEFT
            up -> 0             // UP
            right -> 2          // RIGHT
            down -> 4           // DOWN
            left -> 6           // LEFT
            else -> 0x0F        // CENTERED (null state)
        }
    }

    /**
     * Packs all 16 buttons into a 16-bit bitmask.
     * Bit 0 = Button 1 (A), Bit 15 = Button 16.
     */
    fun buttonBitmask(): Int {
        var mask = 0
        if (buttonA) mask = mask or (1 shl 0)
        if (buttonB) mask = mask or (1 shl 1)
        if (buttonX) mask = mask or (1 shl 2)
        if (buttonY) mask = mask or (1 shl 3)
        if (buttonL1) mask = mask or (1 shl 4)
        if (buttonR1) mask = mask or (1 shl 5)
        if (buttonSelect) mask = mask or (1 shl 6)
        if (buttonStart) mask = mask or (1 shl 7)
        if (buttonHome) mask = mask or (1 shl 8)
        if (buttonL3) mask = mask or (1 shl 9)
        if (buttonR3) mask = mask or (1 shl 10)
        if (buttonCapture) mask = mask or (1 shl 11)
        return mask
    }
}
