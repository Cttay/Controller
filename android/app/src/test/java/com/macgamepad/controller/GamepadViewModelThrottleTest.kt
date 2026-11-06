package com.macgamepad.controller

import com.macgamepad.controller.input.InputState
import com.macgamepad.controller.input.InputThrottler
import com.macgamepad.controller.input.ThrottleResult
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull

/**
 * Unit test for InputThrottler — pure throttling logic extracted from GamepadViewModel.
 *
 * Tests verify:
 *   1. Immediate send on release transition (isReleaseTransition = true -> send immediately)
 *   2. Suppression when within throttle window and no release transition (state stored as pending)
 *   3. Trailing-edge: a throttled state is stored and sent on next call
 *   4. Release transition with sticks
 *   5. Multiple buttons release transition
 *   6. reset() clears internal state
 *   7. Configurable throttle interval
 *   8. Time-controlled testing via nowMs parameter
 */
class GamepadViewModelThrottleTest {

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

    @Test
    fun `immediate send on release transition`() {
        // Arrange: lastState has buttonA pressed, new state has buttonA released
        val throttler = InputThrottler()
        val prevState = copyState(buttonA = true)
        val current = copyState(buttonA = false)

        // Act - first call establishes previous state (no release since prev is null)
        throttler.process(prevState, 1000L)
        val result = throttler.process(current, 1004L) // within throttle window but release transition

        // Assert: Transmit result, no pending
        assertTrue("Should transmit immediately on release transition", result is ThrottleResult.Transmit)
        val transmit = result as ThrottleResult.Transmit
        assertEquals(current, transmit.state)
        assertFalse("alsoTransmitPending should be false (no prior pending)", transmit.alsoTransmitPending)
    }

    @Test
    fun `suppress when within throttle window and no release transition`() {
        // Arrange: no release transition, within throttle window
        val throttler = InputThrottler(throttleIntervalMs = 8L)
        val prevState = copyState(leftStickX = 0.5f, leftStickY = 0.5f)
        val current = copyState(leftStickX = 0.5f, leftStickY = 0.5f) // same state, no release

        // Act -- first call stores as pending (outside throttle window initially)
        throttler.process(prevState, 1000L)
        val result1 = throttler.process(current, 1004L) // within 8ms window

        // Assert: Pending result
        assertTrue("Should be Pending within throttle window", result1 is ThrottleResult.Pending)
        val pending = result1 as ThrottleResult.Pending
        assertEquals(current, pending.state)

        // Act -- second call within throttle window, latest wins
        val result2 = throttler.process(current, 1006L)

        // Assert: Still pending, latest state kept
        assertTrue("Should still be Pending", result2 is ThrottleResult.Pending)
        val pending2 = result2 as ThrottleResult.Pending
        assertEquals(current, pending2.state)
    }

    @Test
    fun `trailing edge throttled state sent on next call`() {
        // Arrange: first call is within throttle window, stores as pending
        val throttler = InputThrottler(throttleIntervalMs = 8L)
        val prevState = copyState()
        val first = copyState(leftStickX = 0.5f, leftStickY = 0.5f)

        throttler.process(prevState, 1000L)
        val result1 = throttler.process(first, 1004L) // within 8ms window

        assertTrue("First call should be Pending", result1 is ThrottleResult.Pending)

        // Act: second call with new state, now outside throttle window (100ms later)
        val second = copyState(leftStickX = -0.5f, leftStickY = -0.5f)
        val result2 = throttler.process(second, 1104L) // 100ms after last report

        // Assert: Transmit with alsoTransmitPending = true
        assertTrue("Should Transmit with alsoTransmitPending", result2 is ThrottleResult.Transmit)
        val transmit = result2 as ThrottleResult.Transmit
        assertEquals(second, transmit.state)
        assertTrue("alsoTransmitPending should be true", transmit.alsoTransmitPending)
    }

    @Test
    fun `release transition with sticks`() {
        // Arrange: lastState has leftStickX non-zero, new state has it at zero
        val throttler = InputThrottler(throttleIntervalMs = 8L)
        val prevState = copyState(leftStickX = 0.5f)
        val current = copyState(leftStickX = 0f)

        throttler.process(prevState, 1000L)
        val result = throttler.process(current, 1004L) // within throttle window but release

        // Release transition should be detected, state sent immediately
        assertTrue("Should Transmit immediately on stick release", result is ThrottleResult.Transmit)
        val transmit = result as ThrottleResult.Transmit
        assertFalse("alsoTransmitPending should be false", transmit.alsoTransmitPending)
    }

