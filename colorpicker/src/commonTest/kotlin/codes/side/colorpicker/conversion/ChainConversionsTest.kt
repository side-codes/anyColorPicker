package codes.side.colorpicker.conversion

import codes.side.colorpicker.model.CmykColor
import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.model.LabColor
import codes.side.colorpicker.model.RgbColor
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChainConversionsTest {

    private val eps = 2e-5f
    private val labEps = 0.5f // LAB uses transcendental functions, needs wider tolerance

    private fun assertNear(
        expected: Float,
        actual: Float,
        tolerance: Float = eps,
        msg: String = "",
    ) {
        assertTrue(
            abs(expected - actual) <= tolerance,
            "$msg expected=$expected actual=$actual diff=${abs(expected - actual)}"
        )
    }

    // ===========================================================
    // CMYK chain conversions
    // ===========================================================

    @Test
    fun cmykToHsl() {
        val cmyk = CmykColor(0f, 1f, 1f, 0f) // pure red in CMYK
        val hsl = cmyk.toHsl()
        assertNear(0f, hsl.hue, msg = "hue")
        assertNear(1f, hsl.saturation, msg = "saturation")
        assertNear(0.5f, hsl.lightness, msg = "lightness")
    }

    @Test
    fun hslToCmyk() {
        val hsl = HslColor(hue = 0f, saturation = 1f, lightness = 0.5f) // red
        val cmyk = hsl.toCmyk()
        assertNear(0f, cmyk.cyan, msg = "cyan")
        assertNear(1f, cmyk.magenta, msg = "magenta")
        assertNear(1f, cmyk.yellow, msg = "yellow")
        assertNear(0f, cmyk.key, msg = "key")
    }

    @Test
    fun cmykToArgbInt() {
        val cmyk = CmykColor(0f, 0f, 0f, 0f) // white
        val argb = cmyk.toArgbInt()
        val rgb = argb.toRgbColor()
        assertEquals(255, rgb.intRed)
        assertEquals(255, rgb.intGreen)
        assertEquals(255, rgb.intBlue)
    }

    @Test
    fun intToCmykColor() {
        val white = 0xFFFFFFFF.toInt()
        val cmyk = white.toCmykColor()
        assertNear(0f, cmyk.cyan)
        assertNear(0f, cmyk.magenta)
        assertNear(0f, cmyk.yellow)
        assertNear(0f, cmyk.key)
    }

    // ===========================================================
    // LAB chain conversions
    // ===========================================================

    @Test
    fun labToHsl() {
        val lab = LabColor(l = 100f, a = 0f, b = 0f) // white
        val hsl = lab.toHsl()
        assertNear(1f, hsl.lightness, tolerance = 0.01f, msg = "lightness")
    }

    @Test
    fun hslToLab() {
        val hsl = HslColor(hue = 0f, saturation = 0f, lightness = 0f) // black
        val lab = hsl.toLab()
        assertNear(0f, lab.l, tolerance = 0.5f, msg = "L")
    }

    @Test
    fun labToArgbInt() {
        val lab = LabColor(l = 0f, a = 0f, b = 0f) // black
        val argb = lab.toArgbInt()
        val rgb = argb.toRgbColor()
        assertEquals(0, rgb.intRed)
        assertEquals(0, rgb.intGreen)
        assertEquals(0, rgb.intBlue)
    }

    @Test
    fun intToLabColor() {
        val black = 0xFF000000.toInt()
        val lab = black.toLabColor()
        assertNear(0f, lab.l, tolerance = 0.5f)
    }

    // ===========================================================
    // CRITICAL: Round-trip precision tests (Float-based, no quantization)
    // These prove the Float API eliminates the Int quantization bug.
    // ===========================================================

    // ---- HSL -> RGB -> HSL must be EXACT (within Float epsilon) ----

    @Test
    fun roundTripHslRgbHslExact_nonTrivialColor() {
        val original = HslColor(hue = 210.5f, saturation = 0.47f, lightness = 0.63f)
        val roundTripped = original.toRgb().toHsl()
        assertNear(original.hue, roundTripped.hue, tolerance = eps, msg = "hue")
        assertNear(
            original.saturation,
            roundTripped.saturation,
            tolerance = eps,
            msg = "saturation",
        )
        assertNear(original.lightness, roundTripped.lightness, tolerance = eps, msg = "lightness")
    }

    @Test
    fun roundTripHslRgbHslExact_warmDesaturated() {
        val original = HslColor(hue = 33.7f, saturation = 0.22f, lightness = 0.77f)
        val roundTripped = original.toRgb().toHsl()
        assertNear(original.hue, roundTripped.hue, tolerance = eps, msg = "hue")
        assertNear(
            original.saturation,
            roundTripped.saturation,
            tolerance = eps,
            msg = "saturation",
        )
        assertNear(original.lightness, roundTripped.lightness, tolerance = eps, msg = "lightness")
    }

    @Test
    fun roundTripHslRgbHslExact_deepPurple() {
        val original = HslColor(hue = 275.3f, saturation = 0.85f, lightness = 0.31f)
        val roundTripped = original.toRgb().toHsl()
        assertNear(original.hue, roundTripped.hue, tolerance = eps, msg = "hue")
        assertNear(
            original.saturation,
            roundTripped.saturation,
            tolerance = eps,
            msg = "saturation",
        )
        assertNear(original.lightness, roundTripped.lightness, tolerance = eps, msg = "lightness")
    }

    @Test
    fun roundTripHslRgbHslExact_nearRed() {
        val original = HslColor(hue = 5.5f, saturation = 0.91f, lightness = 0.44f)
        val roundTripped = original.toRgb().toHsl()
        assertNear(original.hue, roundTripped.hue, tolerance = eps, msg = "hue")
        assertNear(
            original.saturation,
            roundTripped.saturation,
            tolerance = eps,
            msg = "saturation",
        )
        assertNear(original.lightness, roundTripped.lightness, tolerance = eps, msg = "lightness")
    }

    @Test
    fun roundTripHslRgbHslExact_cyan() {
        val original = HslColor(hue = 185.2f, saturation = 0.63f, lightness = 0.52f)
        val roundTripped = original.toRgb().toHsl()
        assertNear(original.hue, roundTripped.hue, tolerance = eps, msg = "hue")
        assertNear(
            original.saturation,
            roundTripped.saturation,
            tolerance = eps,
            msg = "saturation",
        )
        assertNear(original.lightness, roundTripped.lightness, tolerance = eps, msg = "lightness")
    }

    // ---- HSL -> RGB -> CMYK -> RGB -> HSL must be EXACT ----

    @Test
    fun roundTripHslRgbCmykRgbHslExact() {
        val original = HslColor(hue = 210.5f, saturation = 0.47f, lightness = 0.63f)
        val result = original.toRgb().toCmyk().toRgb().toHsl()
        assertNear(original.hue, result.hue, tolerance = eps, msg = "hue")
        assertNear(original.saturation, result.saturation, tolerance = eps, msg = "saturation")
        assertNear(original.lightness, result.lightness, tolerance = eps, msg = "lightness")
    }

    @Test
    fun roundTripHslRgbCmykRgbHslExact_green() {
        val original = HslColor(hue = 120f, saturation = 1f, lightness = 0.5f)
        val result = original.toRgb().toCmyk().toRgb().toHsl()
        assertNear(original.hue, result.hue, tolerance = eps, msg = "hue")
        assertNear(original.saturation, result.saturation, tolerance = eps, msg = "saturation")
        assertNear(original.lightness, result.lightness, tolerance = eps, msg = "lightness")
    }

    @Test
    fun roundTripHslRgbCmykRgbHslExact_nonTrivial2() {
        val original = HslColor(hue = 97.3f, saturation = 0.38f, lightness = 0.71f)
        val result = original.toRgb().toCmyk().toRgb().toHsl()
        assertNear(original.hue, result.hue, tolerance = eps, msg = "hue")
        assertNear(original.saturation, result.saturation, tolerance = eps, msg = "saturation")
        assertNear(original.lightness, result.lightness, tolerance = eps, msg = "lightness")
    }

    // ---- HSL -> RGB -> LAB -> RGB -> HSL must be near-exact (LAB uses transcendental) ----

    @Test
    fun roundTripHslRgbLabRgbHslNearExact() {
        val original = HslColor(hue = 210.5f, saturation = 0.47f, lightness = 0.63f)
        val result = original.toRgb().toLab().toRgb().toHsl()
        assertNear(original.hue, result.hue, tolerance = labEps, msg = "hue")
        assertNear(original.saturation, result.saturation, tolerance = 0.01f, msg = "saturation")
        assertNear(original.lightness, result.lightness, tolerance = 0.01f, msg = "lightness")
    }

    @Test
    fun roundTripHslRgbLabRgbHslNearExact_warmDesaturated() {
        val original = HslColor(hue = 33.7f, saturation = 0.22f, lightness = 0.77f)
        val result = original.toRgb().toLab().toRgb().toHsl()
        assertNear(original.hue, result.hue, tolerance = labEps, msg = "hue")
        assertNear(original.saturation, result.saturation, tolerance = 0.01f, msg = "saturation")
        assertNear(original.lightness, result.lightness, tolerance = 0.01f, msg = "lightness")
    }

    @Test
    fun roundTripHslRgbLabRgbHslNearExact_deepPurple() {
        val original = HslColor(hue = 275.3f, saturation = 0.85f, lightness = 0.31f)
        val result = original.toRgb().toLab().toRgb().toHsl()
        assertNear(original.hue, result.hue, tolerance = labEps, msg = "hue")
        assertNear(original.saturation, result.saturation, tolerance = 0.01f, msg = "saturation")
        assertNear(original.lightness, result.lightness, tolerance = 0.01f, msg = "lightness")
    }

    // ===========================================================
    // Cross-space round trips (RGB through all spaces)
    // ===========================================================

    @Test
    fun rgbThroughAllSpaces() {
        val original = RgbColor(0.392f, 0.588f, 0.784f)
        val hsl = original.toHsl()
        val cmyk = original.toCmyk()
        val lab = original.toLab()

        val fromHsl = hsl.toRgb()
        val fromCmyk = cmyk.toRgb()
        val fromLab = lab.toRgb()

        // HSL round-trip: exact within Float epsilon
        assertNear(original.red, fromHsl.red, tolerance = eps, msg = "HSL red")
        assertNear(original.green, fromHsl.green, tolerance = eps, msg = "HSL green")
        assertNear(original.blue, fromHsl.blue, tolerance = eps, msg = "HSL blue")

        // CMYK round-trip: exact within Float epsilon
        assertNear(original.red, fromCmyk.red, tolerance = eps, msg = "CMYK red")
        assertNear(original.green, fromCmyk.green, tolerance = eps, msg = "CMYK green")
        assertNear(original.blue, fromCmyk.blue, tolerance = eps, msg = "CMYK blue")

        // LAB round-trip: near-exact (transcendental functions)
        assertNear(original.red, fromLab.red, tolerance = 0.01f, msg = "LAB red")
        assertNear(original.green, fromLab.green, tolerance = 0.01f, msg = "LAB green")
        assertNear(original.blue, fromLab.blue, tolerance = 0.01f, msg = "LAB blue")
    }

    @Test
    fun rgbThroughAllSpaces_brightOrange() {
        val original = RgbColor(0.95f, 0.55f, 0.1f)
        val fromHsl = original.toHsl().toRgb()
        val fromCmyk = original.toCmyk().toRgb()
        val fromLab = original.toLab().toRgb()

        assertNear(original.red, fromHsl.red, tolerance = eps, msg = "HSL red")
        assertNear(original.green, fromHsl.green, tolerance = eps, msg = "HSL green")
        assertNear(original.blue, fromHsl.blue, tolerance = eps, msg = "HSL blue")

        assertNear(original.red, fromCmyk.red, tolerance = eps, msg = "CMYK red")
        assertNear(original.green, fromCmyk.green, tolerance = eps, msg = "CMYK green")
        assertNear(original.blue, fromCmyk.blue, tolerance = eps, msg = "CMYK blue")

        assertNear(original.red, fromLab.red, tolerance = 0.01f, msg = "LAB red")
        assertNear(original.green, fromLab.green, tolerance = 0.01f, msg = "LAB green")
        assertNear(original.blue, fromLab.blue, tolerance = 0.01f, msg = "LAB blue")
    }

    // ===========================================================
    // Alpha preservation across chains
    // ===========================================================

    @Test
    fun alphaPreservedThroughCmykChain() {
        val hsl = HslColor(hue = 60f, saturation = 0.8f, lightness = 0.4f, alpha = 0.392f)
        val result = hsl.toCmyk().toRgb().toHsl()
        assertEquals(0.392f, result.alpha)
    }

    @Test
    fun alphaPreservedThroughLabChain() {
        val hsl = HslColor(hue = 60f, saturation = 0.8f, lightness = 0.4f, alpha = 0.392f)
        val result = hsl.toLab().toRgb().toHsl()
        assertEquals(0.392f, result.alpha)
    }

    @Test
    fun alphaPreservedThroughFullChain() {
        val original = HslColor(hue = 200f, saturation = 0.6f, lightness = 0.5f, alpha = 0.333f)
        val rgb = original.toRgb()
        assertEquals(0.333f, rgb.alpha)
        val cmyk = rgb.toCmyk()
        assertEquals(0.333f, cmyk.alpha)
        val lab = rgb.toLab()
        assertEquals(0.333f, lab.alpha)
        val backFromCmyk = cmyk.toRgb()
        assertEquals(0.333f, backFromCmyk.alpha)
        val backFromLab = lab.toRgb()
        assertEquals(0.333f, backFromLab.alpha)
    }

    // ===========================================================
    // Achromatic edge cases
    // ===========================================================

    @Test
    fun achromaticBlackThroughAllSpaces() {
        val hsl = HslColor(hue = 0f, saturation = 0f, lightness = 0f)
        val cmyk = hsl.toCmyk()
        assertEquals(1f, cmyk.key)
        val lab = hsl.toLab()
        assertNear(0f, lab.l, tolerance = 0.5f, msg = "Lab L for black")
        val backHsl = cmyk.toHsl()
        assertNear(0f, backHsl.lightness, tolerance = eps, msg = "lightness for black via CMYK")
    }

    @Test
    fun achromaticWhiteThroughAllSpaces() {
        val hsl = HslColor(hue = 0f, saturation = 0f, lightness = 1f)
        val cmyk = hsl.toCmyk()
        assertNear(0f, cmyk.key, msg = "key for white")
        val lab = hsl.toLab()
        assertNear(100f, lab.l, tolerance = 0.5f, msg = "Lab L for white")
    }
}
