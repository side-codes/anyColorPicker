package codes.side.colorpicker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import codes.side.color.ColorChannel
import codes.side.color.Hsl
import codes.side.color.Hsv
import codes.side.color.Hwb
import codes.side.color.Lch
import codes.side.color.OkLch
import codes.side.color.Okhsl
import codes.side.color.Okhsv
import codes.side.color.Srgb
import codes.side.color.compose.toComposeColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

/** The samples a plane is rasterized at, across and down. */
@Immutable
internal class PlaneGrid(val columns: Int, val rows: Int)

/**
 * HWB's whiteness × blackness: 1.99/255 at worst, measured at four times the grid's density over every
 * whole hue. Halving either axis measures 3.98.
 */
private val HWB_GRID = PlaneGrid(32, 32)

/**
 * Okhsv's saturation × value: 2.41/255 at worst, at hue 265. 32 × 32 measures 5.62 and 64 × 16 4.39.
 * Okhsv's cusp sits on the corner of the square, so no crease crosses it.
 */
private val OKHSV_GRID = PlaneGrid(64, 32)

/**
 * Okhsl's saturation × lightness: no grid holds 2.5/255, because a crease runs along the cusp's
 * lightness where the gamut turns its corner. At 256 × 256 it measures 29.34 at worst, at hue 111,
 * with a median of 3.42 over the hues.
 */
private val OKHSL_GRID = PlaneGrid(256, 256)

/**
 * OkLCh's chroma × lightness: no grid holds 2.5/255, because the color creases where chroma reduction
 * starts and jumps where it changes stretch near sRGB blue. At 256 × 256 it measures 34.14 at worst,
 * at hue 110, with a median of 4.90.
 */
private val OKLCH_GRID = PlaneGrid(256, 256)

/**
 * LCH's chroma × lightness: no grid holds 2.5/255. Most of the plane lies beyond real colors, and the
 * mapped colors there crease and jump, as at hue 42, where an olive band steps in under the reds. At
 * 256 × 256 it measures 80.29 at worst, at hue 43, with a median of 10.83.
 */
private val LCH_GRID = PlaneGrid(256, 256)

/** Any other pair of channels. */
private val DEFAULT_GRID = PlaneGrid(64, 64)

/** The grid a plane over [x] and [y] is rasterized at. */
internal fun planeGridOf(x: ColorChannel, y: ColorChannel): PlaneGrid = when {
    x === Hwb.W && y === Hwb.B -> HWB_GRID
    x === Okhsv.S && y === Okhsv.V -> OKHSV_GRID
    x === Okhsl.S && y === Okhsl.L -> OKHSL_GRID
    x === OkLch.C && y === OkLch.L -> OKLCH_GRID
    x === Lch.C && y === Lch.L -> LCH_GRID
    else -> DEFAULT_GRID
}

/**
 * The axis value sample [index] of [count] stands for, the first and last exactly 0 and 1, so the
 * plane's edges, where the most colorful colors are picked, are sampled rather than clamped to.
 */
internal fun gridSample(index: Int, count: Int): Double = if (count <= 1) 0.0 else index.toDouble() / (count - 1)

/**
 * The sRGB colors of the plane over [x] and [y] on a [columns] × [rows] grid, filled a row at a time
 * so a caller can pause between rows. Three values a sample in [rgb], row 0 at the top, where [y] is at
 * the end of its range; the other channels are at [held]. Each row goes through one bulk call into sRGB
 * by chroma reduction.
 */
internal class PlaneRows(
    private val x: ColorChannel,
    private val y: ColorChannel,
    held: DoubleArray,
    private val columns: Int,
    private val rows: Int,
) {
    private val size = x.space.channels.size
    private val mapper = Srgb.gamut.mapper(x.space)
    private val row = DoubleArray(columns * size).also { row ->
        for (column in 0 until columns) {
            held.copyInto(row, column * size)
            row[column * size + x.index] = channelValueAt(x, x.referenceRange, gridSample(column, columns))
        }
    }

    val rgb: DoubleArray = DoubleArray(columns * rows * 3)

    /** Fills row [r] of [rgb]. */
    fun fill(r: Int) {
        val yValue = channelValueAt(y, y.referenceRange, 1.0 - gridSample(r, rows))
        for (column in 0 until columns) row[column * size + y.index] = yValue
        mapper.convert(row, 0, rgb, r * columns * 3, columns)
    }
}

/** Every row of [PlaneRows] at once. */
internal fun planeColors(x: ColorChannel, y: ColorChannel, held: DoubleArray, columns: Int, rows: Int): DoubleArray {
    val grid = PlaneRows(x, y, held, columns, rows)
    for (r in 0 until rows) grid.fill(r)
    return grid.rgb
}

/** [planeColors] packed as opaque `0xAARRGGBB`, a pixel per sample. */
internal fun planePixels(x: ColorChannel, y: ColorChannel, held: DoubleArray, grid: PlaneGrid): IntArray =
    packPixels(planeColors(x, y, held, grid.columns, grid.rows), grid)

/** The plane over [x] and [y] as an image of [grid]'s size, to be drawn scaled. */
internal fun rasterizePlane(x: ColorChannel, y: ColorChannel, held: DoubleArray, grid: PlaneGrid): ImageBitmap =
    imageBitmapFromPixels(planePixels(x, y, held, grid), grid.columns, grid.rows)