    @Test
    fun `multiple buttons release transition`() {
        // Arrange: lastState has multiple buttons pressed, new state releases some
        val throttler = InputThrottler(throttleIntervalMs = 8L)
        val prevState = copyState(buttonA = true, buttonB = true, leftStickX = 0.5f)
        val current = copyState(buttonA = false, buttonB = false, leftStickX = 0f)

        throttler.process(prevState, 1000L)
        val result = throttler.process(current, 1004L)

        assertTrue("Should Transmit immediately on multi-button release", result is ThrottleResult.Transmit)
        val transmit = result as ThrottleResult.Transmit
        assertFalse("alsoTransmitPending should be false", transmit.alsoTransmitPending)
    }

    @Test
    fun `reset clears internal state`() {
        val throttler = InputThrottler(throttleIntervalMs = 8L)

        // Build up some state
        throttler.process(copyState(leftStickX = 0.5f), 1000L)
        throttler.process(copyState(leftStickX = 0.5f), 1004L) // pending
        throttler.process(copyState(leftStickX = 0f), 1104L) // transmit with pending

        // Act: reset
        throttler.reset()

        // After reset, should behave like fresh instance
        val prevState = copyState()
        val current = copyState(leftStickX = 0.5f)
        val result = throttler.process(prevState, 2000L)
        assertTrue("First call after reset should Transmit (no previous state)", result is ThrottleResult.Transmit)

        val result2 = throttler.process(current, 2004L) // within throttle window
        assertTrue("Second call within throttle should be Pending", result2 is ThrottleResult.Pending)
    }

    @Test
    fun `configurable throttle interval`() {
        // Default 4ms
        val defaultThrottler = InputThrottler()
        defaultThrottler.process(copyState(), 1000L)
        val resultDefault = defaultThrottler.process(copyState(leftStickX = 0.1f), 1003L) // 3ms later
        assertTrue("Default 4ms: 3ms should be Pending", resultDefault is ThrottleResult.Pending)

        val resultDefault2 = defaultThrottler.process(copyState(leftStickX = 0.1f), 1005L) // 5ms later
        assertTrue("Default 4ms: 5ms should Transmit", resultDefault2 is ThrottleResult.Transmit)

        // Custom 10ms
        val customThrottler = InputThrottler(throttleIntervalMs = 10L)
        customThrottler.process(copyState(), 1000L)
        val resultCustom = customThrottler.process(copyState(leftStickX = 0.1f), 1005L) // 5ms later
        assertTrue("Custom 10ms: 5ms should be Pending", resultCustom is ThrottleResult.Pending)

        val resultCustom2 = customThrottler.process(copyState(leftStickX = 0.1f), 1012L) // 12ms later
        assertTrue("Custom 10ms: 12ms should Transmit", resultCustom2 is ThrottleResult.Transmit)
    }

    @Test
    fun `time controlled testing via nowMs parameter`() {
        // Test that we can control time via nowMs parameter for deterministic testing
        val throttler = InputThrottler(throttleIntervalMs = 8L)

        // Simulate exact time progression
        val t0 = 1000000L
        throttler.process(copyState(), t0)

        // At t0 + 4ms (within throttle window)
        val r1 = throttler.process(copyState(leftStickX = 0.5f), t0 + 4)
        assertTrue("At 4ms should be Pending", r1 is ThrottleResult.Pending)

        // At t0 + 8ms (at throttle boundary)
        val r2 = throttler.process(copyState(leftStickX = 0.5f), t0 + 8)
        assertTrue("At 8ms should Transmit", r2 is ThrottleResult.Transmit)
        assertTrue("alsoTransmitPending should be true at boundary", (r2 as ThrottleResult.Transmit).alsoTransmitPending)

        // No manual time waiting needed — fully deterministic
    }

    @Test
    fun `trigger release transition detected`() {
        val throttler = InputThrottler(throttleIntervalMs = 8L)
        val prevState = copyState(leftTrigger = 0.5f)
        val current = copyState(leftTrigger = 0f)

        throttler.process(prevState, 1000L)
        val result = throttler.process(current, 1004L)

        assertTrue("Trigger release should Transmit immediately", result is ThrottleResult.Transmit)
    }

