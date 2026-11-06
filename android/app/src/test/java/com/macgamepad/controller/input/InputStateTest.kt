package com.macgamepad.controller.input

import com.macgamepad.controller.input.InputState
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

/**
 * Tests for InputState hatDirection() and buttonBitmask().
 * Pure JVM tests — no Android dependencies.
 */
class InputStateTest {

    @Test
    fun `hatDirection centered when no dpad buttons pressed`() {
        val state = InputState()
        assertEquals(0x0F, state.hatDirection())
    }

    @Test
    fun `hatDirection UP when only dpadUp`() {
        val state = InputState(dpadUp = true, dpadDown = false, dpadLeft = false, dpadRight = false)
        assertEquals(0, state.hatDirection())
    }

    @Test
    fun `hatDirection UP_RIGHT when dpadUp and dpadRight`() {
        val state = InputState(dpadUp = true, dpadDown = false, dpadLeft = false, dpadRight = true)
        assertEquals(1, state.hatDirection())
    }

    @Test
    fun `hatDirection RIGHT when only dpadRight`() {
        val state = InputState(dpadUp = false, dpadDown = false, dpadLeft = false, dpadRight = true)
        assertEquals(2, state.hatDirection())
    }

    @Test
    fun `hatDirection DOWN_RIGHT when dpadDown and dpadRight`() {
        val state = InputState(dpadUp = false, dpadDown = true, dpadLeft = false, dpadRight = true)
        assertEquals(3, state.hatDirection())
    }

    @Test
    fun `hatDirection DOWN when only dpadDown`() {
        val state = InputState(dpadUp = false, dpadDown = true, dpadLeft = false, dpadRight = false)
        assertEquals(4, state.hatDirection())
    }

    @Test
    fun `hatDirection DOWN_LEFT when dpadDown and dpadLeft`() {
        val state = InputState(dpadUp = false, dpadDown = true, dpadLeft = true, dpadRight = false)
        assertEquals(5, state.hatDirection())
    }

    @Test
    fun `hatDirection LEFT when only dpadLeft`() {
        val state = InputState(dpadUp = false, dpadDown = false, dpadLeft = true, dpadRight = false)
        assertEquals(6, state.hatDirection())
    }

    @Test
    fun `hatDirection UP_LEFT when dpadUp and dpadLeft`() {
        val state = InputState(dpadUp = true, dpadDown = false, dpadLeft = true, dpadRight = false)
        assertEquals(7, state.hatDirection())
    }

    @Test
    fun `buttonBitmask bit 0 for buttonA`() {
        val state = InputState(buttonA = true, buttonB = false, buttonX = false, buttonY = false,
            buttonL1 = false, buttonR1 = false, buttonSelect = false, buttonStart = false,
            buttonHome = false, buttonL3 = false, buttonR3 = false, buttonCapture = false)
        val mask = state.buttonBitmask()
        assertTrue("Bit 0 should be set (button A)", (mask and 1) != 0)
    }

    @Test
    fun `buttonBitmask bit 1 for buttonB`() {
        val state = InputState(buttonA = false, buttonB = true, buttonX = false, buttonY = false,
            buttonL1 = false, buttonR1 = false, buttonSelect = false, buttonStart = false,
            buttonHome = false, buttonL3 = false, buttonR3 = false, buttonCapture = false)
        val mask = state.buttonBitmask()
        assertTrue("Bit 1 should be set (button B)", (mask and 2) != 0)
    }

    @Test
    fun `buttonBitmask bit 2 for buttonX`() {
        val state = InputState(buttonA = false, buttonB = false, buttonX = true, buttonY = false,
            buttonL1 = false, buttonR1 = false, buttonSelect = false, buttonStart = false,
            buttonHome = false, buttonL3 = false, buttonR3 = false, buttonCapture = false)
        val mask = state.buttonBitmask()
        assertTrue("Bit 2 should be set (button X)", (mask and 4) != 0)
    }

    @Test
    fun `buttonBitmask bit 3 for buttonY`() {
        val state = InputState(buttonA = false, buttonB = false, buttonX = false, buttonY = true,
            buttonL1 = false, buttonR1 = false, buttonSelect = false, buttonStart = false,
            buttonHome = false, buttonL3 = false, buttonR3 = false, buttonCapture = false)
        val mask = state.buttonBitmask()
        assertTrue("Bit 3 should be set (button Y)", (mask and 8) != 0)
    }

