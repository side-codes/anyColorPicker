package codes.side.colorpicker.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import codes.side.colorpicker.foundation.AlphaSliderScope
import codes.side.colorpicker.foundation.BasicAlphaSlider
import codes.side.colorpicker.foundation.ColorPickerStrings
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.theme.ColorPickerColors
import codes.side.colorpicker.theme.ColorPickerDefaults
import codes.side.colorpicker.theme.ColorPickerDimensions
import codes.side.colorpicker.theme.ColorPickerShapes

/**
 * Slider for the alpha (opacity) of [state], from transparent to opaque over a transparency
 * checkerboard. It edits alpha alone and keeps the color's space. A missing alpha reads 0, as CSS reads
 * `none`, and moving the slider gives it a value.
 *
 * Left and Right move alpha by 0.01, Page Up and Page Down by 0.1, and Home and End to transparent and
 * opaque; a screen reader steps by 0.01.
 *
 * @param onValueChangeFinished called when a tap or drag ends, and after each key press or screen reader
 * step that changes the value.
 * @param semanticLabel accessibility description of the slider; "Alpha" from [ColorPickerStrings] by
 * default.
 * @param semanticValueText accessibility announcement of the current value (`0..255`), in the locale's
 * number format by default.
 * @param dimensions the track's height and the room it leaves for the thumb; see [ColorSlider].
 * @param interactionSource receives the slider's interactions; see [ColorSlider]. Note that if `null` is
 * provided, interactions will still happen internally.
 * @param thumb draws the thumb, reading the alpha and the opaque color from [AlphaSliderScope];
 * [ColorPickerDefaults.SliderThumb] by default.
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
    dimensions: ColorPickerDimensions = ColorPickerDefaults.currentDimensions(),
    interactionSource: MutableInteractionSource? = null,
    // this., or this function's own interactionSource would shadow the scope's resolved one.
    thumb: @Composable AlphaSliderScope.() -> Unit = { ColorPickerDefaults.SliderThumb(this.interactionSource, thumbColor) },
) {
    SliderFrame(modifier, enabled, colors, label, valueLabel) { sliderModifier ->
        BasicAlphaSlider(
            state = state,
            modifier = sliderModifier,
            enabled = enabled,
            onValueChangeFinished = onValueChangeFinished,
            semanticLabel = semanticLabel,
            semanticValueText = semanticValueText,
            interactionSource = interactionSource,
            track = { SliderTrack(gradient, colors, shapes, dimensions, showCheckerboard = true) },
            thumb = thumb,
        )
    }
}