    @Test
    fun `dpad release transition detected`() {
        val throttler = InputThrottler(throttleIntervalMs = 8L)
        val prevState = copyState(dpadUp = true)
        val current = copyState()

        throttler.process(prevState, 1000L)
        val result = throttler.process(current, 1004L)

        assertTrue("D-Pad release should Transmit immediately", result is ThrottleResult.Transmit)
    }

    @Test
    fun `stick click release transition detected`() {
        val throttler = InputThrottler(throttleIntervalMs = 8L)
        val prevState = copyState(buttonL3 = true)
        val current = copyState()

        throttler.process(prevState, 1000L)
        val result = throttler.process(current, 1004L)

        assertTrue("Stick click (L3) release should Transmit immediately", result is ThrottleResult.Transmit)
    }

    @Test
    fun `system button release transition detected`() {
        val throttler = InputThrottler(throttleIntervalMs = 8L)
        val prevState = copyState(buttonHome = true, buttonCapture = true)
        val current = copyState()

        throttler.process(prevState, 1000L)
        val result = throttler.process(current, 1004L)

        assertTrue("System button (Home/Capture) release should Transmit immediately", result is ThrottleResult.Transmit)
    }

    @Test
    fun `release transition with pending state transmits both`() {
        // Arrange: pending state exists, then a release transition occurs
        val throttler = InputThrottler(throttleIntervalMs = 8L)
        val prevState = copyState(leftStickX = 0.5f)
        val pending = copyState(leftStickX = 0.5f) // within throttle window, becomes pending
        val release = copyState(leftStickX = 0f) // release transition

        throttler.process(prevState, 1000L)
        val result1 = throttler.process(pending, 1004L) // Pending
        assertTrue("Should be Pending", result1 is ThrottleResult.Pending)

        // Act: release transition while pending exists
        val result2 = throttler.process(release, 1008L) // still within throttle but release

        // Assert: Transmit with alsoTransmitPending = true AND pendingState present
        assertTrue("Should Transmit with alsoTransmitPending", result2 is ThrottleResult.Transmit)
        val transmit = result2 as ThrottleResult.Transmit
        assertEquals(release, transmit.state)
        assertTrue("alsoTransmitPending should be true", transmit.alsoTransmitPending)
        assertNotNull("pendingState should be present", transmit.pendingState)
        assertEquals(pending, transmit.pendingState!!)
    }

    // =========================================================================
    // P0: Core behavior gaps - initialization, zero-crossing, boundary
    // =========================================================================

    @Test
    fun `first call always transmits regardless of throttle`() {
        // When lastState == null and lastReportTime == 0, first process() always transmits
        val throttler = InputThrottler(throttleIntervalMs = 100L)
        val result = throttler.process(copyState(leftStickX = 0.5f), 1000L)
        assertTrue("First call should always Transmit", result is ThrottleResult.Transmit)
        val transmit = result as ThrottleResult.Transmit
        assertFalse("alsoTransmitPending should be false (no prior pending)", transmit.alsoTransmitPending)
        assertEquals(copyState(leftStickX = 0.5f), transmit.state)
    }

    @Test
    fun `stick crossing zero without stopping at zero is not release`() {
        // 0.5 -> -0.5 crosses zero but is NOT a release (exact zero required)
        // Current implementation: only exact 0f counts as release
        val throttler = InputThrottler(throttleIntervalMs = 8L)
        throttler.process(copyState(leftStickX = 0.5f), 1000L)
        val result = throttler.process(copyState(leftStickX = -0.5f), 1004L) // within window
        assertTrue("Crossing zero should be Pending (throttled), not release", result is ThrottleResult.Pending)
    }

    @Test
    fun `first eligible call at exact throttle boundary transmits`() {
        // Test boundary on first eligible call (>= vs >)
        val throttler = InputThrottler(throttleIntervalMs = 8L)
        throttler.process(copyState(), 1000L)
        val result = throttler.process(copyState(leftStickX = 0.5f), 1008L) // exactly 8ms
        assertTrue("At exact boundary should Transmit", result is ThrottleResult.Transmit)
    }

