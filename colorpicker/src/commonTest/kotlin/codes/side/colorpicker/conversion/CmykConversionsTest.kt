package codes.side.colorpicker.conversion

import codes.side.colorpicker.model.CmykColor
import codes.side.colorpicker.model.RgbColor
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CmykConversionsTest {

    private val eps = 1e-5f

    private fun assertNear(
        expected: Float,
        actual: Float,
        tolerance: Float = eps,
        msg: String = "",
    ) {
        assertTrue(
            abs(expected - actual) <= tolerance,
            "$msg expected=$expected actual=$actual diff=${abs(expected - actual)}",
        )
    }

    // ---- CMYK -> RGB known colors ----

    @Test
    fun blackCmykToRgb() {
        val cmyk = CmykColor(0f, 0f, 0f, 1f)
        val rgb = cmyk.toRgb()
        assertNear(0f, rgb.red)
        assertNear(0f, rgb.green)
        assertNear(0f, rgb.blue)
    }

    @Test
    fun whiteCmykToRgb() {
        val cmyk = CmykColor(0f, 0f, 0f, 0f)
        val rgb = cmyk.toRgb()
        assertNear(1f, rgb.red)
        assertNear(1f, rgb.green)
        assertNear(1f, rgb.blue)
    }

    @Test
    fun pureCyanToRgb() {
        val cmyk = CmykColor(1f, 0f, 0f, 0f)
        val rgb = cmyk.toRgb()
        assertNear(0f, rgb.red)
        assertNear(1f, rgb.green)
        assertNear(1f, rgb.blue)
    }

    @Test
    fun pureMagentaToRgb() {
        val cmyk = CmykColor(0f, 1f, 0f, 0f)
        val rgb = cmyk.toRgb()
        assertNear(1f, rgb.red)
        assertNear(0f, rgb.green)
        assertNear(1f, rgb.blue)
    }

    @Test
    fun pureYellowToRgb() {
        val cmyk = CmykColor(0f, 0f, 1f, 0f)
        val rgb = cmyk.toRgb()
        assertNear(1f, rgb.red)
        assertNear(1f, rgb.green)
        assertNear(0f, rgb.blue)
    }

    @Test
    fun cmykToRgbKnownValue() {
        // CMYK(0.30, 0.60, 0.10, 0.20)
        // R = (1-0.30)*(1-0.20) = 0.70*0.80 = 0.56
        // G = (1-0.60)*(1-0.20) = 0.40*0.80 = 0.32
        // B = (1-0.10)*(1-0.20) = 0.90*0.80 = 0.72
        val cmyk = CmykColor(cyan = 0.3f, magenta = 0.6f, yellow = 0.1f, key = 0.2f)
        val rgb = cmyk.toRgb()
        assertNear(0.56f, rgb.red, msg = "red")
        assertNear(0.32f, rgb.green, msg = "green")
        assertNear(0.72f, rgb.blue, msg = "blue")
    }

    // ---- RGB -> CMYK ----

    @Test
    fun rgbBlackToCmyk() {
        val rgb = RgbColor(0f, 0f, 0f)
        val cmyk = rgb.toCmyk()
        assertEquals(1f, cmyk.key)
    }

    @Test
    fun rgbWhiteToCmyk() {
        val rgb = RgbColor(1f, 1f, 1f)
        val cmyk = rgb.toCmyk()
        assertNear(0f, cmyk.cyan)
        assertNear(0f, cmyk.magenta)
        assertNear(0f, cmyk.yellow)
        assertNear(0f, cmyk.key)
    }

    @Test
    fun rgbRedToCmyk() {
        // Pure red -> C=0, M=1, Y=1, K=0
        val rgb = RgbColor(1f, 0f, 0f)
        val cmyk = rgb.toCmyk()
        assertNear(0f, cmyk.cyan)
        assertNear(1f, cmyk.magenta)
        assertNear(1f, cmyk.yellow)
        assertNear(0f, cmyk.key)
    }

    @Test
    fun rgbGreenToCmyk() {
        val rgb = RgbColor(0f, 1f, 0f)
        val cmyk = rgb.toCmyk()
        assertNear(1f, cmyk.cyan)
        assertNear(0f, cmyk.magenta)
        assertNear(1f, cmyk.yellow)
        assertNear(0f, cmyk.key)
    }

    @Test
    fun rgbBlueToCmyk() {
        val rgb = RgbColor(0f, 0f, 1f)
        val cmyk = rgb.toCmyk()
        assertNear(1f, cmyk.cyan)
        assertNear(1f, cmyk.magenta)
        assertNear(0f, cmyk.yellow)
        assertNear(0f, cmyk.key)
    }

    // ---- Round-trip ----

    @Test
    fun cmykRoundTripPreservesVisualColor() {
        // CMYK -> RGB -> CMYK does NOT preserve CMYK components because key is
        // recomputed from max(r,g,b). But the visual result (RGB) must be identical.
        val original = CmykColor(cyan = 0.3f, magenta = 0.6f, yellow = 0.1f, key = 0.2f)
        val rgb1 = original.toRgb()
        val rgb2 = original.toRgb().toCmyk().toRgb()
        assertNear(rgb1.red, rgb2.red, msg = "red")
        assertNear(rgb1.green, rgb2.green, msg = "green")
        assertNear(rgb1.blue, rgb2.blue, msg = "blue")
    }

    @Test
    fun rgbToCmykAndBack() {
        val original = RgbColor(0.4f, 0.6f, 0.8f)
        val roundTripped = original.toCmyk().toRgb()
        assertNear(original.red, roundTripped.red, msg = "red")
        assertNear(original.green, roundTripped.green, msg = "green")
        assertNear(original.blue, roundTripped.blue, msg = "blue")
    }

    // ---- Alpha preserved ----

    @Test
    fun alphaPreservedCmykToRgb() {
        val cmyk = CmykColor(0.5f, 0.5f, 0.5f, 0.5f, alpha = 0.392f)
        val rgb = cmyk.toRgb()
        assertEquals(0.392f, rgb.alpha)
    }

    @Test
    fun alphaPreservedRgbToCmyk() {
        val rgb = RgbColor(0.5f, 0.5f, 0.5f, alpha = 0.67f)
        val cmyk = rgb.toCmyk()
        assertEquals(0.67f, cmyk.alpha)
    }
}
