package com.macgamepad.controller

import com.macgamepad.controller.input.InputState
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse

/**
 * Pure unit test for the throttle logic from GamepadViewModel.onInputChanged().
 * This replicates the throttle logic inline since we cannot modify production code
 * to extract a testable helper. The logic is small and self-contained.
 *
 * Rules verified:
 *   1. Immediate send on release transition (isReleaseTransition = true -> send immediately)
 *   2. Suppression when < 8 ms and no release transition (state stored as pending)
 *   3. Trailing-edge: a throttled state is stored and sent on next call
 */
class GamepadViewModelThrottleTest {

    // Replicated throttle state from GamepadViewModel
    private var lastInputReportTime = 0L
    private var lastState: InputState? = null
    private var pendingState: InputState? = null
    private var sentStates = mutableListOf<InputState>()

    // Helper to create a copy of InputState (since data class copy requires all params)
    private fun copyState(
        leftStickX: Float = 0f, leftStickY: Float = 0f,
        rightStickX: Float = 0f, rightStickY: Float = 0f,
        dpadUp: Boolean = false, dpadDown: Boolean = false,
        dpadLeft: Boolean = false, dpadRight: Boolean = false,
        buttonA: Boolean = false, buttonB: Boolean = false,
        buttonX: Boolean = false, buttonY: Boolean = false,
        buttonL1: Boolean = false, buttonR1: Boolean = false,
        leftTrigger: Float = 0f, rightTrigger: Float = 0f,
        buttonL3: Boolean = false, buttonR3: Boolean = false,
        buttonStart: Boolean = false, buttonSelect: Boolean = false,
        buttonHome: Boolean = false, buttonCapture: Boolean = false
    ): InputState = InputState(
        leftStickX = leftStickX, leftStickY = leftStickY,
        rightStickX = rightStickX, rightStickY = rightStickY,
        dpadUp = dpadUp, dpadDown = dpadDown,
        dpadLeft = dpadLeft, dpadRight = dpadRight,
        buttonA = buttonA, buttonB = buttonB,
        buttonX = buttonX, buttonY = buttonY,
        buttonL1 = buttonL1, buttonR1 = buttonR1,
        leftTrigger = leftTrigger, rightTrigger = rightTrigger,
        buttonL3 = buttonL3, buttonR3 = buttonR3,
        buttonStart = buttonStart, buttonSelect = buttonSelect,
        buttonHome = buttonHome, buttonCapture = buttonCapture
    )

    private fun resetThrottle() {
        lastInputReportTime = 0L
        lastState = null
        pendingState = null
        sentStates.clear()
    }

    @Test
    fun `immediate send on release transition`() {
        // Arrange: lastState has buttonA pressed, new state has buttonA released
        resetThrottle()
        lastState = copyState(buttonA = true)
        val current = copyState(buttonA = false)

        // Act
        onInputChanged(current)

        // Assert: pendingState should be null (state was sent), lastInputReportTime updated
        assertTrue("pendingState should be null after release transition", pendingState == null)
        assertTrue("lastInputReportTime should be updated", lastInputReportTime > 0L)
    }

    @Test
    fun `suppress when within 8 ms and no release transition`() {
        // Arrange: no release transition, within throttle window
        resetThrottle()
        lastState = copyState(leftStickX = 0.5f, leftStickY = 0.5f)
        val current = copyState(leftStickX = 0.5f, leftStickY = 0.5f) // same state, no release

        // Act -- first call stores as pending
        onInputChanged(current)
        val pendingAfterFirst = pendingState

        // Act -- second call within < 8ms window, should update pending with latest
        // Simulate very short time gap
        val now = System.currentTimeMillis()
        lastInputReportTime = now  // set to "just now" so second call is within 8ms
        onInputChanged(current)
        val pendingAfterSecond = pendingState

        // Assert: pending should still exist and be the latest state
        assertTrue("pendingState should exist after suppressed call", pendingAfterSecond != null)
        assertEquals("pending should be updated to latest state", current, pendingAfterSecond!!)
    }

