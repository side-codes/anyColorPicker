package codes.side.colorpicker.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object ColorPickerDefaults {

    @Composable
    fun colors(
        thumbColor: Color = MaterialTheme.colorScheme.surface,
        thumbElevation: Dp = 2.dp,
    ): ColorPickerColors = ColorPickerColors(
        thumbColor = thumbColor,
        thumbElevation = thumbElevation,
    )

    @Composable
    fun shapes(
        trackShape: Shape = MaterialTheme.shapes.small,
        swatchShape: Shape = MaterialTheme.shapes.medium,
    ): ColorPickerShapes = ColorPickerShapes(
        trackShape = trackShape,
        swatchShape = swatchShape,
    )
}
