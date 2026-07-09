package codes.side.colorpicker.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HslColorTest {

    @Test
    fun defaultValues() {
        val hsl = HslColor()
        assertEquals(0f, hsl.hue)
        assertEquals(1f, hsl.saturation)
        assertEquals(0.5f, hsl.lightness)
        assertEquals(1f, hsl.alpha)
    }

    // ---- Hue boundaries ----

    @Test
    fun boundaryHue360IsValid() {
        val hsl = HslColor(hue = 360f)
        assertEquals(360f, hsl.hue)
    }

    @Test
    fun boundaryHue0IsValid() {
        val hsl = HslColor(hue = 0f)
        assertEquals(0f, hsl.hue)
    }

    @Test
    fun hueAbove360Rejected() {
        assertFailsWith<IllegalArgumentException> { HslColor(hue = 360.01f) }
    }

    @Test
    fun hueNegativeRejected() {
        assertFailsWith<IllegalArgumentException> { HslColor(hue = -0.01f) }
    }

    @Test
    fun hueFractionalValues() {
        val hsl = HslColor(hue = 210.5f)
        assertEquals(210.5f, hsl.hue)
    }

    // ---- Saturation boundaries ----

    @Test
    fun saturationBoundaries() {
        assertEquals(0f, HslColor(saturation = 0f).saturation)
        assertEquals(1f, HslColor(saturation = 1f).saturation)
        assertFailsWith<IllegalArgumentException> { HslColor(saturation = 1.01f) }
        assertFailsWith<IllegalArgumentException> { HslColor(saturation = -0.01f) }
    }

    @Test
    fun saturationFractionalValue() {
        val hsl = HslColor(saturation = 0.47f)
        assertEquals(0.47f, hsl.saturation)
    }

    // ---- Lightness boundaries ----

    @Test
    fun lightnessBoundaries() {
        assertEquals(0f, HslColor(lightness = 0f).lightness)
        assertEquals(1f, HslColor(lightness = 1f).lightness)
        assertFailsWith<IllegalArgumentException> { HslColor(lightness = 1.01f) }
        assertFailsWith<IllegalArgumentException> { HslColor(lightness = -0.01f) }
    }

    @Test
    fun lightnessFractionalValue() {
        val hsl = HslColor(lightness = 0.63f)
        assertEquals(0.63f, hsl.lightness)
    }

    // ---- Alpha boundaries ----

    @Test
    fun alphaBoundaries() {
        assertEquals(0f, HslColor(alpha = 0f).alpha)
        assertEquals(1f, HslColor(alpha = 1f).alpha)
        assertFailsWith<IllegalArgumentException> { HslColor(alpha = 1.01f) }
        assertFailsWith<IllegalArgumentException> { HslColor(alpha = -0.01f) }
    }

    @Test
    fun alphaFractionalValue() {
        val hsl = HslColor(alpha = 0.75f)
        assertEquals(0.75f, hsl.alpha)
    }

    // ---- Int accessors ----

    @Test
    fun intAccessorsMidValues() {
        val hsl = HslColor(hue = 180f, saturation = 0.5f, lightness = 0.25f, alpha = 128f / 255f)
        assertEquals(180, hsl.intHue)
        assertEquals(50, hsl.intSaturation)
        assertEquals(25, hsl.intLightness)
        assertEquals(128, hsl.intAlpha)
    }

    @Test
    fun intAccessorsAtZero() {
        val hsl = HslColor(hue = 0f, saturation = 0f, lightness = 0f, alpha = 0f)
        assertEquals(0, hsl.intHue)
        assertEquals(0, hsl.intSaturation)
        assertEquals(0, hsl.intLightness)
        assertEquals(0, hsl.intAlpha)
    }

    @Test
    fun intAccessorsAtMax() {
        val hsl = HslColor(hue = 360f, saturation = 1f, lightness = 1f, alpha = 1f)
        assertEquals(360, hsl.intHue)
        assertEquals(100, hsl.intSaturation)
        assertEquals(100, hsl.intLightness)
        assertEquals(255, hsl.intAlpha)
    }

    @Test
    fun intHueRoundsCorrectly() {
        val hsl = HslColor(hue = 210.5f)
        assertEquals(211, hsl.intHue) // roundToInt rounds 210.5 -> 211 (banker's toward even on .5, but 210.5 -> 211 is nearest)
    }

    // ---- Companion constants ----

    @Test
    fun companionBlack() {
        assertEquals(0f, HslColor.Black.lightness)
        assertEquals(0f, HslColor.Black.saturation)
    }

    @Test
    fun companionWhite() {
        assertEquals(1f, HslColor.White.lightness)
        assertEquals(0f, HslColor.White.saturation)
    }

    @Test
    fun companionRed() {
        assertEquals(0f, HslColor.Red.hue)
        assertEquals(1f, HslColor.Red.saturation)
        assertEquals(0.5f, HslColor.Red.lightness)
    }

    // ---- data class semantics ----

    @Test
    fun dataCopyCopiesIndependently() {
        val original = HslColor(hue = 100f, saturation = 0.8f, lightness = 0.6f)
        val copy = original.copy(hue = 200f)
        assertEquals(100f, original.hue)
        assertEquals(200f, copy.hue)
        assertEquals(0.8f, copy.saturation)
    }

    @Test
    fun equalityByValue() {
        val a = HslColor(hue = 42f, saturation = 0.5f, lightness = 0.6f, alpha = 0.78f)
        val b = HslColor(hue = 42f, saturation = 0.5f, lightness = 0.6f, alpha = 0.78f)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    // ---- fromInt factory ----

    @Test
    fun fromIntBasicConversion() {
        val hsl = HslColor.fromInt(hue = 180, saturation = 50, lightness = 25, alpha = 255)
        assertEquals(180f, hsl.hue)
        assertEquals(0.5f, hsl.saturation)
        assertEquals(0.25f, hsl.lightness)
        assertEquals(1f, hsl.alpha)
    }

    @Test
    fun fromIntDefaultAlpha() {
        val hsl = HslColor.fromInt(hue = 0, saturation = 100, lightness = 50)
        assertEquals(1f, hsl.alpha)
    }

    @Test
    fun fromIntClampsValues() {
        val hsl = HslColor.fromInt(hue = 999, saturation = 200, lightness = -10, alpha = 300)
        assertEquals(360f, hsl.hue)
        assertEquals(1f, hsl.saturation)
        assertEquals(0f, hsl.lightness)
        assertEquals(1f, hsl.alpha)
    }

    @Test
    fun fromIntClampsNegatives() {
        val hsl = HslColor.fromInt(hue = -10, saturation = -5, lightness = -1, alpha = -1)
        assertEquals(0f, hsl.hue)
        assertEquals(0f, hsl.saturation)
        assertEquals(0f, hsl.lightness)
        assertEquals(0f, hsl.alpha)
    }
}
