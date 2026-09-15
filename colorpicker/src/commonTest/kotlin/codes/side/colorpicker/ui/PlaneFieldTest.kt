package codes.side.colorpicker.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import codes.side.colorpicker.conversion.toComposeColor
import codes.side.colorpicker.model.OkhslColor
import codes.side.colorpicker.model.OkhsvColor
import kotlin.test.Test
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A plane field lifts the work that does not vary per pixel out of the inner loop, which is
 * only sound if the surface it draws is still exactly the one the public conversion returns.
 *
 * Equality here is exact rather than within a budget. Hoisting moves arithmetic out of a
 * loop, it does not reorder or approximate any of it, so every pixel has to come back
 * bit-identical — a tolerance would hide the one thing this guards against.
 */
class PlaneFieldTest {

    private fun assertFieldMatches(
        columns: Int,
        rows: Int,
        field: (Float) -> (Float) -> Color,
        expected: (x: Float, y: Float) -> Color,
    ) {
        for (row in 0 until rows) {
            val y = 1f - planeSample(row, rows)
            val colorAt = field(y)
            for (column in 0 until columns) {
                val x = planeSample(column, columns)
                assertEquals(expected(x, y), colorAt(x), "at x=$x y=$y")
            }
        }
    }

    @Test
    fun theOkhslFieldDrawsWhatTheConversionReturns() {
        for (hue in 0 until 360 step 10) {
            assertFieldMatches(
                columns = OK_PLANE_COLUMNS,
                rows = OKHSL_PLANE_ROWS,
                field = okhslPlaneField(hue.toFloat()),
            ) { x, y ->
                OkhslColor(hue = hue.toFloat(), saturation = x, lightness = y).toComposeColor()
            }
        }
    }

    @Test
    fun theOkhsvFieldDrawsWhatTheConversionReturns() {
        for (hue in 0 until 360 step 10) {
            assertFieldMatches(
                columns = OK_PLANE_COLUMNS,
                rows = OKHSV_PLANE_ROWS,
                field = okhsvPlaneField(hue.toFloat()),
            ) { x, y ->
                OkhsvColor(hue = hue.toFloat(), saturation = x, value = y).toComposeColor()
            }
        }
    }

    /** The poles are the rows where the conversions answer early, before any anchor exists. */
    @Test
    fun theOkhslFieldHoldsAtTheLightnessPoles() {
        val field = okhslPlaneField(142f)
        for (y in listOf(0f, 1f)) {
            val colorAt = field(y)
            for (column in 0 until OK_PLANE_COLUMNS) {
                val x = planeSample(column, OK_PLANE_COLUMNS)
                assertEquals(
                    OkhslColor(hue = 142f, saturation = x, lightness = y).toComposeColor(),
                    colorAt(x),
                    "at x=$x y=$y",
                )
            }
        }
    }

    /**
     * The rasterizer packs each colour into a pixel itself now rather than handing it to the
     * toolkit a rectangle at a time, so the packing is worth pinning: within half a step of
     * 255, which is all an 8-bit channel can carry.
     */
    @Test
    fun theBitmapHoldsWhatTheFieldReturned() {
        val field = okhslPlaneField(142f)
        val pixels = buildPlaneBitmap(OK_PLANE_COLUMNS, OKHSL_PLANE_ROWS, field).toPixelMap()
        for (row in 0 until OKHSL_PLANE_ROWS) {
            val colorAt = field(1f - planeSample(row, OKHSL_PLANE_ROWS))
            for (column in 0 until OK_PLANE_COLUMNS) {
                val expected = colorAt(planeSample(column, OK_PLANE_COLUMNS))
                val drawn = pixels[column, row]
                val off = maxOf(
                    abs(expected.red - drawn.red),
                    abs(expected.green - drawn.green),
                    abs(expected.blue - drawn.blue),
                ) * 255f
                assertTrue(off <= 0.5f, "pixel ($column, $row) is off by $off of 255")
            }
        }
    }
}
