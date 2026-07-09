package codes.side.colorpicker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun TransparencyCheckerboard(
    modifier: Modifier = Modifier,
    cellSize: Dp = 6.dp,
    lightColor: Color = Color(0xFFFFFFFF),
    darkColor: Color = Color(0xFFCCCCCC),
) {
    Canvas(modifier = modifier) {
        val cellPx = cellSize.toPx()
        val cols = (size.width / cellPx).toInt() + 1
        val rows = (size.height / cellPx).toInt() + 1
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val color = if ((row + col) % 2 == 0) lightColor else darkColor
                drawRect(
                    color = color,
                    topLeft = Offset(col * cellPx, row * cellPx),
                    size = Size(cellPx, cellPx),
                )
            }
        }
    }
}
