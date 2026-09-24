package codes.side.color

import codes.side.color.internal.cssNumber
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CssFormattingTest {

    @Test
    fun eachSpaceIsWrittenInItsOwnSyntax() {
        assertEquals("color(srgb 1 0.5 0)", Srgb(1.0, 0.5, 0.0).toCssString())
        assertEquals("color(srgb-linear 1 0.5 0)", SrgbLinear(1.0, 0.5, 0.0).toCssString())
        assertEquals("color(display-p3 1 0.5 0)", DisplayP3(1.0, 0.5, 0.0).toCssString())
        assertEquals("color(xyz-d65 0.1 0.2 0.3)", XyzD65(0.1, 0.2, 0.3).toCssString())
        assertEquals("color(xyz-d50 0.1 0.2 0.3)", XyzD50(0.1, 0.2, 0.3).toCssString())
        assertEquals("hsl(120 50% 25%)", Hsl(120.0, 50.0, 25.0).toCssString())
        assertEquals("hwb(120 10% 20%)", Hwb(120.0, 10.0, 20.0).toCssString())
        assertEquals("lab(53.241 80.093 67.203)", Lab(53.2408, 80.0925, 67.2032).toCssString())
        assertEquals("lch(53.241 104.55 39.999)", Lch(53.2408, 104.5518, 39.9990).toCssString())
        assertEquals("oklab(0.62796 0.22486 0.12585)", Oklab(0.627955, 0.224863, 0.125846).toCssString())
        assertEquals("oklch(0.7 0.15 140)", OkLch(0.7, 0.15, 140.0).toCssString())
        assertEquals("color(--hsv 120 50 25)", Hsv(120.0, 50.0, 25.0).toCssString())
        assertEquals("color(--okhsl 120 0.5 0.25)", Okhsl(120.0, 0.5, 0.25).toCssString())
        assertEquals("color(--okhsv 120 0.5 0.25)", Okhsv(120.0, 0.5, 0.25).toCssString())
        assertEquals("color(--cmyk 0 0 0 1)", Cmyk(0.0, 0.0, 0.0, 1.0).toCssString())
        val space = ColorSpace.polar("--my-lch", Lab, chromaReference = 150.0, powerlessChroma = 0.0015, hueFamily = HueFamily.CieLab)
        assertEquals("color(--my-lch 50 20 30)", space(50.0, 20.0, 30.0).toCssString())
    }

    @Test
    fun hslAndHwbWritePercentagesAsTheDraftAndOlderBrowsersWant() {
        // Browsers took numbers in hsl() and hwb() only from Chrome 121, Firefox 122 and Safari 18.
        assertEquals("hsl(120 50% none)", Hsl(120.0, 50.0, null).toCssString())
        assertEquals("hwb(none 10% 20% / 0.5)", Hwb(null, 10.0, 20.0, alpha = 0.5).toCssString())
        assertEquals("color(--hsv 120 50 25)", Hsv(120.0, 50.0, 25.0).toCssString())
    }

    @Test
    fun missingComponentsAreNoneAndAlphaIsLeftOutAtOne() {
        assertEquals("oklch(0.5 0 none)", OkLch(0.5, 0.0, null).toCssString())
        assertEquals("oklch(0.5 0 none / 0.5)", OkLch(0.5, 0.0, null, alpha = 0.5).toCssString())
        assertEquals("oklch(0.5 0 30 / none)", OkLch(0.5, 0.0, 30.0, alpha = null).toCssString())
        assertEquals("oklch(0.5 0 30)", OkLch(0.5, 0.0, 30.0, alpha = 0.9999999).toCssString())
    }

    @Test
    fun extendedValuesSurvive() {
        assertEquals("color(srgb 1.2 -0.1 0)", Srgb(1.2, -0.1, 0.0).toCssString())
        assertEquals("color(display-p3 -0.5 1.5 0)", DisplayP3(-0.5, 1.5, 0.0).toCssString())
    }

    @Test
    fun numbersRoundAsColorJsRoundsThem() {
        assertEquals("53.241", cssNumber(53.2408, 5))
        assertEquals("0.12346", cssNumber(0.123456, 5))
        assertEquals("0", cssNumber(3e-17, 5))
        assertEquals("0", cssNumber(-0.0000001, 5))
        assertEquals("0", cssNumber(-0.0, 5))
        assertEquals("123460", cssNumber(123456.7, 5))
        assertEquals("1000000000000000000000", cssNumber(1e21, 5))
        assertEquals("-1.5", cssNumber(-1.5, 5))
        assertEquals("1", cssNumber(0.999996, 5))
        assertEquals("0.3", cssNumber(0.25, 1))
        assertEquals("0.1", cssNumber(0.1, 17))
        assertEquals(309, cssNumber(Double.MAX_VALUE, 5).length)
        assertFailsWith<IllegalArgumentException> { Srgb(1.0, 0.0, 0.0).toCssString(precision = 0) }
        assertFailsWith<IllegalArgumentException> { Srgb(1.0, 0.0, 0.0).toCssString(precision = 18) }
    }

    @Test
    fun legacyUsesTheCommaSyntaxForSrgbAndHsl() {
        assertEquals("rgb(255, 127.5, 0)", Srgb(1.0, 0.5, 0.0).toCssString(legacy = true))
        assertEquals("rgba(255, 127.5, 0, 0.5)", Srgb(1.0, 0.5, 0.0, alpha = 0.5).toCssString(legacy = true))
        assertEquals("rgba(0, 127.5, 0, 0)", Srgb(null, 0.5, 0.0, alpha = null).toCssString(legacy = true))
        assertEquals("rgb(306, -25.5, 0)", Srgb(1.2, -0.1, 0.0).toCssString(legacy = true))
        assertEquals("hsl(120, 50%, 25%)", Hsl(120.0, 50.0, 25.0).toCssString(legacy = true))
        assertEquals("hsla(0, 50%, 25%, 0.25)", Hsl(null, 50.0, 25.0, alpha = 0.25).toCssString(legacy = true))
        assertEquals("oklch(0.7 0.15 140)", OkLch(0.7, 0.15, 140.0).toCssString(legacy = true))
    }

    @Test
    fun whatIsWrittenParsesBack() {
        val random = Random(20260924)
        val app = ColorSpace.polar("--my-lch", Lab, chromaReference = 150.0, powerlessChroma = 0.0015, hueFamily = HueFamily.CieLab)
        val known = ColorSpaces.all + app
        for (space in known) {
            repeat(200) {
                val color = randomColor(space, random)
                assertRoundTrip(color, color.toCssString(precision = 17), known, relative = 1e-15)
                assertRoundTrip(color, color.toCssString(), known, relative = 1e-4)
                if (space == Srgb || space == Hsl) {
                    // The comma syntax writes a missing component or alpha as 0.
                    val filled = space.color(color.components(), color.alpha)
                    assertRoundTrip(filled, color.toCssString(precision = 17, legacy = true), known, relative = 1e-15)
                }
            }
        }
    }

    // Where parsing clamps nothing, and half as far again past the reference range where it would
    // not; about one component in five, and alpha, missing.
    private fun randomColor(space: ColorSpace, random: Random): ColorValue {
        var missing = 0
        val components = DoubleArray(space.channels.size) { i ->
            val channel = space.channels[i]
            if (random.nextInt(5) == 0) missing = missing or (1 shl i)
            val clamped = channel.limit != null || space == Srgb || i == 0 && space.id in setOf("lab", "lch", "oklab", "oklch")
            val range = channel.referenceRange
            range.start + random.nextDouble() * (range.endInclusive - range.start) * (if (clamped) 1.0 else 1.5)
        }
        if (random.nextInt(5) == 0) missing = missing or ColorValue.MISSING_ALPHA
        return space.color(components, random.nextDouble(), missing)
    }

    private fun assertRoundTrip(color: ColorValue, text: String, known: List<ColorSpace>, relative: Double) {
        val parsed = ColorValue.parseCss(text, known)
        assertEquals(color.space, parsed.space, text)
        assertEquals(color.missingMask, parsed.missingMask, text)
        val expected = color.components()
        val actual = parsed.components()
        for (i in expected.indices) {
            val channel = color.space.channels[i]
            var difference = abs(expected[i] - actual[i])
            if (channel.isHue) difference = minOf(difference, 360.0 - difference)
            val scale = max(abs(expected[i]), channel.referenceRange.endInclusive)
            assertTrue(difference <= relative * scale, "$text: ${channel.id} was ${expected[i]}, parsed ${actual[i]}")
        }
        assertTrue(abs(color.alpha - parsed.alpha) <= relative, "$text: alpha was ${color.alpha}, parsed ${parsed.alpha}")
    }
}
