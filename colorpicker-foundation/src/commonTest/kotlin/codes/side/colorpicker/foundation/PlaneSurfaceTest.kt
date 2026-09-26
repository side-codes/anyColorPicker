package codes.side.colorpicker.foundation

import codes.side.color.ColorSpace
import codes.side.color.DisplayP3
import codes.side.color.Hsl
import codes.side.color.Hsv
import codes.side.color.Hwb
import codes.side.color.Lch
import codes.side.color.OkLch
import codes.side.color.Okhsl
import codes.side.color.Okhsv
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaneSurfaceTest {

    private fun channels(argb: Int) = listOf((argb shr 16) and 0xFF, (argb shr 8) and 0xFF, argb and 0xFF)

    @Test
    fun samplesAreEndpointAligned() {
        assertEquals(0.0, gridSample(0, 64))
        assertEquals(1.0, gridSample(63, 64))
        assertEquals(0.0, gridSample(0, 1))
    }

    @Test
    fun rowZeroIsTheTopOfThePlane() {
        // Okhsl at lightness 1 is white and at 0 black, whatever the saturation.
        val pixels = planePixels(Okhsl.S, Okhsl.L, doubleArrayOf(30.0, 0.0, 0.0), PlaneGrid(4, 3))
        assertEquals(listOf(255, 255, 255), channels(pixels[0]), "top left")
        assertEquals(listOf(255, 255, 255), channels(pixels[3]), "top right")
        assertEquals(listOf(0, 0, 0), channels(pixels[8]), "bottom left")
        assertEquals(listOf(0, 0, 0), channels(pixels[11]), "bottom right")
        for (pixel in pixels) assertEquals(0xFF, pixel ushr 24, "opaque")
    }

    @Test
    fun theLeftEdgeOfAnOkhslPlaneIsGrey() {
        val pixels = planePixels(Okhsl.S, Okhsl.L, doubleArrayOf(120.0, 0.0, 0.0), PlaneGrid(4, 5))
        for (row in 0 until 5) {
            val (r, g, b) = channels(pixels[row * 4])
            assertTrue(r == g && g == b, "row $row starts grey, was $r $g $b")
        }
    }

    @Test
    fun theOtherChannelsComeFromHeld() {
        // HSL at hue 120, rasterized rather than brushed: white along the top, pure green at the right
        // of the middle row, black along the bottom.
        val pixels = planePixels(Hsl.S, Hsl.L, doubleArrayOf(120.0, 0.0, 0.0), PlaneGrid(2, 3))
        assertEquals(listOf(255, 255, 255), channels(pixels[1]), "top right")
        assertEquals(listOf(0, 255, 0), channels(pixels[3]), "middle right")
        assertEquals(listOf(0, 0, 0), channels(pixels[5]), "bottom right")
    }

    @Test
    fun rowsFilledOneAtATimeMakeTheSameGrid() {
        // A raster built in steps fills its rows between pauses; each row stands on its own.
        val held = doubleArrayOf(40.0, 0.0, 0.0)
        val rows = PlaneRows(Okhsv.S, Okhsv.V, held, columns = 8, rows = 5)
        for (r in 4 downTo 0) rows.fill(r)
        assertEquals(planeColors(Okhsv.S, Okhsv.V, held, columns = 8, rows = 5).toList(), rows.rgb.toList())
    }

    @Test
    fun onlyHslAndHsvSaturationPlanesAreExact() {
        assertTrue(isExactPlane(Hsl.S, Hsl.L))
        assertTrue(isExactPlane(Hsv.S, Hsv.V))
        assertFalse(isExactPlane(Hsl.L, Hsl.S))
        assertFalse(isExactPlane(Hwb.W, Hwb.B))
        assertFalse(isExactPlane(Okhsl.S, Okhsl.L))
        // HSL over Display P3 is bilinear in P3, not in the sRGB it is drawn in.
        val p3Hsl = ColorSpace.hsl("--surface-hsl-p3", DisplayP3)
        assertFalse(isExactPlane(p3Hsl.S, p3Hsl.L))
        assertEquals(listOf(64, 64), planeGridOf(p3Hsl.S, p3Hsl.L).let { listOf(it.columns, it.rows) })
    }

    @Test
    fun eachLibraryPlaneHasItsMeasuredGrid() {
        assertEquals(listOf(32, 32), planeGridOf(Hwb.W, Hwb.B).let { listOf(it.columns, it.rows) })
        assertEquals(listOf(64, 32), planeGridOf(Okhsv.S, Okhsv.V).let { listOf(it.columns, it.rows) })
        assertEquals(listOf(256, 256), planeGridOf(Okhsl.S, Okhsl.L).let { listOf(it.columns, it.rows) })
        assertEquals(listOf(256, 256), planeGridOf(Lch.C, Lch.L).let { listOf(it.columns, it.rows) })
        assertEquals(listOf(256, 256), planeGridOf(OkLch.C, OkLch.L).let { listOf(it.columns, it.rows) })
        assertEquals(listOf(64, 64), planeGridOf(Lch.L, Lch.C).let { listOf(it.columns, it.rows) }, "any other pair")
    }
}
