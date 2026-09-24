package codes.side.color

import codes.side.color.internal.NAMED_COLORS
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

class CssParsingTest {

    @Test
    fun wptValidColorsParseAsBrowsersResolveThem() {
        for ((text, serialized) in WPT.valid) assertParsesLike(text, serialized)
    }

    @Test
    fun wptColorFunctionParsesInEverySupportedSpace() {
        for (space in listOf("srgb", "srgb-linear", "display-p3", "xyz", "xyz-d50", "xyz-d65")) {
            for ((text, serialized) in WPT.validColorFunction) {
                assertParsesLike(text.replace("{space}", space), serialized.replace("{space}", space))
            }
        }
    }

    @Test
    fun wptColorFunctionRefusesTheSpacesNotYetSupported() {
        for (space in listOf("a98-rgb", "rec2020", "prophoto-rgb", "display-p3-linear")) {
            for ((text, _) in WPT.validColorFunction) {
                val error = assertFailsWith<CssColorParseException>(text) { ColorValue.parseCss(text.replace("{space}", space)) }
                assertTrue("not supported" in error.message.orEmpty(), error.message)
            }
        }
    }

    @Test
    fun wptInvalidColorsAreRefused() {
        for (text in WPT.invalid) {
            assertFailsWith<CssColorParseException>("'$text' parsed") { ColorValue.parseCss(text) }
        }
    }

    @Test
    fun theColorStaysInTheSpaceItIsWrittenIn() {
        assertSame(Srgb, ColorValue.parseCss("#123").space)
        assertSame(Srgb, ColorValue.parseCss("teal").space)
        assertSame(Srgb, ColorValue.parseCss("rgb(1 2 3)").space)
        assertSame(Hsl, ColorValue.parseCss("hsl(1 2 3)").space)
        assertSame(Hwb, ColorValue.parseCss("hwb(1 2 3)").space)
        assertSame(Lab, ColorValue.parseCss("lab(1 2 3)").space)
        assertSame(Lch, ColorValue.parseCss("lch(1 2 3)").space)
        assertSame(Oklab, ColorValue.parseCss("oklab(0.1 0.2 0.3)").space)
        assertSame(OkLch, ColorValue.parseCss("oklch(0.1 0.2 3)").space)
        assertSame(DisplayP3, ColorValue.parseCss("color(display-p3 1 0 0)").space)
        assertSame(XyzD65, ColorValue.parseCss("color(xyz 1 0 0)").space)
        assertSame(Hsv, ColorValue.parseCss("color(--hsv 1 2 3)").space)
        assertSame(Okhsl, ColorValue.parseCss("color(--okhsl 1 0.2 0.3)").space)
        assertSame(Okhsv, ColorValue.parseCss("color(--okhsv 1 0.2 0.3)").space)
        assertSame(Cmyk, ColorValue.parseCss("color(--cmyk 0.1 0.2 0.3 0.4)").space)
    }

    @Test
    fun eightBitValuesAreExactFractions() {
        assertEquals(Srgb(0.2, 0.4, 0.6), ColorValue.parseCss("rgb(51 102 153)"))
        assertEquals(Srgb(0.2, 0.4, 0.6), ColorValue.parseCss("rgb(20% 40% 60%)"))
        assertEquals(Srgb(0.2, 0.4, 0.6, 0.0), ColorValue.parseCss("#33669900"))
    }

    @Test
    fun namedColorsAreCssColor4s() {
        // CSS Color 4's table exactly: every name it lists with its value, and no name it does not.
        val css = CSS.namedColors.associate { (name, rgb) -> name to (rgb[0] shl 16 or (rgb[1] shl 8) or rgb[2]) }
        val differences = (css.keys + NAMED_COLORS.keys).filter { css[it] != NAMED_COLORS[it] }
        assertTrue(differences.isEmpty(), differences.joinToString("\n") { "$it: ${NAMED_COLORS[it]?.toString(16)} here, ${css[it]?.toString(16)} in CSS" })
        for ((name, rgb) in CSS.namedColors) {
            assertEquals(Srgb(rgb[0] / 255.0, rgb[1] / 255.0, rgb[2] / 255.0), ColorValue.parseCss(name), name)
        }
        assertEquals(Srgb(0x66 / 255.0, 0x33 / 255.0, 0x99 / 255.0), ColorValue.parseCss("RebeccaPurple"))
        assertEquals(Srgb(0.0, 0.0, 0.0, 0.0), ColorValue.parseCss("transparent"))
    }

