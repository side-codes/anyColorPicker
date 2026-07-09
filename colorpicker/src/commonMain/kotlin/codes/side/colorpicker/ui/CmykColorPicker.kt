package codes.side.colorpicker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.ColoringMode

@Composable
fun CmykColorPicker(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    showAlpha: Boolean = true,
    coloringMode: ColoringMode = ColoringMode.Contextual,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CyanSlider(state = state, coloringMode = coloringMode)
        MagentaSlider(state = state, coloringMode = coloringMode)
        YellowSlider(state = state, coloringMode = coloringMode)
        KeySlider(state = state, coloringMode = coloringMode)
        if (showAlpha) {
            AlphaSlider(state = state)
        }
    }
}
