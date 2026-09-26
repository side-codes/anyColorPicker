package codes.side.colorpicker.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
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

/**
 * Draws a transparency checkerboard behind the content: square cells of [cellSize], [light] at the top left
 * and alternating with [dark]. Under a translucent color it shows how much of what is behind the color
 * shows through, which is how a swatch or an alpha track reads its opacity.
 */
public fun Modifier.checkerboard(light: Color, dark: Color, cellSize: Dp = 6.dp): Modifier =
    drawWithCache {
        val brush = checkerboardBrush(cellSize.roundToPx().coerceAtLeast(1), light, dark)
        onDrawBehind { drawRect(brush) }
    }

/**
 * The checkerboard [Modifier.checkerboard] draws, as a [Brush] for a shape of your own, such as the two
 * segments of an alpha track either side of its thumb. It tiles from the origin of whatever it fills.
 */
@Composable
public fun rememberCheckerboardBrush(light: Color, dark: Color, cellSize: Dp = 6.dp): Brush {
    val cellPx = with(LocalDensity.current) { cellSize.roundToPx() }.coerceAtLeast(1)
    return remember(cellPx, light, dark) { checkerboardBrush(cellPx, light, dark) }
}

/**
 * A [Brush] that tiles a checkerboard of [cellPx]-pixel cells.
 *
 * A 2x2-cell tile is rendered once into an [ImageBitmap] and repeated via
 * [ImageShader], so drawing the pattern is a single `drawRect(brush)` call
 * instead of a per-cell rect loop on every frame.
 */
internal fun checkerboardBrush(cellPx: Int, light: Color, dark: Color): Brush {
    val tileSizePx = cellPx * 2
    val bitmap = ImageBitmap(tileSizePx, tileSizePx)
    val canvas = Canvas(bitmap)
    val paint = Paint()
    val cell = cellPx.toFloat()
    paint.color = light
    canvas.drawRect(0f, 0f, cell, cell, paint)
    canvas.drawRect(cell, cell, cell * 2f, cell * 2f, paint)
    paint.color = dark
    canvas.drawRect(cell, 0f, cell * 2f, cell, paint)
    canvas.drawRect(0f, cell, cell, cell * 2f, paint)
    return ShaderBrush(ImageShader(bitmap, TileMode.Repeated, TileMode.Repeated))
}
