package com.macgamepad.controller.bt

import com.macgamepad.controller.input.InputState
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Packs an [InputState] into a 14-byte HID input report matching the macOS bridge descriptor.
 *
 * Report layout (14 bytes, excluding report ID):
 *   Byte 0-1:   Left Stick X   (int16 LE)
 *   Byte 2-3:   Left Stick Y   (int16 LE)
 *   Byte 4-5:   Right Stick X  (int16 LE)
 *   Byte 6-7:   Right Stick Y  (int16 LE)
 *   Byte 8:     Hat Switch (4-bit, 0-7 + 0xF for null)
 *   Byte 9:     Padding (4-bit) - high nibble of byte 9
 *   Byte 10-11: Buttons 1-16 (A,B,X,Y,LB,RB,LT,RT,Back,Start,L3,R3,Guide,DPadU,D,L,R)
 *   Byte 12:    Left Trigger (Z) - uint8, 0-255
 *   Byte 13:    Right Trigger (Rz) - uint8, 0-255
 *
 * Button mapping matches W3C "standard" gamepad layout:
 *   0: A, 1: B, 2: X, 3: Y, 4: LB, 5: RB, 6: LT, 7: RT,
 *   8: Back/Select, 9: Start, 10: L3, 11: R3, 12: Guide/Home,
 *   13: DPadUp, 14: DPadDown, 15: DPadLeft, 16: DPadRight
 *
 * This class is designed for zero-allocation hot-path usage:
 * it reuses a single ByteBuffer and ByteArray across calls.
 */
class HidReportPacker {

    // Pre-allocated buffer — reused every frame to avoid GC pressure
    private val buffer = ByteBuffer.allocate(14)
        .order(ByteOrder.LITTLE_ENDIAN)
    // Reused output array - eliminates 125 allocations/sec
    private val output = ByteArray(14)

    /**
     * Pack the current input state into a byte array suitable for
     * [android.bluetooth.BluetoothHidDevice.sendReport].
     *
     * @param state The current controller input state
     * @return A 14-byte array containing the packed HID report (reused instance, caller must not mutate)
     */
    fun pack(state: InputState): ByteArray {
        buffer.clear()

        // Sticks: convert -1.0..1.0 float to -32767..32767 int16
        buffer.putShort(floatToAxis16(state.leftStickX))
        buffer.putShort(floatToAxis16(state.leftStickY))
        buffer.putShort(floatToAxis16(state.rightStickX))
        buffer.putShort(floatToAxis16(state.rightStickY))

        // Byte 8: Hat Switch (4-bit, 0-7 for directions, 0xF for centered)
        val hat = state.hatDirection()
        // Byte 9: Padding (4 bits) in high nibble
        buffer.put(hat.toByte())
        buffer.put(0x00.toByte())

        // Byte 10-11: 16 buttons (2 bytes, little-endian)
        var buttons = 0  // 16 buttons packed into single 16-bit value

        // Button 1: A (bit 0)
        if (state.buttonA) buttons = buttons or (1 shl 0)
        // Button 2: B (bit 1)
        if (state.buttonB) buttons = buttons or (1 shl 1)
        // Button 3: X (bit 2)
        if (state.buttonX) buttons = buttons or (1 shl 2)
        // Button 4: Y (bit 3)
        if (state.buttonY) buttons = buttons or (1 shl 3)
        // Button 5: LB (bit 4)
        if (state.buttonL1) buttons = buttons or (1 shl 4)
        // Button 6: RB (bit 5)
        if (state.buttonR1) buttons = buttons or (1 shl 5)
        // Button 7: LT (treated as digital button 7 when pressed) (bit 6)
        if (state.leftTrigger > 0.5f) buttons = buttons or (1 shl 6)
        // Button 8: RT (treated as digital button 8 when pressed) (bit 7)
        if (state.rightTrigger > 0.5f) buttons = buttons or (1 shl 7)
        // Button 9: Back/Select (bit 8)
        if (state.buttonSelect) buttons = buttons or (1 shl 8)
        // Button 10: Start (bit 9)
        if (state.buttonStart) buttons = buttons or (1 shl 9)
        // Button 11: L3 (bit 10)
        if (state.buttonL3) buttons = buttons or (1 shl 10)
        // Button 12: R3 (bit 11)
        if (state.buttonR3) buttons = buttons or (1 shl 11)
        // Button 13: Guide/Home (bit 12)
        if (state.buttonHome) buttons = buttons or (1 shl 12)
        // Button 14: DPadUp (bit 13)
        if (state.dpadUp) buttons = buttons or (1 shl 13)
        // Button 15: DPadDown (bit 14)
        if (state.dpadDown) buttons = buttons or (1 shl 14)
        // Button 16: DPadLeft (bit 15)
        if (state.dpadLeft) buttons = buttons or (1 shl 15)

        buffer.putShort(buttons.toShort())

        // Byte 12: Left Trigger (Z) - uint8, 0-255
        buffer.put(floatToTrigger8(state.leftTrigger))
        // Byte 13: Right Trigger (Rz) - uint8, 0-255
        buffer.put(floatToTrigger8(state.rightTrigger))

        // Zero-copy: copy from buffer backing array to pre-allocated output
        val backing = buffer.array()
        System.arraycopy(backing, buffer.arrayOffset(), output, 0, 14)
        return output
    }

    companion object {
        /**
         * Convert a normalized float (-1.0 to 1.0) to a 16-bit signed axis value.
         * Clamps to [-32767, 32767] — we avoid -32768 because some HID parsers
         * treat it as a special value.
         */
        fun floatToAxis16(value: Float): Short {
            val clamped = value.coerceIn(-1f, 1f)
            return (clamped * 32767f).toInt().toShort()
        }

        /**
         * Convert a normalized float (0.0 to 1.0) to an 8-bit unsigned trigger value.
         */
        fun floatToTrigger8(value: Float): Byte {
            val clamped = value.coerceIn(0f, 1f)
            return (clamped * 255f).toInt().toByte()
        }
    }
}