package codes.side.colorpicker.foundation

import codes.side.color.Cmyk
import codes.side.color.ColorChannel
import codes.side.color.ColorSpace
import codes.side.color.ColorSpaces
import codes.side.color.DisplayP3
import codes.side.color.Hsl
import codes.side.color.Hsv
import codes.side.color.Hwb
import codes.side.color.Lab
import codes.side.color.Lch
import codes.side.color.OkLch
import codes.side.color.Okhsl
import codes.side.color.Okhsv
import codes.side.color.Oklab
import codes.side.color.RgbPrimaries
import codes.side.color.Srgb
import codes.side.color.SrgbLinear
import codes.side.color.TransferFunction
import codes.side.color.WhitePoint
import codes.side.color.XyzD50
import codes.side.color.XyzD65
import codes.side.colorpicker.ui.PlaneActionLabels
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EnglishTextTest {

    private val en = NumberFormatter("en-US")

    private fun assertShows(channel: ColorChannel, value: Double, label: String, text: String) {
        assertEquals(label, EnglishText.channelName(channel), "$channel's label")
        assertEquals(text, EnglishText.channelValue(channel, value, en), "$channel at $value")
    }

    @Test
    fun rgbReadsInBytes() {
        assertShows(Srgb.R, 0.5, "Red", "128")
        assertShows(DisplayP3.G, 1.0, "Green", "255")
        assertShows(Srgb.B, 0.0, "Blue", "0")
    }

    @Test
    fun linearRgbAndXyzReadInThousandths() {
        assertShows(SrgbLinear.B, 0.21404, "Blue", "0.214")
        assertShows(XyzD65.Y, 0.5, "Y", "0.500")
        assertShows(XyzD50.Z, -0.0001, "Z", "0.000")
    }

    @Test
    fun labReadsInWholeUnitsAndIsSpokenWithItsStars() {
        assertShows(Lab.L, 53.2, "L", "53")
        assertShows(Lab.A, -20.6, "a", "-21")
        assertEquals("L*", EnglishText.channelSpokenName(Lab.L))
        assertEquals("b*", EnglishText.channelSpokenName(Lab.B))
        assertEquals("Chroma", EnglishText.channelSpokenName(Lch.C))
    }

    @Test
    fun lchReadsInWholeUnitsAndOkLchLikeOklab() {
        assertShows(Lch.L, 70.4, "Lightness", "70")
        assertShows(Lch.C, 50.5, "Chroma", "51")
        assertShows(Lch.H, 12.5, "Hue", "13°")
        assertShows(OkLch.L, 0.754, "Lightness", "75%")
        assertShows(OkLch.C, 0.1234, "Chroma", "0.123")
        assertShows(OkLch.H, 264.05, "Hue", "264°")
    }

    @Test
    fun oklabShowsLightnessInPercentAndItsAxesInThousandths() {
        assertShows(Oklab.L, 0.5, "Lightness", "50%")
        assertShows(Oklab.A, -0.1, "a", "-0.100")
        assertShows(Oklab.B, 0.0625, "b", "0.063")
    }

    @Test
    fun hueSaturationSpacesReadInDegreesAndPercent() {
        assertShows(Hsl.H, 200.4, "Hue", "200°")
        assertShows(Hsl.S, 50.0, "Saturation", "50%")
        assertShows(Hsl.L, 49.6, "Lightness", "50%")
        assertShows(Hsv.V, 25.0, "Value", "25%")
        assertShows(Hwb.W, 10.0, "Whiteness", "10%")
        assertShows(Hwb.B, 0.0, "Blackness", "0%")
    }

    @Test
    fun okhslOkhsvAndCmykReadTheirFractionsInPercent() {
        assertShows(Okhsl.S, 0.856, "Saturation", "86%")
        assertShows(Okhsl.L, 0.5, "Lightness", "50%")
        assertShows(Okhsv.H, 30.0, "Hue", "30°")
        assertShows(Okhsv.V, 1.0, "Value", "100%")
        assertShows(Cmyk.C, 0.0, "Cyan", "0%")
        assertShows(Cmyk.K, 0.4, "Key", "40%")
    }

    @Test
    fun everyLibraryChannelHasItsOwnText() {
        for (space in ColorSpaces.all) {
            for (channel in space.channels) assertTrue(EnglishText.hasEntry(channel), "$channel falls back to an app channel's text")
        }
    }

    @Test
    fun alphaReadsInBytes() {
        assertEquals("Alpha", EnglishText.alphaName())
        assertEquals("128", EnglishText.alphaValue(0.5, en))
        assertEquals("0", EnglishText.alphaValue(0.0, en))
    }

    @Test
    fun aSliderWithNoChannelReadsItsPositionInPercent() {
        assertEquals("37%", EnglishText.sliderPosition(0.37f, en))
    }

    @Test
    fun aPositionOffTheTrackReadsWhereTheThumbIs() {
        // A caller's value can be NaN (0/0 from an empty range) or past an end; the thumb then sits at an end.
        assertEquals("0%", EnglishText.sliderPosition(Float.NaN, en))
        assertEquals("100%", EnglishText.sliderPosition(1.2f, en))
        assertEquals("0%", EnglishText.sliderPosition(-0.3f, en))
    }

    @Test
    fun anAppChannelShowsItsIdAndTheDecimalsItsStepNeeds() {
        val hsl = ColorSpace.hsl("--text-hsl", DisplayP3)
        assertShows(hsl.H, 200.4, "h", "200°")
        assertShows(hsl.S, 50.25, "s", "50")
        val rgb = ColorSpace.rgb("--text-rgb", RgbPrimaries.DisplayP3, WhitePoint.D65, TransferFunction.Srgb)
        assertShows(rgb.R, 0.5, "r", "0.500")
        assertEquals("r", EnglishText.channelSpokenName(rgb.R))
    }

    @Test
    fun anAppChannelFollowsTheLocalesSeparator() {
        val rgb = ColorSpace.rgb("--text-rgb-fr", RgbPrimaries.DisplayP3, WhitePoint.D65, TransferFunction.Srgb)
        assertEquals("0,500", EnglishText.channelValue(rgb.R, 0.5, NumberFormatter("fr-FR")))
    }

    @Test
    fun aLibraryValueFollowsTheLocale() {
        val text = EnglishText.channelValue(Hsl.S, 40.0, NumberFormatter("fr-FR"))
        assertTrue(Regex("40[\u00A0\u202F]%").matches(text), text)
    }

    @Test
    fun aPlaneIsNamedAfterItsChannels() {
        assertEquals("Saturation and lightness", EnglishText.planeDescription(Hsl.S, Hsl.L))
        assertEquals("a* and b*", EnglishText.planeDescription(Lab.A, Lab.B))
        assertEquals("40% saturation, 60% lightness", EnglishText.planeValue(Hsl.S, 40.0, Hsl.L, 60.0, en))
        assertEquals(
            PlaneActionLabels(
                increaseX = "Increase chroma",
                decreaseX = "Decrease chroma",
                increaseY = "Increase lightness",
                decreaseY = "Decrease lightness",
            ),
            EnglishText.planeActions(OkLch.C, OkLch.L),
        )
    }

    @Test
    fun aPlaneWithNoChannelsNamesItsAxes() {
        assertEquals(
            PlaneActionLabels(
                increaseX = "Increase horizontally",
                decreaseX = "Decrease horizontally",
                increaseY = "Increase vertically",
                decreaseY = "Decrease vertically",
            ),
            EnglishText.planeAxisActions(),
        )
    }

    @Test
    fun everyLibrarySpaceHasItsOwnName() {
        val names = ColorSpaces.all.map { EnglishText.spaceName(it) }
        assertEquals(names.size, names.toSet().size, "two spaces share a name: $names")
        assertEquals(listOf("Okhsl", "OkLCh", "HSV", "RGB"), listOf(Okhsl, OkLch, Hsv, Srgb).map { EnglishText.spaceName(it) })
        assertEquals("--text-hsl-name", EnglishText.spaceName(ColorSpace.hsl("--text-hsl-name", DisplayP3)))
    }

    @Test
    fun theDialogWordsFollowMaterialsPickers() {
        assertEquals("Select color", EnglishText.dialogTitle())
        assertEquals("OK", EnglishText.confirm())
        assertEquals("Cancel", EnglishText.dismiss())
        assertEquals("Original color", EnglishText.originalColor())
        assertEquals("New color", EnglishText.newColor())
        assertEquals("Restore original color", EnglishText.restoreOriginal())
    }
}
