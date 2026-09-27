package codes.side.colorpicker.foundation

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import codes.side.color.ColorChannel
import codes.side.color.EdgeSolver
import codes.side.color.GamutMapping
import codes.side.color.Hsl
import codes.side.color.Hsv
import codes.side.color.Hwb
import codes.side.color.Lch
import codes.side.color.Oklch
import codes.side.color.Okhsl
import codes.side.color.Okhsv
import codes.side.color.Srgb
import codes.side.color.compose.toComposeColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlin.math.roundToInt

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
 * lightness where the gamut turns its corner, and along the right edge a channel reaches zero, where the
 * sRGB curve rises steeply from it. At 256 × 256 it measures 29.34 at worst, at hue 111, with a median
 * of 3.42 over the hues.
 */
private val OKHSL_GRID = PlaneGrid(256, 256)

/**
 * Okhsl's saturation × lightness above its crease under [PlaneRendering.Fast]: 12.82/255 at worst, at
 * hue 110, measured as the grids above over the band's own range. What is left is the right edge's,
 * which more columns only whittle, so the band keeps the single grid's 256 columns and draws that edge
 * no worse; 32 rows measure 14.53.
 */
private val OKHSL_UPPER_GRID = PlaneGrid(256, 64)

/** Okhsl's saturation × lightness below its crease: 12.82/255 at worst, at hue 110; 8 rows measure 35.90. */
private val OKHSL_LOWER_GRID = PlaneGrid(256, 16)

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

/**
 * The Okhsl lightness, as a fraction of the axis, where the saturation × lightness plane at [hue] creases:
 * sRGB's cusp at that hue, whose Oklab lightness Okhsl's toe carries over exactly. Below it an Okhsl
 * row's chroma scales with lightness, above it follows the gamut's upper edge.
 */
internal fun okhslCrease(hue: Double): Double = Srgb.gamut.cusp(hue).to(Okhsl)[Okhsl.L]!!

/** A band of a plane, [from]..[to] of its y axis from the bottom, rasterized on [grid] of its own. */
internal class PlaneBand(val from: Double, val to: Double, val grid: PlaneGrid)

/**
 * The bands a plane over [x] and [y] is built in: one over the whole axis, but under
 * [PlaneRendering.Fast] Okhsl's saturation × lightness is two that meet on its crease, each sampling it
 * as its edge, so filtering never blends across it.
 */
internal fun planeBands(x: ColorChannel, y: ColorChannel, held: DoubleArray, rendering: PlaneRendering): List<PlaneBand> {
    if (rendering !== PlaneRendering.Fast || x !== Okhsl.S || y !== Okhsl.L) return listOf(PlaneBand(0.0, 1.0, planeGridOf(x, y)))
    val crease = okhslCrease(held[Okhsl.H.index])
    return listOf(PlaneBand(crease, 1.0, OKHSL_UPPER_GRID), PlaneBand(0.0, crease, OKHSL_LOWER_GRID))
}

/** The grid a plane over [x] and [y] is rasterized at. */
internal fun planeGridOf(x: ColorChannel, y: ColorChannel): PlaneGrid = when {
    x === Hwb.W && y === Hwb.B -> HWB_GRID
    x === Okhsv.S && y === Okhsv.V -> OKHSV_GRID
    x === Okhsl.S && y === Okhsl.L -> OKHSL_GRID
    x === Oklch.C && y === Oklch.L -> OKLCH_GRID
    x === Lch.C && y === Lch.L -> LCH_GRID
    else -> DEFAULT_GRID
}

/**
 * The axis value sample [index] of [count] stands for, the first and last exactly 0 and 1, so the
 * plane's edges, where the most colorful colors are picked, are sampled rather than clamped to.
 */
internal fun gridSample(index: Int, count: Int): Double = if (count <= 1) 0.0 else index.toDouble() / (count - 1)

/**
 * The plane over [x] and [y] on a [columns] × [rows] grid, filled a row at a time so a caller can pause
 * between rows: [pixels], opaque `0xAARRGGBB`, row 0 at the top, where [y] is at [yTo] of its range and
 * the last row at [yFrom], as fractions; the other channels are at [held]. Each row goes through one bulk call into sRGB by chroma reduction, with
 * [rendering]'s solver: [PlaneRendering.Fast] packs it straight into pixels, [PlaneRendering.Canonical] maps
 * it to Doubles in one row of colors reused for every row and rounds them.
 */
