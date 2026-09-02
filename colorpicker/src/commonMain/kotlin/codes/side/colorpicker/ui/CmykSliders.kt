package codes.side.colorpicker.ui

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import codes.side.colorpicker.conversion.toComposeColor
import codes.side.colorpicker.model.CmykColor
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.ColoringMode
import codes.side.colorpicker.theme.ColorPickerColors
import codes.side.colorpicker.theme.ColorPickerDefaults
import codes.side.colorpicker.theme.ColorPickerShapes
import kotlinx.collections.immutable.persistentListOf

private val PureCyan = CmykColor(cyan = 1f, magenta = 0f, yellow = 0f, key = 0f).toComposeColor()
private val PureMagenta = CmykColor(cyan = 0f, magenta = 1f, yellow = 0f, key = 0f).toComposeColor()
private val PureYellow = CmykColor(cyan = 0f, magenta = 0f, yellow = 1f, key = 0f).toComposeColor()

/**
 * Slider for the CMYK cyan channel of [state], in `0..1` (displayed as `0..100`%).
 *
 * @param coloringMode with [ColoringMode.Contextual] (the default) the track previews
 * the resulting color at the current magenta, yellow, and key; with
 * [ColoringMode.Independent] it runs from white to pure cyan.
 * @param semanticLabel accessibility description of the slider; pass a localized string
 * to replace the English default, or `null` to omit.
 * @param semanticValueText accessibility announcement of the current value in percent.
 */
