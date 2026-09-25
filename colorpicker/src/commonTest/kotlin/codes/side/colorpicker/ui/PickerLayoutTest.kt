package codes.side.colorpicker.ui

import codes.side.color.Cmyk
import codes.side.color.ColorSpace
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
import codes.side.color.Srgb
import codes.side.color.SrgbLinear
import codes.side.color.XyzD50
import codes.side.color.XyzD65
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PickerLayoutTest {

    @Test
    fun aSpaceWithOneHueAndTwoOtherChannelsHasAPlane() {
        for (space in listOf(Hsl, Hsv, Hwb, Lch, OkLch, Okhsl, Okhsv)) assertTrue(hasPlane(space), space.id)
        for (space in listOf(Srgb, SrgbLinear, DisplayP3, XyzD65, XyzD50, Lab, Oklab, Cmyk)) assertFalse(hasPlane(space), space.id)
        assertTrue(hasPlane(ColorSpace.hsl("--layout-hsl", DisplayP3)), "an app's HSL")
    }

    @Test
    fun thePlaneShowsColorfulnessAcrossAndTheOtherChannelUp() {
        assertEquals(Hsl.S to Hsl.L, planeAxes(Hsl))
        assertEquals(Hsv.S to Hsv.V, planeAxes(Hsv))
        assertEquals(Hwb.W to Hwb.B, planeAxes(Hwb), "HWB tags no colorfulness, so whiteness comes first")
        assertEquals(Lch.C to Lch.L, planeAxes(Lch))
        assertEquals(OkLch.C to OkLch.L, planeAxes(OkLch))
        assertEquals(Okhsl.S to Okhsl.L, planeAxes(Okhsl))
        assertEquals(Okhsv.S to Okhsv.V, planeAxes(Okhsv))
        assertNull(planeAxes(Srgb))
        assertNull(planeAxes(Cmyk))
    }
}
