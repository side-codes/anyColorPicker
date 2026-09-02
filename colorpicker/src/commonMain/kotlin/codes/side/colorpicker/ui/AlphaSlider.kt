package codes.side.colorpicker.ui

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import codes.side.colorpicker.conversion.toComposeColor
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.theme.ColorPickerColors
import codes.side.colorpicker.theme.ColorPickerDefaults
import codes.side.colorpicker.theme.ColorPickerShapes
import kotlinx.collections.immutable.persistentListOf

/**
 * Slider for the alpha (opacity) channel of [state], from transparent to opaque over
 * a transparency checkerboard. Updating alpha keeps the state's current origin space.
 *
 * @param semanticLabel accessibility description of the slider; pass a localized
 * string to replace the English default.
 * @param semanticValueText accessibility announcement of the current value (`0..255`).
 */
@Composable
public fun AlphaSlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = { SliderLabel("Alpha") },
    valueLabel: (@Composable () -> Unit)? = { SliderValueLabel("${state.hslColor.intAlpha}") },
    semanticLabel: String? = "Alpha",
    semanticValueText: String? = "${state.hslColor.intAlpha}",
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.shapes(),
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
    thumbWidth: Dp = ColorPickerDefaults.ThumbWidth,
    thumbTrackGap: Dp = ColorPickerDefaults.ThumbTrackGap,
) {
    val hsl = state.hslColor
    val opaqueColor = remember(hsl.hue, hsl.saturation, hsl.lightness) {
        hsl.copy(alpha = 1f).toComposeColor()
    }
    // Fade the current hue from transparent to opaque so the gradient previews
    // the actual color instead of fading through transparent black.
    val gradientColors = remember(opaqueColor) {
        persistentListOf(opaqueColor.copy(alpha = 0f), opaqueColor)
    }
    val thumbColor = remember(hsl) { hsl.toComposeColor() }

    val interaction = remember(state) { SliderInteractionGuard(state) }
    ColorSlider(
        value = hsl.alpha,
        onValueChange = {
            interaction.begin()
            state.updateAlpha(it)
        },
        gradientColors = gradientColors,
        thumbColor = thumbColor,
        label = label,
        valueLabel = valueLabel,
        showCheckerboard = true,
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