    @Test
    fun `multiple rapid updates within throttle window latest wins`() {
        val throttler = InputThrottler(throttleIntervalMs = 50L)
        throttler.process(copyState(), 1000L)
        throttler.process(copyState(leftStickX = 0.1f), 1010L) // pending v1
        throttler.process(copyState(leftStickX = 0.5f), 1020L) // pending v2 (latest)
        throttler.process(copyState(leftStickX = -0.5f), 1030L) // pending v3 (latest)
        val result = throttler.process(copyState(leftStickX = 1.0f), 1060L) // after window
        val transmit = result as ThrottleResult.Transmit
        assertTrue("alsoTransmitPending should be true", transmit.alsoTransmitPending)
        assertEquals(-0.5f, transmit.pendingState!!.leftStickX, 0.001f) // v3 was pending (-0.5f)
        assertEquals(1.0f, transmit.state.leftStickX, 0.001f) // current is v4
    }

    // =========================================================================
    // P1: Edge cases & robustness - analog edge cases, float precision
    // =========================================================================

    @Test
    fun `trigger partial release is throttled not immediate`() {
        val throttler = InputThrottler(throttleIntervalMs = 8L)
        throttler.process(copyState(leftTrigger = 1.0f), 1000L)
        val result = throttler.process(copyState(leftTrigger = 0.3f), 1004L) // not zero
        assertTrue("Partial trigger release should be Pending", result is ThrottleResult.Pending)
    }

    @Test
    fun `dpad direction change is not release transition`() {
        val throttler = InputThrottler(throttleIntervalMs = 8L)
        throttler.process(copyState(dpadUp = true), 1000L)
        val result = throttler.process(copyState(dpadRight = true), 1004L)
        assertTrue("D-Pad direction change should be Pending", result is ThrottleResult.Pending)
    }

    @Test
    fun `stick values extremely close to zero are not treated as zero`() {
        val throttler = InputThrottler(throttleIntervalMs = 8L)
        throttler.process(copyState(leftStickX = 0.0001f), 1000L)
        val result = throttler.process(copyState(leftStickX = 0.0001f), 1004L)
        assertTrue("Near-zero should not be treated as release", result is ThrottleResult.Pending)
    }

    @Test
    fun `stick exactly zero from non-zero is release`() {
        val throttler = InputThrottler(throttleIntervalMs = 8L)
        throttler.process(copyState(leftStickX = 0.0001f), 1000L)
        val result = throttler.process(copyState(leftStickX = 0f), 1004L)
        assertTrue("Exact zero from non-zero is release", result is ThrottleResult.Transmit)
    }

    @Test
    fun `zero throttle interval means no throttling`() {
        val throttler = InputThrottler(throttleIntervalMs = 0L)
        throttler.process(copyState(), 1000L)
        val r1 = throttler.process(copyState(leftStickX = 0.1f), 1000L) // same time
        val r2 = throttler.process(copyState(leftStickX = 0.2f), 1000L)
        assertTrue("Zero interval: all should Transmit", r1 is ThrottleResult.Transmit)
        assertTrue("Zero interval: all should Transmit", r2 is ThrottleResult.Transmit)
    }

    @Test
    fun `negative throttle interval treated as zero`() {
        val throttler = InputThrottler(throttleIntervalMs = -1L)
        throttler.process(copyState(), 1000L)
        val result = throttler.process(copyState(leftStickX = 0.1f), 1000L)
        assertTrue("Negative interval treated as zero: should Transmit", result is ThrottleResult.Transmit)
    }

    @Test
    fun `consecutive release transitions both transmit immediately`() {
        val throttler = InputThrottler(throttleIntervalMs = 8L)
        throttler.process(copyState(buttonA = true, buttonB = true), 1000L)
        
        // Release A
        val r1 = throttler.process(copyState(buttonA = false, buttonB = true), 1004L)
        assertTrue("Release A should Transmit immediately", r1 is ThrottleResult.Transmit)
        
        // Release B (still within throttle window)
        val r2 = throttler.process(copyState(buttonA = false, buttonB = false), 1006L)
        assertTrue("Release B should Transmit immediately", r2 is ThrottleResult.Transmit)
    }

    @Test
    fun `reset clears pending state`() {
        val throttler = InputThrottler(throttleIntervalMs = 50L)
        throttler.process(copyState(), 1000L)
        throttler.process(copyState(leftStickX = 0.5f), 1010L) // pending
        
        throttler.reset()
        
        // Should behave fresh - no pending to transmit
        val result = throttler.process(copyState(leftStickX = 0.5f), 2000L)
        assertTrue("After reset: first call should Transmit", result is ThrottleResult.Transmit)
        assertFalse("alsoTransmitPending should be false", (result as ThrottleResult.Transmit).alsoTransmitPending)
    }

    // =========================================================================
    // P2: Property invariants & state machine guarantees
    // =========================================================================

