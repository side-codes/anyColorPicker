package codes.side.colorpicker.foundation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class PlanePartsDrawTest {

    // The red channel a quarter of the way down a box filled from a 2 × 2 raster, black above white.
    private fun quarterDown(rendering: PlaneRendering): Float {
        var red = -1f
        runComposeUiTest {
            val raster = imageBitmapFromPixels(intArrayOf(0xFF000000.toInt(), 0xFF000000.toInt(), -1, -1), 2, 2)
            val parts = listOf(BandRaster(PlaneBand(0.0, 1.0, PlaneGrid(2, 2)), raster))
            setContent { Canvas(Modifier.size(100.dp).testTag("plane")) { drawPlaneParts(parts, rendering) } }
            val pixels = onNodeWithTag("plane").captureToImage().toPixelMap()
            red = pixels[pixels.width / 2, pixels.height / 4].red
        }
        return red
    }

    @Test
    fun aFastPlanesSamplesSitOnItsEdges() {
        // Its first and last rows are the plane's top and bottom, as its grid is measured, so a quarter of the way
        // down is a quarter of the way to white.
        val fast = quarterDown(PlaneRendering.Fast)
        assertTrue(fast in 0.15f..0.35f, "a quarter of the way to white, was $fast")
    }

    @Test
    fun aCanonicalPlaneIsDrawnAsBefore() {
        // Scaled whole, its first row's texel sits half a row in, and above it the edge is held.
        val canonical = quarterDown(PlaneRendering.Canonical)
        assertTrue(canonical < 0.05f, "still black, was $canonical")
    }
}
