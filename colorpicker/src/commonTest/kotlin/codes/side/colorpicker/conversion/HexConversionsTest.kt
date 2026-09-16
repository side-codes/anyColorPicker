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
        assertEquals("#FFFF0000", RgbColor.Red.toHexString(HexAlpha.First))
    }

    @Test
    fun rgbToHexStringWithoutAlpha() {
        assertEquals("#FF0000", RgbColor.Red.toHexString(HexAlpha.None))
    }

    @Test
    fun rgbToHexStringWithTranslucentAlpha() {
        val color = RgbColor.fromInt(red = 255, green = 128, blue = 64, alpha = 128)
        assertEquals("#80FF8040", color.toHexString(HexAlpha.First))
        assertEquals("#FF8040", color.toHexString(HexAlpha.None))
    }

    @Test
    fun hexStringPadsLeadingZeros() {
        val color = RgbColor.fromInt(red = 0, green = 0, blue = 18, alpha = 0)
        assertEquals("#00000012", color.toHexString(HexAlpha.First))
        assertEquals("#000012", color.toHexString(HexAlpha.None))
    }

    @Test
    fun hslToHexString() {
        assertEquals("#FFFF0000", HslColor.Red.toHexString(HexAlpha.First))
    }

    @Test
    fun cmykToHexString() {
        assertEquals("#FFFFFFFF", CmykColor.White.toHexString(HexAlpha.First))
    }

    @Test
    fun labToHexString() {
        assertEquals("#FF000000", LabColor.Black.toHexString(HexAlpha.First))
    }

    // ---- Int.toHexColorString ----

    @Test
    fun intToHexColorString() {
        assertEquals("#80FF8040", 0x80FF8040.toInt().toHexColorString(HexAlpha.First))
        assertEquals("#FF8040", 0x80FF8040.toInt().toHexColorString(HexAlpha.None))
        assertEquals("#00000000", 0x00000000.toHexColorString(HexAlpha.First))
        assertEquals("#FFFFFFFF", 0xFFFFFFFF.toInt().toHexColorString(HexAlpha.First))
    }

    // ---- String.toRgbColorOrNull ----

    @Test
    fun parseSixDigitWithHash() {
        assertEquals(RgbColor.Red, "#FF0000".toRgbColorOrNull(HexAlpha.None))
    }

    @Test
    fun parseSixDigitWithoutHash() {
        assertEquals(RgbColor.Red, "FF0000".toRgbColorOrNull(HexAlpha.None))
    }

    @Test
    fun parseIsCaseInsensitive() {
        assertEquals(RgbColor.Red, "#ff0000".toRgbColorOrNull(HexAlpha.None))
        assertEquals("#fF0000".toRgbColorOrNull(HexAlpha.None), "#Ff0000".toRgbColorOrNull(HexAlpha.None))
    }

    @Test
    fun parseSixDigitDefaultsAlphaToOpaque() {
        val color = "#336699".toRgbColorOrNull(HexAlpha.None)!!
        assertEquals(1f, color.alpha)
        assertEquals(RgbColor.fromInt(red = 0x33, green = 0x66, blue = 0x99), color)
    }

    @Test
    fun parseShorthandExpandsDigits() {
        // #ABC expands to #AABBCC with opaque alpha
        val color = "#abc".toRgbColorOrNull(HexAlpha.None)!!
        assertEquals(RgbColor.fromInt(red = 0xAA, green = 0xBB, blue = 0xCC), color)
        assertEquals(1f, color.alpha)
    }

    @Test
    fun parseShorthandWithoutHash() {
        assertEquals("#F00".toRgbColorOrNull(HexAlpha.None), "F00".toRgbColorOrNull(HexAlpha.None))
        assertEquals(RgbColor.Red, "F00".toRgbColorOrNull(HexAlpha.None))
    }

    @Test
    fun parseEightDigitReadsAlphaFirst() {
        val color = "#80FF0000".toRgbColorOrNull(HexAlpha.First)!!
        assertEquals(128, color.intAlpha)
        assertEquals(255, color.intRed)
        assertEquals(0, color.intGreen)
        assertEquals(0, color.intBlue)
    }

    @Test
    fun parseInvalidInputsReturnNull() {
        assertNull("".toRgbColorOrNull(HexAlpha.None))
        assertNull("#".toRgbColorOrNull(HexAlpha.None))
        assertNull("#12".toRgbColorOrNull(HexAlpha.None))
        assertNull("#12345".toRgbColorOrNull(HexAlpha.None))
        assertNull("#1234567".toRgbColorOrNull(HexAlpha.None))
        assertNull("#123456789".toRgbColorOrNull(HexAlpha.None))
        assertNull("#GGHHII".toRgbColorOrNull(HexAlpha.None))
        assertNull("#-1FF000".toRgbColorOrNull(HexAlpha.None))
        assertNull("not a color".toRgbColorOrNull(HexAlpha.None))
        assertNull("##FF0000".toRgbColorOrNull(HexAlpha.None))
    }

    // ---- String.toRgbColor ----

    @Test
    fun strictParseReturnsColor() {
        assertEquals(RgbColor.Red, "#FF0000".toRgbColor(HexAlpha.None))
    }

    @Test
    fun strictParseThrowsWithOffendingString() {
        val exception = assertFailsWith<IllegalArgumentException> { "nope".toRgbColor(HexAlpha.None) }
        assertTrue(
            exception.message!!.contains("nope"),
            "message should contain the input: ${exception.message}",
        )
    }

    // ---- Round-trips ----

    @Test
    fun hexToColorToHexRoundTrip() {
        assertEquals("#80FF8040", "#80FF8040".toRgbColor(HexAlpha.First).toHexString(HexAlpha.First))
        assertEquals("#FF336699", "#336699".toRgbColor(HexAlpha.None).toHexString(HexAlpha.First))
        assertEquals("#336699", "#336699".toRgbColor(HexAlpha.None).toHexString(HexAlpha.None))
    }

    @Test
    fun colorToHexToColorRoundTrip() {
        // Formatting writes the alpha first; reading it back is where that has to be said, and
        // the parser has no default precisely so this line cannot be written without saying it.
        val original = RgbColor.fromInt(red = 12, green = 200, blue = 99, alpha = 42)
        assertEquals(original, original.toHexString(HexAlpha.First).toRgbColor(HexAlpha.First))
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
        // The same colour the Android way.
        assertEquals(css, "#FF3380CC".toRgbColorOrNull(HexAlpha.First))
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
        assertEquals("#ABC".toRgbColorOrNull(HexAlpha.None), "#FABC".toRgbColorOrNull(HexAlpha.First))
        assertEquals("#ABC".toRgbColorOrNull(HexAlpha.None), "#ABCF".toRgbColorOrNull(HexAlpha.Last))
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

    // ---- None refuses what it cannot resolve ----

    @Test
    fun noneRefusesTheLengthsThatCarryAlpha() {
        // Answering either way would be a colour that is wrong and looks right; null is the
        // only honest answer to a string nobody has said how to read.
        assertNull("#FF000080".toRgbColorOrNull(HexAlpha.None), "eight digits")
        assertNull("#F00C".toRgbColorOrNull(HexAlpha.None), "four digits")
    }

    @Test
    fun noneTakesTheLengthsThatCannotBeAmbiguous() {
        assertEquals(RgbColor(1f, 0f, 0f), "#FF0000".toRgbColorOrNull(HexAlpha.None))
        assertEquals(RgbColor(1f, 0f, 0f), "#F00".toRgbColorOrNull(HexAlpha.None))
    }

    @Test
    fun aCssStringReadWithTheAndroidOrderingIsTheWrongColour() {
        // Why the parser makes the caller say: #F00C is a red at 80% in a stylesheet, and
        // reading it alpha-first turns it into an opaque navy without a word.
        val asAndroid = "#F00C".toRgbColorOrNull(HexAlpha.First)!!
        assertEquals(0, asAndroid.intRed)
        assertEquals(204, asAndroid.intBlue)
        assertEquals(255, asAndroid.intAlpha)

        val asCss = "#F00C".toRgbColorOrNull(HexAlpha.Last)!!
        assertEquals(255, asCss.intRed)
        assertEquals(0, asCss.intBlue)
        assertEquals(204, asCss.intAlpha)
    }

    @Test
    fun theThrowingParserRefusesThemToo() {
        assertFailsWith<IllegalArgumentException> { "#FF000080".toRgbColor(HexAlpha.None) }
    }
}
