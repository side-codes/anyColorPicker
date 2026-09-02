package codes.side.colorpicker.ui

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import codes.side.colorpicker.conversion.toComposeColor
import codes.side.colorpicker.model.OkhsvColor
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.ColoringMode
import codes.side.colorpicker.theme.ColorPickerColors
import codes.side.colorpicker.theme.ColorPickerDefaults
import codes.side.colorpicker.theme.ColorPickerShapes

// Okhsv's hue track meets the same gamut corners as Okhsl's, so it is drawn just off the
// boundary for the same reason; see HUE_TRACK_SATURATION in OkhslSliders.kt.
private const val HUE_TRACK_SATURATION = 0.85f

private const val OK_HUE_STOPS = 32
private const val OK_CHANNEL_STOPS = 16

/**
 * Slider for the Okhsv hue channel of [state], in degrees `0..360`.
 *
 * @param coloringMode with [ColoringMode.Independent] (the default) the track shows the
 * full spectrum at a fixed saturation and value; with [ColoringMode.Contextual] it is
 * rendered at the current saturation and value.
 * @param semanticLabel accessibility description of the slider; pass a localized string
 * to replace the English default, or `null` to omit.
 * @param semanticValueText accessibility announcement of the current value in degrees.
 */
@Composable
public fun OkhsvHueSlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    coloringMode: ColoringMode = ColoringMode.Independent,
    label: (@Composable () -> Unit)? = { SliderLabel("Hue") },
    valueLabel: (@Composable () -> Unit)? = { SliderValueLabel("${state.okhsvColor.intHue}°") },
    semanticLabel: String? = "Hue",
    semanticValueText: String? = "${state.okhsvColor.intHue}°",
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.shapes(),
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
    thumbWidth: Dp = ColorPickerDefaults.ThumbWidth,
    thumbTrackGap: Dp = ColorPickerDefaults.ThumbTrackGap,
) {
    val okhsv = state.okhsvColor
    // The two coloring modes differ only in which saturation and value the strip is drawn
    // at, so they pick the pair rather than each building a gradient of its own.
    val trackSaturation = when (coloringMode) {
        ColoringMode.Independent -> HUE_TRACK_SATURATION
        ColoringMode.Contextual -> okhsv.saturation
    }
    val trackValue = when (coloringMode) {
        ColoringMode.Independent -> 1f
        ColoringMode.Contextual -> okhsv.value
    }
    val gradientColors = remember(trackSaturation, trackValue) {
        buildOkGradient(OK_HUE_STOPS) { fraction ->
            OkhsvColor(
                hue = hueFromFraction(fraction),
                saturation = trackSaturation,
                value = trackValue,
            ).toComposeColor()
        }
    }
    // Matches the track so the thumb never disagrees with the strip under it.
    val thumbColor = remember(okhsv.hue, trackSaturation, trackValue) {
        OkhsvColor(
            hue = okhsv.hue,
            saturation = trackSaturation,
            value = trackValue,
        ).toComposeColor()
    }

    val interaction = remember(state) { SliderInteractionGuard(state) }
    ColorSlider(
        value = okhsv.hue / 360f,
        onValueChange = {
            interaction.begin()
            state.updateOkhsvHue(hueFromFraction(it))
        },
        gradientColors = gradientColors,
        thumbColor = thumbColor,
        label = label,
        valueLabel = valueLabel,
        semanticLabel = semanticLabel,
        semanticValueText = semanticValueText,
        colors = colors,
        shapes = shapes,
        modifier = modifier,
        onValueChangeFinished = { interaction.end() },
        thumb = thumb,
        thumbWidth = thumbWidth,
        thumbTrackGap = thumbTrackGap,
    )
}

/**
 * Slider for the Okhsv saturation channel of [state], in `0..1`, from gray to the most
 * colorful the hue and value allow on the display.
 *
 * @param coloringMode with [ColoringMode.Independent] (the default) the track is drawn at
 * full value; with [ColoringMode.Contextual] it is drawn at the current value.
 * @param semanticLabel accessibility description of the slider; pass a localized string
 * to replace the English default, or `null` to omit.
 * @param semanticValueText accessibility announcement of the current value in percent.
 */
