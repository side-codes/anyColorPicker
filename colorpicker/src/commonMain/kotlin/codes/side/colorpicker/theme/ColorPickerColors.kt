package codes.side.colorpicker.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class ColorPickerColors(
    val thumbColor: Color = Color.White,
    val thumbElevation: Dp = 2.dp,
)
