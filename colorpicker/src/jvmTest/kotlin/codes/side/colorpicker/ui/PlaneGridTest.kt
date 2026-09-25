package codes.side.colorpicker.ui

import codes.side.color.ColorChannel
import codes.side.color.Hwb
import codes.side.color.Lch
import codes.side.color.OkLch
import codes.side.color.Okhsl
import codes.side.color.Okhsv
import kotlin.math.abs
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Each library plane's grid, measured as it was chosen: the worst difference, in 1/255 of an sRGB
 * channel, between the true colors at four times the grid's density and what bilinear filtering reads
 * from the grid there.
 */
class PlaneGridTest {

    private fun worstError(x: ColorChannel, y: ColorChannel, hue: Double, columns: Int, rows: Int): Double {
        val held = DoubleArray(x.space.channels.size)
        held[x.space.channels.first { it.isHue }.index] = hue
        val grid = planeColors(x, y, held, columns, rows)
        val fineColumns = 4 * (columns - 1) + 1
        val fineRows = 4 * (rows - 1) + 1
        val truth = planeColors(x, y, held, fineColumns, fineRows)
        var worst = 0.0
        for (j in 0 until fineRows) {
            val y0 = min(j / 4, rows - 1)
            val y1 = min(y0 + 1, rows - 1)
            val ty = (j % 4) / 4.0
            for (i in 0 until fineColumns) {
                val x0 = min(i / 4, columns - 1)
                val x1 = min(x0 + 1, columns - 1)
                val tx = (i % 4) / 4.0
                for (c in 0..2) {
                    val top = grid[(y0 * columns + x0) * 3 + c] * (1 - tx) + grid[(y0 * columns + x1) * 3 + c] * tx
                    val bottom = grid[(y1 * columns + x0) * 3 + c] * (1 - tx) + grid[(y1 * columns + x1) * 3 + c] * tx
                    worst = maxOf(worst, abs(top * (1 - ty) + bottom * ty - truth[(j * fineColumns + i) * 3 + c]))
                }
            }
        }
        return worst * 255.0
    }

    // Every 30 degrees, and the hue where the plane's error peaked in a sweep over every whole degree.
    private fun hues(peak: Double) = (0 until 360 step 30).map { it.toDouble() } + peak

    private fun assertHolds(x: ColorChannel, y: ColorChannel, peak: Double, budget: Double) {
        val grid = planeGridOf(x, y)
        val worst = hues(peak).maxOf { worstError(x, y, it, grid.columns, grid.rows) }
        assertTrue(worst <= budget, "$x × $y at ${grid.columns} × ${grid.rows}: ${decimals(worst, 2)}/255, over $budget")
    }

    // A grid that meets the budget is the smallest that does: halving either axis breaks it.
    private fun assertSmallest(x: ColorChannel, y: ColorChannel, peak: Double) {
        val grid = planeGridOf(x, y)
        val narrower = hues(peak).maxOf { worstError(x, y, it, grid.columns / 2, grid.rows) }
        val shorter = hues(peak).maxOf { worstError(x, y, it, grid.columns, grid.rows / 2) }
        assertTrue(narrower > 2.5, "${grid.columns / 2} columns would do: ${decimals(narrower, 2)}/255")
        assertTrue(shorter > 2.5, "${grid.rows / 2} rows would do: ${decimals(shorter, 2)}/255")
    }

    @Test
    fun hwbMeetsTheBudgetAtItsGrid() {
        assertHolds(Hwb.W, Hwb.B, peak = 0.0, budget = 2.5)
        assertSmallest(Hwb.W, Hwb.B, peak = 0.0)
    }

    @Test
    fun okhsvMeetsTheBudgetAtItsGrid() {
        assertHolds(Okhsv.S, Okhsv.V, peak = 265.0, budget = 2.5)
        assertSmallest(Okhsv.S, Okhsv.V, peak = 265.0)
    }

    @Test
    fun okhslHoldsItsRecordedError() = assertHolds(Okhsl.S, Okhsl.L, peak = 111.0, budget = 29.35)

    @Test
    fun okLchHoldsItsRecordedError() = assertHolds(OkLch.C, OkLch.L, peak = 110.0, budget = 34.15)

    @Test
    fun lchHoldsItsRecordedError() = assertHolds(Lch.C, Lch.L, peak = 43.0, budget = 80.30)
}