internal class PlaneRows(
    private val x: ColorChannel,
    private val y: ColorChannel,
    private val held: DoubleArray,
    private val columns: Int,
    private val rows: Int,
    private val rendering: PlaneRendering = PlaneRendering.Canonical,
    val pixels: IntArray = IntArray(columns * rows),
    private val yFrom: Double = 0.0,
    private val yTo: Double = 1.0,
) {
    /** How many rows [pixels] holds. */
    val rowCount: Int get() = rows

    private val size = x.space.channels.size
    private val fast = rendering === PlaneRendering.Fast
    private val mapper = Srgb.gamut.mapper(x.space, if (fast) GamutMapping.ChromaReduction(EdgeSolver.Iterative) else GamutMapping.ChromaReduction())
    private val row = DoubleArray(columns * size).also { row ->
        for (column in 0 until columns) {
            held.copyInto(row, column * size)
            row[column * size + x.index] = channelValueAt(x, x.referenceRange, gridSample(column, columns))
        }
    }
    private val rowColors = DoubleArray(columns * 3)

    /** The sRGB colors of row [r], three values a sample, into [into] from [offset]. */
    fun colors(r: Int, into: DoubleArray, offset: Int) {
        holdRow(r)
        mapper.convert(row, 0, into, offset, columns)
    }

    /** Fills row [r] of [pixels]. */
    fun fill(r: Int) {
        val start = r * columns
        if (fast) {
            holdRow(r)
            mapper.convertToArgb(row, 0, pixels, start, columns)
        } else {
            colors(r, rowColors, 0)
            for (column in 0 until columns) pixels[start + column] = srgbArgb(rowColors, 3 * column)
        }
    }

    /** Another set of scratch rows for another worker, filling the same [pixels]. */
    fun sharingPixels(): PlaneRows = PlaneRows(x, y, held, columns, rows, rendering, pixels, yFrom, yTo)

    private fun holdRow(r: Int) {
        val yValue = channelValueAt(y, y.referenceRange, yFrom + (yTo - yFrom) * (1.0 - gridSample(r, rows)))
        for (column in 0 until columns) row[column * size + y.index] = yValue
    }
}

/**
 * The sRGB colors of every row of [PlaneRows], three values a sample, as the grids above are measured,
 * over [yFrom]..[yTo] of the y axis.
 */
internal fun planeColors(x: ColorChannel, y: ColorChannel, held: DoubleArray, columns: Int, rows: Int, yFrom: Double = 0.0, yTo: Double = 1.0): DoubleArray {
    val grid = PlaneRows(x, y, held, columns, rows, yFrom = yFrom, yTo = yTo)
    return DoubleArray(columns * rows * 3).also { rgb -> for (r in 0 until rows) grid.colors(r, rgb, r * columns * 3) }
}

/** Every row of [PlaneRows] at once. */
internal fun planePixels(x: ColorChannel, y: ColorChannel, held: DoubleArray, grid: PlaneGrid, rendering: PlaneRendering = PlaneRendering.Canonical): IntArray {
    val rows = PlaneRows(x, y, held, grid.columns, grid.rows, rendering)
    for (r in 0 until grid.rows) rows.fill(r)
    return rows.pixels
}

// Rows filled between two yields. On wasm, Dispatchers.Default is the thread that handles input, so a
// raster that never yielded would hold every event until it finished.
private const val ROWS_PER_YIELD = 16

// The most workers a Fast plane's rows are shared among: past four the gain measured small beside what they
// would take from the rest of the app.
private const val MOST_WORKERS = 4

/** How many workers build a plane with [rendering]: [PlaneRendering.Fast]'s rows are shared out, Canonical's are not. */
internal fun planeWorkerCount(rendering: PlaneRendering): Int =
    if (rendering === PlaneRendering.Fast) planeWorkers().coerceIn(1, MOST_WORKERS) else 1

/**
 * Fills every row of [pixels][PlaneRows.pixels], shared among [workers] coroutines in the caller's
 * context, each taking the next row as soon as it is free: on a phone's mix of fast and slow cores, equal
 * shares would leave the plane waiting on its slowest. A build that [yields], as in the browser, where
 * it shares its thread with input, steps aside every few rows; elsewhere it runs on threads of its own,
 * where stepping aside costs a wake-up that on a phone outlasts the rows between, and only checks that it
 * is still wanted. Either way it stops at the next row when cancelled.
 */
