package codes.side.colorpicker.ui

import codes.side.colorpicker.conversion.toRgb
import codes.side.colorpicker.model.OkhslColor
import codes.side.colorpicker.model.OkhsvColor
import codes.side.colorpicker.model.RgbColor
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlaneRasterTest {

    @Test
    fun samplesAreEndpointAligned() {
        // Texel-centre sampling would put the last column at 0.992, and the plane's
        // full-saturation edge — the whole reason to reach for Okhsl — would not be on it.
        assertEquals(0f, planeSample(0, 64))
        assertEquals(1f, planeSample(63, 64))
        assertEquals(0.5f, planeSample(32, 65))
    }

    @Test
    fun aSingleSampleDoesNotDivideByZero() {
        assertEquals(0f, planeSample(0, 1))
    }

    /**
     * Bilinear read of the grid, matching what drawImage does when it scales the bitmap up
     * with FilterQuality.Low.
     */
    private fun readGrid(
        grid: Array<Array<RgbColor>>,
        columns: Int,
        rows: Int,
        x: Float,
        y: Float,
    ): FloatArray {
        val fx = (x * (columns - 1)).coerceIn(0f, columns - 1f)
        val fy = ((1f - y) * (rows - 1)).coerceIn(0f, rows - 1f)
        val x0 = fx.toInt().coerceAtMost(columns - 1)
        val x1 = (x0 + 1).coerceAtMost(columns - 1)
        val y0 = fy.toInt().coerceAtMost(rows - 1)
        val y1 = (y0 + 1).coerceAtMost(rows - 1)
        val tx = fx - x0
        val ty = fy - y0
        fun channel(pick: (RgbColor) -> Float): Float {
            val top = pick(grid[y0][x0]) * (1 - tx) + pick(grid[y0][x1]) * tx
            val bottom = pick(grid[y1][x0]) * (1 - tx) + pick(grid[y1][x1]) * tx
            return top * (1 - ty) + bottom * ty
        }
        return floatArrayOf(channel { it.red }, channel { it.green }, channel { it.blue })
    }

    private fun grid(
        columns: Int,
        rows: Int,
        color: (Float, Float) -> RgbColor,
    ): Array<Array<RgbColor>> = Array(rows) { row ->
        val y = 1f - planeSample(row, rows)
        Array(columns) { column -> color(planeSample(column, columns), y) }
    }

    /**
     * Worst error at one hue; [color] has that hue already bound.
     *
     * 121x121 points, because the error peaks in a narrow needle near the gamut cusp rather
     * than across a broad region. A coarser grid steps over the needle entirely and reports
     * a worst error a fraction of the true one, so this density is what makes the budget
     * assertions mean anything.
     */
    private fun worstError(
        columns: Int,
        rows: Int,
        color: (Float, Float) -> RgbColor,
    ): Double {
        val g = grid(columns, rows, color)
        var worst = 0.0
        for (i in 0..120) {
            for (j in 0..120) {
                val x = i / 120f
                val y = j / 120f
                val truth = color(x, y)
                val drawn = readGrid(g, columns, rows, x, y)
                val d = maxOf(
                    abs(truth.red - drawn[0]).toDouble(),
                    abs(truth.green - drawn[1]).toDouble(),
                    abs(truth.blue - drawn[2]).toDouble(),
                ) * 255.0
                if (d > worst) worst = d
            }
        }
        return worst
    }

    @Test
    fun theOkhslGridHoldsItsBudget() {
        // The residual sits on a crease at the cusp lightness, where the gamut boundary
        // turns a corner and no amount of linear interpolation crosses it cleanly. Rows
        // are what move it: 64 of them measure 23 of 255, 256 measure 9.
        var hue = 0f
        var worst = 0.0
        while (hue < 360f) {
            val h = hue
            worst = maxOf(
                worst,
                worstError(OK_PLANE_COLUMNS, OKHSL_PLANE_ROWS) { x, y ->
                    OkhslColor(hue = h, saturation = x, lightness = y).toRgb()
                },
            )
            hue += 20f
        }
        assertTrue(worst <= 12.0, "Okhsl plane grid drifted by $worst of 255")
    }

    @Test
    fun theOkhsvGridHoldsItsBudget() {
        // Okhsv's cusp lands on the corner of the square rather than crossing it, so it has
        // no crease and needs a fraction of the rows.
        var hue = 0f
        var worst = 0.0
        while (hue < 360f) {
            val h = hue
            worst = maxOf(
                worst,
                worstError(OK_PLANE_COLUMNS, OKHSV_PLANE_ROWS) { x, y ->
                    OkhsvColor(hue = h, saturation = x, value = y).toRgb()
                },
            )
            hue += 20f
        }
        assertTrue(worst <= 3.0, "Okhsv plane grid drifted by $worst of 255")
    }
}