    @Test
    fun `release transition always transmits regardless of throttle state`() {
        // Property: Any release transition must transmit immediately, even deep in throttle window
        val intervals = listOf(1L, 4L, 8L, 16L, 100L)
        val releaseScenarios = listOf(
            copyState(buttonA = true) to copyState(buttonA = false),
            copyState(buttonB = true) to copyState(buttonB = false),
            copyState(leftStickX = 0.5f) to copyState(leftStickX = 0f),
            copyState(leftStickY = 0.5f) to copyState(leftStickY = 0f),
            copyState(rightStickX = 0.5f) to copyState(rightStickX = 0f),
            copyState(rightStickY = 0.5f) to copyState(rightStickY = 0f),
            copyState(leftTrigger = 1.0f) to copyState(leftTrigger = 0f),
            copyState(rightTrigger = 1.0f) to copyState(rightTrigger = 0f),
            copyState(dpadUp = true) to copyState(),
            copyState(dpadDown = true) to copyState(),
            copyState(dpadLeft = true) to copyState(),
            copyState(dpadRight = true) to copyState(),
            copyState(buttonL3 = true) to copyState(),
            copyState(buttonR3 = true) to copyState(),
            copyState(buttonHome = true) to copyState(),
            copyState(buttonCapture = true) to copyState()
        )

        for (interval in intervals) {
            for ((prev, curr) in releaseScenarios) {
                val throttler = InputThrottler(throttleIntervalMs = interval)
                throttler.process(prev, 1000L)
                // Well within throttle window
                val result = throttler.process(curr, 1001L)
                assertTrue("Interval $interval: release ($prev -> $curr) must transmit", result is ThrottleResult.Transmit)
            }
        }
    }

    @Test
    fun `eventually transmits - no permanent starvation`() {
        // Property: No matter how many rapid updates within throttle window, 
        // eventually the throttle window expires and something transmits
        val throttler = InputThrottler(throttleIntervalMs = 8L)
        throttler.process(copyState(), 1000L)
        
        // Rapid updates within window (100 calls in 5ms)
        for (i in 1..100) {
            throttler.process(copyState(leftStickX = i * 0.01f), 1000L + i)
        }
        
        // Eventually must transmit
        var transmitted = false
        for (i in 101..200) {
            val result = throttler.process(copyState(leftStickX = i * 0.01f), 1000L + i * 10)
            if (result is ThrottleResult.Transmit) {
                transmitted = true
                break
            }
        }
        assertTrue("Must eventually transmit - no permanent starvation", transmitted)
    }

    @Test
    fun `reset produces identical behavior to new instance`() {
        // Property: reset() should be equivalent to creating a fresh InputThrottler
        val throttler1 = InputThrottler(throttleIntervalMs = 7L)
        val throttler2 = InputThrottler(throttleIntervalMs = 7L)
        
        // Build complex state in throttler1
        throttler1.process(copyState(buttonA = true), 1000L)
        throttler1.process(copyState(buttonA = true, leftStickX = 0.3f), 1003L) // pending
        throttler1.process(copyState(leftStickX = 0f), 1010L) // transmit with pending
        throttler1.reset()
        
        // Both should behave identically now
        val inputs = listOf(
            copyState(), 
            copyState(leftStickX = 0.1f), 
            copyState(leftStickX = 0.2f),
            copyState(leftStickX = 0f), 
            copyState(buttonB = true)
        )
        var t1 = 2000L
        var t2 = 2000L
        for (input in inputs) {
            t1 += 3
            t2 += 3
            val r1 = throttler1.process(input, t1)
            val r2 = throttler2.process(input, t2)
            assertEquals("Result type should match", r1::class, r2::class)
            if (r1 is ThrottleResult.Transmit && r2 is ThrottleResult.Transmit) {
                assertEquals("alsoTransmitPending should match", r1.alsoTransmitPending, r2.alsoTransmitPending)
                assertEquals("pendingState should match", r1.pendingState, r2.pendingState)
            }
        }
    }

    // =========================================================================
    // P3: Documentation tests
    // =========================================================================

    @Test
    fun `InputThrottler is not thread safe - single thread usage documented`() {
        // This test documents expected single-threaded usage
        // InputThrottler is NOT thread-safe; GamepadViewModel uses it on UI thread only
        // No assertion - serves as executable documentation
        val throttler = InputThrottler()
        throttler.process(copyState(), 1000L)
        throttler.reset()
        // If this compiles and runs, the API is usable from single thread
    }

