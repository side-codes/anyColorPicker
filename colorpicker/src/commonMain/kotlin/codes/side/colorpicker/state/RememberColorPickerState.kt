package codes.side.colorpicker.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.model.PickerColor

@Composable
fun rememberColorPickerState(
    initialColor: PickerColor = HslColor(),
): ColorPickerState {
    return remember { ColorPickerState(initialColor) }
}
