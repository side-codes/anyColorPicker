package codes.side.colorpicker.foundation

import codes.side.color.Lch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class PlaneBuildCancellationTest {

    @Test
    fun aBuildOnWorkerThreadsStopsWhenCancelled() = runBlocking {
        // Four workers on real threads, never yielding: cancelled once a row is in, they stop at their next row rather
        // than finishing a plane of 20,000 rows.
        val rows = PlaneRows(Lch.C, Lch.L, doubleArrayOf(0.0, 0.0, 200.0), 64, 20_000, PlaneRendering.Fast)
        val build = launch(Dispatchers.Default) { rows.fillInSteps(workers = 4, yields = false) }
        while ((0 until 20_000).none { r -> rows.pixels[r * 64] != 0 }) Thread.onSpinWait()
        build.cancel()
        build.join()
        val filled = (0 until 20_000).count { r -> rows.pixels[r * 64] != 0 }
        assertTrue(filled < 20_000, "$filled of 20000 rows filled")
    }
}
