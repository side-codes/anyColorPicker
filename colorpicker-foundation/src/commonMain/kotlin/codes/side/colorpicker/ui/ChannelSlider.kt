package codes.side.colorpicker.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import codes.side.color.ColorChannel
import codes.side.colorpicker.foundation.BasicChannelSlider
import codes.side.colorpicker.foundation.ChannelSliderScope
import codes.side.colorpicker.foundation.ColorPickerStrings
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.ColoringMode
import codes.side.colorpicker.theme.ColorPickerColors
import codes.side.colorpicker.theme.ColorPickerDefaults
import codes.side.colorpicker.theme.ColorPickerDimensions
import codes.side.colorpicker.theme.ColorPickerShapes

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
 * @param onValueChangeFinished called when a tap or drag ends, and after each key press or screen reader
 * step that changes the value.
 * @param label slot above the track's start; the channel's name from [ColorPickerStrings] by default.
 * See [SliderLabel].
 * @param valueLabel slot above the track's end; the value in the channel's usual units, in the locale's
 * number format, by default. See [SliderValueLabel].
 * @param semanticLabel what a screen reader calls the slider; `null` omits it.
 * @param semanticValueText how a screen reader announces the value, in the channel's units by default;
 * `null` omits it.
 * @param dimensions the track's height and the room it leaves for the thumb; see [ColorSlider].
 * @param interactionSource receives the slider's interactions; see [ColorSlider]. Note that if `null` is
 * provided, interactions will still happen internally.
 * @param thumb draws the thumb, reading the channel, its value and the opaque color under it from
 * [ChannelSliderScope]; [ColorPickerDefaults.SliderThumb] by default.
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
    coloringMode: ColoringMode = ColoringMode.defaultFor(channel.space),
    onValueChangeFinished: () -> Unit = {},
    label: (@Composable () -> Unit)? = { SliderLabel(ColorPickerStrings.current.channelName(channel)) },
    valueLabel: (@Composable () -> Unit)? = {
        SliderValueLabel(ColorPickerStrings.current.channelValue(channel, state.displayValue(channel), false))
    },
    semanticLabel: String? = ColorPickerStrings.current.channelSpokenName(channel),
    semanticValueText: String? = ColorPickerStrings.current.channelValue(channel, state.displayValue(channel), true),
    colors: ColorPickerColors = ColorPickerDefaults.currentColors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.currentShapes(),
    dimensions: ColorPickerDimensions = ColorPickerDefaults.currentDimensions(),
    interactionSource: MutableInteractionSource? = null,
    // this., or this function's own interactionSource would shadow the scope's resolved one.
    thumb: @Composable ChannelSliderScope.() -> Unit = { ColorPickerDefaults.SliderThumb(this.interactionSource, thumbColor) },
) {
    SliderFrame(modifier, enabled, colors, label, valueLabel) { sliderModifier ->
        BasicChannelSlider(
            state = state,
            channel = channel,
            modifier = sliderModifier,
            enabled = enabled,
            range = range,
            coloringMode = coloringMode,
            onValueChangeFinished = onValueChangeFinished,
            semanticLabel = semanticLabel,
            semanticValueText = semanticValueText,
            interactionSource = interactionSource,
            track = { SliderTrack(gradient, colors, shapes, dimensions) },
            thumb = thumb,
        )
    }
}
