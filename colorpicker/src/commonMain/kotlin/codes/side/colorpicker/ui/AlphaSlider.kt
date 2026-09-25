package codes.side.colorpicker.ui

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import codes.side.color.GamutMapping
import codes.side.color.compose.toComposeColor
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.theme.ColorPickerColors
import codes.side.colorpicker.theme.ColorPickerDefaults
import codes.side.colorpicker.theme.ColorPickerShapes
import kotlinx.collections.immutable.persistentListOf

/**
 * Slider for the alpha (opacity) of [state], from transparent to opaque over a transparency
 * checkerboard. It edits alpha alone and keeps the color's space. A missing alpha reads 0, as CSS reads
 * `none`, and moving the slider gives it a value.
 *
 * @param semanticLabel accessibility description of the slider; pass a localized string to replace the
 * English default.
 * @param semanticValueText accessibility announcement of the current value (`0..255`).
 */
@Composable
public fun AlphaSlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: (@Composable () -> Unit)? = { SliderLabel(ALPHA_LABEL) },
    valueLabel: (@Composable () -> Unit)? = { SliderValueLabel(alphaValueText(state.value.alpha)) },
    semanticLabel: String? = ALPHA_LABEL,
    semanticValueText: String? = alphaValueText(state.value.alpha),
    colors: ColorPickerColors = ColorPickerDefaults.currentColors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.currentShapes(),
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
    thumbWidth: Dp = ColorPickerDefaults.currentDimensions().thumbWidth,
    thumbTrackGap: Dp = ColorPickerDefaults.currentDimensions().thumbTrackGap,
) {
    val value = state.value
    val opaque = value.withAlpha(1.0)
    // The color itself, brought into sRGB as the other tracks are, faded in from transparent so the
    // gradient previews the color instead of fading through transparent black.
    val opaqueColor = remember(opaque) { opaque.toComposeColor(mapping = GamutMapping.ChromaReduction) }
    val gradientColors = remember(opaqueColor) { persistentListOf(opaqueColor.copy(alpha = 0f), opaqueColor) }

    val interaction = remember(state) { SliderInteractionGuard(state) }
    ColorSlider(
        value = value.alpha.toFloat(),
        onValueChange = {
            interaction.begin()
            state.editAlpha(it.toDouble())
        },
        gradientColors = gradientColors,
        thumbColor = opaqueColor,
        label = label,
        valueLabel = valueLabel,
        showCheckerboard = true,
        semanticLabel = semanticLabel,
        semanticValueText = semanticValueText,
        colors = colors,
        shapes = shapes,
        modifier = modifier,
        enabled = enabled,
        onValueChangeFinished = { interaction.end() },
        thumb = thumb,
        thumbWidth = thumbWidth,
        thumbTrackGap = thumbTrackGap,
    )
}