    @Test
    fun hueUnitsAreDegrees() {
        assertNear(180.0, ColorValue.parseCss("hsl(0.5turn 100% 50%)")[Hsl.H]!!, 1e-12)
        assertNear(180.0, ColorValue.parseCss("hsl(200grad 100% 50%)")[Hsl.H]!!, 1e-12)
        assertNear(180.0, ColorValue.parseCss("hsl(3.141592653589793rad 100% 50%)")[Hsl.H]!!, 1e-12)
        assertNear(270.0, ColorValue.parseCss("hsl(-90DEG 100% 50%)")[Hsl.H]!!, 1e-12)
        assertNear(270.0, ColorValue.parseCss("color(--okhsl -0.25turn 0.5 0.5)")[Okhsl.H]!!, 1e-12)
    }

    @Test
    fun percentagesResolveThroughTheChannelsReferenceRange() {
        assertComponents(doubleArrayOf(120.0, 50.0, 25.0), ColorValue.parseCss("color(--hsv 120 50% 25%)"), 1e-12)
        assertComponents(doubleArrayOf(0.1, 0.2, 0.3, 0.4), ColorValue.parseCss("color(--cmyk 10% 20% 30% 40%)"), 1e-12)
        assertComponents(doubleArrayOf(180.0, 0.5, 0.25), ColorValue.parseCss("color(--okhsl 180 50% 25%)"), 1e-12)
        assertFailsWith<CssColorParseException> { ColorValue.parseCss("color(--okhsl 50% 50% 25%)") }
    }

    @Test
    fun aPercentageOfAHundredIsItsOwnNumber() {
        // p × 100 / 100 lands an ulp off for about one percentage in seven.
        val hsl = ColorValue.parseCss("hsl(0 13.436424411240122% 84.74337369372327%)")
        assertEquals(13.436424411240122, hsl[Hsl.S])
        assertEquals(84.74337369372327, hsl[Hsl.L])
        assertEquals(43.27670679050534, ColorValue.parseCss("lab(43.27670679050534% 0 0)")[Lab.L])
    }

    @Test
    fun componentsClampToTheirChannelsLimit() {
        assertComponents(doubleArrayOf(0.0, 1.0, 0.0), ColorValue.parseCss("color(--okhsl 0 1.5 -0.5)"), 0.0)
        assertComponents(doubleArrayOf(50.0, 0.0, 20.0), ColorValue.parseCss("lch(50 -10 20)"), 0.0)
        assertComponents(doubleArrayOf(0.0, 150.0, 100.0), ColorValue.parseCss("color(--hsv 0 150 100)"), 0.0)
    }

    @Test
    fun anAppsSpaceParsesWhenItIsKnown() {
        val space = ColorSpace.polar("--my-lch", Lab, chromaReference = 150.0, powerlessChroma = 0.0015, hueFamily = HueFamily.CieLab)
        val color = ColorValue.parseCss("color(--my-lch 50 20% 0.25turn / 50%)", ColorSpaces.all + space)
        assertSame(space, color.space)
        assertComponents(doubleArrayOf(50.0, 30.0, 90.0), color, 1e-12)
        assertEquals(0.5, color.alpha)
        val error = assertFailsWith<CssColorParseException> { ColorValue.parseCss("color(--my-lch 50 20 90)") }
        assertEquals(6, error.index)
    }

    @Test
    fun dashedNamesAreCaseSensitive() {
        assertFailsWith<CssColorParseException> { ColorValue.parseCss("color(--HSV 1 2 3)") }
    }

    @Test
    fun cssOwnSpacesParseWithoutKnownSpaces() {
        assertSame(Srgb, ColorValue.parseCss("color(srgb 1 0 0)", knownSpaces = emptyList()).space)
        assertSame(Hsl, ColorValue.parseCss("hsl(1 2 3)", knownSpaces = emptyList()).space)
        assertFailsWith<CssColorParseException> { ColorValue.parseCss("color(--hsv 1 2 3)", knownSpaces = emptyList()) }
    }

