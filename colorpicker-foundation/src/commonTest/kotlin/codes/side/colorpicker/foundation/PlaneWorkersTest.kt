package codes.side.colorpicker.foundation

import codes.side.color.ColorChannel
import codes.side.color.Hwb
import codes.side.color.Lch
import codes.side.color.OkLch
import codes.side.color.Okhsl
import codes.side.color.Okhsv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlaneWorkersTest {

    private fun rows(x: ColorChannel, y: ColorChannel, hue: Double, grid: PlaneGrid): PlaneRows {
        val held = DoubleArray(x.space.channels.size)
        held[x.space.channels.first { it.isHue }.index] = hue
        return PlaneRows(x, y, held, grid.columns, grid.rows, PlaneRendering.Fast)
    }

    @Test
    fun rowsSharedAmongWorkersAreTheRowsOfOne() = runTest {
        for ((x, y) in listOf(Okhsl.S to Okhsl.L, OkLch.C to OkLch.L, Lch.C to Lch.L, Okhsv.S to Okhsv.V, Hwb.W to Hwb.B)) {
            for (hue in listOf(0.0, 200.0, 264.1)) {
                val grid = PlaneGrid(24, 70)
                val one = rows(x, y, hue, grid).also { it.fillInSteps(workers = 1) }
                val four = rows(x, y, hue, grid).also { withContext(Dispatchers.Default) { it.fillInSteps(workers = 4) } }
                assertEquals(one.pixels.toList(), four.pixels.toList(), "$x × $y at $hue°")
            }
        }
    }

    @Test
    fun cancellingABuildStopsEveryWorker() = runTest {
        // Every worker yields before its first row and after every sixteen; cancelled two rounds in, each has filled
        // at most thirty-two of its sixty-four rows.
        val rows = rows(OkLch.C, OkLch.L, 200.0, PlaneGrid(8, 256))
        val build = launch { rows.fillInSteps(workers = 4) }
        launch {
            repeat(3) { yield() }
            build.cancel()
        }
        testScheduler.advanceUntilIdle()
        val filled = (0 until 256).count { r -> rows.pixels[r * 8] != 0 }
        assertTrue(build.isCancelled, "the build was cancelled")
        assertTrue(filled in 1..128, "$filled of 256 rows filled")
    }

    @Test
    fun onlyFastUsesWorkers() {
        assertEquals(1, planeWorkerCount(PlaneRendering.Canonical))
        assertTrue(planeWorkerCount(PlaneRendering.Fast) in 1..4)
    }
}
