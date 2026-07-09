package codes.side.colorpicker.conversion

import codes.side.colorpicker.model.CmykColor
import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.model.LabColor
import codes.side.colorpicker.model.RgbColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HexConversionsTest {

    // ---- PickerColor.toHexString ----

    @Test
    fun rgbToHexStringWithAlpha() {
        assertEquals("#FFFF0000", RgbColor.Red.toHexString())
    }

    @Test
    fun rgbToHexStringWithoutAlpha() {
        assertEquals("#FF0000", RgbColor.Red.toHexString(includeAlpha = false))
    }

    @Test
    fun rgbToHexStringWithTranslucentAlpha() {
        val color = RgbColor.fromInt(red = 255, green = 128, blue = 64, alpha = 128)
        assertEquals("#80FF8040", color.toHexString())
        assertEquals("#FF8040", color.toHexString(includeAlpha = false))
    }

    @Test
    fun hexStringPadsLeadingZeros() {
        val color = RgbColor.fromInt(red = 0, green = 0, blue = 18, alpha = 0)
        assertEquals("#00000012", color.toHexString())
        assertEquals("#000012", color.toHexString(includeAlpha = false))
    }

    @Test
    fun hslToHexString() {
        assertEquals("#FFFF0000", HslColor.Red.toHexString())
    }

    @Test
    fun cmykToHexString() {
        assertEquals("#FFFFFFFF", CmykColor.White.toHexString())
    }

    @Test
    fun labToHexString() {
        assertEquals("#FF000000", LabColor.Black.toHexString())
    }

    // ---- Int.toHexColorString ----

    @Test
    fun intToHexColorString() {
        assertEquals("#80FF8040", 0x80FF8040.toInt().toHexColorString())
        assertEquals("#FF8040", 0x80FF8040.toInt().toHexColorString(includeAlpha = false))
        assertEquals("#00000000", 0x00000000.toHexColorString())
        assertEquals("#FFFFFFFF", 0xFFFFFFFF.toInt().toHexColorString())
    }

    // ---- String.toRgbColorOrNull ----

    @Test
    fun parseSixDigitWithHash() {
        assertEquals(RgbColor.Red, "#FF0000".toRgbColorOrNull())
    }

    @Test
    fun parseSixDigitWithoutHash() {
        assertEquals(RgbColor.Red, "FF0000".toRgbColorOrNull())
    }

    @Test
    fun parseIsCaseInsensitive() {
        assertEquals(RgbColor.Red, "#ff0000".toRgbColorOrNull())
        assertEquals("#fF0000".toRgbColorOrNull(), "#Ff0000".toRgbColorOrNull())
    }

    @Test
    fun parseSixDigitDefaultsAlphaToOpaque() {
        val color = "#336699".toRgbColorOrNull()!!
        assertEquals(1f, color.alpha)
        assertEquals(RgbColor.fromInt(red = 0x33, green = 0x66, blue = 0x99), color)
    }

    @Test
    fun parseShorthandExpandsDigits() {
        // #ABC expands to #AABBCC with opaque alpha
        val color = "#abc".toRgbColorOrNull()!!
        assertEquals(RgbColor.fromInt(red = 0xAA, green = 0xBB, blue = 0xCC), color)
        assertEquals(1f, color.alpha)
    }

    @Test
    fun parseShorthandWithoutHash() {
        assertEquals("#F00".toRgbColorOrNull(), "F00".toRgbColorOrNull())
        assertEquals(RgbColor.Red, "F00".toRgbColorOrNull())
    }

    @Test
    fun parseEightDigitReadsAlphaFirst() {
        val color = "#80FF0000".toRgbColorOrNull()!!
        assertEquals(128, color.intAlpha)
        assertEquals(255, color.intRed)
        assertEquals(0, color.intGreen)
        assertEquals(0, color.intBlue)
    }

    @Test
    fun parseInvalidInputsReturnNull() {
        assertNull("".toRgbColorOrNull())
        assertNull("#".toRgbColorOrNull())
        assertNull("#12".toRgbColorOrNull())
        assertNull("#12345".toRgbColorOrNull())
        assertNull("#1234567".toRgbColorOrNull())
        assertNull("#123456789".toRgbColorOrNull())
        assertNull("#GGHHII".toRgbColorOrNull())
        assertNull("#-1FF000".toRgbColorOrNull())
        assertNull("not a color".toRgbColorOrNull())
        assertNull("##FF0000".toRgbColorOrNull())
    }

    // ---- String.toRgbColor ----

    @Test
    fun strictParseReturnsColor() {
        assertEquals(RgbColor.Red, "#FF0000".toRgbColor())
    }

    @Test
    fun strictParseThrowsWithOffendingString() {
        val exception = assertFailsWith<IllegalArgumentException> { "nope".toRgbColor() }
        assertTrue(exception.message!!.contains("nope"), "message should contain the input: ${exception.message}")
    }

    // ---- Round-trips ----

    @Test
    fun hexToColorToHexRoundTrip() {
        assertEquals("#80FF8040", "#80FF8040".toRgbColor().toHexString())
        assertEquals("#FF336699", "#336699".toRgbColor().toHexString())
        assertEquals("#336699", "#336699".toRgbColor().toHexString(includeAlpha = false))
    }

    @Test
    fun colorToHexToColorRoundTrip() {
        val original = RgbColor.fromInt(red = 12, green = 200, blue = 99, alpha = 42)
        assertEquals(original, original.toHexString().toRgbColor())
    }
}
