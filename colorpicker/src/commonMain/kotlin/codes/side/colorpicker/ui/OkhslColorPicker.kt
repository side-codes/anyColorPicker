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
 * Complete Okhsl picker: hue, saturation, and lightness sliders, plus an optional alpha
 * slider.
 *
 * The perceptual counterpart to [HslColorPicker]. Lightness here means perceived
 * lightness, so the middle of the track looks equally light at every hue, and saturation
 * is measured against the display gamut, so `100%` is reachable at every hue and
 * lightness rather than running off the end of what the screen can show.
 *
 * @param showAlpha whether to include the [AlphaSlider].
 * @param coloringMode defaults to [ColoringMode.Independent], for the same reason as
 * [HslColorPicker]: a contextual hue track collapses into a near-uniform strip at low
 * saturation or extreme lightness, and stops being navigable.
 * @param colors checkerboard colors; see [ColorPickerDefaults.colors].
 * @param shapes track shape; see [ColorPickerDefaults.shapes].
 */
@Composable
public fun OkhslColorPicker(
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
        OkhslHueSlider(state = state, coloringMode = coloringMode, colors = colors, shapes = shapes)
        OkhslSaturationSlider(
            state = state,
            coloringMode = coloringMode,
            colors = colors,
            shapes = shapes,
        )
        OkhslLightnessSlider(
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