    @Test
    fun twoSpacesWrittenAlikeAreRefused() {
        val twins = listOf(ColorSpace.hsv("--twin", Srgb), ColorSpace.hsv("--twin", DisplayP3))
        assertFailsWith<IllegalArgumentException> { ColorValue.parseCss("red", ColorSpaces.all + twins) }
        assertSame(Hsv, ColorValue.parseCss("color(--hsv 1 2 3)", ColorSpaces.all + ColorSpaces.all).space)
    }

    @Test
    fun anAppSpacesIdIsACssNameTheLibraryDoesNotUse() {
        for (id in listOf("--my hsv", "--a.b", "--x/y", "--a(b)", "--a,b", "--a:b", "--hsv", "--okhsl", "--okhsv", "--cmyk")) {
            assertFailsWith<IllegalArgumentException>(id) { ColorSpace.hsv(id, Srgb) }
        }
        val space = ColorSpace.hsv("--my_hsv-2é", Srgb)
        val color = space.color(doubleArrayOf(10.0, 20.0, 30.0))
        assertEquals(color, ColorValue.parseCss(color.toCssString(), ColorSpaces.all + space))
    }

    @Test
    fun namesAndUnitsIgnoreAsciiCase() {
        assertEquals(ColorValue.parseCss("rgb(1 2 3 / 0.5)"), ColorValue.parseCss("RGB(1 2 3 / 0.5)"))
        assertEquals(ColorValue.parseCss("color(srgb 1 0 0)"), ColorValue.parseCss("Color(SRGB 1 0 0)"))
        assertEquals(ColorValue.parseCss("lch(none 2 3)"), ColorValue.parseCss("LCH(NONE 2 3)"))
        assertEquals(ColorValue.parseCss("#aabbcc"), ColorValue.parseCss("#AaBbCc"))
        assertEquals(ColorValue.parseCss("red"), ColorValue.parseCss("ReD"))
    }

    @Test
    fun commentsAndWhitespaceAreSkipped() {
        assertEquals(Srgb(1 / 255.0, 2 / 255.0, 3 / 255.0), ColorValue.parseCss(" /* a */ rgb(\t1 /* b */2\n3 ) /* c */"))
        assertEquals(Srgb(1 / 255.0, 2 / 255.0, 3 / 255.0), ColorValue.parseCss("rgb(1,2,3)"))
    }

    @Test
    fun everyFormOfCssNumberParses() {
        assertEquals(Srgb(0.5, -2.5, 1.0), ColorValue.parseCss("color(srgb +.5 -.25e1 1E+0)"))
        assertEquals(Srgb(0.5, 0.0, 0.01), ColorValue.parseCss("color(srgb 50E-2 -0 1e-2)"))
        assertFailsWith<CssColorParseException> { ColorValue.parseCss("color(srgb 1. 0 0)") }
        assertFailsWith<CssColorParseException> { ColorValue.parseCss("color(srgb 1e 0 0)") }
        assertFailsWith<CssColorParseException> { ColorValue.parseCss("rgb(١ 2 3)") }
    }

    @Test
    fun numbersTooLargeForADoubleAreTheLargestOne() {
        assertEquals(Double.MAX_VALUE, ColorValue.parseCss("color(srgb 1e400 0 0)")[Srgb.R])
        assertEquals(-Double.MAX_VALUE, ColorValue.parseCss("color(srgb -1e400 0 0)")[Srgb.R])
        assertEquals(Double.MAX_VALUE, ColorValue.parseCss("lab(50 1e308% 0)")[Lab.A])
        assertEquals(100.0, ColorValue.parseCss("lab(1e400 0 0)")[Lab.L])
        assertTrue(ColorValue.parseCss("hsl(1e400 50% 50%)")[Hsl.H]!! in 0.0..<360.0)
        assertTrue(ColorValue.parseCss("hsl(1e308turn 50% 50%)")[Hsl.H]!! in 0.0..<360.0)
    }

