package codes.side.colorpicker.conversion

import codes.side.colorpicker.model.LabColor
import codes.side.colorpicker.model.RgbColor
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LabConversionsTest {

    private val eps = 1e-5f
    // LAB uses transcendental functions so needs a slightly wider tolerance
    private val labEps = 0.5f

    private fun assertNear(expected: Float, actual: Float, tolerance: Float = eps, msg: String = "") {
        assertTrue(abs(expected - actual) <= tolerance, "$msg expected=$expected actual=$actual diff=${abs(expected - actual)}")
    }

    // ---- LAB -> RGB known colors ----

    @Test
    fun whiteLabToRgb() {
        val lab = LabColor(l = 100f, a = 0f, b = 0f)
        val rgb = lab.toRgb()
        assertNear(1f, rgb.red, tolerance = 0.01f, msg = "red")
        assertNear(1f, rgb.green, tolerance = 0.01f, msg = "green")
        assertNear(1f, rgb.blue, tolerance = 0.01f, msg = "blue")
    }

    @Test
    fun blackLabToRgb() {
        val lab = LabColor(l = 0f, a = 0f, b = 0f)
        val rgb = lab.toRgb()
        assertNear(0f, rgb.red, tolerance = 0.01f, msg = "red")
        assertNear(0f, rgb.green, tolerance = 0.01f, msg = "green")
        assertNear(0f, rgb.blue, tolerance = 0.01f, msg = "blue")
    }

    @Test
    fun midGrayLabToRgb() {
        // L=50 should give a mid-gray
        val lab = LabColor(l = 50f, a = 0f, b = 0f)
        val rgb = lab.toRgb()
        // Mid gray L=50 is roughly sRGB 0.184 (not 0.5, due to gamma)
        assertTrue(rgb.red > 0.1f && rgb.red < 0.6f, "red should be mid-range: ${rgb.red}")
        assertNear(rgb.red, rgb.green, tolerance = 0.01f, msg = "gray R==G")
        assertNear(rgb.red, rgb.blue, tolerance = 0.01f, msg = "gray R==B")
    }

    // ---- RGB -> LAB known colors ----

    @Test
    fun rgbWhiteToLab() {
        val rgb = RgbColor(1f, 1f, 1f)
        val lab = rgb.toLab()
        assertNear(100f, lab.l, tolerance = 0.5f, msg = "L")
        assertNear(0f, lab.a, tolerance = 0.5f, msg = "a")
        assertNear(0f, lab.b, tolerance = 0.5f, msg = "b")
    }

    @Test
    fun rgbBlackToLab() {
        val rgb = RgbColor(0f, 0f, 0f)
        val lab = rgb.toLab()
        assertNear(0f, lab.l, tolerance = 0.5f, msg = "L")
        assertNear(0f, lab.a, tolerance = 0.5f, msg = "a")
        assertNear(0f, lab.b, tolerance = 0.5f, msg = "b")
    }

    @Test
    fun rgbRedToLab() {
        val rgb = RgbColor(1f, 0f, 0f)
        val lab = rgb.toLab()
        // Red in Lab is approximately L=53.23, a=80.11, b=67.22
        assertTrue(lab.l > 50f && lab.l < 56f, "L for red: ${lab.l}")
        assertTrue(lab.a > 75f, "a for red: ${lab.a}")
        assertTrue(lab.b > 60f, "b for red: ${lab.b}")
    }

    // ---- Round-trip ----

    @Test
    fun roundTripLab() {
        val original = LabColor(l = 50f, a = 20f, b = -30f)
        val roundTripped = original.toRgb().toLab()
        assertNear(original.l, roundTripped.l, tolerance = labEps, msg = "L")
        assertNear(original.a, roundTripped.a, tolerance = labEps, msg = "a")
        assertNear(original.b, roundTripped.b, tolerance = labEps, msg = "b")
    }

    @Test
    fun roundTripLabNonTrivial() {
        val original = LabColor(l = 65.3f, a = -15.7f, b = 42.1f)
        val roundTripped = original.toRgb().toLab()
        assertNear(original.l, roundTripped.l, tolerance = labEps, msg = "L")
        assertNear(original.a, roundTripped.a, tolerance = labEps, msg = "a")
        assertNear(original.b, roundTripped.b, tolerance = labEps, msg = "b")
    }

    @Test
    fun roundTripRgbThroughLab() {
        val original = RgbColor(0.4f, 0.6f, 0.8f)
        val roundTripped = original.toLab().toRgb()
        assertNear(original.red, roundTripped.red, tolerance = 0.01f, msg = "red")
        assertNear(original.green, roundTripped.green, tolerance = 0.01f, msg = "green")
        assertNear(original.blue, roundTripped.blue, tolerance = 0.01f, msg = "blue")
    }

    // ---- Alpha preserved ----

    @Test
    fun alphaPreservedLabToRgb() {
        val lab = LabColor(l = 50f, a = 10f, b = 10f, alpha = 0.502f)
        val rgb = lab.toRgb()
        assertEquals(0.502f, rgb.alpha)
    }

    @Test
    fun alphaPreservedRgbToLab() {
        val rgb = RgbColor(0.5f, 0.3f, 0.1f, alpha = 0.75f)
        val lab = rgb.toLab()
        assertEquals(0.75f, lab.alpha)
    }

    // ---- Int round-trip via ARGB ----

    @Test
    fun labToArgbIntAndBack() {
        val lab = LabColor(l = 50f, a = 20f, b = -30f)
        val argb = lab.toArgbInt()
        val restored = argb.toLabColor()
        // Going through 8-bit ARGB loses precision
        assertNear(lab.l, restored.l, tolerance = 2f, msg = "L")
        assertNear(lab.a, restored.a, tolerance = 2f, msg = "a")
        assertNear(lab.b, restored.b, tolerance = 2f, msg = "b")
    }

    @Test
    fun intToLabColor() {
        val black = 0xFF000000.toInt()
        val lab = black.toLabColor()
        assertNear(0f, lab.l, tolerance = 0.5f)
    }
}
