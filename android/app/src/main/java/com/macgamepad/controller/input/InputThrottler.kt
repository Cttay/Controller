package com.macgamepad.controller.input

/**
 * Pure input throttling logic to prevent Bluetooth queue buildup by limiting
 * report frequency while ensuring release transitions are transmitted immediately.
 *
 * All logic is deterministic and has no Android framework dependencies.
 */
sealed interface ThrottleResult {
    /** Transmit the given state. If alsoTransmitPending is true, the pending state should also be transmitted first. */
    data class Transmit(
        val state: InputState,
        val alsoTransmitPending: Boolean,
        val pendingState: InputState? = null
    ) : ThrottleResult
    /** State is being held as pending (not transmitted yet) */
    data class Pending(val state: InputState) : ThrottleResult
}

/**
 * Throttles input reports to a minimum interval (default 4ms ≈ 250 Hz)
 * while ensuring release transitions (buttons/sticks/triggers returning to neutral)
 * are transmitted immediately.
 */
class InputThrottler(
    private val throttleIntervalMs: Long = DEFAULT_THROTTLE_MS
) {
    companion object {
        /** Default throttle interval: 4ms (≈ 250 Hz) — matches current implementation */
        const val DEFAULT_THROTTLE_MS = 4L
    }

    private var lastReportTime = 0L
    private var lastState: InputState? = null
    private var pendingState: InputState? = null

    /**
     * Process a new input state and determine whether to transmit, skip, or hold as pending.
     *
     * @param state The new input state
     * @param nowMs Current time in milliseconds (for testability; defaults to System.currentTimeMillis())
     * @return ThrottleResult indicating what action to take
     */
    fun process(state: InputState, nowMs: Long = System.currentTimeMillis()): ThrottleResult {
        // Check for release transition from previous state
        val isReleaseTransition = isReleaseTransition(lastState, state)

        // Update last state for next call
        lastState = state

        // Release transitions always transmit immediately
        if (isReleaseTransition) {
            lastReportTime = nowMs
            if (pendingState != null) {
                val pending = pendingState!!
                pendingState = null
                return ThrottleResult.Transmit(state, alsoTransmitPending = true, pendingState = pending)
            }
            return ThrottleResult.Transmit(state, alsoTransmitPending = false)
        }

        // Check if we're outside the throttle window
        if (nowMs - lastReportTime >= throttleIntervalMs) {
            lastReportTime = nowMs
            if (pendingState != null) {
                val pending = pendingState!!
                pendingState = null
                return ThrottleResult.Transmit(state, alsoTransmitPending = true, pendingState = pending)
            }
            return ThrottleResult.Transmit(state, alsoTransmitPending = false)
        }

        // Within throttle window — store as pending (latest wins)
        pendingState = state
        return ThrottleResult.Pending(state)
    }

    /**
     * Reset all internal state to initial values.
     */
    fun reset() {
        lastReportTime = 0L
        lastState = null
        pendingState = null
    }

    /**
     * Determine if transition from prev to current represents a release (return to neutral).
     */
    private fun isReleaseTransition(prev: InputState?, current: InputState): Boolean {
        if (prev == null) return false

        // Sticks returning to zero
        if (prev.leftStickX != 0f && current.leftStickX == 0f) return true
        if (prev.leftStickY != 0f && current.leftStickY == 0f) return true
        if (prev.rightStickX != 0f && current.rightStickX == 0f) return true
        if (prev.rightStickY != 0f && current.rightStickY == 0f) return true

        // Buttons transitioning from pressed to released
        if (prev.buttonA && !current.buttonA) return true
        if (prev.buttonB && !current.buttonB) return true
        if (prev.buttonX && !current.buttonX) return true
        if (prev.buttonY && !current.buttonY) return true
        if (prev.buttonL1 && !current.buttonL1) return true
        if (prev.buttonR1 && !current.buttonR1) return true
        if (prev.buttonL3 && !current.buttonL3) return true
        if (prev.buttonR3 && !current.buttonR3) return true
        if (prev.buttonStart && !current.buttonStart) return true
        if (prev.buttonSelect && !current.buttonSelect) return true
        if (prev.buttonHome && !current.buttonHome) return true
        if (prev.buttonCapture && !current.buttonCapture) return true

        // Triggers returning to zero
        if (prev.leftTrigger != 0f && current.leftTrigger == 0f) return true
        if (prev.rightTrigger != 0f && current.rightTrigger == 0f) return true

        // D-Pad returning to center
        val prevDpad = prev.dpadUp || prev.dpadDown || prev.dpadLeft || prev.dpadRight
        val currentDpad = current.dpadUp || current.dpadDown || current.dpadLeft || current.dpadRight
        if (prevDpad && !currentDpad) return true

        return false
    }
}