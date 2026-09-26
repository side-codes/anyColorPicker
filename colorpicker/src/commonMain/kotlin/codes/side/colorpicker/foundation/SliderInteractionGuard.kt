package codes.side.colorpicker.foundation

import androidx.compose.runtime.RememberObserver
import codes.side.colorpicker.state.ColorPickerState

/**
 * Manages [ColorPickerState.isInteracting] for one component's drag gesture — a
 * [BasicChannelSlider], [BasicAlphaSlider] or [BasicChannelPlane].
 *
 * A gesture whose node leaves composition mid-drag may end without reporting it: its
 * pointer-input coroutine can be cancelled rather than finished, which would leave the flag
 * stuck at `true` on a state that outlives the component (e.g. hoisted in a view model).
 * Remembering this guard makes the flag self-healing: [onForgotten] clears it if — and
 * only if — this component set it and the gesture never finished.
 */
internal class SliderInteractionGuard(private val state: ColorPickerState) : RememberObserver {

    private var active = false

    /**
     * Call from `onValueChange`, or when a pointer goes down: marks the gesture active.
     *
     * `onValueChange` fires on every movement, so this counts the component in once and
     * ignores the rest until it ends.
     */
    fun begin() {
        if (active) return
        active = true
        state.beginInteraction()
    }

    /** Call from `onValueChangeFinished`, or when the drag ends: marks the gesture finished. */
    fun end() {
        if (!active) return
        active = false
        state.endInteraction()
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
