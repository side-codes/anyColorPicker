package codes.side.colorpicker.ui

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import codes.side.color.ColorChannel
import codes.side.colorpicker.foundation.ColorPickerStrings
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.ColoringMode
import codes.side.colorpicker.theme.ColorPickerColors
import codes.side.colorpicker.theme.ColorPickerDefaults
import codes.side.colorpicker.theme.ColorPickerShapes
import kotlin.math.roundToInt

/**
 * A slider for one [channel] of any color space, the library's or an app's. It shows the channel's
 * value in [state] and edits it there, leaving the color in [channel]'s space.
 *
 * The thumb sits at [ColorPickerState.displayValue], so a grey's hue slider shows the hue last chosen.
 * A value outside [range], such as OkLCh chroma 0.5 or extended sRGB, pins the thumb to that end while
 * the label keeps its true number; it changes only when the user moves the slider. The right end of a
 * hue reads just below 360, so a thumb dragged there stays there rather than wrapping to 0.
 *
 * With [ColoringMode.Contextual], each point of the track is the color the slider would make there.
 * With [ColoringMode.Independent], the other channels sit at fixed anchors, clear colors of middle
 * lightness, and a hue stays at the displayed one. Either way the track is computed in [channel]'s
 * space and brought into sRGB by chroma reduction, and the thumb is painted opaque, with the color
 * under it, so it stays visible at alpha 0.
 *
 * Left and Right move by [ColorChannel.step] and Page Up and Page Down by [ColorChannel.pageStep], held
 * to [range], and Home and End jump to its ends; Up and Down are left for moving focus. A screen
 * reader's increments move by [ColorChannel.step]. The track and the left and right arrows are
 * mirrored in right-to-left layouts.
 *
 * @param range the values the track spans, within [ColorChannel.limit].
 * @param coloringMode [ColoringMode.Independent] by default for a space with a hue, else
 * [ColoringMode.Contextual].
 * @param onValueChangeFinished called when a drag ends, and after each key press or screen reader step
 * that changes the value.
 * @param label slot above the track's start; the channel's name from [ColorPickerStrings] by default.
 * See [SliderLabel].
 * @param valueLabel slot above the track's end; the value in the channel's usual units, in the locale's
 * number format, by default. See [SliderValueLabel].
 * @param semanticLabel what a screen reader calls the slider; `null` omits it.
 * @param semanticValueText how a screen reader announces the value, in the channel's units by default;
 * `null` omits it.
 * @param interactionSource receives the slider's interactions; see [ColorSlider]. Note that if `null` is
 * provided, interactions will still happen internally.
 * @param thumb optional replacement for the thumb; see [ColorSlider].
 * @param thumbWidth how much room the track leaves for the thumb; see [ColorSlider].
 * @param thumbTrackGap clearance between the thumb and each track end.
 * @throws IllegalArgumentException if [range] is not a finite span of increasing values within
 * [ColorChannel.limit].
 */
@Composable
public fun ChannelSlider(
    state: ColorPickerState,
    channel: ColorChannel,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    range: ClosedFloatingPointRange<Double> = channel.referenceRange,
    coloringMode: ColoringMode = defaultColoringMode(channel.space),
    onValueChangeFinished: () -> Unit = {},
    label: (@Composable () -> Unit)? = { SliderLabel(ColorPickerStrings.current.channelName(channel)) },
    valueLabel: (@Composable () -> Unit)? = {
        SliderValueLabel(ColorPickerStrings.current.channelValue(channel, state.displayValue(channel), false))
    },
    semanticLabel: String? = ColorPickerStrings.current.channelSpokenName(channel),
    semanticValueText: String? = ColorPickerStrings.current.channelValue(channel, state.displayValue(channel), true),
    colors: ColorPickerColors = ColorPickerDefaults.currentColors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.currentShapes(),
    interactionSource: MutableInteractionSource? = null,
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
    thumbWidth: Dp = ColorPickerDefaults.currentDimensions().thumbWidth,
    thumbTrackGap: Dp = ColorPickerDefaults.currentDimensions().thumbTrackGap,
) {
    requireSliderRange(channel, range)
    val displayed = state.displayComponents(channel.space)
    val value = displayed[channel.index]
    val held = heldComponents(channel, displayed, coloringMode)
    val heldKey = held.toList()
    val stops = remember(channel, range, heldKey) { trackStops(channel, held, range) }
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

    ColorSliderImpl(
        value = fraction,
        onValueChange = {
            interaction.begin()
            state.edit(channel, channelValueAt(channel, range, it.toDouble()))
        },
        onStep = ::step,
        accessibilitySteps = accessibilitySteps(range, channel.step),
        stops = stops,
        thumbColor = thumbColor,
        modifier = modifier,
        label = label,
        valueLabel = valueLabel,
        onValueChangeFinished = {
            interaction.end()
            currentFinished()
        },
        enabled = enabled,
        semanticLabel = semanticLabel,
        semanticValueText = semanticValueText,
        colors = colors,
        shapes = shapes,
        interactionSource = interactionSource,
        thumb = thumb,
        thumbWidth = thumbWidth,
        thumbTrackGap = thumbTrackGap,
    )
}

/**
 * The steps a slider over [range] reports to accessibility services. Compose moves a slider by a
 * (steps + 1)th of its range per screen reader increment, so a range [step] fits into n times takes
 * n − 1.
 */
internal fun accessibilitySteps(range: ClosedFloatingPointRange<Double>, step: Double): Int =
    (((range.endInclusive - range.start) / step).roundToInt() - 1).coerceAtLeast(0)
