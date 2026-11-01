package com.macgamepad.controller.bt

import com.macgamepad.controller.input.InputState
import android.util.Log

public object KeyboardMapper {
    const val MAX_KEYS = 6

    fun keysFor(state: InputState, gyroEnabled: Boolean): List<Byte> {
        val keys = mutableListOf<Byte>()
        // Map Left stick to WASD digital zones
        if (state.leftStickY < -0.4f) keys.add(KeyboardHidDescriptor.Keycodes.W)
        if (state.leftStickY > 0.4f) keys.add(KeyboardHidDescriptor.Keycodes.S)
        if (state.leftStickX < -0.4f) keys.add(KeyboardHidDescriptor.Keycodes.A)
        if (state.leftStickX > 0.4f) keys.add(KeyboardHidDescriptor.Keycodes.D)

        // If gyroscope is disabled, map Right Stick to I/K/J/L keys for native camera/aiming look
        if (!gyroEnabled) {
            if (state.rightStickY < -0.4f) keys.add(KeyboardHidDescriptor.Keycodes.I)
            if (state.rightStickY > 0.4f) keys.add(KeyboardHidDescriptor.Keycodes.K)
            if (state.rightStickX < -0.4f) keys.add(KeyboardHidDescriptor.Keycodes.J)
            if (state.rightStickX > 0.4f) keys.add(KeyboardHidDescriptor.Keycodes.L)
        }

        // Map D-pad to Arrow Keys
        if (state.dpadUp) keys.add(KeyboardHidDescriptor.Keycodes.UP)
        if (state.dpadDown) keys.add(KeyboardHidDescriptor.Keycodes.DOWN)
        if (state.dpadLeft) keys.add(KeyboardHidDescriptor.Keycodes.LEFT)
        if (state.dpadRight) keys.add(KeyboardHidDescriptor.Keycodes.RIGHT)

        // Map Face Buttons to Ryujinx/Astris default keys
        if (state.buttonA) keys.add(KeyboardHidDescriptor.Keycodes.Z)       // Switch A -> Keyboard Z
        if (state.buttonB) keys.add(KeyboardHidDescriptor.Keycodes.X)       // Switch B -> Keyboard X
        if (state.buttonX) keys.add(KeyboardHidDescriptor.Keycodes.C)       // Switch X -> Keyboard C
        if (state.buttonY) keys.add(KeyboardHidDescriptor.Keycodes.V)       // Switch Y -> Keyboard V

        // Map shoulder bumpers
        if (state.buttonL1) keys.add(KeyboardHidDescriptor.Keycodes.E)      // L -> Keyboard E
        if (state.buttonR1) keys.add(KeyboardHidDescriptor.Keycodes.U)      // R -> Keyboard U

        // Map analog triggers (digital mapping threshold in Keyboard Mode)
        if (state.leftTrigger > 0.4f) keys.add(KeyboardHidDescriptor.Keycodes.Q)   // ZL -> Keyboard Q
        if (state.rightTrigger > 0.4f) keys.add(KeyboardHidDescriptor.Keycodes.O)  // ZR -> Keyboard O

        // Map L3/R3 clicks
        if (state.buttonL3) keys.add(KeyboardHidDescriptor.Keycodes.F)      // L3 -> Keyboard F
        if (state.buttonR3) keys.add(KeyboardHidDescriptor.Keycodes.H)      // R3 -> Keyboard H

        // Map System buttons (+ / -)
        if (state.buttonStart) keys.add(KeyboardHidDescriptor.Keycodes.EQUALS)  // Start (+) -> Keyboard =
        if (state.buttonSelect) keys.add(KeyboardHidDescriptor.Keycodes.MINUS)  // Select (-) -> Keyboard -

        if (keys.size > MAX_KEYS) {
            Log.w("KeyboardMapper", "key overflow: ${keys.size} keys, HID supports $MAX_KEYS")
        }

        return keys
    }
}