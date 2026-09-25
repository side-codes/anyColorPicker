package codes.side.colorpicker.state

import codes.side.color.Cmyk
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
import codes.side.color.asCmyk
import codes.side.color.asDisplayP3
import codes.side.color.asHsl
import codes.side.color.asHsv
import codes.side.color.asHwb
import codes.side.color.asLab
import codes.side.color.asLch
import codes.side.color.asOkLch
import codes.side.color.asOkhsl
import codes.side.color.asOkhsv
import codes.side.color.asOklab
import codes.side.color.asSrgb
import codes.side.color.asSrgbLinear
import codes.side.color.asXyzD50
import codes.side.color.asXyzD65
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ColorPickerStateViewsTest {

    @Test
    fun eachViewIsTheValueConverted() {
        val color = OkLch(0.6, 0.1, 200.0)
        val state = ColorPickerState(color)
        assertEquals(color.to(Srgb), state.srgb.value)
        assertEquals(color.to(SrgbLinear), state.srgbLinear.value)
        assertEquals(color.to(DisplayP3), state.displayP3.value)
        assertEquals(color.to(XyzD65), state.xyzD65.value)
        assertEquals(color.to(XyzD50), state.xyzD50.value)
        assertEquals(color.to(Lab), state.lab.value)
        assertEquals(color.to(Lch), state.lch.value)
        assertEquals(color.to(Oklab), state.oklab.value)
        assertEquals(color, state.okLch.value)
        assertEquals(color.to(Hsl), state.hsl.value)
        assertEquals(color.to(Hwb), state.hwb.value)
        assertEquals(color.to(Hsv), state.hsv.value)
        assertEquals(color.to(Okhsl), state.okhsl.value)
        assertEquals(color.to(Okhsv), state.okhsv.value)
        assertEquals(color.to(Cmyk), state.cmyk.value)
    }

    @Test
    fun settingAViewReplacesTheValue() {
        val state = ColorPickerState(Srgb(1.0, 0.0, 0.0))

        val srgb = Srgb(0.1, 0.2, 0.3)
        state.set(srgb.asSrgb())
        assertEquals(srgb, state.value)

        val srgbLinear = SrgbLinear(0.1, 0.2, 0.3)
        state.set(srgbLinear.asSrgbLinear())
        assertEquals(srgbLinear, state.value)

        val displayP3 = DisplayP3(0.1, 0.2, 0.3)
        state.set(displayP3.asDisplayP3())
        assertEquals(displayP3, state.value)

        val xyzD65 = XyzD65(0.1, 0.2, 0.3)
        state.set(xyzD65.asXyzD65())
        assertEquals(xyzD65, state.value)

        val xyzD50 = XyzD50(0.1, 0.2, 0.3)
        state.set(xyzD50.asXyzD50())
        assertEquals(xyzD50, state.value)

        val lab = Lab(50.0, 10.0, 20.0)
        state.set(lab.asLab())
        assertEquals(lab, state.value)

        val lch = Lch(50.0, 10.0, 20.0)
        state.set(lch.asLch())
        assertEquals(lch, state.value)

        val oklab = Oklab(0.5, 0.1, 0.1)
        state.set(oklab.asOklab())
        assertEquals(oklab, state.value)

        val okLch = OkLch(0.5, 0.1, 20.0)
        state.set(okLch.asOkLch())
        assertEquals(okLch, state.value)

        val hsl = Hsl(20.0, 50.0, 50.0)
        state.set(hsl.asHsl())
        assertEquals(hsl, state.value)

        val hwb = Hwb(20.0, 10.0, 10.0)
        state.set(hwb.asHwb())
        assertEquals(hwb, state.value)

        val hsv = Hsv(20.0, 50.0, 50.0)
        state.set(hsv.asHsv())
        assertEquals(hsv, state.value)

        val okhsl = Okhsl(20.0, 0.5, 0.5)
        state.set(okhsl.asOkhsl())
        assertEquals(okhsl, state.value)

        val okhsv = Okhsv(20.0, 0.5, 0.5)
        state.set(okhsv.asOkhsv())
        assertEquals(okhsv, state.value)

        val cmyk = Cmyk(0.1, 0.2, 0.3, 0.4)
        state.set(cmyk.asCmyk())
        assertEquals(cmyk, state.value)
    }

    @Test
    fun aGreysViewHasNoHueWhileSlidersShowTheRememberedOne() {
        val state = ColorPickerState(Hsl(200.0, 80.0, 50.0))
        state.value = Srgb(0.5, 0.5, 0.5)
        assertNull(state.hsl.h)
        assertEquals(200.0, state.displayValue(Hsl.H))
    }
}