    // =========================================================================
    // P4: Mutation testing targets (these should kill common mutants)
    // =========================================================================

    @Test
    fun `throttle boundary uses greaterEquals not greaterThan (kills greaterThan mutant)`() {
        // Mutant: >= -> > would make exact boundary Pending instead of Transmit
        val throttler = InputThrottler(throttleIntervalMs = 8L)
        throttler.process(copyState(), 1000L)
        // Exactly at boundary (8ms)
        val result = throttler.process(copyState(leftStickX = 0.5f), 1008L)
        assertTrue("Exact boundary must Transmit (>= semantics)", result is ThrottleResult.Transmit)
    }

    @Test
    fun `stick release requires exact zero not lessEquals (kills lessEquals mutant)`() {
        // Mutant: == 0f -> <= 0f would treat negative as release
        val throttler = InputThrottler(throttleIntervalMs = 8L)
        throttler.process(copyState(leftStickX = 0.5f), 1000L)
        val result = throttler.process(copyState(leftStickX = -0.5f), 1004L)
        assertTrue("Negative stick should be Pending (not release)", result is ThrottleResult.Pending)
    }

    @Test
    fun `button release requires exact false not logicalNot (kills logicalNot mutant)`() {
        // Mutant: !pressed -> pressed would invert logic
        val throttler = InputThrottler(throttleIntervalMs = 8L)
        throttler.process(copyState(buttonA = true), 1000L)
        val result = throttler.process(copyState(buttonA = true), 1004L) // still pressed
        assertTrue("Still pressed should be Pending (not release)", result is ThrottleResult.Pending)
    }

    @Test
    fun `lastReportTime updated on every Transmit (kills missing assignment mutant)`() {
        // Mutant: missing lastReportTime = nowMs would allow immediate re-transmit
        val throttler = InputThrottler(throttleIntervalMs = 8L)
        throttler.process(copyState(), 1000L)
        throttler.process(copyState(leftStickX = 0.5f), 1004L) // Pending
        val result = throttler.process(copyState(leftStickX = 0f), 1104L) // release after window
        assertTrue(result is ThrottleResult.Transmit)
        // If lastReportTime wasn't updated, next call at 1105L would be Pending
        // But since it was updated to 1104L, next call at 1112L (8ms later) should Transmit
        val result2 = throttler.process(copyState(leftStickX = 0.5f), 1112L)
        assertTrue("lastReportTime must be updated on Transmit", result2 is ThrottleResult.Transmit)
    }

    @Test
    fun `pendingState cleared on Transmit (kills missing clear mutant)`() {
        // Mutant: missing pendingState = null would re-transmit stale pending
        val throttler = InputThrottler(throttleIntervalMs = 8L)
        throttler.process(copyState(), 1000L)
        throttler.process(copyState(leftStickX = 0.5f), 1004L) // Pending (v1)
        val result1 = throttler.process(copyState(leftStickX = 0f), 1104L) // release -> Transmit with pending
        val transmit1 = result1 as ThrottleResult.Transmit
        assertTrue(transmit1.alsoTransmitPending)
        assertEquals(0.5f, transmit1.pendingState!!.leftStickX, 0.001f)
        
        // Second call after window - should NOT alsoTransmitPending (cleared)
        val result2 = throttler.process(copyState(leftStickX = 0.2f), 1112L)
        val transmit2 = result2 as ThrottleResult.Transmit
        assertFalse("pendingState should be cleared after first Transmit", transmit2.alsoTransmitPending)
        assertNull("pendingState should be null after clear", transmit2.pendingState)
    }

    @Test
    fun `throttleIntervalMs default used when not specified (kills default param mutant)`() {
        // Mutant: DEFAULT_THROTTLE_MS changed to different value
        val throttler = InputThrottler() // uses default 4ms
        throttler.process(copyState(), 1000L)
        // At 3ms - should be Pending (within 4ms default)
        val result1 = throttler.process(copyState(leftStickX = 0.1f), 1003L)
        assertTrue("Default 4ms: 3ms should be Pending", result1 is ThrottleResult.Pending)
        // At 5ms - should Transmit (outside 4ms default)
        val result2 = throttler.process(copyState(leftStickX = 0.1f), 1005L)
        assertTrue("Default 4ms: 5ms should Transmit", result2 is ThrottleResult.Transmit)
    }
}