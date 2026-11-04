package com.macgamepad.controller.bt

import com.macgamepad.controller.input.InputState
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Packs an [InputState] into a 12-byte HID input report matching our descriptor.
 *
 * Report layout (12 bytes, excluding report ID):
 *   Byte 0-1:   Left Stick X   (int16 LE)
 *   Byte 2-3:   Left Stick Y   (int16 LE)
 *   Byte 4-5:   Right Stick X  (int16 LE)
 *   Byte 6-7:   Right Stick Y  (int16 LE)
 *   Byte 8:     Buttons 1-6 (A,B,X,Y,LB,RB) in bits 0-5, bits 6-7 = padding
 *   Byte 9:     Left Trigger  (uint8, 0-255) — Button 7
 *   Byte 10:    Right Trigger (uint8, 0-255) — Button 8
 *   Byte 11:    Buttons 9-17 (Back,Start,L3,R3,DPadU,D,D,L,R,Guide) in bits 0-8, bits 9-15 = padding
 *
 * This class is designed for zero-allocation hot-path usage:
 * it reuses a single ByteBuffer and ByteArray across calls.
 */
class HidReportPacker {

    // Pre-allocated buffer — reused every frame to avoid GC pressure
    private val buffer = ByteBuffer.allocate(12)
        .order(ByteOrder.LITTLE_ENDIAN)
    // Reused output array - eliminates 125 allocations/sec
    private val output = ByteArray(12)

    /**
     * Pack the current input state into a byte array suitable for
     * [android.bluetooth.BluetoothHidDevice.sendReport].
     *
     * @param state The current controller input state
     * @return A 12-byte array containing the packed HID report (reused instance, caller must not mutate)
     */
    fun pack(state: InputState): ByteArray {
        buffer.clear()

        // Sticks: convert -1.0..1.0 float to -32767..32767 int16
        buffer.putShort(floatToAxis16(state.leftStickX))
        buffer.putShort(floatToAxis16(state.leftStickY))
        buffer.putShort(floatToAxis16(state.rightStickX))
        buffer.putShort(floatToAxis16(state.rightStickY))

        // Byte 8: Buttons 1-6 (A,B,X,Y,LB,RB) in bits 0-5
        var byte8 = 0
        if (state.buttonA) byte8 = byte8 or (1 shl 0)      // Button 1: A
        if (state.buttonB) byte8 = byte8 or (1 shl 1)      // Button 2: B
        if (state.buttonX) byte8 = byte8 or (1 shl 2)      // Button 3: X
        if (state.buttonY) byte8 = byte8 or (1 shl 3)      // Button 4: Y
        if (state.buttonL1) byte8 = byte8 or (1 shl 4)     // Button 5: LB
        if (state.buttonR1) byte8 = byte8 or (1 shl 5)     // Button 6: RB
        // bits 6-7 = padding (0)
        buffer.put(byte8.toByte())

        // Byte 9: Left Trigger (Button 7) - 8-bit analog 0-255
        buffer.put(floatToTrigger8(state.leftTrigger))

        // Byte 10: Right Trigger (Button 8) - 8-bit analog 0-255
        buffer.put(floatToTrigger8(state.rightTrigger))

        // Byte 11: Buttons 9-17 in bits 0-8
        var byte11 = 0
        if (state.buttonSelect) byte11 = byte11 or (1 shl 0)   // Button 9: Back/Select
        if (state.buttonStart) byte11 = byte11 or (1 shl 1)    // Button 10: Start
        if (state.buttonL3) byte11 = byte11 or (1 shl 2)       // Button 11: L3
        if (state.buttonR3) byte11 = byte11 or (1 shl 3)       // Button 12: R3
        if (state.dpadUp) byte11 = byte11 or (1 shl 4)         // Button 13: DPadUp
        if (state.dpadDown) byte11 = byte11 or (1 shl 5)       // Button 14: DPadDown
        if (state.dpadLeft) byte11 = byte11 or (1 shl 6)       // Button 15: DPadLeft
        if (state.dpadRight) byte11 = byte11 or (1 shl 7)      // Button 16: DPadRight
        if (state.buttonHome) byte11 = byte11 or (1 shl 8)     // Button 17: Guide/Home
        // bits 9-15 = padding (0)
        buffer.put(byte11.toByte())

        // Zero-copy: copy from buffer backing array to pre-allocated output
        val backing = buffer.array()
        System.arraycopy(backing, buffer.arrayOffset(), output, 0, 12)
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