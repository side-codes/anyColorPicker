package codes.side.colorpicker.ui

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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ChannelTextTest {

    private fun assertShows(channel: ColorChannel, value: Double, label: String, text: String) {
        assertEquals(label, channelLabel(channel), "$channel's label")
        assertEquals(text, channelValueText(channel, value), "$channel at $value")
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
        assertEquals("L*", channelSpokenLabel(Lab.L))
        assertEquals("b*", channelSpokenLabel(Lab.B))
        assertEquals("Chroma", channelSpokenLabel(Lch.C))
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
            for (channel in space.channels) assertNotNull(libraryText(channel), "$channel falls back to an app channel's text")
        }
    }

    @Test
    fun alphaReadsInBytes() {
        assertEquals("Alpha", ALPHA_LABEL)
        assertEquals("128", alphaValueText(0.5))
        assertEquals("0", alphaValueText(0.0))
    }

    @Test
    fun anAppChannelShowsItsIdAndTheDecimalsItsStepNeeds() {
        val hsl = ColorSpace.hsl("--text-hsl", DisplayP3)
        assertShows(hsl.H, 200.4, "h", "200°")
        assertShows(hsl.S, 50.25, "s", "50")
        val rgb = ColorSpace.rgb("--text-rgb", RgbPrimaries.DisplayP3, WhitePoint.D65, TransferFunction.Srgb)
        assertShows(rgb.R, 0.5, "r", "0.500")
        assertEquals("r", channelSpokenLabel(rgb.R))
    }

    @Test
    fun decimalsRoundHalfAwayFromZeroAndDropTheSignOfZero() {
        assertEquals("0.000", decimals(-0.0004, 3))
        assertEquals("-1.24", decimals(-1.2351, 2))
        assertEquals("3", decimals(2.5, 0))
        assertEquals("0.100", decimals(0.1, 3))
        assertEquals("12.050", decimals(12.05, 3))
    }

    @Test
    fun aPlaneIsNamedAfterItsChannels() {
        assertEquals("Saturation and lightness", planeLabel(Hsl.S, Hsl.L))
        assertEquals("a* and b*", planeLabel(Lab.A, Lab.B))
        assertEquals("40% saturation, 60% lightness", planeValueText(Hsl.S, 40.0, Hsl.L, 60.0))
        assertEquals(
            PlaneActionLabels(
                increaseX = "Increase chroma",
                decreaseX = "Decrease chroma",
                increaseY = "Increase lightness",
                decreaseY = "Decrease lightness",
            ),
            planeActionLabels(OkLch.C, OkLch.L),
        )
    }
}
