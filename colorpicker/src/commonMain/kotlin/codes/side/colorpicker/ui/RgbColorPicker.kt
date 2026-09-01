package codes.side.colorpicker.ui

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

/**
 * Complete RGB picker: red, green, and blue sliders, plus an optional alpha slider.
 *
 * @param showAlpha whether to include the [AlphaSlider].
 * @param coloringMode defaults to [ColoringMode.Contextual] so each track previews
 * the resulting color at the current values of the other channels.
 * @param colors checkerboard colors; see [ColorPickerDefaults.colors].
 * @param shapes track shape; see [ColorPickerDefaults.shapes].
 */
@Composable
public fun RgbColorPicker(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    showAlpha: Boolean = true,
    coloringMode: ColoringMode = ColoringMode.Contextual,
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.shapes(),
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RedSlider(state = state, coloringMode = coloringMode, colors = colors, shapes = shapes)
        GreenSlider(state = state, coloringMode = coloringMode, colors = colors, shapes = shapes)
        BlueSlider(state = state, coloringMode = coloringMode, colors = colors, shapes = shapes)
        if (showAlpha) {
            AlphaSlider(state = state, colors = colors, shapes = shapes)
        }
    }
}
