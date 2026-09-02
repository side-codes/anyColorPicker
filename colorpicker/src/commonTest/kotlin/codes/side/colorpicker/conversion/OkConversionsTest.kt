package codes.side.colorpicker.conversion

import codes.side.colorpicker.model.OkhslColor
import codes.side.colorpicker.model.OkhsvColor
import codes.side.colorpicker.model.OklchColor
import codes.side.colorpicker.model.RgbColor
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OkConversionsTest {

    private fun assertNear(expected: Float, actual: Float, tolerance: Float, msg: String = "") {
        assertTrue(
            abs(expected - actual) <= tolerance,
            "$msg expected=$expected actual=$actual diff=${abs(expected - actual)}",
        )
    }

    // ===========================================================
    // Oklab against Björn Ottosson's published values
    // ===========================================================

    @Test
    fun oklabWhite() {
        val lab = RgbColor(1f, 1f, 1f).toOklab()
        assertNear(1f, lab.l, 1e-4f, "L")
        assertNear(0f, lab.a, 1e-4f, "a")
        assertNear(0f, lab.b, 1e-4f, "b")
    }

    @Test
    fun oklabBlack() {
        val lab = RgbColor(0f, 0f, 0f).toOklab()
        assertNear(0f, lab.l, 1e-4f, "L")
        assertNear(0f, lab.a, 1e-4f, "a")
        assertNear(0f, lab.b, 1e-4f, "b")
    }

    @Test
    fun oklabRed() {
        val lab = RgbColor(1f, 0f, 0f).toOklab()
        assertNear(0.6279f, lab.l, 1e-3f, "L")
        assertNear(0.2249f, lab.a, 1e-3f, "a")
        assertNear(0.1258f, lab.b, 1e-3f, "b")
    }

    @Test
    fun oklabGreen() {
        val lab = RgbColor(0f, 1f, 0f).toOklab()
        assertNear(0.8664f, lab.l, 1e-3f, "L")
        assertNear(-0.2339f, lab.a, 1e-3f, "a")
        assertNear(0.1795f, lab.b, 1e-3f, "b")
    }

    @Test
    fun oklabBlue() {
        val lab = RgbColor(0f, 0f, 1f).toOklab()
        assertNear(0.4520f, lab.l, 1e-3f, "L")
        assertNear(-0.0324f, lab.a, 1e-3f, "a")
        assertNear(-0.3115f, lab.b, 1e-3f, "b")
    }

    @Test
    fun oklabGrayHasNoChroma() {
        val lab = RgbColor(0.5f, 0.5f, 0.5f).toOklab()
        assertNear(0f, lab.a, 1e-5f, "a")
        assertNear(0f, lab.b, 1e-5f, "b")
    }

    // ===========================================================
    // Round trips through sRGB
    // ===========================================================

    private val sweep: List<RgbColor> = buildList {
        for (r in 0..255 step 17) {
            for (g in 0..255 step 17) {
                for (b in 0..255 step 17) {
                    add(RgbColor(r / 255f, g / 255f, b / 255f))
                }
            }
        }
    }

    private fun assertSurvivesRoundTrip(
        tolerance: Float,
        label: String,
        convert: (RgbColor) -> RgbColor,
    ) {
        var worst = 0f
        var worstColor = ""
        for (rgb in sweep) {
            val back = convert(rgb)
            val delta = maxOf(
                abs(back.red - rgb.red),
                abs(back.green - rgb.green),
                abs(back.blue - rgb.blue),
            )
            if (delta > worst) {
                worst = delta
                worstColor = "$rgb -> $back"
            }
        }
        assertTrue(worst <= tolerance, "$label worst delta $worst exceeds $tolerance at $worstColor")
    }

    @Test
    fun oklabRoundTrip() {
        assertSurvivesRoundTrip(1e-4f, "Oklab") { it.toOklab().toRgb() }
    }

    @Test
    fun oklchRoundTrip() {
        assertSurvivesRoundTrip(1e-4f, "OkLCh") { it.toOklch().toRgb() }
    }

    // Okhsl and Okhsv are looser than Oklab on purpose. Below the cusp the reference
    // implementation approximates the gamut with a straight line to black, and about 4%
    // of sRGB — the most saturated blues and violets — lies just outside it, by at most
    // 2.6% of the boundary chroma. Ottosson lets saturation exceed 1 there; a 0..1
    // channel cannot, so those colours come back with slightly less chroma. Measured
    // worst case over the full cube is 4 steps of 255. Matching the reference matters
    // more than closing that gap: an Okhsl value has to mean the same here as it does
    // in colorjs or a browser.

    @Test
    fun okhslRoundTrip() {
        assertSurvivesRoundTrip(0.02f, "Okhsl") { it.toOkhsl().toRgb() }
    }

    @Test
    fun okhsvRoundTrip() {
        assertSurvivesRoundTrip(0.02f, "Okhsv") { it.toOkhsv().toRgb() }
    }

    @Test
    fun okhslRoundTripIsExactAwayFromTheGamutBoundary() {
        // Away from the boundary the triangle approximation does not bind, so the round
        // trip has to be tight. This is the test that would catch a real regression.
        var worst = 0f
        for (rgb in sweep) {
            val okhsl = rgb.toOkhsl()
            if (okhsl.saturation > 0.9f) continue
            val back = okhsl.toRgb()
            worst = maxOf(
                worst,
                abs(back.red - rgb.red),
                abs(back.green - rgb.green),
                abs(back.blue - rgb.blue),
            )
        }
        assertTrue(worst <= 1e-3f, "Okhsl round trip below saturation 0.9 drifted by $worst")
    }

    // ===========================================================
    // Okhsl / Okhsv structure
    // ===========================================================

    @Test
    fun okhslFullSaturationStaysInGamut() {
        // s = 1 means the gamut boundary, so the conversion must not need clamping:
        // going out and back has to return the same coordinates.
        var hue = 0f
        while (hue < 360f) {
            var lightness = 0.1f
            while (lightness <= 0.9f) {
                val original = OkhslColor(hue = hue, saturation = 1f, lightness = lightness)
                val back = original.toRgb().toOkhsl()
                assertNear(1f, back.saturation, 2e-2f, "saturation at hue=$hue l=$lightness")
                lightness += 0.1f
            }
            hue += 15f
        }
    }

    @Test
    fun okhslZeroSaturationIsGray() {
        val rgb = OkhslColor(hue = 200f, saturation = 0f, lightness = 0.5f).toRgb()
        assertNear(rgb.red, rgb.green, 1e-4f, "red vs green")
        assertNear(rgb.green, rgb.blue, 1e-4f, "green vs blue")
    }

    @Test
    fun okhslLightnessIsPerceptual() {
        // The complaint about HSL: blue and yellow at the same lightness look nothing
        // alike. In Okhsl the same lightness has to mean the same perceived lightness,
        // so their Oklab L must agree even though their hues do not.
        val blue = OkhslColor(hue = 264f, saturation = 1f, lightness = 0.5f).toRgb().toOklab()
        val yellow = OkhslColor(hue = 110f, saturation = 1f, lightness = 0.5f).toRgb().toOklab()
        assertNear(blue.l, yellow.l, 1e-2f, "perceived lightness")
    }

    @Test
    fun hslLightnessIsNotPerceptual() {
        // The contrast case for the test above, pinning why Okhsl is worth having.
        val blue = codes.side.colorpicker.model.HslColor(240f, 1f, 0.5f).toRgb().toOklab()
        val yellow = codes.side.colorpicker.model.HslColor(60f, 1f, 0.5f).toRgb().toOklab()
        assertTrue(
            abs(blue.l - yellow.l) > 0.3f,
            "HSL blue and yellow should differ widely in perceived lightness, got ${blue.l} and ${yellow.l}",
        )
    }

    @Test
    fun okhsvFullValueFullSaturationSitsOnTheGamutBoundary() {
        // Okhsv hue is Oklab's hue angle, so hue 0 is not sRGB red. What s=1, v=1 does
        // promise at every hue is the most vivid color the display can reach, which
        // means one channel pinned at its maximum.
        var hue = 0f
        while (hue < 360f) {
            val rgb = OkhsvColor(hue = hue, saturation = 1f, value = 1f).toRgb()
            val brightest = maxOf(rgb.red, rgb.green, rgb.blue)
            assertNear(1f, brightest, 1e-2f, "brightest channel at hue=$hue")
            hue += 15f
        }
    }

    @Test
    fun okhsvZeroValueIsBlack() {
        val rgb = OkhsvColor(hue = 123f, saturation = 0.7f, value = 0f).toRgb()
        assertEquals(0f, rgb.red)
        assertEquals(0f, rgb.green)
        assertEquals(0f, rgb.blue)
    }

    // ===========================================================
    // Alpha and degenerate inputs
    // ===========================================================

    @Test
    fun alphaSurvivesEverySpace() {
        val rgb = RgbColor(0.2f, 0.6f, 0.9f, alpha = 0.37f)
        assertEquals(0.37f, rgb.toOklab().alpha, "oklab")
        assertEquals(0.37f, rgb.toOklch().alpha, "oklch")
        assertEquals(0.37f, rgb.toOkhsl().alpha, "okhsl")
        assertEquals(0.37f, rgb.toOkhsv().alpha, "okhsv")
        assertEquals(0.37f, rgb.toOklab().toRgb().alpha, "oklab back")
        assertEquals(0.37f, rgb.toOkhsl().toRgb().alpha, "okhsl back")
        assertEquals(0.37f, rgb.toOkhsv().toRgb().alpha, "okhsv back")
    }

    @Test
    fun grayHasNoHueAndNoSaturation() {
        for (level in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
            val gray = RgbColor(level, level, level)
            val okhsl = gray.toOkhsl()
            val okhsv = gray.toOkhsv()
            assertNear(0f, okhsl.saturation, 1e-5f, "okhsl saturation at $level")
            assertNear(0f, okhsv.saturation, 1e-5f, "okhsv saturation at $level")
        }
    }

    @Test
    fun everyOkhslCoordinateProducesAFiniteColor() {
        var hue = 0f
        while (hue < 360f) {
            var s = 0f
            while (s <= 1f) {
                var l = 0f
                while (l <= 1f) {
                    val rgb = OkhslColor(hue, s, l).toRgb()
                    assertTrue(rgb.red.isFinite(), "red at ($hue, $s, $l)")
                    assertTrue(rgb.green.isFinite(), "green at ($hue, $s, $l)")
                    assertTrue(rgb.blue.isFinite(), "blue at ($hue, $s, $l)")
                    l += 0.125f
                }
                s += 0.125f
            }
            hue += 30f
        }
    }

    @Test
    fun everyOkhsvCoordinateProducesAFiniteColor() {
        var hue = 0f
        while (hue < 360f) {
            var s = 0f
            while (s <= 1f) {
                var v = 0f
                while (v <= 1f) {
                    val rgb = OkhsvColor(hue, s, v).toRgb()
                    assertTrue(rgb.red.isFinite(), "red at ($hue, $s, $v)")
                    assertTrue(rgb.green.isFinite(), "green at ($hue, $s, $v)")
                    assertTrue(rgb.blue.isFinite(), "blue at ($hue, $s, $v)")
                    v += 0.125f
                }
                s += 0.125f
            }
            hue += 30f
        }
    }

    // ===========================================================
    // Gamut mapping
    // ===========================================================

    @Test
    fun inGamutColorsPassThroughUntouched() {
        for (rgb in sweep) {
            val lab = rgb.toOklab()
            val back = lab.toRgb()
            assertNear(rgb.red, back.red, 1e-4f, "red for $rgb")
            assertNear(rgb.green, back.green, 1e-4f, "green for $rgb")
            assertNear(rgb.blue, back.blue, 1e-4f, "blue for $rgb")
        }
    }

    @Test
    fun gamutMappingGivesUpChromaFirst() {
        // An Oklab chroma this high is outside sRGB at every hue. What must survive is
        // lightness and hue; chroma is the channel that pays.
        var hue = 0f
        while (hue < 360f) {
            val original = OklchColor(l = 0.5f, chroma = 0.4f, hue = hue)
            val mapped = original.toRgb().toOklch()
            assertNear(0.5f, mapped.l, 3e-2f, "lightness at hue=$hue")
            assertTrue(
                mapped.chroma < original.chroma,
                "chroma should have been reduced at hue=$hue",
            )
            hue += 15f
        }
    }

    @Test
    fun gamutMappingBeatsPerChannelClamp() {
        // The comparison the CSS algorithm exists to win. Local-MINDE finishes on a clip,
        // so it does not hold lightness and hue exactly — but it has to hold them far
        // better than clamping each channel independently, which is what the LAB path
        // does and what CSS Color 4 rejects.
        var mappingWins = 0
        var total = 0
        var hue = 0f
        while (hue < 360f) {
            val original = OklchColor(l = 0.5f, chroma = 0.4f, hue = hue)
            val lab = original.toOklab()

            val mapped = original.toRgb().toOklch()

            val clampedLinear = oklabToLinearSrgb(
                OkLab(lab.l.toDouble(), lab.a.toDouble(), lab.b.toDouble()),
            ).clipToUnit()
            val clamped = RgbColor(
                red = delinearize(clampedLinear.r).toFloat().coerceIn(0f, 1f),
                green = delinearize(clampedLinear.g).toFloat().coerceIn(0f, 1f),
                blue = delinearize(clampedLinear.b).toFloat().coerceIn(0f, 1f),
            ).toOklch()

            fun drift(candidate: OklchColor): Float {
                val dL = abs(candidate.l - original.l)
                val dHue = abs(candidate.hue - original.hue).let { minOf(it, 360f - it) } / 360f
                return dL + dHue
            }

            total++
            if (drift(mapped) <= drift(clamped)) mappingWins++
            hue += 15f
        }
        assertTrue(
            mappingWins * 4 >= total * 3,
            "gamut mapping should beat per-channel clamping on most hues, won $mappingWins of $total",
        )
    }
}
