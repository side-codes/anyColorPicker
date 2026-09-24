package codes.side.colorpicker.conversion

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.model.RgbColor
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ComposeColorExtTest {

    private fun assertNear(expected: Float, actual: Float, tolerance: Float, msg: String = "") {
        assertTrue(
            abs(expected - actual) <= tolerance,
            "$msg expected=$expected actual=$actual diff=${abs(expected - actual)}",
        )
    }

    // ---- sRGB round-trips ----

    @Test
    fun srgbRoundTripIsExact() {
        val original = Color(red = 0.25f, green = 0.5f, blue = 0.75f, alpha = 0.9f)
        val roundTripped = original.toRgbColor().toComposeColor()
        assertEquals(original, roundTripped)
    }

    @Test
    fun srgbRoundTripBlackAndWhite() {
        assertEquals(Color.Black, Color.Black.toRgbColor().toComposeColor())
        assertEquals(Color.White, Color.White.toRgbColor().toComposeColor())
    }

    @Test
    fun srgbColorChannelsAreReadDirectly() {
        val rgb = Color.Red.toRgbColor()
        assertEquals(RgbColor(red = 1f, green = 0f, blue = 0f, alpha = 1f), rgb)
    }

    // ---- Wide gamut ----

    @Test
    fun displayP3WhiteConvertsToSrgbWhite() {
        val p3White = Color(red = 1f, green = 1f, blue = 1f, colorSpace = ColorSpaces.DisplayP3)
        val rgb = p3White.toRgbColor()
        assertNear(1f, rgb.red, tolerance = 0.005f, msg = "red")
        assertNear(1f, rgb.green, tolerance = 0.005f, msg = "green")
        assertNear(1f, rgb.blue, tolerance = 0.005f, msg = "blue")
        assertEquals(1f, rgb.alpha)
    }

    @Test
    fun displayP3ColorIsConvertedToSrgbBeforeReadingChannels() {
        val p3 = Color(red = 0.5f, green = 0.25f, blue = 0.75f, colorSpace = ColorSpaces.DisplayP3)
        val rgb = p3.toRgbColor()
        // The raw P3 channel values must have gone through a real sRGB conversion
        assertTrue(abs(rgb.red - 0.5f) > 0.001f, "red should differ from raw P3 value: ${rgb.red}")
        assertTrue(
            rgb.blue > rgb.green,
            "hue ordering preserved: blue=${rgb.blue} green=${rgb.green}",
        )
        assertEquals(1f, rgb.alpha)
    }

    @Test
    fun displayP3RedIsGamutMappedRatherThanClipped() {
        // Clipping each channel returns sRGB red, which sits 0.28 degrees of Oklab hue away and
        // loses the lightness the source had. Mapping holds the hue and gives up chroma instead.
        val p3Red = Color(red = 1f, green = 0f, blue = 0f, colorSpace = ColorSpaces.DisplayP3)
        val rgb = p3Red.toRgbColor()
        assertTrue(rgb.green > 0.02f, "clipping would leave green at zero: $rgb")
        val source = p3Red.convert(ColorSpaces.Oklab)
        val sourceHue = atan2(source.blue, source.green) * 180f / PI.toFloat()
        assertNear(sourceHue, rgb.toOklch().hue, tolerance = 0.1f, msg = "hue")
    }

    @Test
    fun aDisplayP3GreyStaysNeutral() {
        // A grey carries no hue, so neither HSL nor OkLCh may find one in the conversion's rounding.
        for (v in listOf(0.2f, 0.5f, 0.9f, 0.99f, 0.995f)) {
            val rgb = Color(red = v, green = v, blue = v, colorSpace = ColorSpaces.DisplayP3).toRgbColor()
            assertEquals(0f, rgb.toHsl().saturation, "HSL saturation at $v: $rgb")
            assertEquals(0f, rgb.toOklch().chroma, "OkLCh chroma at $v: $rgb")
        }
    }

    @Test
    fun aDisplayP3ColorInsideSrgbIsOnlyConverted() {
        val p3 = Color(red = 0.6f, green = 0.4f, blue = 0.3f, colorSpace = ColorSpaces.DisplayP3)
        val expected = p3.convert(ColorSpaces.Srgb)
        val rgb = p3.toRgbColor()
        assertNear(expected.red, rgb.red, tolerance = 0.003f, msg = "red")
        assertNear(expected.green, rgb.green, tolerance = 0.003f, msg = "green")
        assertNear(expected.blue, rgb.blue, tolerance = 0.003f, msg = "blue")
    }

    @Test
    fun anOklabComposeColorConverts() {
        // sRGB red as Compose's own Oklab space holds it, in half floats.
        val oklabRed = Color(
            red = 0.6279554f,
            green = 0.22486307f,
            blue = 0.1258463f,
            colorSpace = ColorSpaces.Oklab,
        )
        val rgb = oklabRed.toRgbColor()
        assertNear(1f, rgb.red, tolerance = 0.01f, msg = "red")
        assertNear(0f, rgb.green, tolerance = 0.01f, msg = "green")
        assertNear(0f, rgb.blue, tolerance = 0.01f, msg = "blue")
    }

    @Test
    fun anExtendedSrgbColorBeyondWhiteLandsInRange() {
        // RgbColor's constructor rejects anything outside 0..1, so returning at all is the check.
        val bright = Color(red = 1.5f, green = 0.5f, blue = 0.5f, colorSpace = ColorSpaces.ExtendedSrgb)
        val rgb = bright.toRgbColor()
        assertTrue(rgb.red > rgb.green, "still red: $rgb")
    }

    // ---- Unspecified ----

    @Test
    fun unspecifiedColorThrows() {
        assertFailsWith<IllegalArgumentException> { Color.Unspecified.toRgbColor() }
    }

    @Test
    fun unspecifiedColorThrowsForModelConversions() {
        assertFailsWith<IllegalArgumentException> { Color.Unspecified.toHslColor() }
        assertFailsWith<IllegalArgumentException> { Color.Unspecified.toCmykColor() }
        assertFailsWith<IllegalArgumentException> { Color.Unspecified.toLabColor() }
    }

    // ---- Model conversion symmetry ----

    @Test
    fun composeColorToHslColor() {
        assertEquals(HslColor(hue = 0f, saturation = 1f, lightness = 0.5f), Color.Red.toHslColor())
    }

    @Test
    fun composeColorToCmykColor() {
        val cmyk = Color.Black.toCmykColor()
        assertEquals(1f, cmyk.key)
        assertEquals(0f, cmyk.cyan)
        assertEquals(0f, cmyk.magenta)
        assertEquals(0f, cmyk.yellow)
    }

    @Test
    fun composeColorToLabColor() {
        val lab = Color.White.toLabColor()
        assertNear(100f, lab.l, tolerance = 0.01f, msg = "L")
        assertNear(0f, lab.a, tolerance = 0.01f, msg = "a")
        assertNear(0f, lab.b, tolerance = 0.01f, msg = "b")
    }

    @Test
    fun alphaPropagatesThroughModelConversions() {
        val color = Color(red = 0f, green = 0f, blue = 1f, alpha = 0.5f)
        // Compose quantizes sRGB alpha to 8 bits, so allow one quantization step
        assertNear(0.5f, color.toHslColor().alpha, tolerance = 1f / 255f, msg = "hsl alpha")
        assertNear(0.5f, color.toCmykColor().alpha, tolerance = 1f / 255f, msg = "cmyk alpha")
        assertNear(0.5f, color.toLabColor().alpha, tolerance = 1f / 255f, msg = "lab alpha")
    }
}
