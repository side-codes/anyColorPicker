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
 * Complete HSL picker: hue, saturation, and lightness sliders, plus an optional
 * alpha slider.
 *
 * @param showAlpha whether to include the [AlphaSlider].
 * @param coloringMode defaults to [ColoringMode.Independent], unlike the
 * RGB/CMYK/LAB pickers, because the hue slider must always show the full spectrum
 * to stay navigable — at low saturation or extreme lightness a contextual hue
 * track collapses into a near-uniform strip.
 * @param colors checkerboard colors; see [ColorPickerDefaults.colors].
 * @param shapes track shape; see [ColorPickerDefaults.shapes].
 */
@Composable
public fun HslColorPicker(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    showAlpha: Boolean = true,
    // Independent by default (unlike the RGB/CMYK/LAB pickers): the hue slider
    // must always show the full spectrum to stay navigable.
    coloringMode: ColoringMode = ColoringMode.Independent,
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.shapes(),
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HueSlider(state = state, coloringMode = coloringMode, colors = colors, shapes = shapes)
        SaturationSlider(
            state = state,
            coloringMode = coloringMode,
            colors = colors,
            shapes = shapes,
        )
        LightnessSlider(
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