internal suspend fun PlaneRows.fillInSteps(workers: Int, yields: Boolean = planeBuildSharesInputThread) {
    val next = Channel<Int>(Channel.UNLIMITED)
    for (r in 0 until rowCount) next.trySend(r)
    next.close()
    coroutineScope {
        for (worker in 0 until workers) {
            val rows = if (worker == 0) this@fillInSteps else sharingPixels()
            launch {
                var filled = 0
                while (true) {
                    if (!yields) ensureActive() else if (filled % ROWS_PER_YIELD == 0) yield()
                    val r = next.tryReceive().getOrNull() ?: break
                    rows.fill(r)
                    filled++
                }
            }
        }
    }
}

/** One band's raster, to be drawn scaled over the band. */
internal class PlanePart(val band: PlaneBand, val bitmap: ImageBitmap)

/** The parts of a plane, as [planeBands] divides it, each built by [fillInSteps] with [rendering]'s workers. */
internal suspend fun rasterizePlaneInSteps(x: ColorChannel, y: ColorChannel, held: DoubleArray, rendering: PlaneRendering): List<PlanePart> =
    planeBands(x, y, held, rendering).map { band ->
        val rows = PlaneRows(x, y, held, band.grid.columns, band.grid.rows, rendering, yFrom = band.from, yTo = band.to)
        rows.fillInSteps(planeWorkerCount(rendering))
        PlanePart(band, imageBitmapFromPixels(rows.pixels, band.grid.columns, band.grid.rows))
    }

/** [rasterizePlaneInSteps] at once, for a preview's single frame. */
internal fun rasterizePlane(x: ColorChannel, y: ColorChannel, held: DoubleArray, rendering: PlaneRendering): List<PlanePart> =
    planeBands(x, y, held, rendering).map { band ->
        val rows = PlaneRows(x, y, held, band.grid.columns, band.grid.rows, rendering, yFrom = band.from, yTo = band.to)
        for (r in 0 until band.grid.rows) rows.fill(r)
        PlanePart(band, imageBitmapFromPixels(rows.pixels, band.grid.columns, band.grid.rows))
    }

/**
 * Draws [parts] stretched over the drawing area, each over its band. Bands meet on the whole pixel
 * nearest their shared edge, so neither overlaps nor leaves a gap, and a band too thin for a pixel draws
 * nothing. The plane's own top and bottom are its drawing area's.
 *
 * [PlaneRendering.Fast] puts each sample where the grids are measured with it: the first and last rows
 * and columns on the band's edges. Scaled whole, as [PlaneRendering.Canonical]'s rasters are, a
 * raster's outer samples sit half a cell in, and the filter holds the edge over the half cell outside
 * them: on a band of 16 rows 160 pixels tall, a flat strip five pixels deep. A band whose cells are
 * smaller than a pixel is scaled whole too, where the strip cannot be seen: drawn aligned, its raster
 * overhangs the band by half a cell, and only a cell of a pixel or more covers the half pixel a rounded
 * edge can move.
 */
internal fun DrawScope.drawPlaneParts(parts: List<PlanePart>, rendering: PlaneRendering) {
    for (part in parts) {
        val top = if (part.band.to == 1.0) 0 else ((1.0 - part.band.to) * size.height).roundToInt()
        val bottom = if (part.band.from == 0.0) size.height.toInt() else ((1.0 - part.band.from) * size.height).roundToInt()
        if (bottom <= top) continue
        val columns = part.bitmap.width
        val rows = part.bitmap.height
        val cellWidth = if (columns < 2) 0f else size.width / (columns - 1)
        val cellHeight = if (rows < 2) 0f else ((part.band.to - part.band.from) * size.height).toFloat() / (rows - 1)
        if (rendering !== PlaneRendering.Fast || cellWidth < 1f || cellHeight < 1f) {
            drawPlaneBitmap(part.bitmap, top, bottom - top)
            continue
        }
        val bandTop = ((1.0 - part.band.to) * size.height).toFloat()
        clipRect(0f, top.toFloat(), size.width, bottom.toFloat()) {
            withTransform({
                translate(-cellWidth / 2f, bandTop - cellHeight / 2f)
                scale(cellWidth, cellHeight, pivot = Offset.Zero)
            }) {
                drawImage(
                    image = part.bitmap,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(columns, rows),
                    dstOffset = IntOffset.Zero,
                    dstSize = IntSize(columns, rows),
                    filterQuality = FilterQuality.Low,
                )
            }
        }
    }
}

