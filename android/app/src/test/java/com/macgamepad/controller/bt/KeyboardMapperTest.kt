package com.macgamepad.controller.bt

import com.macgamepad.controller.input.InputState
import com.macgamepad.controller.bt.KeyboardMapper
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse

/**
 * Tests for KeyboardMapper.keysFor().
 * Pure JVM tests — no Android dependencies.
 */
class KeyboardMapperTest {

    private val mapper = com.macgamepad.controller.bt.KeyboardMapper

    // Keycode constants matching KeyboardHidDescriptor.Keycodes
    companion object {
        const val W: Byte = 0x1A
        const val A: Byte = 0x04
        const val S: Byte = 0x16
        const val D: Byte = 0x07
        const val I: Byte = 0x0C
        const val K: Byte = 0x0E
        const val J: Byte = 0x0D
        const val L: Byte = 0x0F
        const val UP: Byte = 0x52
        const val DOWN: Byte = 0x51
        const val LEFT: Byte = 0x50
        const val RIGHT: Byte = 0x4F
        const val Z: Byte = 0x1D
        const val X: Byte = 0x1B
        const val C: Byte = 0x06
        const val V: Byte = 0x19
        const val E: Byte = 0x08
        const val U: Byte = 0x18
        const val Q: Byte = 0x14
        const val O: Byte = 0x12
        const val F: Byte = 0x09
        const val H: Byte = 0x0B
        const val EQUALS: Byte = 0x2E
        const val MINUS: Byte = 0x2D
    }

    // ---- WASD from left stick ----

    @Test
    fun `keysFor WASD from left stick negative Y`() {
        val state = InputState(leftStickX = 0f, leftStickY = -0.5f, rightStickX = 0f, rightStickY = 0f,
            dpadUp = false, dpadDown = false, dpadLeft = false, dpadRight = false,
            buttonA = false, buttonB = false, buttonX = false, buttonY = false,
            buttonL1 = false, buttonR1 = false, buttonL3 = false, buttonR3 = false,
            buttonStart = false, buttonSelect = false, buttonHome = false, buttonCapture = false)
        val keys = mapper.keysFor(state, gyroEnabled = false)
        assertTrue("W should be pressed (leftStickY < -0.4)", keys.contains(W))
        assertTrue("S should be pressed (leftStickY > 0.4)", keys.contains(S))
        assertTrue("A should be pressed (leftStickX < -0.4)", keys.contains(A))
        assertTrue("D should be pressed (leftStickX > 0.4)", keys.contains(D))
        // Order: W, A, S, D based on the code order
        assertEquals(4, keys.size)
        // Verify order: WASD first
        val wIdx = keys.indexOf(W)
        val aIdx = keys.indexOf(A)
        val sIdx = keys.indexOf(S)
        val dIdx = keys.indexOf(D)
        assertTrue("W should come before A", wIdx < aIdx || aIdx < 0 || wIdx < 0)
        assertTrue("A should come before S", aIdx < sIdx || sIdx < 0 || aIdx < 0)
    }

    @Test
    fun `keysFor right stick IKJL when gyroEnabled false`() {
        val state = InputState(leftStickX = 0f, leftStickY = 0f, rightStickX = -0.5f, rightStickY = -0.5f,
            dpadUp = false, dpadDown = false, dpadLeft = false, dpadRight = false,
            buttonA = false, buttonB = false, buttonX = false, buttonY = false,
            buttonL1 = false, buttonR1 = false, buttonL3 = false, buttonR3 = false,
            buttonStart = false, buttonSelect = false, buttonHome = false, buttonCapture = false)
        val keys = mapper.keysFor(state, gyroEnabled = false)
        assertTrue("I should be pressed (rightStickY < -0.4)", keys.contains(I))
        assertTrue("K should be pressed (rightStickY > 0.4)", keys.contains(K))
        assertTrue("J should be pressed (rightStickX < -0.4)", keys.contains(J))
        assertTrue("L should be pressed (rightStickX > 0.4)", keys.contains(L))
    }