    @Test
    fun unsupportedCssSaysSo() {
        for (text in listOf(
            "currentcolor",
            "Canvas",
            "calc(1)",
            "rgb(calc(1) 2 3)",
            "rgb(var(--x) 2 3)",
            "color-mix(in srgb, red, blue)",
            "light-dark(red, blue)",
            "rgb(from red r g b)",
            "color(rec2020 1 0 0)",
        )) {
            val error = assertFailsWith<CssColorParseException>(text) { ColorValue.parseCss(text) }
            assertTrue("not supported" in error.message.orEmpty(), "$text: ${error.message}")
        }
    }

    @Test
    fun errorsPointAtTheOffendingToken() {
        assertEquals(8, indexOfError("rgb(1 2 x)"))
        assertEquals(13, indexOfError("hsl(10, 50%, 0)"))
        assertEquals(9, indexOfError("rgb(1 2 3"))
        assertEquals(2, indexOfError("  bananas"))
        assertEquals(0, indexOfError("#12345"))
        assertEquals(21, indexOfError("color(srgb 1 1 1 / 1 cucumber)"))
        assertEquals(10, indexOfError("rgb(1 2 3)x"))
        assertEquals(0, indexOfError(""))
        assertEquals(1, indexOfError("r\\gb(1 2 3)"))
        assertEquals(4, indexOfError("rgb (1 2 3)"))
    }

    @Test
    fun theOrNullFormGivesNullOnlyForText() {
        assertNull(ColorValue.parseCssOrNull("rgb(1 2)"))
        assertEquals(Srgb(1.0, 0.0, 0.0), ColorValue.parseCssOrNull("red"))
    }

    @Test
    fun randomTextOnlyEverFailsToParse() {
        val random = Random(20260924)
        val alphabet = "rgbahslwkcolrxyzdp3-0123456789.+-eE%#(),/ \tnonefromcalcdegturn*\\éK"
        repeat(20_000) {
            val text = CharArray(random.nextInt(0, 24)) { alphabet[random.nextInt(alphabet.length)] }.concatToString()
            parseOrParseError(text)
        }
        val valid = WPT.valid.map { it.color }
        repeat(20_000) {
            val chars = valid[random.nextInt(valid.size)].toCharArray().toMutableList()
            repeat(random.nextInt(1, 4)) {
                val at = random.nextInt(chars.size + 1)
                when (random.nextInt(3)) {
                    0 -> chars.add(at, alphabet[random.nextInt(alphabet.length)])
                    1 -> if (at < chars.size) chars.removeAt(at)
                    else -> if (at < chars.size) chars[at] = alphabet[random.nextInt(alphabet.length)]
                }
            }
            parseOrParseError(chars.joinToString(""))
        }
    }

    private fun parseOrParseError(text: String) {
        try {
            ColorValue.parseCss(text)
        } catch (e: CssColorParseException) {
            assertTrue(e.index in 0..text.length, "index ${e.index} outside '$text'")
        } catch (e: Exception) {
            fail("'$text' threw $e")
        }
    }

    private fun indexOfError(text: String): Int = assertFailsWith<CssColorParseException>(text) { ColorValue.parseCss(text) }.index

    // A browser writes sRGB-based colors as 8-bit rgb(); anything else in its own syntax, rounded.
    private fun assertParsesLike(text: String, serialized: String) {
        val actual = try {
            ColorValue.parseCss(text)
        } catch (e: CssColorParseException) {
            fail("'$text' did not parse: ${e.message}")
        }
        val expected = ColorValue.parseCss(serialized)
        if (serialized.startsWith("rgb")) {
            val rgb = actual.to(Srgb).components()
            val want = expected.components()
            for (i in 0..2) {
                assertTrue(abs(rgb[i] * 255.0 - want[i] * 255.0) <= 0.5 + 1e-9, "'$text': channel $i is ${rgb[i] * 255.0}, browsers say ${want[i] * 255.0}")
            }
        } else {
            assertSame(expected.space, actual.space, text)
            assertEquals(expected.missingMask and ColorValue.MISSING_ALPHA.inv(), actual.missingMask and ColorValue.MISSING_ALPHA.inv(), text)
            assertComponents(expected.components(), actual, 1e-4)
            assertEquals(expected.isAlphaMissing, actual.isAlphaMissing, text)
        }
        assertNear(expected.alpha, actual.alpha, 1e-9, text)
    }
}