@Composable
public fun CyanSlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    coloringMode: ColoringMode = ColoringMode.Contextual,
    label: (@Composable () -> Unit)? = { SliderLabel("Cyan") },
    valueLabel: (@Composable () -> Unit)? = { SliderValueLabel("${state.cmykColor.intCyan}%") },
    semanticLabel: String? = "Cyan",
    semanticValueText: String? = "${state.cmykColor.intCyan}%",
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.shapes(),
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
    thumbWidth: Dp = ColorPickerDefaults.ThumbWidth,
    thumbTrackGap: Dp = ColorPickerDefaults.ThumbTrackGap,
) {
    val cmyk = state.cmykColor
    val gradientColors = remember(cmyk.magenta, cmyk.yellow, cmyk.key, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> persistentListOf(Color.White, PureCyan)
            ColoringMode.Contextual -> persistentListOf(
                CmykColor(
                    cyan = 0f,
                    magenta = cmyk.magenta,
                    yellow = cmyk.yellow,
                    key = cmyk.key,
                ).toComposeColor(),
                CmykColor(
                    cyan = 1f,
                    magenta = cmyk.magenta,
                    yellow = cmyk.yellow,
                    key = cmyk.key,
                ).toComposeColor(),
            )
        }
    }
    val thumbColor = remember(cmyk, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> CmykColor(
                cyan = cmyk.cyan,
                magenta = 0f,
                yellow = 0f,
                key = 0f,
            ).toComposeColor()

            ColoringMode.Contextual -> cmyk.toComposeColor()
        }
    }

    val interaction = remember(state) { SliderInteractionGuard(state) }
    ColorSlider(
        value = cmyk.cyan,
        onValueChange = {
            interaction.begin()
            state.updateCyan(it)
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
 * Slider for the CMYK magenta channel of [state], in `0..1` (displayed as `0..100`%).
 *
 * @param coloringMode with [ColoringMode.Contextual] (the default) the track previews
 * the resulting color at the current cyan, yellow, and key; with
 * [ColoringMode.Independent] it runs from white to pure magenta.
 * @param semanticLabel accessibility description of the slider; pass a localized string
 * to replace the English default, or `null` to omit.
 * @param semanticValueText accessibility announcement of the current value in percent.
 */
@Composable
public fun MagentaSlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    coloringMode: ColoringMode = ColoringMode.Contextual,
    label: (@Composable () -> Unit)? = { SliderLabel("Magenta") },
    valueLabel: (@Composable () -> Unit)? = { SliderValueLabel("${state.cmykColor.intMagenta}%") },
    semanticLabel: String? = "Magenta",
    semanticValueText: String? = "${state.cmykColor.intMagenta}%",
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.shapes(),
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
    thumbWidth: Dp = ColorPickerDefaults.ThumbWidth,
    thumbTrackGap: Dp = ColorPickerDefaults.ThumbTrackGap,
) {
    val cmyk = state.cmykColor
    val gradientColors = remember(cmyk.cyan, cmyk.yellow, cmyk.key, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> persistentListOf(Color.White, PureMagenta)
            ColoringMode.Contextual -> persistentListOf(
                CmykColor(
                    cyan = cmyk.cyan,
                    magenta = 0f,
                    yellow = cmyk.yellow,
                    key = cmyk.key,
                ).toComposeColor(),
                CmykColor(
                    cyan = cmyk.cyan,
                    magenta = 1f,
                    yellow = cmyk.yellow,
                    key = cmyk.key,
                ).toComposeColor(),
            )
        }
    }
    val thumbColor = remember(cmyk, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> CmykColor(
                cyan = 0f,
                magenta = cmyk.magenta,
                yellow = 0f,
                key = 0f,
            ).toComposeColor()

            ColoringMode.Contextual -> cmyk.toComposeColor()
        }
    }

    val interaction = remember(state) { SliderInteractionGuard(state) }
    ColorSlider(
        value = cmyk.magenta,
        onValueChange = {
            interaction.begin()
            state.updateMagenta(it)
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
 * Slider for the CMYK yellow channel of [state], in `0..1` (displayed as `0..100`%).
 *
 * @param coloringMode with [ColoringMode.Contextual] (the default) the track previews
 * the resulting color at the current cyan, magenta, and key; with
 * [ColoringMode.Independent] it runs from white to pure yellow.
 * @param semanticLabel accessibility description of the slider; pass a localized string
 * to replace the English default, or `null` to omit.
 * @param semanticValueText accessibility announcement of the current value in percent.
 */
@Composable
public fun YellowSlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    coloringMode: ColoringMode = ColoringMode.Contextual,
    label: (@Composable () -> Unit)? = { SliderLabel("Yellow") },
    valueLabel: (@Composable () -> Unit)? = { SliderValueLabel("${state.cmykColor.intYellow}%") },
    semanticLabel: String? = "Yellow",
    semanticValueText: String? = "${state.cmykColor.intYellow}%",
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.shapes(),
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
    thumbWidth: Dp = ColorPickerDefaults.ThumbWidth,
    thumbTrackGap: Dp = ColorPickerDefaults.ThumbTrackGap,
) {
    val cmyk = state.cmykColor
    val gradientColors = remember(cmyk.cyan, cmyk.magenta, cmyk.key, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> persistentListOf(Color.White, PureYellow)
            ColoringMode.Contextual -> persistentListOf(
                CmykColor(
                    cyan = cmyk.cyan,
                    magenta = cmyk.magenta,
                    yellow = 0f,
                    key = cmyk.key,
                ).toComposeColor(),
                CmykColor(
                    cyan = cmyk.cyan,
                    magenta = cmyk.magenta,
                    yellow = 1f,
                    key = cmyk.key,
                ).toComposeColor(),
            )
        }
    }
    val thumbColor = remember(cmyk, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> CmykColor(
                cyan = 0f,
                magenta = 0f,
                yellow = cmyk.yellow,
                key = 0f,
            ).toComposeColor()

            ColoringMode.Contextual -> cmyk.toComposeColor()
        }
    }

    val interaction = remember(state) { SliderInteractionGuard(state) }
    ColorSlider(
        value = cmyk.yellow,
        onValueChange = {
            interaction.begin()
            state.updateYellow(it)
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
 * Slider for the CMYK key (black) channel of [state], in `0..1` (displayed as `0..100`%).
 *
 * @param coloringMode with [ColoringMode.Contextual] (the default) the track previews
 * the resulting color at the current cyan, magenta, and yellow; with
 * [ColoringMode.Independent] it runs from white to black.
 * @param semanticLabel accessibility description of the slider; pass a localized string
 * to replace the English default, or `null` to omit.
 * @param semanticValueText accessibility announcement of the current value in percent.
 */
@Composable
public fun KeySlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    coloringMode: ColoringMode = ColoringMode.Contextual,
    label: (@Composable () -> Unit)? = { SliderLabel("Key") },
    valueLabel: (@Composable () -> Unit)? = { SliderValueLabel("${state.cmykColor.intKey}%") },
    semanticLabel: String? = "Key",
    semanticValueText: String? = "${state.cmykColor.intKey}%",
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.shapes(),
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
    thumbWidth: Dp = ColorPickerDefaults.ThumbWidth,
    thumbTrackGap: Dp = ColorPickerDefaults.ThumbTrackGap,
) {
    val cmyk = state.cmykColor
    val gradientColors = remember(cmyk.cyan, cmyk.magenta, cmyk.yellow, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> persistentListOf(Color.White, Color.Black)
            ColoringMode.Contextual -> persistentListOf(
                CmykColor(
                    cyan = cmyk.cyan,
                    magenta = cmyk.magenta,
                    yellow = cmyk.yellow,
                    key = 0f,
                ).toComposeColor(),
                CmykColor(
                    cyan = cmyk.cyan,
                    magenta = cmyk.magenta,
                    yellow = cmyk.yellow,
                    key = 1f,
                ).toComposeColor(),
            )
        }
    }
    val thumbColor = remember(cmyk, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> CmykColor(
                cyan = 0f,
                magenta = 0f,
                yellow = 0f,
                key = cmyk.key,
            ).toComposeColor()

            ColoringMode.Contextual -> cmyk.toComposeColor()
        }
    }

    val interaction = remember(state) { SliderInteractionGuard(state) }
    ColorSlider(
        value = cmyk.key,
        onValueChange = {
            interaction.begin()
            state.updateKey(it)
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
