package codes.side.colorpicker.foundation

import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.offset
import codes.side.colorpicker.ui.LocalPickerEnabled
import codes.side.colorpicker.ui.accessibilitySteps
import kotlin.math.max
import kotlin.math.roundToInt

/** What a [BasicColorSlider]'s track and thumb draw from. Only the library implements it. */
@Stable
public sealed interface ColorSliderScope {
    /** Where the thumb is: `0` at the track's start, `1` at its end. */
    public val fraction: Float

    /** False while the slider, or a picker around it, is disabled. */
    public val enabled: Boolean

    /** The slider's press, drag, focus and hover interactions. */
    public val interactionSource: InteractionSource

    /** The color the thumb shows, opaque; [Color.Unspecified] when the slider was given none. */
    public val thumbColor: Color
}

/**
 * A slider over `0..1` with no look of its own: [track] and [thumb] draw it, reading where the thumb is
 * and what the slider is doing from [ColorSliderScope].
 *
 * The track is measured at the slider's width less the thumb's and placed half a thumb in, so the
 * thumb's centre travels it from `0` at the start to `1` at the end. Both are centred vertically and
 * mirrored in right-to-left layouts, and the slider is as tall as the taller of the two. It imposes no
 * minimum size.
 *
 * A tap moves the thumb to the point tapped, and a drag, once past the touch slop, moves it with the
 * finger. Left and Right move it by [step], swapped in right-to-left layouts, Page Up and Page Down by
 * [pageStep], and Home and End to the ends; Up and Down are left for moving focus. A screen reader
 * steps by [step].
 *
 * @param value where the thumb is, in `0..1`. A value outside is drawn at the nearer end, and NaN at
 * the start.
 * @param onValueChange called with each position the user picks, in `0..1`.
 * @param enabled false takes the slider out of pointer, key and screen reader input and out of focus,
 * and tells the slots through [ColorSliderScope.enabled].
 * @param thumbColor the color the thumb shows, handed to the slots opaque so a thumb stays visible over
 * a transparent color.
 * @param onValueChangeFinished called when a tap or drag ends, and after each key press or screen reader
 * step that changes the value.
 * @param step how far Left, Right and a screen reader's step move the value; above 0.
 * @param pageStep how far Page Up and Page Down move it; above 0.
 * @param semanticLabel what a screen reader calls the slider; `null` omits it.
 * @param semanticValueText how a screen reader announces the value, the position as a percentage in the
 * locale's number format by default; `null` omits it.
 * @param interactionSource receives the slider's press, drag, focus and hover interactions, which the
 * slots read from [ColorSliderScope.interactionSource]. Note that if `null` is provided, interactions
 * will still happen internally.
 * @param track draws the track at the width it is measured at.
 * @param thumb draws the thumb at the size it measures to.
 * @throws IllegalArgumentException unless [step] and [pageStep] are above 0.
 */
@Composable
public fun BasicColorSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    thumbColor: Color = Color.Unspecified,
    onValueChangeFinished: (() -> Unit)? = null,
    step: Float = 0.01f,
    pageStep: Float = 0.1f,
    semanticLabel: String? = null,
    semanticValueText: String? = ColorPickerStrings.current.sliderPosition(value),
    interactionSource: MutableInteractionSource? = null,
    track: @Composable ColorSliderScope.() -> Unit,
    thumb: @Composable ColorSliderScope.() -> Unit,
) {
    requireSliderSteps(step, pageStep)
    BasicColorSliderImpl(
        value = value,
        onValueChange = onValueChange,
        onStep = rememberFractionSteps(value, step, pageStep, onValueChange),
        accessibilitySteps = accessibilitySteps(0.0..1.0, step.toDouble()),
        modifier = modifier,
        enabled = enabled,
        thumbColor = thumbColor,
        onValueChangeFinished = onValueChangeFinished,
        semanticLabel = semanticLabel,
        semanticValueText = semanticValueText,
        interactionSource = interactionSource,
        track = track,
        thumb = thumb,
    )
}

/**
 * [BasicColorSlider] with the keys' steps left to [onStep], which gets the direction, `1` toward the end
 * and `-1` toward the start, and whether a page was asked for, and returns false when the step changes
 * nothing. [accessibilitySteps] is what the progress range reports; see [accessibilitySteps].
 */
