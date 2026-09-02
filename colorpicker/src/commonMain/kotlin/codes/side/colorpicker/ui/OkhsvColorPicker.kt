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
 * Complete Okhsv picker: hue, saturation, and value sliders, plus an optional alpha
 * slider.
 *
 * Shares Okhsl's perceptual hue and gamut-relative saturation, but keeps the HSV
 * arrangement: full saturation at full value is the most vivid form of a hue, and pulling
 * value down darkens toward black. Pick this over [OkhslColorPicker] when users expect
 * the shape of a paint-program picker; pick Okhsl when the midpoint of the lightness
 * track should be a mid tone.
 *
 * @param showAlpha whether to include the [AlphaSlider].
 * @param coloringMode defaults to [ColoringMode.Independent], for the same reason as
 * [HslColorPicker]: a contextual hue track collapses into a near-uniform strip at low
 * saturation or value, and stops being navigable.
 * @param colors checkerboard colors; see [ColorPickerDefaults.colors].
 * @param shapes track shape; see [ColorPickerDefaults.shapes].
 */
@Composable
public fun OkhsvColorPicker(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    showAlpha: Boolean = true,
    coloringMode: ColoringMode = ColoringMode.Independent,
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.shapes(),
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OkhsvHueSlider(state = state, coloringMode = coloringMode, colors = colors, shapes = shapes)
        OkhsvSaturationSlider(
            state = state,
            coloringMode = coloringMode,
            colors = colors,
            shapes = shapes,
        )
        OkhsvValueSlider(
            state = state,
            coloringMode = coloringMode,
            colors = colors,
            shapes = shapes,
        )
        if (showAlpha) {
            AlphaSlider(state = state, colors = colors, shapes = shapes)
        }
    }
}