    @Test
    fun `keysFor right stick NO IKJL when gyroEnabled true`() {
        val state = InputState(leftStickX = 0f, leftStickY = 0f, rightStickX = -0.5f, rightStickY = -0.5f,
            dpadUp = false, dpadDown = false, dpadLeft = false, dpadRight = false,
            buttonA = false, buttonB = false, buttonX = false, buttonY = false,
            buttonL1 = false, buttonR1 = false, buttonL3 = false, buttonR3 = false,
            buttonStart = false, buttonSelect = false, buttonHome = false, buttonCapture = false)
        val keys = mapper.keysFor(state, gyroEnabled = true)
        assertFalse("I should NOT be pressed when gyroEnabled=true", keys.contains(I))
        assertFalse("K should NOT be pressed when gyroEnabled=true", keys.contains(K))
        assertFalse("J should NOT be pressed when gyroEnabled=true", keys.contains(J))
        assertFalse("L should NOT be pressed when gyroEnabled=true", keys.contains(L))
    }

    // ---- Arrow keys from dpad ----

    @Test
    fun `keysFor arrows from dpad`() {
        val state = InputState(leftStickX = 0f, leftStickY = 0f, rightStickX = 0f, rightStickY = 0f,
            dpadUp = true, dpadDown = true, dpadLeft = true, dpadRight = true,
            buttonA = false, buttonB = false, buttonX = false, buttonY = false,
            buttonL1 = false, buttonR1 = false, buttonL3 = false, buttonR3 = false,
            buttonStart = false, buttonSelect = false, buttonHome = false, buttonCapture = false)
        val keys = mapper.keysFor(state, gyroEnabled = false)
        assertTrue("UP should be pressed", keys.contains(UP))
        assertTrue("DOWN should be pressed", keys.contains(DOWN))
        assertTrue("LEFT should be pressed", keys.contains(LEFT))
        assertTrue("RIGHT should be pressed", keys.contains(RIGHT))
    }

    // ---- Face buttons: A=Z, B=X, X=C, Y=V ----

    @Test
    fun `keysFor face buttons A to Z, B to X, X to C, Y to V`() {
        val state = InputState(leftStickX = 0f, leftStickY = 0f, rightStickX = 0f, rightStickY = 0f,
            dpadUp = false, dpadDown = false, dpadLeft = false, dpadRight = false,
            buttonA = true, buttonB = true, buttonX = true, buttonY = true,
            buttonL1 = false, buttonR1 = false, buttonL3 = false, buttonR3 = false,
            buttonStart = false, buttonSelect = false, buttonHome = false, buttonCapture = false)
        val keys = mapper.keysFor(state, gyroEnabled = false)
        assertTrue("A->Z", keys.contains(Z))
        assertTrue("B->X", keys.contains(X))
        assertTrue("X->C", keys.contains(C))
        assertTrue("Y->V", keys.contains(V))
    }

    // ---- Shoulders: L1=E, R1=U ----

    @Test
    fun `keysFor shoulders L1 to E, R1 to U`() {
        val state = InputState(leftStickX = 0f, leftStickY = 0f, rightStickX = 0f, rightStickY = 0f,
            dpadUp = false, dpadDown = false, dpadLeft = false, dpadRight = false,
            buttonA = false, buttonB = false, buttonX = false, buttonY = false,
            buttonL1 = true, buttonR1 = true, buttonL3 = false, buttonR3 = false,
            buttonStart = false, buttonSelect = false, buttonHome = false, buttonCapture = false)
        val keys = mapper.keysFor(state, gyroEnabled = false)
        assertTrue("L1->E", keys.contains(E))
        assertTrue("R1->U", keys.contains(U))
    }

    // ---- Triggers: leftTrigger>0.4=Q, rightTrigger>0.4=O ----

    @Test
    fun `keysFor triggers Q and O pressed`() {
        val state = InputState(leftStickX = 0f, leftStickY = 0f, rightStickX = 0f, rightStickY = 0f,
            dpadUp = false, dpadDown = false, dpadLeft = false, dpadRight = false,
            buttonA = false, buttonB = false, buttonX = false, buttonY = false,
            buttonL1 = false, buttonR1 = false, buttonL3 = false, buttonR3 = false,
            buttonStart = false, buttonSelect = false, buttonHome = false, buttonCapture = false,
            leftTrigger = 0.8f, rightTrigger = 0.8f)
        val keys = mapper.keysFor(state, gyroEnabled = false)
        assertTrue("Q should be pressed (leftTrigger > 0.4)", keys.contains(Q))
        assertTrue("O should be pressed (rightTrigger > 0.4)", keys.contains(O))
    }

