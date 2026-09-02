package codes.side.colorpicker.ui

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import codes.side.colorpicker.conversion.toComposeColor
import codes.side.colorpicker.model.RgbColor
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.ColoringMode
import codes.side.colorpicker.theme.ColorPickerColors
import codes.side.colorpicker.theme.ColorPickerDefaults
import codes.side.colorpicker.theme.ColorPickerShapes
import kotlinx.collections.immutable.persistentListOf

/**
 * Slider for the RGB red channel of [state], in `0..1` (displayed as `0..255`).
 *
 * @param coloringMode with [ColoringMode.Contextual] (the default) the track previews
 * the resulting color at the current green and blue; with [ColoringMode.Independent]
 * it runs from black to pure red.
 * @param semanticLabel accessibility description of the slider; pass a localized string
 * to replace the English default, or `null` to omit.
 * @param semanticValueText accessibility announcement of the current value (`0..255`).
 */
@Composable
public fun RedSlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    coloringMode: ColoringMode = ColoringMode.Contextual,
    label: (@Composable () -> Unit)? = { SliderLabel("Red") },
    valueLabel: (@Composable () -> Unit)? = { SliderValueLabel("${state.rgbColor.intRed}") },
    semanticLabel: String? = "Red",
    semanticValueText: String? = "${state.rgbColor.intRed}",
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.shapes(),
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
    thumbWidth: Dp = ColorPickerDefaults.ThumbWidth,
    thumbTrackGap: Dp = ColorPickerDefaults.ThumbTrackGap,
) {
    val rgb = state.rgbColor
    val gradientColors = remember(rgb.green, rgb.blue, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> persistentListOf(Color.Black, Color.Red)
            ColoringMode.Contextual -> persistentListOf(
                RgbColor(red = 0f, green = rgb.green, blue = rgb.blue).toComposeColor(),
                RgbColor(red = 1f, green = rgb.green, blue = rgb.blue).toComposeColor(),
            )
        }
    }
    val thumbColor = remember(rgb, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> RgbColor(red = rgb.red, green = 0f, blue = 0f).toComposeColor()
            ColoringMode.Contextual -> rgb.toComposeColor()
        }
    }

    val interaction = remember(state) { SliderInteractionGuard(state) }
    ColorSlider(
        value = rgb.red,
        onValueChange = {
            interaction.begin()
            state.updateRed(it)
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
 * Slider for the RGB green channel of [state], in `0..1` (displayed as `0..255`).
 *
 * @param coloringMode with [ColoringMode.Contextual] (the default) the track previews
 * the resulting color at the current red and blue; with [ColoringMode.Independent]
 * it runs from black to pure green.
 * @param semanticLabel accessibility description of the slider; pass a localized string
 * to replace the English default, or `null` to omit.
 * @param semanticValueText accessibility announcement of the current value (`0..255`).
 */
@Composable
public fun GreenSlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    coloringMode: ColoringMode = ColoringMode.Contextual,
    label: (@Composable () -> Unit)? = { SliderLabel("Green") },
    valueLabel: (@Composable () -> Unit)? = { SliderValueLabel("${state.rgbColor.intGreen}") },
    semanticLabel: String? = "Green",
    semanticValueText: String? = "${state.rgbColor.intGreen}",
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.shapes(),
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
    thumbWidth: Dp = ColorPickerDefaults.ThumbWidth,
    thumbTrackGap: Dp = ColorPickerDefaults.ThumbTrackGap,
) {
    val rgb = state.rgbColor
    val gradientColors = remember(rgb.red, rgb.blue, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> persistentListOf(Color.Black, Color.Green)
            ColoringMode.Contextual -> persistentListOf(
                RgbColor(red = rgb.red, green = 0f, blue = rgb.blue).toComposeColor(),
                RgbColor(red = rgb.red, green = 1f, blue = rgb.blue).toComposeColor(),
            )
        }
    }
    val thumbColor = remember(rgb, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> RgbColor(red = 0f, green = rgb.green, blue = 0f).toComposeColor()
            ColoringMode.Contextual -> rgb.toComposeColor()
        }
    }

    val interaction = remember(state) { SliderInteractionGuard(state) }
    ColorSlider(
        value = rgb.green,
        onValueChange = {
            interaction.begin()
            state.updateGreen(it)
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
 * Slider for the RGB blue channel of [state], in `0..1` (displayed as `0..255`).
 *
 * @param coloringMode with [ColoringMode.Contextual] (the default) the track previews
 * the resulting color at the current red and green; with [ColoringMode.Independent]
 * it runs from black to pure blue.
 * @param semanticLabel accessibility description of the slider; pass a localized string
 * to replace the English default, or `null` to omit.
 * @param semanticValueText accessibility announcement of the current value (`0..255`).
 */
@Composable
public fun BlueSlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    coloringMode: ColoringMode = ColoringMode.Contextual,
    label: (@Composable () -> Unit)? = { SliderLabel("Blue") },
    valueLabel: (@Composable () -> Unit)? = { SliderValueLabel("${state.rgbColor.intBlue}") },
    semanticLabel: String? = "Blue",
    semanticValueText: String? = "${state.rgbColor.intBlue}",
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.shapes(),
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
    thumbWidth: Dp = ColorPickerDefaults.ThumbWidth,
    thumbTrackGap: Dp = ColorPickerDefaults.ThumbTrackGap,
) {
    val rgb = state.rgbColor
    val gradientColors = remember(rgb.red, rgb.green, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> persistentListOf(Color.Black, Color.Blue)
            ColoringMode.Contextual -> persistentListOf(
                RgbColor(red = rgb.red, green = rgb.green, blue = 0f).toComposeColor(),
                RgbColor(red = rgb.red, green = rgb.green, blue = 1f).toComposeColor(),
            )
        }
    }
    val thumbColor = remember(rgb, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> RgbColor(red = 0f, green = 0f, blue = rgb.blue).toComposeColor()
            ColoringMode.Contextual -> rgb.toComposeColor()
        }
    }

    val interaction = remember(state) { SliderInteractionGuard(state) }
    ColorSlider(
        value = rgb.blue,
        onValueChange = {
            interaction.begin()
            state.updateBlue(it)
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
