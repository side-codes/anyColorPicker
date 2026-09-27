package codes.side.colorpicker.foundation

import codes.side.color.Hwb
import codes.side.color.Lch
import codes.side.color.Oklch
import codes.side.color.Okhsl
import codes.side.color.Okhsv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class PlaneTimingTest {

    @Test
    fun recordRasterTimes() {
        // Recorded, not gated: each library plane at hue 200 under each preset, the median of 15 builds after 5,
        // off the main thread, with the preset's workers and bands, printed to the test's output.
        val planes = listOf(Hwb.W to Hwb.B, Lch.C to Lch.L, Oklch.C to Oklch.L, Okhsl.S to Okhsl.L, Okhsv.S to Okhsv.V)
        for (rendering in listOf(PlaneRendering.Canonical, PlaneRendering.Fast)) {
            for ((x, y) in planes) {
                val held = DoubleArray(3)
                held[x.space.channels.first { it.isHue }.index] = 200.0
                val build = {
                    runBlocking(Dispatchers.Default) {
                        for (band in planeBands(x, y, held, rendering)) {
                            PlaneRows(x, y, held, band.grid.columns, band.grid.rows, rendering, yFrom = band.from, yTo = band.to).fillInSteps(planeWorkerCount(rendering))
                        }
                    }
                }
                repeat(5) { build() }
                val times = LongArray(15) {
                    val start = System.nanoTime()
                    build()
                    System.nanoTime() - start
                }
                times.sort()
                println("%-9s %-12s %5.1f ms each".format("$rendering", "$x", times[times.size / 2] / 1e6))
            }
        }
    }
}
