package codes.side.colorpicker.ui

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import codes.side.color.GamutMapping
import codes.side.color.compose.toComposeColor
import codes.side.colorpicker.foundation.ColorPickerStrings
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.theme.ColorPickerColors
import codes.side.colorpicker.theme.ColorPickerDefaults
import codes.side.colorpicker.theme.ColorPickerShapes

// How far Left or Right and Page Up or Page Down move alpha; a screen reader steps by the first.
private const val ALPHA_STEP = 0.01
private const val ALPHA_PAGE_STEP = 0.1

/**
 * Slider for the alpha (opacity) of [state], from transparent to opaque over a transparency
 * checkerboard. It edits alpha alone and keeps the color's space. A missing alpha reads 0, as CSS reads
 * `none`, and moving the slider gives it a value.
 *
 * Left and Right move alpha by 0.01, Page Up and Page Down by 0.1, and Home and End to transparent and
 * opaque; a screen reader steps by 0.01.
 *
 * @param onValueChangeFinished called when a drag ends, and after each key press or screen reader step
 * that changes the value.
 * @param semanticLabel accessibility description of the slider; "Alpha" from [ColorPickerStrings] by
 * default.
 * @param semanticValueText accessibility announcement of the current value (`0..255`), in the locale's
 * number format by default.
 * @param interactionSource receives the slider's interactions; see [ColorSlider]. Note that if `null` is
 * provided, interactions will still happen internally.
 */
@Composable
public fun AlphaSlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChangeFinished: () -> Unit = {},
    label: (@Composable () -> Unit)? = { SliderLabel(ColorPickerStrings.current.alphaName()) },
    valueLabel: (@Composable () -> Unit)? = { SliderValueLabel(ColorPickerStrings.current.alphaValue(state.value.alpha, false)) },
    semanticLabel: String? = ColorPickerStrings.current.alphaName(),
    semanticValueText: String? = ColorPickerStrings.current.alphaValue(state.value.alpha, true),
    colors: ColorPickerColors = ColorPickerDefaults.currentColors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.currentShapes(),
    interactionSource: MutableInteractionSource? = null,
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
    thumbWidth: Dp = ColorPickerDefaults.currentDimensions().thumbWidth,
    thumbTrackGap: Dp = ColorPickerDefaults.currentDimensions().thumbTrackGap,
) {
    val value = state.value
    val opaque = value.withAlpha(1.0)
    // The color itself, brought into sRGB as the other tracks are, faded in from transparent so the
    // gradient previews the color instead of fading through transparent black.
    val opaqueColor = remember(opaque) { opaque.toComposeColor(mapping = GamutMapping.ChromaReduction) }
    val stops = remember(opaqueColor) { TrackStops(listOf(opaqueColor.copy(alpha = 0f), opaqueColor), null) }
    val interaction = remember(state) { SliderInteractionGuard(state) }
    val currentFinished by rememberUpdatedState(onValueChangeFinished)

    // A key press moves from the alpha edits build on, so two presses before a recomposition move two
    // steps.
    fun step(direction: Int, page: Boolean): Boolean {
        val current = state.editBase.alpha
        val next = (current + direction * if (page) ALPHA_PAGE_STEP else ALPHA_STEP).coerceIn(0.0, 1.0)
        if (next == current) return false
        state.editAlpha(next)
        return true
    }

    ColorSliderImpl(
        value = value.alpha.toFloat(),
        onValueChange = {
            interaction.begin()
            state.editAlpha(it.toDouble())
        },
        onStep = ::step,
        accessibilitySteps = accessibilitySteps(0.0..1.0, ALPHA_STEP),
        stops = stops,
        thumbColor = opaqueColor,
        modifier = modifier,
        label = label,
        valueLabel = valueLabel,
        onValueChangeFinished = {
            interaction.end()
            currentFinished()
        },
        enabled = enabled,
        showCheckerboard = true,
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
