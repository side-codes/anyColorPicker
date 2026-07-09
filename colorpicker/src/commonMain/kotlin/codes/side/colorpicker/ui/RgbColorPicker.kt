package codes.side.colorpicker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.ColoringMode

@Composable
fun RgbColorPicker(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    showAlpha: Boolean = true,
    coloringMode: ColoringMode = ColoringMode.Contextual,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RedSlider(state = state, coloringMode = coloringMode)
        GreenSlider(state = state, coloringMode = coloringMode)
        BlueSlider(state = state, coloringMode = coloringMode)
        if (showAlpha) {
            AlphaSlider(state = state)
        }
    }
}
