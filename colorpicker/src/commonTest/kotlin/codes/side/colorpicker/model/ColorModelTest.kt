package codes.side.colorpicker.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class ColorModelTest {

    // ---- HslColor defaults & validation ----

    @Test
    fun hslColorDefaultValues() {
        val hsl = HslColor()
        assertEquals(0f, hsl.hue)
        assertEquals(1f, hsl.saturation)
        assertEquals(0.5f, hsl.lightness)
        assertEquals(1f, hsl.alpha)
    }

    @Test
    fun hslColorRejectsInvalidHue() {
        assertFailsWith<IllegalArgumentException> { HslColor(hue = -0.1f) }
        assertFailsWith<IllegalArgumentException> { HslColor(hue = 360.1f) }
    }

    @Test
    fun hslColorRejectsInvalidSaturation() {
        assertFailsWith<IllegalArgumentException> { HslColor(saturation = -0.01f) }
        assertFailsWith<IllegalArgumentException> { HslColor(saturation = 1.01f) }
    }

    @Test
    fun hslColorRejectsInvalidLightness() {
        assertFailsWith<IllegalArgumentException> { HslColor(lightness = -0.01f) }
        assertFailsWith<IllegalArgumentException> { HslColor(lightness = 1.01f) }
    }

    @Test
    fun hslColorRejectsInvalidAlpha() {
        assertFailsWith<IllegalArgumentException> { HslColor(alpha = -0.01f) }
        assertFailsWith<IllegalArgumentException> { HslColor(alpha = 1.01f) }
    }

    @Test
    fun hslIntAccessors() {
        val hsl = HslColor(hue = 180f, saturation = 0.5f, lightness = 0.25f, alpha = 0.5f)
        assertEquals(180, hsl.intHue)
        assertEquals(50, hsl.intSaturation)
        assertEquals(25, hsl.intLightness)
        assertEquals(128, hsl.intAlpha)
    }

    @Test
    fun hslCopyCreatesNewInstance() {
        val original = HslColor(hue = 100f, saturation = 0.8f, lightness = 0.6f)
        val copy = original.copy(hue = 200f)
        assertNotEquals(original, copy)
        assertEquals(200f, copy.hue)
        assertEquals(0.8f, copy.saturation)
    }

    // ---- RgbColor validation ----

    @Test
    fun rgbColorRejectsInvalidValues() {
        assertFailsWith<IllegalArgumentException> { RgbColor(red = 1.01f) }
        assertFailsWith<IllegalArgumentException> { RgbColor(green = -0.01f) }
        assertFailsWith<IllegalArgumentException> { RgbColor(blue = 1.5f) }
        assertFailsWith<IllegalArgumentException> { RgbColor(alpha = -0.1f) }
    }

    // ---- CmykColor validation ----

    @Test
    fun cmykColorRejectsInvalidValues() {
        assertFailsWith<IllegalArgumentException> { CmykColor(cyan = 1.01f) }
        assertFailsWith<IllegalArgumentException> { CmykColor(key = -0.01f) }
        assertFailsWith<IllegalArgumentException> { CmykColor(magenta = 1.5f) }
        assertFailsWith<IllegalArgumentException> { CmykColor(alpha = -0.01f) }
    }

    // ---- LabColor validation ----

    @Test
    fun labColorRanges() {
        val lab = LabColor(l = 0f, a = -128f, b = 127f)
        assertEquals(0f, lab.l)
        assertEquals(-128f, lab.a)
        assertEquals(127f, lab.b)

        assertFailsWith<IllegalArgumentException> { LabColor(a = -128.1f) }
        assertFailsWith<IllegalArgumentException> { LabColor(b = 127.1f) }
        assertFailsWith<IllegalArgumentException> { LabColor(l = -0.1f) }
        assertFailsWith<IllegalArgumentException> { LabColor(l = 100.1f) }
    }

    // ---- PickerColor sealed interface ----

    @Test
    fun alphaIsFloat() {
        val color = HslColor(alpha = 0.502f)
        assertEquals(0.502f, color.alpha)
    }

    @Test
    fun sealedInterfacePolymorphism() {
        val colors: List<PickerColor> = listOf(
            HslColor(),
            RgbColor(),
            CmykColor(),
            LabColor(),
        )
        assertEquals(4, colors.size)
        colors.forEach { assertEquals(1f, it.alpha) }
    }

    // ---- fromInt factories ----

    @Test
    fun hslFromIntFactory() {
        val hsl = HslColor.fromInt(hue = 180, saturation = 50, lightness = 25, alpha = 128)
        assertEquals(180f, hsl.hue)
        assertEquals(0.5f, hsl.saturation)
        assertEquals(0.25f, hsl.lightness)
        assertEquals(128 / 255f, hsl.alpha)
    }

    @Test
    fun rgbFromIntFactory() {
        val rgb = RgbColor.fromInt(red = 128, green = 64, blue = 32, alpha = 200)
        assertEquals(128 / 255f, rgb.red)
        assertEquals(64 / 255f, rgb.green)
        assertEquals(32 / 255f, rgb.blue)
        assertEquals(200 / 255f, rgb.alpha)
    }

    @Test
    fun cmykFromIntFactory() {
        val cmyk = CmykColor.fromInt(cyan = 50, magenta = 25, yellow = 75, key = 10, alpha = 128)
        assertEquals(0.5f, cmyk.cyan)
        assertEquals(0.25f, cmyk.magenta)
        assertEquals(0.75f, cmyk.yellow)
        assertEquals(0.1f, cmyk.key)
        assertEquals(128 / 255f, cmyk.alpha)
    }

    @Test
    fun labFromIntFactory() {
        val lab = LabColor.fromInt(l = 50, a = -60, b = 80, alpha = 200)
        assertEquals(50f, lab.l)
        assertEquals(-60f, lab.a)
        assertEquals(80f, lab.b)
        assertEquals(200 / 255f, lab.alpha)
    }
}
