package codes.side.colorpicker.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class OkColorModelTest {

    // ---- OklabColor ----

    @Test
    fun oklabColorDefaultValues() {
        val oklab = OklabColor()
        assertEquals(0.5f, oklab.l)
        assertEquals(0f, oklab.a)
        assertEquals(0f, oklab.b)
        assertEquals(1f, oklab.alpha)
    }

    @Test
    fun oklabColorRejectsInvalidValues() {
        assertFailsWith<IllegalArgumentException> { OklabColor(l = -0.01f) }
        assertFailsWith<IllegalArgumentException> { OklabColor(l = 1.01f) }
        assertFailsWith<IllegalArgumentException> { OklabColor(a = 0.41f) }
        assertFailsWith<IllegalArgumentException> { OklabColor(a = -0.41f) }
        assertFailsWith<IllegalArgumentException> { OklabColor(b = 0.41f) }
        assertFailsWith<IllegalArgumentException> { OklabColor(alpha = 1.01f) }
    }

    @Test
    fun oklabColorRejectsNan() {
        assertFailsWith<IllegalArgumentException> { OklabColor(l = Float.NaN) }
        assertFailsWith<IllegalArgumentException> { OklabColor(a = Float.NaN) }
        assertFailsWith<IllegalArgumentException> { OklabColor(b = Float.NaN) }
        assertFailsWith<IllegalArgumentException> { OklabColor(alpha = Float.NaN) }
    }

    @Test
    fun oklabIntAccessors() {
        val oklab = OklabColor(l = 0.63f, a = 0.2f, b = -0.1f, alpha = 0.5f)
        assertEquals(63, oklab.intL)
        // a and b report a percentage of the -0.4..0.4 reference range, per CSS oklab().
        assertEquals(50, oklab.intA)
        assertEquals(-25, oklab.intB)
        assertEquals(128, oklab.intAlpha)
    }

    @Test
    fun oklabFromIntClampsInsteadOfThrowing() {
        val oklab = OklabColor.fromInt(l = 150, a = 200, b = -200, alpha = 300)
        assertEquals(1f, oklab.l)
        assertEquals(0.4f, oklab.a)
        assertEquals(-0.4f, oklab.b)
        assertEquals(1f, oklab.alpha)
    }

    @Test
    fun oklabCopyCreatesNewInstance() {
        val original = OklabColor(l = 0.4f, a = 0.1f, b = -0.1f)
        val copy = original.copy(l = 0.8f)
        assertNotEquals(original, copy)
        assertEquals(0.8f, copy.l)
        assertEquals(0.1f, copy.a)
    }

    // ---- OklchColor ----

    @Test
    fun oklchColorDefaultValues() {
        val oklch = OklchColor()
        assertEquals(0.5f, oklch.l)
        assertEquals(0f, oklch.chroma)
        assertEquals(0f, oklch.hue)
        assertEquals(1f, oklch.alpha)
    }

    @Test
    fun oklchColorRejectsInvalidValues() {
        assertFailsWith<IllegalArgumentException> { OklchColor(l = 1.01f) }
        assertFailsWith<IllegalArgumentException> { OklchColor(chroma = -0.01f) }
        assertFailsWith<IllegalArgumentException> { OklchColor(chroma = 0.41f) }
        assertFailsWith<IllegalArgumentException> { OklchColor(hue = -0.1f) }
        assertFailsWith<IllegalArgumentException> { OklchColor(hue = 360.1f) }
        assertFailsWith<IllegalArgumentException> { OklchColor(alpha = -0.01f) }
    }

    @Test
    fun oklchNormalizesHue360ToZero() {
        assertEquals(0f, OklchColor(hue = 360f).hue)
        assertEquals(OklchColor(hue = 0f), OklchColor(hue = 360f))
    }

    @Test
    fun oklchIntAccessors() {
        val oklch = OklchColor(l = 0.63f, chroma = 0.2f, hue = 29.6f, alpha = 1f)
        assertEquals(63, oklch.intL)
        assertEquals(50, oklch.intChroma)
        assertEquals(30, oklch.intHue)
        assertEquals(255, oklch.intAlpha)
    }

    @Test
    fun oklchFromIntClampsInsteadOfThrowing() {
        val oklch = OklchColor.fromInt(l = -10, chroma = 500, hue = 400, alpha = -1)
        assertEquals(0f, oklch.l)
        assertEquals(0.4f, oklch.chroma)
        assertEquals(0f, oklch.hue) // 400 clamps to 360, which normalizes to 0
        assertEquals(0f, oklch.alpha)
    }

    // ---- OkhslColor ----

    @Test
    fun okhslColorDefaultValues() {
        val okhsl = OkhslColor()
        assertEquals(0f, okhsl.hue)
        assertEquals(1f, okhsl.saturation)
        assertEquals(0.5f, okhsl.lightness)
        assertEquals(1f, okhsl.alpha)
    }

    @Test
    fun okhslColorRejectsInvalidValues() {
        assertFailsWith<IllegalArgumentException> { OkhslColor(hue = -0.1f) }
        assertFailsWith<IllegalArgumentException> { OkhslColor(hue = 360.1f) }
        assertFailsWith<IllegalArgumentException> { OkhslColor(saturation = 1.01f) }
        assertFailsWith<IllegalArgumentException> { OkhslColor(lightness = -0.01f) }
        assertFailsWith<IllegalArgumentException> { OkhslColor(alpha = 1.01f) }
    }

    @Test
    fun okhslColorRejectsNan() {
        assertFailsWith<IllegalArgumentException> { OkhslColor(hue = Float.NaN) }
        assertFailsWith<IllegalArgumentException> { OkhslColor(saturation = Float.NaN) }
        assertFailsWith<IllegalArgumentException> { OkhslColor(lightness = Float.NaN) }
    }

    @Test
    fun okhslNormalizesHue360ToZero() {
        assertEquals(0f, OkhslColor(hue = 360f).hue)
        assertEquals(OkhslColor(hue = 0f), OkhslColor(hue = 360f))
    }

    @Test
    fun okhslIntAccessors() {
        val okhsl = OkhslColor(hue = 180f, saturation = 0.5f, lightness = 0.25f, alpha = 0.5f)
        assertEquals(180, okhsl.intHue)
        assertEquals(50, okhsl.intSaturation)
        assertEquals(25, okhsl.intLightness)
        assertEquals(128, okhsl.intAlpha)
    }

    @Test
    fun okhslFromIntClampsInsteadOfThrowing() {
        val okhsl = OkhslColor.fromInt(hue = -20, saturation = 150, lightness = -5, alpha = 999)
        assertEquals(0f, okhsl.hue)
        assertEquals(1f, okhsl.saturation)
        assertEquals(0f, okhsl.lightness)
        assertEquals(1f, okhsl.alpha)
    }

    // ---- OkhsvColor ----

    @Test
    fun okhsvColorDefaultValues() {
        val okhsv = OkhsvColor()
        assertEquals(0f, okhsv.hue)
        assertEquals(1f, okhsv.saturation)
        assertEquals(1f, okhsv.value)
        assertEquals(1f, okhsv.alpha)
    }

    @Test
    fun okhsvColorRejectsInvalidValues() {
        assertFailsWith<IllegalArgumentException> { OkhsvColor(hue = 360.1f) }
        assertFailsWith<IllegalArgumentException> { OkhsvColor(saturation = -0.01f) }
        assertFailsWith<IllegalArgumentException> { OkhsvColor(value = 1.01f) }
        assertFailsWith<IllegalArgumentException> { OkhsvColor(alpha = -0.01f) }
    }

    @Test
    fun okhsvNormalizesHue360ToZero() {
        assertEquals(0f, OkhsvColor(hue = 360f).hue)
        assertEquals(OkhsvColor(hue = 0f), OkhsvColor(hue = 360f))
    }

    @Test
    fun okhsvIntAccessors() {
        val okhsv = OkhsvColor(hue = 180f, saturation = 0.5f, value = 0.25f, alpha = 0.5f)
        assertEquals(180, okhsv.intHue)
        assertEquals(50, okhsv.intSaturation)
        assertEquals(25, okhsv.intValue)
        assertEquals(128, okhsv.intAlpha)
    }

    @Test
    fun okhsvFromIntClampsInsteadOfThrowing() {
        val okhsv = OkhsvColor.fromInt(hue = 400, saturation = -50, value = 150, alpha = -1)
        assertEquals(0f, okhsv.hue) // 400 clamps to 360, which normalizes to 0
        assertEquals(0f, okhsv.saturation)
        assertEquals(1f, okhsv.value)
        assertEquals(0f, okhsv.alpha)
    }

    // ---- Shared contracts ----

    @Test
    fun negativeZeroDoesNotSplitEquality() {
        assertEquals(OklabColor(a = 0f), OklabColor(a = -0f))
        assertEquals(OklabColor(a = 0f).hashCode(), OklabColor(a = -0f).hashCode())
        assertEquals(OkhslColor(hue = 0f), OkhslColor(hue = -0f))
        assertEquals(OkhsvColor(hue = 0f), OkhsvColor(hue = -0f))
        assertEquals(OklchColor(hue = 0f), OklchColor(hue = -0f))
    }

    @Test
    fun blackAndWhiteConstants() {
        assertEquals(OklabColor(l = 0f, a = 0f, b = 0f), OklabColor.Black)
        assertEquals(OklabColor(l = 1f, a = 0f, b = 0f), OklabColor.White)
        assertEquals(OklchColor(l = 0f, chroma = 0f, hue = 0f), OklchColor.Black)
        assertEquals(OklchColor(l = 1f, chroma = 0f, hue = 0f), OklchColor.White)
        assertEquals(OkhslColor(hue = 0f, saturation = 0f, lightness = 0f), OkhslColor.Black)
        assertEquals(OkhslColor(hue = 0f, saturation = 0f, lightness = 1f), OkhslColor.White)
        assertEquals(OkhsvColor(hue = 0f, saturation = 0f, value = 0f), OkhsvColor.Black)
        assertEquals(OkhsvColor(hue = 0f, saturation = 0f, value = 1f), OkhsvColor.White)
    }

    @Test
    fun equalsRejectsOtherSpacesWithTheSameChannels() {
        assertNotEquals<PickerColor>(OkhslColor(0f, 1f, 0.5f), OkhsvColor(0f, 1f, 0.5f))
        assertNotEquals<PickerColor>(OklabColor(0.5f, 0f, 0f), OklchColor(0.5f, 0f, 0f))
    }
}