    @Test
    fun `buttonBitmask bit 6 for buttonL1`() {
        val state = InputState(buttonA = false, buttonB = false, buttonX = false, buttonY = false,
            buttonL1 = true, buttonR1 = false, buttonSelect = false, buttonStart = false,
            buttonHome = false, buttonL3 = false, buttonR3 = false, buttonCapture = false)
        val mask = state.buttonBitmask()
        assertTrue("Bit 4 should be set (button L1)", (mask and 16) != 0)
    }

    @Test
    fun `buttonBitmask bit 7 for buttonR1`() {
        val state = InputState(buttonA = false, buttonB = false, buttonX = false, buttonY = false,
            buttonL1 = false, buttonR1 = true, buttonSelect = false, buttonStart = false,
            buttonHome = false, buttonL3 = false, buttonR3 = false, buttonCapture = false)
        val mask = state.buttonBitmask()
        assertTrue("Bit 5 should be set (button R1)", (mask and 32) != 0)
    }

    @Test
    fun `buttonBitmask bit 8 for buttonSelect`() {
        val state = InputState(buttonA = false, buttonB = false, buttonX = false, buttonY = false,
            buttonL1 = false, buttonR1 = false, buttonSelect = true, buttonStart = false,
            buttonHome = false, buttonL3 = false, buttonR3 = false, buttonCapture = false)
        val mask = state.buttonBitmask()
        assertTrue("Bit 8 should be set (button Select)", (mask and 256) != 0)
    }

    @Test
    fun `buttonBitmask bit 9 for buttonStart`() {
        val state = InputState(buttonA = false, buttonB = false, buttonX = false, buttonY = false,
            buttonL1 = false, buttonR1 = false, buttonSelect = false, buttonStart = true,
            buttonHome = false, buttonL3 = false, buttonR3 = false, buttonCapture = false)
        val mask = state.buttonBitmask()
        assertTrue("Bit 9 should be set (button Start)", (mask and 512) != 0)
    }

    @Test
    fun `buttonBitmask bit 10 for buttonL3`() {
        val state = InputState(buttonA = false, buttonB = false, buttonX = false, buttonY = false,
            buttonL1 = false, buttonR1 = false, buttonSelect = false, buttonStart = false,
            buttonHome = false, buttonL3 = true, buttonR3 = false, buttonCapture = false)
        val mask = state.buttonBitmask()
        assertTrue("Bit 10 should be set (button L3)", (mask and 1024) != 0)
    }

    @Test
    fun `buttonBitmask bit 11 for buttonR3`() {
        val state = InputState(buttonA = false, buttonB = false, buttonX = false, buttonY = false,
            buttonL1 = false, buttonR1 = false, buttonSelect = false, buttonStart = false,
            buttonHome = false, buttonL3 = false, buttonR3 = true, buttonCapture = false)
        val mask = state.buttonBitmask()
        assertTrue("Bit 11 should be set (button R3)", (mask and 2048) != 0)
    }

    @Test
    fun `buttonBitmask bit 12 for buttonHome`() {
        val state = InputState(buttonA = false, buttonB = false, buttonX = false, buttonY = false,
            buttonL1 = false, buttonR1 = false, buttonSelect = false, buttonStart = false,
            buttonHome = true, buttonL3 = false, buttonR3 = false, buttonCapture = false)
        val mask = state.buttonBitmask()
        assertTrue("Bit 12 should be set (button Home)", (mask and 4096) != 0)
    }

    @Test
    fun `buttonBitmask no bits when no buttons pressed`() {
        val state = InputState()
        assertEquals(0, state.buttonBitmask())
    }

    @Test
    fun `combined dpad+buttons does not interfere with hatDirection`() {
        // All buttons pressed + all dpad directions
        val state = InputState(
            dpadUp = true, dpadDown = true, dpadLeft = true, dpadRight = true,
            buttonA = true, buttonB = true, buttonX = true, buttonY = true,
            buttonL1 = true, buttonR1 = true, buttonL3 = true, buttonR3 = true,
            buttonStart = true, buttonSelect = true, buttonHome = true, buttonCapture = true,
            leftTrigger = 1f, rightTrigger = 1f
        )
        // With all dpad directions pressed, the logic prioritizes cardinals over diagonals
        // up+right takes precedence: should be 1 (UP_RIGHT)
        assertEquals(1, state.hatDirection())
        // All 16 bits should be set (bits 0-15)
        assertEquals(0xFFFF, state.buttonBitmask())
    }
}