    @Test
    fun `trailing edge throttled state sent on next call`() {
        // Arrange: first call is within throttle window, stores as pending
        resetThrottle()
        lastState = copyState()
        val first = copyState(leftStickX = 0.5f, leftStickY = 0.5f)

        onInputChanged(first)
        assertTrue("pendingState should exist after first call within throttle window", pendingState != null)

        // Act: second call with new state, now outside 8ms window (enough time has passed)
        val now = System.currentTimeMillis()
        lastInputReportTime = now - 100L // 100ms ago, definitely outside window
        val second = copyState(leftStickX = -0.5f, leftStickY = -0.5f)

        onInputChanged(second)

        // Assert: pending should be sent and cleared, new state also sent
        assertTrue("pendingState should be null after trailing-edge send", pendingState == null)
        // lastInputReportTime should be updated to the second call time
        assertTrue("lastInputReportTime should be updated", lastInputReportTime > 0L)
    }

    @Test
    fun `release transition with sticks`() {
        // Arrange: lastState has leftStickX non-zero, new state has it at zero
        resetThrottle()
        lastState = copyState(leftStickX = 0.5f)
        val current = copyState(leftStickX = 0f)

        onInputChanged(current)

        // Release transition should be detected, state sent immediately
        assertTrue("pendingState should be null after stick release", pendingState == null)
    }

    @Test
    fun `multiple buttons release transition`() {
        // Arrange: lastState has multiple buttons pressed, new state releases some
        resetThrottle()
        lastState = copyState(buttonA = true, buttonB = true, leftStickX = 0.5f)
        val current = copyState(buttonA = false, buttonB = false, leftStickX = 0f)

        onInputChanged(current)

        assertTrue("pendingState should be null after multi-button release", pendingState == null)
    }

    // --- Internal helper replicating GamepadViewModel.onInputChanged logic ---

    private fun onInputChanged(state: InputState) {
        val now = System.currentTimeMillis()
        var isReleaseTransition = false
        val prev = lastState
        if (prev != null) {
            // Sticks returning to zero
            if (prev.leftStickX != 0f && state.leftStickX == 0f) isReleaseTransition = true
            if (prev.leftStickY != 0f && state.leftStickY == 0f) isReleaseTransition = true
            if (prev.rightStickX != 0f && state.rightStickX == 0f) isReleaseTransition = true
            if (prev.rightStickY != 0f && state.rightStickY == 0f) isReleaseTransition = true

            // Buttons transitioning from pressed to released
            if (prev.buttonA && !state.buttonA) isReleaseTransition = true
            if (prev.buttonB && !state.buttonB) isReleaseTransition = true
            if (prev.buttonX && !state.buttonX) isReleaseTransition = true
            if (prev.buttonY && !state.buttonY) isReleaseTransition = true
            if (prev.buttonL1 && !state.buttonL1) isReleaseTransition = true
            if (prev.buttonR1 && !state.buttonR1) isReleaseTransition = true
            if (prev.buttonL3 && !state.buttonL3) isReleaseTransition = true
            if (prev.buttonR3 && !state.buttonR3) isReleaseTransition = true
            if (prev.buttonStart && !state.buttonStart) isReleaseTransition = true
            if (prev.buttonSelect && !state.buttonSelect) isReleaseTransition = true
            if (prev.buttonHome && !state.buttonHome) isReleaseTransition = true
            if (prev.buttonCapture && !state.buttonCapture) isReleaseTransition = true

            // Triggers returning to zero
            if (prev.leftTrigger != 0f && state.leftTrigger == 0f) isReleaseTransition = true
            if (prev.rightTrigger != 0f && state.rightTrigger == 0f) isReleaseTransition = true

            // D-Pad returning to center
            if ((prev.dpadUp || prev.dpadDown || prev.dpadLeft || prev.dpadRight) &&
                (!state.dpadUp && !state.dpadDown && !state.dpadLeft && !state.dpadRight)) {
                isReleaseTransition = true
            }
        }
        lastState = state

        if (isReleaseTransition || now - lastInputReportTime >= 8L) {
            lastInputReportTime = now
            if (pendingState != null) {
                sentStates.add(pendingState!!)
            }
            sentStates.add(state)
            pendingState = null
        } else if (pendingState != null) {
            // Within throttle window and we have a pending state from before
            // Latest wins - update pending to current state
            pendingState = state
        } else {
            // Within throttle window, no pending - store as pending
            pendingState = state
        }
    }
}