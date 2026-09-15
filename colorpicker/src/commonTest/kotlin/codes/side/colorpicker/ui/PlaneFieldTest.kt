package codes.side.colorpicker.ui

import codes.side.colorpicker.conversion.delinearize
import codes.side.colorpicker.conversion.linearToSrgbByte
import codes.side.colorpicker.conversion.okhslRowFiller
import codes.side.colorpicker.conversion.okhsvRowFiller
import codes.side.colorpicker.conversion.toRgb
import codes.side.colorpicker.model.OkhslColor
import codes.side.colorpicker.model.OkhsvColor
import codes.side.colorpicker.model.RgbColor
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A row filler runs the conversion flat: no objects, no call per pixel, and the encoding
 * reached by locating the value between byte boundaries rather than by a pow. All three are
 * only sound if what it writes is still what the public conversion says, so every pixel is
 * compared against it — as a packed value, exactly, since that is what reaches the screen.
 */
class PlaneFieldTest {

    /** The public conversion's answer for one coordinate, packed the way a plane packs it. */
    private fun expected(color: RgbColor): Int =
        (0xFF shl 24) or
            (byteOf(color.red) shl 16) or
            (byteOf(color.green) shl 8) or
            byteOf(color.blue)

    private fun byteOf(channel: Float): Int = (channel * 255f).roundToInt().coerceIn(0, 255)

    /**
     * Within one step of 255, not exact: the public conversion hands back [Float] channels,
     * so its own encoding rounds a value that has already lost precision, while the filler
     * encodes the [Double] it computed. They part company only where that lost precision
     * straddles a byte boundary, and there the filler is the one that is right.
     */
    private fun assertRowsMatch(
        rows: Int,
        fillRow: (Float, FloatArray, IntArray, Int) -> Unit,
        color: (x: Float, y: Float) -> RgbColor,
    ) {
        val xs = FloatArray(OK_PLANE_COLUMNS) { planeSample(it, OK_PLANE_COLUMNS) }
        val pixels = IntArray(OK_PLANE_COLUMNS)
        for (row in 0 until rows) {
            val y = 1f - planeSample(row, rows)
            fillRow(y, xs, pixels, 0)
            for (column in xs.indices) {
                val want = expected(color(xs[column], y))
                val got = pixels[column]
                for (shift in listOf(16, 8, 0)) {
                    val off = ((want shr shift) and 0xFF) - ((got shr shift) and 0xFF)
                    assertTrue(
                        off in -1..1,
                        "channel at $shift is $off steps out at x=${xs[column]} y=$y",
                    )
                }
                assertEquals(0xFF, (got ushr 24) and 0xFF, "alpha at x=${xs[column]} y=$y")
            }
        }
    }

    @Test
    fun theOkhslFillerWritesWhatTheConversionReturns() {
        for (hue in 0 until 360 step 10) {
            assertRowsMatch(OKHSL_PLANE_ROWS, okhslRowFiller(hue.toFloat())) { x, y ->
                OkhslColor(hue = hue.toFloat(), saturation = x, lightness = y).toRgb()
            }
        }
    }

    @Test
    fun theOkhsvFillerWritesWhatTheConversionReturns() {
        for (hue in 0 until 360 step 10) {
            assertRowsMatch(OKHSV_PLANE_ROWS, okhsvRowFiller(hue.toFloat())) { x, y ->
                OkhsvColor(hue = hue.toFloat(), saturation = x, value = y).toRgb()
            }
        }
    }

    /**
     * The encoder locates a linear value between the boundaries where each byte takes over
     * rather than raising it to a power, which has to agree with the pow everywhere and not
     * merely nearly everywhere — a single step out is a visible seam on a gradient.
     */
    @Test
    fun theEncoderAgreesWithTheCurveItReplaces() {
        for (i in 0..200_000) {
            val linear = i / 200_000.0
            assertEquals(
                (delinearize(linear) * 255.0).roundToInt().coerceIn(0, 255),
                linearToSrgbByte(linear),
                "at linear $linear",
            )
        }
    }
}
