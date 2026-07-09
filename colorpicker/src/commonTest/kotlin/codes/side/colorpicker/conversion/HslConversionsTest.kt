package codes.side.colorpicker.conversion

import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.model.RgbColor
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HslConversionsTest {

    private val eps = 5e-5f

    private fun assertNear(expected: Float, actual: Float, tolerance: Float = eps, msg: String = "") {
        assertTrue(abs(expected - actual) <= tolerance, "$msg expected=$expected actual=$actual diff=${abs(expected - actual)}")
    }

    // ---- HSL -> RGB primary colors ----

    @Test
    fun redHslToRgb() {
        val hsl = HslColor(hue = 0f, saturation = 1f, lightness = 0.5f)
        val rgb = hsl.toRgb()
        assertNear(1f, rgb.red, msg = "red")
        assertNear(0f, rgb.green, msg = "green")
        assertNear(0f, rgb.blue, msg = "blue")
    }

    @Test
    fun hue360HslToRgbIsPureRed() {
        // Hue 360 is normalized to 0 by the model, so it converts exactly like pure red
        val hsl = HslColor(hue = 360f, saturation = 1f, lightness = 0.5f)
        val rgb = hsl.toRgb()
        assertNear(1f, rgb.red, msg = "red")
        assertNear(0f, rgb.green, msg = "green")
        assertNear(0f, rgb.blue, msg = "blue")
    }

    @Test
    fun greenHslToRgb() {
        val hsl = HslColor(hue = 120f, saturation = 1f, lightness = 0.5f)
        val rgb = hsl.toRgb()
        assertNear(0f, rgb.red, msg = "red")
        assertNear(1f, rgb.green, msg = "green")
        assertNear(0f, rgb.blue, msg = "blue")
    }

    @Test
    fun blueHslToRgb() {
        val hsl = HslColor(hue = 240f, saturation = 1f, lightness = 0.5f)
        val rgb = hsl.toRgb()
        assertNear(0f, rgb.red, msg = "red")
        assertNear(0f, rgb.green, msg = "green")
        assertNear(1f, rgb.blue, msg = "blue")
    }

    @Test
    fun yellowHslToRgb() {
        val hsl = HslColor(hue = 60f, saturation = 1f, lightness = 0.5f)
        val rgb = hsl.toRgb()
        assertNear(1f, rgb.red, msg = "red")
        assertNear(1f, rgb.green, msg = "green")
        assertNear(0f, rgb.blue, msg = "blue")
    }

    @Test
    fun cyanHslToRgb() {
        val hsl = HslColor(hue = 180f, saturation = 1f, lightness = 0.5f)
        val rgb = hsl.toRgb()
        assertNear(0f, rgb.red, msg = "red")
        assertNear(1f, rgb.green, msg = "green")
        assertNear(1f, rgb.blue, msg = "blue")
    }

    @Test
    fun magentaHslToRgb() {
        val hsl = HslColor(hue = 300f, saturation = 1f, lightness = 0.5f)
        val rgb = hsl.toRgb()
        assertNear(1f, rgb.red, msg = "red")
        assertNear(0f, rgb.green, msg = "green")
        assertNear(1f, rgb.blue, msg = "blue")
    }

    // ---- HSL -> RGB achromatic ----

    @Test
    fun whiteHslToRgb() {
        val hsl = HslColor(hue = 0f, saturation = 0f, lightness = 1f)
        val rgb = hsl.toRgb()
        assertNear(1f, rgb.red)
        assertNear(1f, rgb.green)
        assertNear(1f, rgb.blue)
    }

    @Test
    fun blackHslToRgb() {
        val hsl = HslColor(hue = 0f, saturation = 0f, lightness = 0f)
        val rgb = hsl.toRgb()
        assertNear(0f, rgb.red)
        assertNear(0f, rgb.green)
        assertNear(0f, rgb.blue)
    }

    @Test
    fun midGrayHslToRgb() {
        val hsl = HslColor(hue = 0f, saturation = 0f, lightness = 0.5f)
        val rgb = hsl.toRgb()
        assertNear(0.5f, rgb.red)
        assertNear(0.5f, rgb.green)
        assertNear(0.5f, rgb.blue)
    }

    // ---- RGB -> HSL ----

    @Test
    fun rgbRedToHsl() {
        val rgb = RgbColor(1f, 0f, 0f)
        val hsl = rgb.toHsl()
        assertNear(0f, hsl.hue)
        assertNear(1f, hsl.saturation)
        assertNear(0.5f, hsl.lightness)
    }

    @Test
    fun rgbWhiteToHsl() {
        val rgb = RgbColor(1f, 1f, 1f)
        val hsl = rgb.toHsl()
        assertNear(0f, hsl.saturation)
        assertNear(1f, hsl.lightness)
    }

    @Test
    fun rgbBlackToHsl() {
        val rgb = RgbColor(0f, 0f, 0f)
        val hsl = rgb.toHsl()
        assertNear(0f, hsl.saturation)
        assertNear(0f, hsl.lightness)
    }

    @Test
    fun rgbGreenToHsl() {
        val rgb = RgbColor(0f, 1f, 0f)
        val hsl = rgb.toHsl()
        assertNear(120f, hsl.hue)
        assertNear(1f, hsl.saturation)
        assertNear(0.5f, hsl.lightness)
    }

    @Test
    fun rgbBlueToHsl() {
        val rgb = RgbColor(0f, 0f, 1f)
        val hsl = rgb.toHsl()
        assertNear(240f, hsl.hue)
        assertNear(1f, hsl.saturation)
        assertNear(0.5f, hsl.lightness)
    }

    // ---- Non-trivial color round-trip ----

    @Test
    fun roundTripHslNonTrivial() {
        val original = HslColor(hue = 210.5f, saturation = 0.47f, lightness = 0.63f)
        val roundTripped = original.toRgb().toHsl()
        assertNear(original.hue, roundTripped.hue, msg = "hue")
        assertNear(original.saturation, roundTripped.saturation, msg = "saturation")
        assertNear(original.lightness, roundTripped.lightness, msg = "lightness")
    }

    @Test
    fun roundTripHslDesaturated() {
        val original = HslColor(hue = 45f, saturation = 0.15f, lightness = 0.82f)
        val roundTripped = original.toRgb().toHsl()
        assertNear(original.hue, roundTripped.hue, msg = "hue")
        assertNear(original.saturation, roundTripped.saturation, msg = "saturation")
        assertNear(original.lightness, roundTripped.lightness, msg = "lightness")
    }

    @Test
    fun roundTripHslHighSaturation() {
        val original = HslColor(hue = 333.3f, saturation = 0.92f, lightness = 0.37f)
        val roundTripped = original.toRgb().toHsl()
        assertNear(original.hue, roundTripped.hue, msg = "hue")
        assertNear(original.saturation, roundTripped.saturation, msg = "saturation")
        assertNear(original.lightness, roundTripped.lightness, msg = "lightness")
    }

    // ---- Alpha preserved ----

    @Test
    fun alphaPreservedHslToRgb() {
        val hsl = HslColor(hue = 0f, saturation = 1f, lightness = 0.5f, alpha = 0.502f)
        val rgb = hsl.toRgb()
        assertEquals(0.502f, rgb.alpha)
    }

    @Test
    fun alphaPreservedRgbToHsl() {
        val rgb = RgbColor(0.5f, 0.3f, 0.1f, alpha = 0.75f)
        val hsl = rgb.toHsl()
        assertEquals(0.75f, hsl.alpha)
    }

    // ---- ARGB int conversions ----

    @Test
    fun argbIntRoundTrip() {
        val hsl = HslColor(hue = 180f, saturation = 0.5f, lightness = 0.4f, alpha = 200f / 255f)
        val restored = hsl.toArgbInt().toHslColor()
        // ARGB int round-trip quantizes to 8-bit, so allow +-1 int unit tolerance
        assertTrue(abs(hsl.intHue - restored.intHue) <= 1, "hue: ${hsl.intHue} vs ${restored.intHue}")
        assertTrue(abs(hsl.intSaturation - restored.intSaturation) <= 1, "sat: ${hsl.intSaturation} vs ${restored.intSaturation}")
        assertTrue(abs(hsl.intLightness - restored.intLightness) <= 1, "light: ${hsl.intLightness} vs ${restored.intLightness}")
        assertTrue(abs(hsl.intAlpha - restored.intAlpha) <= 1, "alpha: ${hsl.intAlpha} vs ${restored.intAlpha}")
    }

    @Test
    fun intToHslColor() {
        val argb = 0xFFFF0000.toInt() // opaque red
        val hsl = argb.toHslColor()
        assertNear(0f, hsl.hue)
        assertNear(1f, hsl.saturation)
        assertNear(0.5f, hsl.lightness)
        assertNear(1f, hsl.alpha)
    }

    // ---- Hue segment 5 (300-360) ----

    @Test
    fun hslHueSegment5() {
        val hsl = HslColor(hue = 330f, saturation = 1f, lightness = 0.5f)
        val rgb = hsl.toRgb()
        assertNear(1f, rgb.red, msg = "red")
        assertNear(0f, rgb.green, msg = "green")
        assertNear(0.5f, rgb.blue, msg = "blue")
    }
}
