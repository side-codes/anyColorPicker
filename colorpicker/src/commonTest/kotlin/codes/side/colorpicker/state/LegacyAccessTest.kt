package codes.side.colorpicker.state

import codes.side.color.DisplayP3
import codes.side.color.Lab
import codes.side.color.OkLch
import codes.side.colorpicker.model.CmykColor
import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.model.LabColor
import codes.side.colorpicker.model.OkhslColor
import codes.side.colorpicker.model.OkhsvColor
import codes.side.colorpicker.model.OklabColor
import codes.side.colorpicker.model.OklchColor
import codes.side.colorpicker.model.RgbColor
import kotlin.test.Test
import kotlin.test.assertEquals

// The model package's getters and mutators, which the per-channel sliders, planes and pickers use.
class LegacyAccessTest {

    @Test
    fun eachModelReadsBackWhatWasWritten() {
        val state = ColorPickerState()
        val hsl = HslColor(hue = 210f, saturation = 0.4f, lightness = 0.6f, alpha = 0.5f)
        state.updateFromHsl(hsl)
        assertEquals(hsl, state.hslColor)
        assertEquals(hsl, state.pickerColor)
        val rgb = RgbColor(red = 0.1f, green = 0.2f, blue = 0.3f, alpha = 0.4f)
        state.updateFromRgb(rgb)
        assertEquals(rgb, state.rgbColor)
        val cmyk = CmykColor(cyan = 0.1f, magenta = 0.2f, yellow = 0.3f, key = 0.4f, alpha = 0.5f)
        state.updateFromCmyk(cmyk)
        assertEquals(cmyk, state.cmykColor)
        val lab = LabColor(l = 40f, a = -20f, b = 30f)
        state.updateFromLab(lab)
        assertEquals(lab, state.labColor)
        val oklab = OklabColor(l = 0.5f, a = 0.1f, b = -0.1f)
        state.updateFromOklab(oklab)
        assertEquals(oklab, state.oklabColor)
        val oklch = OklchColor(l = 0.5f, chroma = 0.1f, hue = 140f)
        state.updateFromOklch(oklch)
        assertEquals(oklch, state.oklchColor)
        val okhsl = OkhslColor(hue = 140f, saturation = 0.9f, lightness = 0.5f)
        state.updateFromOkhsl(okhsl)
        assertEquals(okhsl, state.okhslColor)
        val okhsv = OkhsvColor(hue = 140f, saturation = 0.9f, value = 0.5f)
        state.updateFromOkhsv(okhsv)
        assertEquals(okhsv, state.okhsvColor)
        assertEquals(okhsv, state.pickerColor)
    }

    @Test
    fun aGreyWrittenInRgbShowsTheRememberedHue() {
        val state = ColorPickerState(HslColor(hue = 200f, saturation = 0.8f, lightness = 0.5f))
        state.updateFromRgb(RgbColor(0.5f, 0.5f, 0.5f))
        assertEquals(200f, state.hslColor.hue)
        assertEquals(0f, state.hslColor.saturation)
    }

    @Test
    fun channelWritesClampAndIgnoreNaN() {
        val state = ColorPickerState(RgbColor(0.5f, 0.5f, 0.5f))
        state.updateRed(2f)
        assertEquals(1f, state.rgbColor.red)
        state.updateRed(Float.NaN)
        assertEquals(1f, state.rgbColor.red)
        state.updateAlpha(-1f)
        assertEquals(0f, state.rgbColor.alpha)
    }

    @Test
    fun aColorOutsideTheOldRangesReadsWithoutThrowing() {
        for (value in listOf(DisplayP3(1.0, 0.0, 0.0), OkLch(0.7, 0.5, 150.0), Lab(50.0, 150.0, 0.0))) {
            val state = ColorPickerState(value)
            state.hslColor
            state.rgbColor
            state.cmykColor
            state.labColor
            state.oklabColor
            state.oklchColor
            state.okhslColor
            state.okhsvColor
            state.pickerColor
        }
        assertEquals(0.4f, ColorPickerState(OkLch(0.7, 0.5, 150.0)).oklchColor.chroma)
    }

    @Test
    fun argbIntIsTheSrgbBytes() {
        assertEquals(0xFFFF0000.toInt(), ColorPickerState(RgbColor(1f, 0f, 0f)).argbInt)
    }
}
