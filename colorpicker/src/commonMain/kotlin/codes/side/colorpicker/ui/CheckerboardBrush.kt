package codes.side.colorpicker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal val CheckerboardCellSize = 6.dp

/**
 * Returns a [Brush] that tiles a transparency checkerboard pattern.
 *
 * A 2x2-cell tile is rendered once into an [ImageBitmap] and repeated via
 * [ImageShader], so drawing the pattern is a single `drawRect(brush)` call
 * instead of a per-cell rect loop on every frame.
 */
@Composable
internal fun rememberCheckerboardBrush(
    cellSize: Dp,
    light: Color,
    dark: Color,
): Brush {
    val cellSizePx = with(LocalDensity.current) { cellSize.roundToPx() }.coerceAtLeast(1)
    return remember(cellSizePx, light, dark) {
        val tileSizePx = cellSizePx * 2
        val bitmap = ImageBitmap(tileSizePx, tileSizePx)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        val cell = cellSizePx.toFloat()
        paint.color = light
        canvas.drawRect(0f, 0f, cell, cell, paint)
        canvas.drawRect(cell, cell, cell * 2f, cell * 2f, paint)
        paint.color = dark
        canvas.drawRect(cell, 0f, cell * 2f, cell, paint)
        canvas.drawRect(0f, cell, cell, cell * 2f, paint)
        ShaderBrush(ImageShader(bitmap, TileMode.Repeated, TileMode.Repeated))
    }
}
