package codes.side.colorpicker.ui

import codes.side.colorpicker.state.ColorPickerState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Regression tests for [SliderInteractionGuard]: `isInteracting` must never be left
 * stuck at `true` when a slider leaves composition mid-drag, because M3 Slider does
 * not invoke `onValueChangeFinished` when its node is disposed during a gesture.
 */
class SliderInteractionGuardTest {

    @Test
    fun beginSetsIsInteracting() {
        val state = ColorPickerState()
        val guard = SliderInteractionGuard(state)
        guard.begin()
        assertTrue(state.isInteracting)
    }

    @Test
    fun endClearsIsInteracting() {
        val state = ColorPickerState()
        val guard = SliderInteractionGuard(state)
        guard.begin()
        guard.end()
        assertFalse(state.isInteracting)
    }

    @Test
    fun forgottenMidDragClearsIsInteracting() {
        // Slider removed from composition while a drag is in progress.
        val state = ColorPickerState()
        val guard = SliderInteractionGuard(state)
        guard.begin()
        guard.onForgotten()
        assertFalse(state.isInteracting)
    }

    @Test
    fun forgottenAfterGestureFinishedDoesNothing() {
        val state = ColorPickerState()
        val guard = SliderInteractionGuard(state)
        guard.begin()
        guard.end()
        guard.onForgotten()
        assertFalse(state.isInteracting)
    }

    @Test
    fun forgottenIdleGuardDoesNotClobberAnotherSlidersGesture() {
        // An idle slider (e.g. AlphaSlider toggled off) leaves composition while a
        // different slider on the same state is mid-drag: the flag must survive.
        val state = ColorPickerState()
        val dragging = SliderInteractionGuard(state)
        val idle = SliderInteractionGuard(state)
        dragging.begin()
        idle.onForgotten()
        assertTrue(state.isInteracting)
    }

    @Test
    fun abandonedMidDragClearsIsInteracting() {
        val state = ColorPickerState()
        val guard = SliderInteractionGuard(state)
        guard.begin()
        guard.onAbandoned()
        assertFalse(state.isInteracting)
    }
}
