package codes.side.colorpicker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * Columns every Ok* plane is rasterized at.
 *
 * The error a scaled bitmap leaves behind is a crease along the cusp lightness, where the
 * sRGB gamut turns a corner. It runs along the x axis, so columns do not move it: at the
 * 256 rows [OkhslPlane] ships at, 64, 128 and 256 columns all measure the same worst error,
 * 35.10 of 255 — extra columns cost up to four times as much to rasterize for no accuracy
 * gain.
 */
internal const val OK_PLANE_COLUMNS = 64

/**
 * Rows an [OkhslPlane] is rasterized at. Rows are what cross the crease: 64 of them measure
 * 78.30 of 255 at worst, 128 measure 39.23 — nearly halving it — and 256 measure 35.10, a
 * much smaller further gain. Averaged over the full range that is still roughly 1.5x per
 * doubling, well short of the 4x a smooth surface would give.
 */
internal const val OKHSL_PLANE_ROWS = 256

/**
 * Rows an [OkhsvPlane] is rasterized at. Okhsv puts its cusp on the corner of the square
 * instead of running it through the middle, so there is no crease to resolve and 64 rows
 * already measure 2.28 of 255.
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
 * [rowAt] is called once per row and returns the function for the pixels along it, which is
 * what lets a caller lift the part of its conversion that only depends on y out of the inner
 * loop. An Ok* cusp costs a polynomial fit and a Halley step, so a field that recomputes one
 * per pixel spends most of its time on an answer that does not vary.
 */
internal fun buildPlaneBitmap(
    width: Int,
    height: Int,
    rowAt: (y: Float) -> (x: Float) -> Color,
): ImageBitmap {
    val pixels = IntArray(width * height)
    for (row in 0 until height) {
        val colorAt = rowAt(1f - planeSample(row, height))
        val offset = row * width
        for (column in 0 until width) {
            val color = colorAt(planeSample(column, width))
            pixels[offset + column] =
                (0xFF shl 24) or
                    (channel(color.red) shl 16) or
                    (channel(color.green) shl 8) or
                    channel(color.blue)
        }
    }
    return imageBitmapFromPixels(pixels, width, height)
}

/** A `0..1` channel as the `0..255` byte a packed pixel holds, rounded rather than truncated. */
private fun channel(value: Float): Int = (value * 255f + 0.5f).toInt().coerceIn(0, 255)

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
    rowAt: (y: Float) -> (x: Float) -> Color,
): ImageBitmap = remember(key, width, height) { buildPlaneBitmap(width, height, rowAt) }
