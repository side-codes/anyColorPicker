package codes.side.colorpicker.state

import codes.side.color.Cmyk
import codes.side.color.DisplayP3
import codes.side.color.Hsl
import codes.side.color.Hsv
import codes.side.color.Hwb
import codes.side.color.Lab
import codes.side.color.Lch
import codes.side.color.Okhsl
import codes.side.color.Okhsv
import codes.side.color.Oklab
import codes.side.color.Oklch
import codes.side.color.Srgb
import codes.side.color.SrgbLinear
import codes.side.color.XyzD50
import codes.side.color.XyzD65
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ColorPickerStateViewsTest {

    @Test
    fun eachViewIsTheValueConverted() {
        val color = Oklch(0.6, 0.1, 200.0)
        val state = ColorPickerState(color)
        assertEquals(color.to(Srgb), state.srgb.value)
        assertEquals(color.to(SrgbLinear), state.srgbLinear.value)
        assertEquals(color.to(DisplayP3), state.displayP3.value)
        assertEquals(color.to(XyzD65), state.xyzD65.value)
        assertEquals(color.to(XyzD50), state.xyzD50.value)
        assertEquals(color.to(Lab), state.lab.value)
        assertEquals(color.to(Lch), state.lch.value)
        assertEquals(color.to(Oklab), state.oklab.value)
        assertEquals(color, state.oklch.value)
        assertEquals(color.to(Hsl), state.hsl.value)
        assertEquals(color.to(Hwb), state.hwb.value)
        assertEquals(color.to(Hsv), state.hsv.value)
        assertEquals(color.to(Okhsl), state.okhsl.value)
        assertEquals(color.to(Okhsv), state.okhsv.value)
        assertEquals(color.to(Cmyk), state.cmyk.value)
    }

    @Test
    fun aGreysViewHasNoHueWhileSlidersShowTheRememberedOne() {
        val state = ColorPickerState(Hsl(200.0, 80.0, 50.0))
        state.value = Srgb(0.5, 0.5, 0.5)
        assertNull(state.hsl.h)
        assertEquals(200.0, state.displayValue(Hsl.H))
    }
}