@Composable
public fun OkhsvSaturationSlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    coloringMode: ColoringMode = ColoringMode.Independent,
    label: (@Composable () -> Unit)? = { SliderLabel("Saturation") },
    valueLabel: (@Composable () -> Unit)? = { SliderValueLabel("${state.okhsvColor.intSaturation}%") },
    semanticLabel: String? = "Saturation",
    semanticValueText: String? = "${state.okhsvColor.intSaturation}%",
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.shapes(),
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
    thumbWidth: Dp = ColorPickerDefaults.ThumbWidth,
    thumbTrackGap: Dp = ColorPickerDefaults.ThumbTrackGap,
) {
    val okhsv = state.okhsvColor
    val trackValue = when (coloringMode) {
        ColoringMode.Independent -> 1f
        ColoringMode.Contextual -> okhsv.value
    }
    val gradientColors = remember(okhsv.hue, trackValue) {
        buildOkGradient(OK_CHANNEL_STOPS) { fraction ->
            OkhsvColor(hue = okhsv.hue, saturation = fraction, value = trackValue).toComposeColor()
        }
    }
    val thumbColor = remember(okhsv, trackValue) {
        OkhsvColor(
            hue = okhsv.hue,
            saturation = okhsv.saturation,
            value = trackValue,
        ).toComposeColor()
    }

    val interaction = remember(state) { SliderInteractionGuard(state) }
    ColorSlider(
        value = okhsv.saturation,
        onValueChange = {
            interaction.begin()
            state.updateOkhsvSaturation(it)
        },
        gradientColors = gradientColors,
        thumbColor = thumbColor,
        label = label,
        valueLabel = valueLabel,
        semanticLabel = semanticLabel,
        semanticValueText = semanticValueText,
        colors = colors,
        shapes = shapes,
        modifier = modifier,
        onValueChangeFinished = { interaction.end() },
        thumb = thumb,
        thumbWidth = thumbWidth,
        thumbTrackGap = thumbTrackGap,
    )
}

/**
 * Slider for the Okhsv value channel of [state], in `0..1`, from black to the full
 * brightness of the current hue.
 *
 * @param coloringMode with [ColoringMode.Independent] (the default) the track is drawn at
 * full saturation; with [ColoringMode.Contextual] it is drawn at the current saturation.
 * @param semanticLabel accessibility description of the slider; pass a localized string
 * to replace the English default, or `null` to omit.
 * @param semanticValueText accessibility announcement of the current value in percent.
 */
@Composable
public fun OkhsvValueSlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    coloringMode: ColoringMode = ColoringMode.Independent,
    label: (@Composable () -> Unit)? = { SliderLabel("Value") },
    valueLabel: (@Composable () -> Unit)? = { SliderValueLabel("${state.okhsvColor.intValue}%") },
    semanticLabel: String? = "Value",
    semanticValueText: String? = "${state.okhsvColor.intValue}%",
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.shapes(),
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
    thumbWidth: Dp = ColorPickerDefaults.ThumbWidth,
    thumbTrackGap: Dp = ColorPickerDefaults.ThumbTrackGap,
) {
    val okhsv = state.okhsvColor
    val trackSaturation = when (coloringMode) {
        ColoringMode.Independent -> HUE_TRACK_SATURATION
        ColoringMode.Contextual -> okhsv.saturation
    }
    val gradientColors = remember(okhsv.hue, trackSaturation) {
        buildOkGradient(OK_CHANNEL_STOPS) { fraction ->
            OkhsvColor(
                hue = okhsv.hue,
                saturation = trackSaturation,
                value = fraction,
            ).toComposeColor()
        }
    }
    val thumbColor = remember(okhsv, trackSaturation) {
        OkhsvColor(
            hue = okhsv.hue,
            saturation = trackSaturation,
            value = okhsv.value,
        ).toComposeColor()
    }

    val interaction = remember(state) { SliderInteractionGuard(state) }
    ColorSlider(
        value = okhsv.value,
        onValueChange = {
            interaction.begin()
            state.updateOkhsvValue(it)
        },
        gradientColors = gradientColors,
        thumbColor = thumbColor,
        label = label,
        valueLabel = valueLabel,
        semanticLabel = semanticLabel,
        semanticValueText = semanticValueText,
        colors = colors,
        shapes = shapes,
        modifier = modifier,
        onValueChangeFinished = { interaction.end() },
        thumb = thumb,
        thumbWidth = thumbWidth,
        thumbTrackGap = thumbTrackGap,
    )
}
