package codes.side.colorpicker.foundation

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalLayoutDirection
import codes.side.color.ColorChannel
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.ColoringMode
import kotlin.math.roundToInt

/**
 * What a [BasicChannelSlider]'s track and thumb draw from: a [ColorSliderScope] with the channel's own.
 * Only the library implements it.
 */
@Stable
public sealed interface ChannelSliderScope : ColorSliderScope {
    /** The channel the slider moves. */
    public val channel: ColorChannel

    /** The channel's value as the slider shows it, [ColorPickerState.displayValue], even past the track's end. */
    public val value: Double

    /**
     * The track's colors from its start to its end in the layout direction, so already mirrored right to
     * left: computed in [channel]'s space and brought into sRGB by chroma reduction.
     */
    public val gradient: Brush
}

/**
 * A slider for one [channel] of [state] with no look of its own: [track] and [thumb] draw it, reading
 * the channel, its value and the track's colors from [ChannelSliderScope]. It edits the channel in
 * [state] and leaves the color in [channel]'s space.
 *
 * The thumb sits at [ColorPickerState.displayValue], so a grey's hue slider shows the hue last chosen.
 * A value outside [range], such as OkLCh chroma 0.5 or extended sRGB, pins the thumb to that end while
 * [ChannelSliderScope.value] keeps its true number; it changes only when the user moves the slider.
 * The right end of a hue reads just below 360, so a thumb dragged there stays there rather than
 * wrapping to 0.
 *
 * With [ColoringMode.Contextual], each point of the gradient is the color the slider would make there.
 * With [ColoringMode.Independent], the other channels sit at fixed anchors, clear colors of middle
 * lightness, and a hue stays at the displayed one. [ColorSliderScope.thumbColor] is the color under
 * the thumb, opaque, so a thumb stays visible at alpha 0.
 *
 * It moves as [BasicColorSlider] does, by [ColorChannel.step] for Left and Right and a screen reader's
 * step and by [ColorChannel.pageStep] for Page Up and Page Down, held to [range]; Home and End jump to
 * its ends.
 *
 * @param range the values the track spans, within [ColorChannel.limit].
 * @param coloringMode how the gradient holds the other channels.
 * @param onValueChangeFinished called when a tap or drag ends, and after each key press or screen reader
 * step that changes the value.
 * @param semanticLabel what a screen reader calls the slider; `null` omits it.
 * @param semanticValueText how a screen reader announces the value, in the channel's units by default;
 * `null` omits it.
 * @param interactionSource receives the slider's press, drag, focus and hover interactions; see
 * [BasicColorSlider]. Note that if `null` is provided, interactions will still happen internally.
 * @param track draws the track at the width it is measured at.
 * @param thumb draws the thumb at the size it measures to.
 * @throws IllegalArgumentException if [range] is not a finite span of increasing values within
 * [ColorChannel.limit].
 */
@Composable
public fun BasicChannelSlider(
    state: ColorPickerState,
    channel: ColorChannel,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    range: ClosedFloatingPointRange<Double> = channel.referenceRange,
    coloringMode: ColoringMode = ColoringMode.defaultFor(channel.space),
    onValueChangeFinished: () -> Unit = {},
    semanticLabel: String? = ColorPickerStrings.current.channelSpokenName(channel),
    semanticValueText: String? = ColorPickerStrings.current.channelValue(channel, state.displayValue(channel), true),
    interactionSource: MutableInteractionSource? = null,
    track: @Composable ChannelSliderScope.() -> Unit,
    thumb: @Composable ChannelSliderScope.() -> Unit,
) {
    requireSliderRange(channel, range)
    val displayed = state.displayComponents(channel.space)
    val value = displayed[channel.index]
    val held = heldComponents(channel, displayed, coloringMode)
    val heldKey = held.toList()
    val stops = remember(channel, range, heldKey) { trackStops(channel, held, range) }
    val layoutDirection = LocalLayoutDirection.current
    val gradient = remember(stops, layoutDirection) { stops.brush(layoutDirection) }
    val shown = value.coerceIn(range.start, range.endInclusive)
    val thumbColor = remember(channel, heldKey, shown) { trackColorAt(channel, held, shown) }
    val fraction = fractionOf(value, range)
    val interaction = remember(state) { SliderInteractionGuard(state) }
    val currentFinished by rememberUpdatedState(onValueChangeFinished)

    // A key press moves from the value edits build on: the state's, or the last one emitted and not
    // yet answered, so two presses before a recomposition move two steps.
    fun step(direction: Int, page: Boolean): Boolean {
        val current = state.displayComponents(channel.space, state.editBase)[channel.index]
        val next = clampToRange(channel, range, current + direction * if (page) channel.pageStep else channel.step)
        if (next == current) return false
        state.edit(channel, next)
        return true
    }

    BasicColorSliderImpl(
        value = fraction,
        onValueChange = {
            interaction.begin()
            state.edit(channel, channelValueAt(channel, range, it.toDouble()))
        },
        onStep = ::step,
        accessibilitySteps = accessibilitySteps(range, channel.step),
        modifier = modifier,
        enabled = enabled,
        thumbColor = thumbColor,
        onValueChangeFinished = {
            interaction.end()
            currentFinished()
        },
        semanticLabel = semanticLabel,
        semanticValueText = semanticValueText,
        interactionSource = interactionSource,
        track = { channelSlots(this, channel, value, gradient).track() },
        thumb = { channelSlots(this, channel, value, gradient).thumb() },
    )
}

// The plain slider's scope with the channel's own members added.
@Composable
private fun channelSlots(base: ColorSliderScope, channel: ColorChannel, value: Double, gradient: Brush): ChannelSliderScope =
    remember(base, channel, value, gradient) { ChannelSliderSlots(base, channel, value, gradient) }

private class ChannelSliderSlots(
    base: ColorSliderScope,
    override val channel: ColorChannel,
    override val value: Double,
    override val gradient: Brush,
) : ChannelSliderScope, ColorSliderScope by base

/**
 * The steps a slider over [range] reports to accessibility services. Compose moves a slider by a
 * (steps + 1)th of its range per screen reader increment, so a range [step] fits into n times takes
 * n − 1.
 */
internal fun accessibilitySteps(range: ClosedFloatingPointRange<Double>, step: Double): Int =
    (((range.endInclusive - range.start) / step).roundToInt() - 1).coerceAtLeast(0)
