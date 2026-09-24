package codes.side.color

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// A polar space over XYZ-D65 with an angle, a limited channel and analogous categories, so the
// value type can be tested before the library's own polar spaces exist.
@OptIn(ExperimentalColorSpaceApi::class)
private object TestPolar : ColorSpace(
    "--test-polar",
    listOf(
        ColorChannel("l", 0.0..1.0, analogous = AnalogousCategory.Lightness),
        ColorChannel("c", 0.0..1.0, limit = 0.0..Double.POSITIVE_INFINITY, analogous = AnalogousCategory.Colorfulness),
        ColorChannel("h", 0.0..360.0, kind = ChannelKind.Hue(HueFamily("test")), analogous = AnalogousCategory.Hue),
    ),
    XyzD65,
) {
    override fun toBase(src: DoubleArray, dst: DoubleArray) {
        dst[0] = src[0]
        dst[1] = src[1]
        dst[2] = src[2] / 360.0
    }

    override fun fromBase(src: DoubleArray, dst: DoubleArray) {
        dst[0] = src[0]
        dst[1] = src[1]
        dst[2] = src[2] * 360.0
    }

    override fun powerless(components: DoubleArray): Int = if (components[1] <= 0.001) 1 shl 2 else 0
}

// Takes XYZ-D65's own channels, which already belong to it.
@OptIn(ExperimentalColorSpaceApi::class)
private class Borrowing : ColorSpace("--borrowing", XyzD65.channels, XyzD65) {
    override fun toBase(src: DoubleArray, dst: DoubleArray) = Unit

    override fun fromBase(src: DoubleArray, dst: DoubleArray) = Unit
}

class ColorValueTest {

    @Test
    fun componentsReadBackAsGiven() {
        val color = XyzD65(0.25, 0.5, 0.75, alpha = 0.4)
        assertEquals(0.25, color[XyzD65.X])
        assertEquals(0.5, color[XyzD65.Y])
        assertEquals(0.75, color[XyzD65.Z])
        assertEquals(0.4, color.alpha)
        assertTrue(color.components().contentEquals(doubleArrayOf(0.25, 0.5, 0.75)))
    }

    @Test
    fun nothingOutsideALimitIsClamped() {
        // Out of every gamut but valid: the model holds it exactly.
        val color = XyzD65(-0.3, 2.5, 7.0)
        assertTrue(color.components().contentEquals(doubleArrayOf(-0.3, 2.5, 7.0)))
    }

    @Test
    fun invalidValuesAreRejected() {
        assertFailsWith<IllegalArgumentException> { XyzD65(Double.NaN, 0.0, 0.0) }
        assertFailsWith<IllegalArgumentException> { XyzD65(Double.POSITIVE_INFINITY, 0.0, 0.0) }
        assertFailsWith<IllegalArgumentException> { XyzD65(0.0, 0.0, 0.0, alpha = 1.5) }
        assertFailsWith<IllegalArgumentException> { TestPolar.color(doubleArrayOf(0.5, -0.1, 0.0)) }
        assertFailsWith<IllegalArgumentException> { XyzD65.color(doubleArrayOf(0.0, 0.0)) }
    }

    @Test
    fun hueWrapsIntoOneTurn() {
        assertEquals(10.0, TestPolar.color(doubleArrayOf(0.5, 0.2, 370.0))[TestPolar.channels[2]])
        assertEquals(350.0, TestPolar.color(doubleArrayOf(0.5, 0.2, -10.0))[TestPolar.channels[2]])
        assertEquals(0.0, TestPolar.color(doubleArrayOf(0.5, 0.2, 360.0))[TestPolar.channels[2]])
        assertEquals(
            TestPolar.color(doubleArrayOf(0.5, 0.2, 0.0)),
            TestPolar.color(doubleArrayOf(0.5, 0.2, 720.0)),
        )
    }

    @Test
    fun missingComponentsReadAsNull() {
        val color = XyzD65(0.2, null, 0.4, alpha = null)
        assertNull(color[XyzD65.Y])
        assertTrue(color.isMissing(XyzD65.Y))
        assertFalse(color.isMissing(XyzD65.X))
        assertTrue(color.isAlphaMissing)
        assertEquals(0.0, color.alpha)
        assertEquals(0.0, color.components()[1])
        assertEquals((1 shl 1) or ColorValue.MISSING_ALPHA, color.missingMask)
    }

    @Test
    fun withReplacesOneComponent() {
        val color = XyzD65(0.2, 0.3, 0.4)
        assertEquals(XyzD65(0.2, 0.9, 0.4), color.with(XyzD65.Y, 0.9))
        assertEquals(XyzD65(0.2, null, 0.4), color.with(XyzD65.Y, null))
        assertEquals(XyzD65(0.2, 0.3, 0.4, alpha = 0.5), color.withAlpha(0.5))
        assertEquals(color, XyzD65(0.2, null, 0.4).with(XyzD65.Y, 0.3))
    }

