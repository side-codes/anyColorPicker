package codes.side.colorpicker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.ColoringMode

@Composable
fun HslColorPicker(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    showAlpha: Boolean = true,
    coloringMode: ColoringMode = ColoringMode.Independent,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HueSlider(state = state, coloringMode = coloringMode)
        SaturationSlider(state = state, coloringMode = coloringMode)
        LightnessSlider(state = state, coloringMode = coloringMode)
        if (showAlpha) {
            AlphaSlider(state = state)
        }
    }
}
