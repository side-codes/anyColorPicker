package codes.side.colorpicker.state

import codes.side.color.Cmyk
import codes.side.color.ColorSpace
import codes.side.color.DisplayP3
import codes.side.color.Hsl
import codes.side.color.Lab
import codes.side.color.OkLch
import codes.side.color.Srgb
import kotlin.test.Test
import kotlin.test.assertEquals

class ColoringModeTest {

    @Test
    fun aSpaceWithAHueDefaultsToIndependentTracks() {
        assertEquals(ColoringMode.Independent, ColoringMode.defaultFor(Hsl))
        assertEquals(ColoringMode.Independent, ColoringMode.defaultFor(OkLch))
        assertEquals(ColoringMode.Contextual, ColoringMode.defaultFor(Srgb))
        assertEquals(ColoringMode.Contextual, ColoringMode.defaultFor(Lab))
        assertEquals(ColoringMode.Contextual, ColoringMode.defaultFor(Cmyk))
    }

    @Test
    fun anAppSpaceWithAHueDefaultsToIndependentTracksToo() {
        assertEquals(ColoringMode.Independent, ColoringMode.defaultFor(ColorSpace.hsl("--coloring-hsl", DisplayP3)))
    }
}
