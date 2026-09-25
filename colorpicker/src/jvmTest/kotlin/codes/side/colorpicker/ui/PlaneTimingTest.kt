package codes.side.colorpicker.ui

import codes.side.color.ColorChannel
import codes.side.color.Hwb
import codes.side.color.Lch
import codes.side.color.OkLch
import codes.side.color.Okhsl
import codes.side.color.Okhsv
import kotlin.test.Test

class PlaneTimingTest {

    @Test
    fun recordRasterTimes() {
        // Recorded, not gated: each library plane's grid through the generic path at hue 200, the
        // median of 15 runs after 5 to warm up, printed to the test's output. It runs off the main thread.
        val planes = listOf(Hwb.W to Hwb.B, Lch.C to Lch.L, OkLch.C to OkLch.L, Okhsl.S to Okhsl.L, Okhsv.S to Okhsv.V)
        for ((x, y) in planes) {
            val held = DoubleArray(3)
            held[x.space.channels.first { it.isHue }.index] = 200.0
            val grid = planeGridOf(x, y)
            repeat(5) { planeColors(x, y, held, grid.columns, grid.rows) }
            val times = LongArray(15) {
                val start = System.nanoTime()
                planeColors(x, y, held, grid.columns, grid.rows)
                System.nanoTime() - start
            }
            times.sort()
            println("%-12s %3d × %3d  %5.1f ms".format("$x", grid.columns, grid.rows, times[times.size / 2] / 1e6))
        }
    }
}
