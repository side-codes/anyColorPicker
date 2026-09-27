package codes.side.colorpicker.foundation

import codes.side.color.Okhsl
import codes.side.color.Okhsv
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaneRastersTest {

    private fun request(hue: Double) = PlaneRequest(Okhsl.S, Okhsl.L, listOf(hue, 0.0, 0.0))

    @Test
    fun aRasterBeingBuiltIsAwaitedRatherThanBuiltAgain() = runTest {
        val rasters = PlaneRasters<String>(kept = 2)
        val gate = CompletableDeferred<Unit>()
        var builds = 0
        val first = async { rasters.raster(request(10.0)) { builds++; gate.await(); "first" } }
        val second = async { rasters.raster(request(10.0)) { builds++; "second" } }
        runCurrent()
        gate.complete(Unit)
        assertEquals(listOf("first", "first"), listOf(first.await(), second.await()))
        assertEquals(1, builds)
    }

    @Test
    fun theLastRastersBuiltAreKept() = runTest {
        val rasters = PlaneRasters<String>(kept = 2)
        val built = mutableListOf<Double>()
        suspend fun raster(hue: Double) = rasters.raster(request(hue)) { built += hue; "$hue" }
        raster(10.0)
        raster(20.0)
        assertEquals("10.0", raster(10.0), "kept")
        raster(30.0)
        raster(40.0)
        raster(10.0)
        assertEquals(listOf(10.0, 20.0, 30.0, 40.0, 10.0), built, "built again once two others came after it")
    }

    @Test
    fun anotherPairOfChannelsIsAnotherRaster() = runTest {
        val rasters = PlaneRasters<String>(kept = 2)
        rasters.raster(request(10.0)) { "okhsl" }
        assertEquals("okhsv", rasters.raster(PlaneRequest(Okhsv.S, Okhsv.V, listOf(10.0, 0.0, 0.0))) { "okhsv" })
    }

    @Test
    fun aPlaneWaitingOnOneThatGoesAwayBuildsItself() = runTest {
        val rasters = PlaneRasters<String>(kept = 2)
        val never = CompletableDeferred<Unit>()
        val first = async { rasters.raster(request(10.0)) { never.await(); "first" } }
        runCurrent()
        val second = async { rasters.raster(request(10.0)) { "second" } }
        runCurrent()
        first.cancel()
        assertEquals("second", second.await())
    }
}
