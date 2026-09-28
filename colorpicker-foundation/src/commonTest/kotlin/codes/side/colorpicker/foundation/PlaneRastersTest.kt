package codes.side.colorpicker.foundation

import codes.side.color.Okhsl
import codes.side.color.Okhsv
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class PlaneRastersTest {

    private fun request(hue: Double) = PlaneRequest(Okhsl.S, Okhsl.L, listOf(hue, 0.0, 0.0), PlaneRendering.Fast)

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
        assertEquals("okhsv", rasters.raster(PlaneRequest(Okhsv.S, Okhsv.V, listOf(10.0, 0.0, 0.0), PlaneRendering.Fast)) { "okhsv" })
    }

    @Test
    fun aPlaneDrawsNoRasterAnotherRenderingBuilt() {
        // Each preset draws its rasters its own way, so after a switch the other's raster is not drawn at all.
        val raster = PlaneRaster(Okhsl.S, Okhsl.L, PlaneRendering.Fast, emptyList())
        assertEquals(emptyList(), raster.partsFor(Okhsl.S, Okhsl.L, PlaneRendering.Fast))
        assertEquals(null, raster.partsFor(Okhsl.S, Okhsl.L, PlaneRendering.Canonical))
        assertEquals(null, raster.partsFor(Okhsv.S, Okhsv.V, PlaneRendering.Fast))
    }

    @Test
    fun aRasterOfAnotherRenderingIsAnotherRaster() = runTest {
        val rasters = PlaneRasters<String>(kept = 2)
        val held = listOf(10.0, 0.0, 0.0)
        rasters.raster(PlaneRequest(Okhsl.S, Okhsl.L, held, PlaneRendering.Fast)) { "fast" }
        assertEquals("canonical", rasters.raster(PlaneRequest(Okhsl.S, Okhsl.L, held, PlaneRendering.Canonical)) { "canonical" })
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
