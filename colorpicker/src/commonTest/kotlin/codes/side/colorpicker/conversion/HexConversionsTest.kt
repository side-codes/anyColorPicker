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
        assertEquals("#FF0000", RgbColor.Red.toHexString(HexAlpha.None))
    }

    @Test
    fun rgbToHexStringWithTranslucentAlpha() {
        val color = RgbColor.fromInt(red = 255, green = 128, blue = 64, alpha = 128)
        assertEquals("#80FF8040", color.toHexString())
        assertEquals("#FF8040", color.toHexString(HexAlpha.None))
    }

    @Test
    fun hexStringPadsLeadingZeros() {
        val color = RgbColor.fromInt(red = 0, green = 0, blue = 18, alpha = 0)
        assertEquals("#00000012", color.toHexString())
        assertEquals("#000012", color.toHexString(HexAlpha.None))
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
        assertEquals("#FF8040", 0x80FF8040.toInt().toHexColorString(HexAlpha.None))
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
        assertTrue(
            exception.message!!.contains("nope"),
            "message should contain the input: ${exception.message}",
        )
    }

    // ---- Round-trips ----

    @Test
    fun hexToColorToHexRoundTrip() {
        assertEquals("#80FF8040", "#80FF8040".toRgbColor().toHexString())
        assertEquals("#FF336699", "#336699".toRgbColor().toHexString())
        assertEquals("#336699", "#336699".toRgbColor().toHexString(HexAlpha.None))
    }

    @Test
    fun colorToHexToColorRoundTrip() {
        val original = RgbColor.fromInt(red = 12, green = 200, blue = 99, alpha = 42)
        assertEquals(original, original.toHexString().toRgbColor())
    }

    // ---- Alpha at the other end ----

    @Test
    fun formatsWithAlphaLastForCss() {
        val color = RgbColor(red = 0.2f, green = 0.5f, blue = 0.8f)
        assertEquals("#3380CCFF", color.toHexString(HexAlpha.Last))
        assertEquals("#FF3380CC", color.toHexString(HexAlpha.First))
        assertEquals("#3380CC", color.toHexString(HexAlpha.None))
    }

    @Test
    fun parsesEightDigitsFromEitherEnd() {
        // The string a stylesheet writes for an opaque mid blue.
        val css = "#3380CCFF".toRgbColorOrNull(HexAlpha.Last)!!
        assertEquals(51, css.intRed)
        assertEquals(128, css.intGreen)
        assertEquals(204, css.intBlue)
        assertEquals(255, css.intAlpha)
        // The same colour the Android way, which is what the default still reads.
        assertEquals(css, "#FF3380CC".toRgbColorOrNull())
    }

    @Test
    fun theTwoOrderingsDisagreeOnTheSameEightDigits() {
        // Half-transparent red to a stylesheet, opaque navy to android.graphics.Color.
        val asCss = "#FF000080".toRgbColorOrNull(HexAlpha.Last)!!
        val asAndroid = "#FF000080".toRgbColorOrNull(HexAlpha.First)!!
        assertEquals(255, asCss.intRed)
        assertEquals(128, asCss.intAlpha)
        assertEquals(0, asAndroid.intRed)
        assertEquals(255, asAndroid.intAlpha)
    }

    @Test
    fun parsesFourDigitShorthand() {
        // #RGBA: opaque red.
        val css = "#F00F".toRgbColorOrNull(HexAlpha.Last)!!
        assertEquals(RgbColor.Red, css)
        // #ARGB: the same digits read the Android way are an opaque blue.
        val android = "#F00F".toRgbColorOrNull(HexAlpha.First)!!
        assertEquals(255, android.intAlpha)
        assertEquals(0, android.intRed)
        assertEquals(255, android.intBlue)
    }

    @Test
    fun fourDigitShorthandExpandsEachDigit() {
        assertEquals("#ABC".toRgbColorOrNull(), "#FABC".toRgbColorOrNull(HexAlpha.First))
        assertEquals("#ABC".toRgbColorOrNull(), "#ABCF".toRgbColorOrNull(HexAlpha.Last))
    }

    @Test
    fun roundTripsThroughEitherOrdering() {
        val color = RgbColor(red = 0.2f, green = 0.5f, blue = 0.8f, alpha = 0.5f)
        for (position in listOf(HexAlpha.First, HexAlpha.Last)) {
            assertEquals(
                color.toHexString(position),
                color.toHexString(position).toRgbColorOrNull(position)!!.toHexString(position),
                "round trip with alpha $position",
            )
        }
    }

    @Test
    fun noneAcceptsOnlyTheFormsWithoutAlpha() {
        assertEquals(RgbColor.Red, "#FF0000".toRgbColorOrNull(HexAlpha.None))
        assertEquals(RgbColor.Red, "#F00".toRgbColorOrNull(HexAlpha.None))
        // Asking for no alpha and being handed some is a rejection, not a quiet reinterpretation.
        assertNull("#FF0000FF".toRgbColorOrNull(HexAlpha.None))
        assertNull("#F00F".toRgbColorOrNull(HexAlpha.None))
    }
}
