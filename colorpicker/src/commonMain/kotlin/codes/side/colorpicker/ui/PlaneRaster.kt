package codes.side.colorpicker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * Columns every Ok* plane is rasterized at.
 *
 * The error a scaled bitmap leaves behind is a crease along the cusp lightness, where the
 * sRGB gamut turns a corner. It runs along the x axis, so columns do not move it: 128 of
 * them measure the same 15.57 of 255 as 64 do, at twice the cost.
 */
internal const val OK_PLANE_COLUMNS = 64

/**
 * Rows an [OkhslPlane] is rasterized at. Rows are what cross the crease, and they buy about
 * 1.5x per doubling rather than the 4x a smooth surface would give: 64 rows measure 23.47
 * of 255, 128 measure 15.57 and 256 measure 9.19.
 */
internal const val OKHSL_PLANE_ROWS = 256

/**
 * Rows an [OkhsvPlane] is rasterized at. Okhsv puts its cusp on the corner of the square
 * instead of running it through the middle, so there is no crease to resolve and 64 rows
 * already measure 1.94 of 255.
 */
internal const val OKHSV_PLANE_ROWS = 64

/**
 * The axis value sample [index] of [count] stands for.
 *
 * Endpoint-aligned, so the first and last samples are exactly `0` and `1`. Sampling at texel
 * centres instead leaves the outer half-texel to be clamped, which renders the plane's
 * full-saturation edge at `0.992` — measured at 23.7 of 255 with 64 columns, right where
 * the most colourful colours are picked.
 */
internal fun planeSample(index: Int, count: Int): Float =
    if (count <= 1) 0f else index / (count - 1f)

/**
 * Rasterizes a plane's field. Row `0` is the top of the surface, where the y axis reads `1`.
 *
 * Only the cheap tail of an Ok* conversion runs per pixel: the cusp depends on hue alone and
 * the chroma anchors on lightness, so both are hoisted out by the caller's [color] closure
 * being called row by row.
 */
internal fun buildPlaneBitmap(
    width: Int,
    height: Int,
    color: (x: Float, y: Float) -> Color,
): ImageBitmap {
    val bitmap = ImageBitmap(width, height)
    val canvas = Canvas(bitmap)
    val paint = Paint()
    for (row in 0 until height) {
        val y = 1f - planeSample(row, height)
        for (column in 0 until width) {
            paint.color = color(planeSample(column, width), y)
            canvas.drawRect(column.toFloat(), row.toFloat(), column + 1f, row + 1f, paint)
        }
    }
    return bitmap
}

/**
 * Draws [bitmap] stretched over the whole drawing area.
 *
 * A ShaderBrush will not do it: an ImageShader paints the bitmap at its own size and clamps
 * outwards, so an 80-pixel box filled from a 4-pixel ramp comes back the ramp's last colour
 * across almost all of it. Scaling needs a destination size, and the low filter quality is
 * the bilinear read the grid sizes above are measured against.
 */
internal fun DrawScope.drawPlaneBitmap(bitmap: ImageBitmap) {
    drawImage(
        image = bitmap,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(bitmap.width, bitmap.height),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(size.width.toInt(), size.height.toInt()),
        filterQuality = FilterQuality.Low,
    )
}

/**
 * Rasterizes once per change of [key] — the hue, for every caller here. Dragging inside a
 * plane never changes it, so the common gesture rebuilds nothing.
 */
@Composable
internal fun rememberPlaneBitmap(
    key: Any?,
    width: Int,
    height: Int,
    color: (x: Float, y: Float) -> Color,
): ImageBitmap = remember(key, width, height) { buildPlaneBitmap(width, height, color) }