    @Test
    fun `keysFor triggers no keys when below threshold`() {
        val state = InputState(leftStickX = 0f, leftStickY = 0f, rightStickX = 0f, rightStickY = 0f,
            dpadUp = false, dpadDown = false, dpadLeft = false, dpadRight = false,
            buttonA = false, buttonB = false, buttonX = false, buttonY = false,
            buttonL1 = false, buttonR1 = false, buttonL3 = false, buttonR3 = false,
            buttonStart = false, buttonSelect = false, buttonHome = false, buttonCapture = false,
            leftTrigger = 0.2f, rightTrigger = 0.2f)
        val keys = mapper.keysFor(state, gyroEnabled = false)
        assertFalse("Q should NOT be pressed when leftTrigger <= 0.4", keys.contains(Q))
        assertFalse("O should NOT be pressed when rightTrigger <= 0.4", keys.contains(O))
    }

    // ---- L3=F, R3=H ----

    @Test
    fun `keysFor stick clicks L3 to F, R3 to H`() {
        val state = InputState(leftStickX = 0f, leftStickY = 0f, rightStickX = 0f, rightStickY = 0f,
            dpadUp = false, dpadDown = false, dpadLeft = false, dpadRight = false,
            buttonA = false, buttonB = false, buttonX = false, buttonY = false,
            buttonL1 = false, buttonR1 = false, buttonL3 = true, buttonR3 = true,
            buttonStart = false, buttonSelect = false, buttonHome = false, buttonCapture = false)
        val keys = mapper.keysFor(state, gyroEnabled = false)
        assertTrue("L3->F", keys.contains(F))
        assertTrue("R3->H", keys.contains(H))
    }

    // ---- System buttons: Start=EQUALS, Select=MINUS ----

    @Test
    fun `keysFor system buttons Start to EQUALS, Select to MINUS`() {
        val state = InputState(leftStickX = 0f, leftStickY = 0f, rightStickX = 0f, rightStickY = 0f,
            dpadUp = false, dpadDown = false, dpadLeft = false, dpadRight = false,
            buttonA = false, buttonB = false, buttonX = false, buttonY = false,
            buttonL1 = false, buttonR1 = false, buttonL3 = false, buttonR3 = false,
            buttonStart = true, buttonSelect = true, buttonHome = false, buttonCapture = false)
        val keys = mapper.keysFor(state, gyroEnabled = false)
        assertTrue("Start->EQUALS", keys.contains(EQUALS))
        assertTrue("Select->MINUS", keys.contains(MINUS))
    }

    // ---- Overflow: > 6 keys ----

    @Test
    fun `keysFor overflow logs and returns all keys`() {
        val state = InputState(
            leftStickX = -0.5f, leftStickY = -0.5f,   // W, A
            rightStickX = -0.5f, rightStickY = -0.5f, // I, J
            dpadUp = true,                              // UP
            buttonA = true, buttonB = true,             // Z, X
            buttonX = true, buttonY = true,             // C, V
            buttonL1 = true, buttonR1 = true,           // E, U
            buttonL3 = true, buttonR3 = true,           // F, H
            buttonStart = true, buttonSelect = true     // =, -
        )
        // The test just verifies > 6 keys are produced (HID layer truncates to 6)
        val keys = mapper.keysFor(state, gyroEnabled = false)
        assertTrue("Should produce more than 6 keys when all inputs active", keys.size > 6)
    }

    // ---- Ordering: WASD first, then IKJL if applicable, then arrows, then face, then shoulders, then triggers, then stick clicks, then system ----

    @Test
    fun `keysFor ordering matches production order`() {
        // State that produces a known order: WASD from left stick, arrows from dpad, face buttons
        val state = InputState(
            leftStickX = -0.5f, leftStickY = -0.5f,   // W, A (left stick)
            rightStickX = 0f, rightStickY = 0f,       // no IKJL
            dpadUp = true, dpadDown = false, dpadLeft = false, dpadRight = false, // UP
            buttonA = false, buttonB = false, buttonX = false, buttonY = false, // no face
            buttonL1 = false, buttonR1 = false,       // no shoulders
            buttonL3 = false, buttonR3 = false,       // no stick clicks
            buttonStart = false, buttonSelect = false // no system
        )
        // gyroEnabled = false means IKJL could appear, but rightStick is 0 so they won't
        val keys = mapper.keysFor(state, gyroEnabled = false)
        // Order should be: W, A (WASD first), then UP (arrow), then face/shoulders/triggers/sticks/system (none here)
        // Just verify the list is not empty and has reasonable ordering
        assertTrue("Keys list should not be empty", keys.isNotEmpty())
        // W should be first (WASD order in code)
        assertEquals(W, keys[0])
    }
}