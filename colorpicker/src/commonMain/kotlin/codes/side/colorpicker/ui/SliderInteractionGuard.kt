package codes.side.colorpicker.ui

import androidx.compose.runtime.RememberObserver
import codes.side.colorpicker.state.ColorPickerState

/**
 * Manages [ColorPickerState.isInteracting] for a single slider's drag gesture.
 *
 * M3 Slider does not invoke `onValueChangeFinished` when its node is removed from
 * composition mid-gesture (the pointer-input coroutine is simply cancelled), which
 * would leave the flag stuck at `true` on a state that outlives the slider (e.g.
 * hoisted in a view model). Remembering this guard makes the flag self-healing:
 * [onForgotten] clears it if — and only if — this slider set it and the gesture
 * never finished.
 */
internal class SliderInteractionGuard(private val state: ColorPickerState) : RememberObserver {

    private var active = false

    /** Call from `onValueChange`: marks the gesture active. */
    fun begin() {
        active = true
        state.isInteracting = true
    }

    /** Call from `onValueChangeFinished`: marks the gesture finished. */
    fun end() {
        active = false
        state.isInteracting = false
    }

    override fun onRemembered() {}

    override fun onForgotten() {
        // Disposed mid-drag: onValueChangeFinished will never fire. Clear the flag
        // only if this slider was the one interacting, so disposing an idle slider
        // never clobbers another slider's in-progress gesture.
        if (active) end()
    }

    override fun onAbandoned() {
        if (active) end()
    }
}
