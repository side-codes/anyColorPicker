package codes.side.colorpicker.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Immutable
data class ColorPickerShapes(
    val trackShape: Shape = RoundedCornerShape(4.dp),
    val swatchShape: Shape = RoundedCornerShape(8.dp),
)