    @Test
    fun aChannelOfAnotherSpaceIsRefused() {
        assertFailsWith<IllegalArgumentException> { XyzD65(0.2, 0.3, 0.4)[XyzD50.X] }
        assertFailsWith<IllegalArgumentException> { XyzD65(0.2, 0.3, 0.4).with(XyzD50.X, 0.1) }
    }

    @Test
    fun equalityIsExactAndStructural() {
        assertEquals(XyzD65(0.0, 0.5, 1.0), XyzD65(-0.0, 0.5, 1.0))
        assertEquals(XyzD65(0.0, 0.5, 1.0).hashCode(), XyzD65(-0.0, 0.5, 1.0).hashCode())
        assertEquals(XyzD65(0.2, null, 0.4), XyzD65(0.2, null, 0.4))
        assertNotEquals(XyzD65(0.2, null, 0.4), XyzD65(0.2, 0.0, 0.4))
        assertNotEquals(XyzD65(0.2, 0.3, 0.4), XyzD50(0.2, 0.3, 0.4))
        assertNotEquals(XyzD65(0.2, 0.3, 0.4), XyzD65(0.2, 0.3, 0.4 + 1e-15))
    }

    @Test
    fun toStringNamesMissingComponents() {
        val text = XyzD65(0.2, null, 0.4, alpha = null).toString()
        assertTrue(text.startsWith("xyz-d65("), text)
        assertEquals(2, Regex("none").findAll(text).count(), text)
    }

    @Test
    fun conversionToTheSameSpaceIsTheValueItself() {
        val color = XyzD65(0.2, null, 0.4)
        assertTrue(color.to(XyzD65) === color)
    }

    @Test
    fun aMissingComponentWithoutAnAnalogCountsAsZero() {
        // XYZ's x, y, z are reds, greens and blues; the test space has none of those, so the
        // missing y is computed as 0 and does not stay missing.
        val converted = XyzD65(0.3, null, 0.5).to(TestPolar)
        assertFalse(converted.isMissing(TestPolar.channels[1]))
        assertEquals(0.0, converted[TestPolar.channels[1]])
    }

    @Test
    fun aHueThatComesOutPowerlessIsMissingAndItsColorfulnessZero() {
        val converted = XyzD65(0.3, 0.0005, 0.5).to(TestPolar)
        assertTrue(converted.isMissing(TestPolar.channels[2]))
        assertEquals(0.0, converted[TestPolar.channels[1]])
    }

    @Test
    fun aPowerlessHueGivenDirectlyIsKept() {
        // Only a conversion makes a hue missing; one written in keeps its value.
        val color = TestPolar.color(doubleArrayOf(0.5, 0.0, 200.0))
        assertEquals(200.0, color[TestPolar.channels[2]])
    }

    @Test
    fun aPowerlessHueConvertsAsTheGreyItIsTakenFor() {
        // CSS Color 4 §11.2: before converting, a powerless hue is missing and its colorfulness 0.
        assertComponents(doubleArrayOf(0.5, 0.5, 0.5), Hsl(120.0, 0.001, 50.0).to(Srgb), 1e-15)
        assertComponents(doubleArrayOf(0.4, 0.4, 0.4), Hsv(120.0, 0.001, 40.0).to(Srgb), 1e-15)
        // HWB's grey short of W + B = 100 is W, as browsers give it.
        assertComponents(doubleArrayOf(0.49999, 0.49999, 0.49999), Hwb(180.0, 49.999, 50.0).to(Srgb), 1e-15)
        assertComponents(doubleArrayOf(0.5, 0.5, 0.5), Hwb(180.0, 60.0, 60.0).to(Srgb), 1e-15)
        val grey = Lch(20.0, 0.0015, 180.0).to(OkLch)
        assertTrue(grey.isMissing(OkLch.H))
        assertEquals(0.0, grey[OkLch.C])
        assertTrue(Lch(20.0, 0.00151, 180.0).to(Lab)[Lab.A]!! < 0.0)
    }

    @Test
    fun aConversionThatOverflowsSaturatesInsteadOfThrowing() {
        // Bradford's first two rows sum past Double.MAX_VALUE.
        val high = XyzD65(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE).to(XyzD50)
        assertEquals(Double.MAX_VALUE, high[XyzD50.X])
        assertEquals(Double.MAX_VALUE, high[XyzD50.Y])
        val low = XyzD65(-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE).to(XyzD50)
        assertEquals(-Double.MAX_VALUE, low[XyzD50.X])
    }

    @Test
    fun aChannelBelongsToOneSpace() {
        assertFailsWith<IllegalStateException> { Borrowing() }
    }

    @Test
    fun missingAlphaStaysMissingThroughAConversion() {
        assertTrue(XyzD65(0.3, 0.4, 0.5, alpha = null).to(XyzD50).isAlphaMissing)
    }
}
