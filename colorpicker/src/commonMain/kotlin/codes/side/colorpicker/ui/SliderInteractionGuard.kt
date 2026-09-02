package codes.side.colorpicker.ui

import androidx.compose.runtime.RememberObserver
import codes.side.colorpicker.state.ColorPickerState

/**
 * Manages [ColorPickerState.isInteracting] for one component's drag gesture — a
 * [ColorSlider] or the [SaturationLightnessPlane].
 *
 * Neither reports the end of a gesture when its node is removed from composition mid-drag:
 * M3 Slider does not invoke `onValueChangeFinished`, and a raw pointer-input coroutine is
 * simply cancelled. Either would leave the flag stuck at `true` on a state that outlives
 * the component (e.g. hoisted in a view model). Remembering this guard makes the flag
 * self-healing: [onForgotten] clears it if — and only if — this component set it and the
 * gesture never finished.
 */
internal class SliderInteractionGuard(private val state: ColorPickerState) : RememberObserver {

    private var active = false

    /** Call from `onValueChange`, or when a pointer goes down: marks the gesture active. */
    fun begin() {
        active = true
        state.isInteracting = true
    }

    /** Call from `onValueChangeFinished`, or when the drag ends: marks the gesture finished. */
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