/** Whether two brushes draw the plane over [x] and [y] exactly: HSL's S × L and HSV's S × V. */
internal fun isExactPlane(x: ColorChannel, y: ColorChannel): Boolean =
    (x === Hsl.S && y === Hsl.L) || (x === Hsv.S && y === Hsv.V)

/** What a raster is built from: the pair of channels, every held component, and how it is built. */
internal data class PlaneRequest(val x: ColorChannel, val y: ColorChannel, val held: List<Double>, val rendering: PlaneRendering)

// Every plane's rasters: a raster is 256 KB at most, and Okhsl's two bands less, so three kept cost under 1 MB.
private val planeRasters = PlaneRasters<List<PlanePart>>(kept = 3)

/** A built raster, the pair of channels it shows and how it was built. */
internal class PlaneRaster(val x: ColorChannel, val y: ColorChannel, val rendering: PlaneRendering, val parts: List<PlanePart>) {
    /** Its parts, if they are what a plane over [x] and [y] with [rendering] draws. */
    fun partsFor(x: ColorChannel, y: ColorChannel, rendering: PlaneRendering): List<PlanePart>? =
        parts.takeIf { x === this.x && y === this.y && rendering === this.rendering }
}

/**
 * What a plane over [x] and [y] draws, the other channels at [displayed]. HSL's and HSV's own planes
 * are two brushes, exactly. Any other pair is a raster, built as [LocalPlaneRendering] says on
 * [Dispatchers.Default] whenever a held channel changes, and shared with any other plane asking for the
 * same, with the previous one drawn until it arrives. A raster of another pair is another space's colors
 * rather than an earlier state of these, and a raster of another [PlaneRendering] is laid out and drawn
 * that rendering's way, so after a change of either nothing is drawn until the new one arrives. A preview
 * draws one frame and has no later one to wait for, so there the raster is built at once.
 */
@Composable
internal fun rememberPlaneSurface(x: ColorChannel, y: ColorChannel, displayed: DoubleArray): DrawScope.() -> Unit {
    val held = DoubleArray(displayed.size) { if (it == x.index || it == y.index) 0.0 else displayed[it] }
    val key = held.toList()
    if (isExactPlane(x, y)) {
        return remember(x, key) { if (x === Hsl.S) hslSurface(held[Hsl.H.index]) else hsvSurface(held[Hsv.H.index]) }
    }
    val rendering = LocalPlaneRendering.current
    val parts = if (LocalInspectionMode.current) {
        remember(x, y, key, rendering) { rasterizePlane(x, y, held, rendering) }
    } else {
        val raster = remember { mutableStateOf<PlaneRaster?>(null) }
        val request by rememberUpdatedState(PlaneRequest(x, y, key, rendering))
        LaunchedEffect(Unit) {
            // One raster at a time, always of the latest request. A drag changes the held channels
            // faster than a raster is built; cancelling the one in flight for each change would land
            // none until the drag stopped. Built in order, an older raster never lands after a newer one.
            snapshotFlow { request }.conflate().collect { next ->
                val built = planeRasters.raster(next) {
                    withContext(Dispatchers.Default) { rasterizePlaneInSteps(next.x, next.y, next.held.toDoubleArray(), next.rendering) }
                }
                raster.value = PlaneRaster(next.x, next.y, next.rendering, built)
            }
        }
        raster.value?.partsFor(x, y, rendering)
    }
    return { if (parts != null) drawPlaneParts(parts, rendering) }
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
 * Draws [bitmap] stretched across the drawing area, [dstHeight] pixels tall from [dstTop]: the whole
 * area unless told otherwise.
 *
 * A ShaderBrush will not do it: an ImageShader paints the bitmap at its own size and clamps
 * outwards, so an 80-pixel box filled from a 4-pixel ramp comes back the ramp's last colour
 * across almost all of it. Scaling needs a destination size, and the low filter quality is
 * the bilinear read the grid sizes above are measured against.
 */
internal fun DrawScope.drawPlaneBitmap(bitmap: ImageBitmap, dstTop: Int = 0, dstHeight: Int = size.height.toInt()) {
    drawImage(
        image = bitmap,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(bitmap.width, bitmap.height),
        dstOffset = IntOffset(0, dstTop),
        dstSize = IntSize(size.width.toInt(), dstHeight),
        filterQuality = FilterQuality.Low,
    )
}