// Rows filled between two yields. On wasm, Dispatchers.Default is the thread that handles input, so a
// raster that never yielded would hold every event until it finished.
private const val ROWS_PER_YIELD = 16

/** [rasterizePlane], yielding every few rows, and stopping there when cancelled. */
internal suspend fun rasterizePlaneInSteps(x: ColorChannel, y: ColorChannel, held: DoubleArray, grid: PlaneGrid): ImageBitmap {
    val rows = PlaneRows(x, y, held, grid.columns, grid.rows)
    for (r in 0 until grid.rows) {
        if (r % ROWS_PER_YIELD == 0) yield()
        rows.fill(r)
    }
    return imageBitmapFromPixels(packPixels(rows.rgb, grid), grid.columns, grid.rows)
}

private fun packPixels(rgb: DoubleArray, grid: PlaneGrid): IntArray = IntArray(grid.columns * grid.rows) { srgbArgb(rgb, 3 * it) }

/** Whether two brushes draw the plane over [x] and [y] exactly: HSL's S × L and HSV's S × V. */
internal fun isExactPlane(x: ColorChannel, y: ColorChannel): Boolean =
    (x === Hsl.S && y === Hsl.L) || (x === Hsv.S && y === Hsv.V)

// What a raster is built from: the pair of channels and every held component.
private data class PlaneRequest(val x: ColorChannel, val y: ColorChannel, val held: List<Double>)

// A built raster and the pair of channels it shows.
private class PlaneRaster(val x: ColorChannel, val y: ColorChannel, val bitmap: ImageBitmap)

/**
 * What a plane over [x] and [y] draws, the other channels at [displayed]. HSL's and HSV's own planes
 * are two brushes, exactly. Any other pair is a raster, built on [Dispatchers.Default] whenever a held
 * channel changes, with the previous one drawn until it arrives. A raster of another pair is another
 * space's colors rather than an earlier state of these, so after a change of channels nothing is drawn
 * until the new pair's arrives. A preview draws one frame and has no later one to wait for, so there the
 * raster is built at once.
 */
@Composable
internal fun rememberPlaneSurface(x: ColorChannel, y: ColorChannel, displayed: DoubleArray): DrawScope.() -> Unit {
    val held = DoubleArray(displayed.size) { if (it == x.index || it == y.index) 0.0 else displayed[it] }
    val key = held.toList()
    if (isExactPlane(x, y)) {
        return remember(x, key) { if (x === Hsl.S) hslSurface(held[Hsl.H.index]) else hsvSurface(held[Hsv.H.index]) }
    }
    val bitmap = if (LocalInspectionMode.current) {
        remember(x, y, key) { rasterizePlane(x, y, held, planeGridOf(x, y)) }
    } else {
        val raster = remember { mutableStateOf<PlaneRaster?>(null) }
        val request by rememberUpdatedState(PlaneRequest(x, y, key))
        LaunchedEffect(Unit) {
            // One raster at a time, always of the latest request. A drag changes the held channels
            // faster than a raster is built; cancelling the one in flight for each change would land
            // none until the drag stopped. Built in order, an older raster never lands after a newer one.
            snapshotFlow { request }.conflate().collect { next ->
                val built = withContext(Dispatchers.Default) {
                    rasterizePlaneInSteps(next.x, next.y, next.held.toDoubleArray(), planeGridOf(next.x, next.y))
                }
                raster.value = PlaneRaster(next.x, next.y, built)
            }
        }
        raster.value?.takeIf { it.x === x && it.y === y }?.bitmap
    }
    return { if (bitmap != null) drawPlaneBitmap(bitmap) }
}

// HSL's saturation × lightness at one hue: the mid-lightness ramp from grey to the pure hue, under white
// fading out toward the middle row and black fading in below it. At lightness L, HSL is that ramp mixed
// with white by 2L − 1 above the middle and with black by 1 − 2L below it, which is what compositing the
// overlay does. Each half fades a color into its own transparent form, so the mix is the same whether a
// toolkit interpolates premultiplied or not.
private fun hslSurface(hue: Double): DrawScope.() -> Unit {
    val ramp = Brush.horizontalGradient(listOf(Hsl(hue, 0.0, 50.0).toComposeColor(), Hsl(hue, 100.0, 50.0).toComposeColor()))
    val shading = Brush.verticalGradient(
        0f to Color.White,
        0.5f to Color.White.copy(alpha = 0f),
        0.5f to Color.Black.copy(alpha = 0f),
        1f to Color.Black,
    )
    return {
        drawRect(ramp)
        drawRect(shading)
    }
}

// HSV's saturation × value at one hue: the top row from white to the pure hue, darkened by black
// composited at 1 − V, which scales it by V as HSV does.
private fun hsvSurface(hue: Double): DrawScope.() -> Unit {
    val ramp = Brush.horizontalGradient(listOf(Color.White, Hsv(hue, 100.0, 100.0).toComposeColor()))
    val shading = Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0f), Color.Black))
    return {
        drawRect(ramp)
        drawRect(shading)
    }
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
