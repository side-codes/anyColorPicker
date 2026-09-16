package codes.side.colorpicker.ui

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.ColoringMode
import codes.side.colorpicker.theme.ColorPickerColors
import codes.side.colorpicker.theme.ColorPickerDefaults
import codes.side.colorpicker.theme.ColorPickerShapes
import codes.side.colorpicker.theme.ColorPickerTheme

/**
 * Complete RGB picker: red, green, and blue sliders, plus an optional alpha slider.
 *
 * Each slider is a slot, defaulted to the channel slider it names. Replace one to relabel or
 * restyle that channel: whatever is passed inherits this picker's colors, shapes and
 * dimensions through the theme, and is dimmed only if [enabled] is forwarded to it — though
 * it is refused input either way.
 *
 * @param showAlpha whether to include the [AlphaSlider].
 * @param coloringMode defaults to [ColoringMode.Contextual] so each track previews
 * the resulting color at the current values of the other channels.
 * @param colors checkerboard colors; see [ColorPickerDefaults.colors].
 * @param shapes track shape; see [ColorPickerDefaults.shapes].
 * @param enabled when `false` the picker is dimmed, refuses input, and reports itself
 * disabled to accessibility. Input is refused by the picker as well as by each slider, so a
 * replaced slider slot cannot stay live even if the call site did not forward this to it.
 * @param thumb optional replacement for every slider's thumb; see [ColorSlider].
 * @param redSlider slot for the red channel; defaults to [RedSlider].
 * @param greenSlider slot for the green channel; defaults to [GreenSlider].
 * @param blueSlider slot for the blue channel; defaults to [BlueSlider].
 * @param alphaSlider the [AlphaSlider], shown only when [showAlpha] is `true`.
 */
@Composable
public fun RgbColorPicker(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    showAlpha: Boolean = true,
    enabled: Boolean = true,
    coloringMode: ColoringMode = ColoringMode.Contextual,
    colors: ColorPickerColors = ColorPickerDefaults.currentColors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.currentShapes(),
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
    redSlider: @Composable () -> Unit = { RedSlider(state, enabled = enabled, coloringMode = coloringMode, thumb = thumb) },
    greenSlider: @Composable () -> Unit = { GreenSlider(state, enabled = enabled, coloringMode = coloringMode, thumb = thumb) },
    blueSlider: @Composable () -> Unit = { BlueSlider(state, enabled = enabled, coloringMode = coloringMode, thumb = thumb) },
    alphaSlider: @Composable () -> Unit = { AlphaSlider(state, enabled = enabled, thumb = thumb) },
) {
    // Provided rather than passed down, so a replaced slider slot inherits the picker's
    // theme without the call site forwarding it.
    ColorPickerTheme(colors = colors, shapes = shapes) {
        Column(
            modifier = modifier.disabledInput(enabled),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            redSlider()
            greenSlider()
            blueSlider()
            if (showAlpha) alphaSlider()
        }
    }
}