@Composable
internal fun BasicColorSliderImpl(
    value: Float,
    onValueChange: (Float) -> Unit,
    onStep: (direction: Int, page: Boolean) -> Boolean,
    accessibilitySteps: Int,
    modifier: Modifier,
    enabled: Boolean,
    thumbColor: Color,
    onValueChangeFinished: (() -> Unit)?,
    semanticLabel: String?,
    semanticValueText: String?,
    interactionSource: MutableInteractionSource?,
    track: @Composable ColorSliderScope.() -> Unit,
    thumb: @Composable ColorSliderScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val active = enabled && LocalPickerEnabled.current
    val fraction = sliderFraction(value)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val slots = remember(fraction, active, source, thumbColor) {
        SliderSlots(fraction, active, source, thumbColor.asOpaqueThumb())
    }

    // A key's step ends where it lands, so one that changes the value reports its end at once.
    fun step(direction: Int, page: Boolean) {
        if (onStep(direction, page)) onValueChangeFinished?.invoke()
    }

    fun jump(to: Float) {
        if (to == fraction) return
        onValueChange(to)
        onValueChangeFinished?.invoke()
    }

    Layout(
        content = {
            Box { slots.thumb() }
            Box { slots.track() }
        },
        modifier = modifier
            .semantics(mergeDescendants = true) {
                semanticLabel?.let { contentDescription = it }
                semanticValueText?.let { stateDescription = it }
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f, accessibilitySteps)
                if (active) {
                    // False when nothing changes, which is how a screen reader learns it is at an end.
                    setProgress { target ->
                        val next = sliderFraction(target)
                        if (next == fraction) {
                            false
                        } else {
                            onValueChange(next)
                            onValueChangeFinished?.invoke()
                            true
                        }
                    }
                } else {
                    disabled()
                }
            }
            .onKeyEvent { event ->
                if (!active || event.key !in SliderKeys) return@onKeyEvent false
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionRight -> step(if (rtl) -1 else 1, page = false)
                        Key.DirectionLeft -> step(if (rtl) 1 else -1, page = false)
                        Key.PageUp -> step(1, page = true)
                        Key.PageDown -> step(-1, page = true)
                        Key.MoveHome -> jump(0f)
                        Key.MoveEnd -> jump(1f)
                    }
                }
                true
            }
            .focusable(active, source),
    ) { measurables, constraints ->
        val thumbPlaceable = measurables[0].measure(constraints.copy(minWidth = 0, minHeight = 0))
        val trackPlaceable = measurables[1].measure(
            constraints.offset(horizontal = -thumbPlaceable.width).copy(minHeight = 0),
        )
        val width = constraints.constrainWidth(thumbPlaceable.width + trackPlaceable.width)
        val height = constraints.constrainHeight(max(thumbPlaceable.height, trackPlaceable.height))
        layout(width, height) {
            trackPlaceable.placeRelative(thumbPlaceable.width / 2, (height - trackPlaceable.height) / 2)
            // Placed after the track, so it is drawn over it.
            thumbPlaceable.placeRelative(
                (trackPlaceable.width * fraction).roundToInt(),
                (height - thumbPlaceable.height) / 2,
            )
        }
    }
}

// The keys a slider takes. Up and Down are not among them: a horizontal slider that kept them would
// trap focus on a device driven by a D-pad alone.
private val SliderKeys = setOf(Key.DirectionRight, Key.DirectionLeft, Key.PageUp, Key.PageDown, Key.MoveHome, Key.MoveEnd)

private class SliderSlots(
    override val fraction: Float,
    override val enabled: Boolean,
    override val interactionSource: InteractionSource,
    override val thumbColor: Color,
) : ColorSliderScope

/** [value] as a position on the track: held to `0..1`, and NaN at the start. */
internal fun sliderFraction(value: Float): Float = if (value.isNaN()) 0f else value.coerceIn(0f, 1f)

/**
 * A thumb is always painted opaque. One that inherited the color's alpha would vanish exactly when the
 * color became transparent, leaving nothing to grab — and on the alpha slider that is the thumb you
 * need in order to drag back.
 */
private fun Color.asOpaqueThumb(): Color = if (isUnspecified || alpha == 1f) this else copy(alpha = 1f)

/** @throws IllegalArgumentException unless [step] and [pageStep] are above 0. */
internal fun requireSliderSteps(step: Float, pageStep: Float) {
    require(step > 0f && pageStep > 0f) { "A slider's step and pageStep must be above 0, were $step and $pageStep" }
}

/**
 * A key's step for a slider over [value] in `0..1`, by [step] or [pageStep], reported to [onValueChange].
 * Keys step from the last value they reported until the next composition answers it, so two presses
 * that arrive before a recomposition move two steps rather than reporting one step twice.
 */
@Composable
internal fun rememberFractionSteps(
    value: Float,
    step: Float,
    pageStep: Float,
    onValueChange: (Float) -> Unit,
): (direction: Int, page: Boolean) -> Boolean {
    val base = remember { FractionBase() }
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val answered = sliderFraction(value)
    SideEffect { base.value = answered }
    return remember(step, pageStep) {
        { direction: Int, page: Boolean ->
            val from = base.value
            val next = (from + direction * if (page) pageStep else step).coerceIn(0f, 1f)
            if (next == from) {
                false
            } else {
                base.value = next
                currentOnValueChange(next)
                true
            }
        }
    }
}

// Where keys step from: written after each composition, and by each step until the next one.
private class FractionBase {
    var value: Float = 0f
}
