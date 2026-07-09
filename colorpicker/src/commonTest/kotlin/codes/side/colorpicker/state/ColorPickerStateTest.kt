package codes.side.colorpicker.state

import codes.side.colorpicker.model.CmykColor
import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.model.LabColor
import codes.side.colorpicker.model.RgbColor
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ColorPickerStateTest {

    private val eps = 1e-5f

    private fun assertNear(expected: Float, actual: Float, tolerance: Float = eps, msg: String = "") {
        assertTrue(
            abs(expected - actual) <= tolerance,
            "$msg expected=$expected actual=$actual diff=${abs(expected - actual)}"
        )
    }

    private fun createState(color: HslColor = HslColor()) = ColorPickerState(color)

    // ---- Initial state ----

    @Test
    fun initialColor() {
        val state = createState(HslColor(hue = 200f, saturation = 0.8f, lightness = 0.5f))
        assertEquals(200f, state.hslColor.hue)
        assertEquals(0.8f, state.hslColor.saturation)
        assertEquals(0.5f, state.hslColor.lightness)
    }

    @Test
    fun initialIsInteractingFalse() {
        val state = createState()
        assertFalse(state.isInteracting)
    }

    @Test
    fun initialAlphaIsOne() {
        val state = createState()
        assertEquals(1f, state.hslColor.alpha)
    }

    // ---- updateHue (Float) ----

    @Test
    fun updateHue() {
        val state = createState()
        state.updateHue(180f)
        assertEquals(180f, state.hslColor.hue)
    }

    @Test
    fun updateHueFractional() {
        val state = createState()
        state.updateHue(210.5f)
        assertEquals(210.5f, state.hslColor.hue)
    }

    @Test
    fun updateHueClampsHigh() {
        val state = createState()
        state.updateHue(999f)
        assertEquals(360f, state.hslColor.hue)
    }

    @Test
    fun updateHueClampsLow() {
        val state = createState()
        state.updateHue(-10f)
        assertEquals(0f, state.hslColor.hue)
    }

    // ---- updateSaturation (Float) ----

    @Test
    fun updateSaturation() {
        val state = createState()
        state.updateSaturation(0.75f)
        assertEquals(0.75f, state.hslColor.saturation)
    }

    @Test
    fun updateSaturationClampsHigh() {
        val state = createState()
        state.updateSaturation(2f)
        assertEquals(1f, state.hslColor.saturation)
    }

    @Test
    fun updateSaturationClampsLow() {
        val state = createState()
        state.updateSaturation(-0.5f)
        assertEquals(0f, state.hslColor.saturation)
    }

    // ---- updateLightness (Float) ----

    @Test
    fun updateLightness() {
        val state = createState()
        state.updateLightness(0.3f)
        assertEquals(0.3f, state.hslColor.lightness)
    }

    @Test
    fun updateLightnessClampsHigh() {
        val state = createState()
        state.updateLightness(1.5f)
        assertEquals(1f, state.hslColor.lightness)
    }

    @Test
    fun updateLightnessClampsLow() {
        val state = createState()
        state.updateLightness(-0.1f)
        assertEquals(0f, state.hslColor.lightness)
    }

    // ---- updateAlpha (Float) ----

    @Test
    fun updateAlpha() {
        val state = createState()
        state.updateAlpha(0.502f)
        assertEquals(0.502f, state.hslColor.alpha)
    }

    @Test
    fun updateAlphaClampsHigh() {
        val state = createState()
        state.updateAlpha(2f)
        assertEquals(1f, state.hslColor.alpha)
    }

    @Test
    fun updateAlphaClampsLow() {
        val state = createState()
        state.updateAlpha(-0.1f)
        assertEquals(0f, state.hslColor.alpha)
    }

    // ---- updateFromHsl ----

    @Test
    fun updateFromHsl() {
        val state = createState()
        val newColor = HslColor(hue = 120f, saturation = 0.6f, lightness = 0.4f, alpha = 0.784f)
        state.updateFromHsl(newColor)
        assertEquals(newColor, state.hslColor)
    }

    // ---- updateFromRgb ----

    @Test
    fun updateFromRgb() {
        val state = createState()
        state.updateFromRgb(RgbColor(1f, 0f, 0f))
        assertNear(0f, state.hslColor.hue, msg = "hue")
        assertNear(1f, state.hslColor.saturation, msg = "saturation")
        assertNear(0.5f, state.hslColor.lightness, msg = "lightness")
    }

    @Test
    fun updateFromRgbPreservesAlpha() {
        val state = createState()
        state.updateFromRgb(RgbColor(1f, 0f, 0f, alpha = 0.392f))
        assertEquals(0.392f, state.hslColor.alpha)
    }

    // ---- updateFromCmyk ----

    @Test
    fun updateFromCmyk() {
        val state = createState()
        state.updateFromCmyk(CmykColor(0f, 0f, 0f, 0f)) // white
        assertNear(1f, state.hslColor.lightness, msg = "lightness")
    }

    @Test
    fun updateFromCmykPreservesAlpha() {
        val state = createState()
        state.updateFromCmyk(CmykColor(0f, 0f, 0f, 0f, alpha = 0.196f))
        assertEquals(0.196f, state.hslColor.alpha)
    }

    // ---- updateFromLab ----

    @Test
    fun updateFromLab() {
        val state = createState()
        state.updateFromLab(LabColor(l = 0f, a = 0f, b = 0f)) // black
        assertNear(0f, state.hslColor.lightness, tolerance = 0.01f, msg = "lightness")
    }

    @Test
    fun updateFromLabPreservesAlpha() {
        val state = createState()
        state.updateFromLab(LabColor(l = 50f, a = 0f, b = 0f, alpha = 0.67f))
        assertEquals(0.67f, state.hslColor.alpha)
    }

    // ---- updateFromArgbInt ----

    @Test
    fun updateFromArgbInt() {
        val state = createState()
        state.updateFromArgbInt(0xFFFF0000.toInt()) // red
        assertNear(0f, state.hslColor.hue, msg = "hue")
        assertNear(1f, state.hslColor.saturation, msg = "saturation")
        assertNear(0.5f, state.hslColor.lightness, msg = "lightness")
        assertNear(1f, state.hslColor.alpha, msg = "alpha")
    }

    @Test
    fun updateFromArgbIntWithAlpha() {
        val state = createState()
        // Half-transparent green: alpha=128, R=0, G=255, B=0
        val argb = (128 shl 24) or (0 shl 16) or (255 shl 8) or 0
        state.updateFromArgbInt(argb)
        assertNear(120f, state.hslColor.hue, msg = "hue")
        assertNear(128f / 255f, state.hslColor.alpha, tolerance = 1f / 255f, msg = "alpha")
    }

    // ---- Derived state ----

    @Test
    fun derivedRgbColor() {
        val state = createState(HslColor(hue = 0f, saturation = 1f, lightness = 0.5f))
        val rgb = state.rgbColor
        assertNear(1f, rgb.red, msg = "red")
        assertNear(0f, rgb.green, msg = "green")
        assertNear(0f, rgb.blue, msg = "blue")
    }

    @Test
    fun derivedCmykColor() {
        val state = createState(HslColor(hue = 0f, saturation = 1f, lightness = 0.5f))
        val cmyk = state.cmykColor
        assertNear(0f, cmyk.cyan, msg = "cyan")
        assertNear(1f, cmyk.magenta, msg = "magenta")
        assertNear(1f, cmyk.yellow, msg = "yellow")
        assertNear(0f, cmyk.key, msg = "key")
    }

    @Test
    fun derivedLabColor() {
        val state = createState(HslColor(hue = 0f, saturation = 0f, lightness = 0f))
        val lab = state.labColor
        assertNear(0f, lab.l, tolerance = 0.5f, msg = "L")
    }

    @Test
    fun derivedArgbInt() {
        val state = createState(HslColor(hue = 0f, saturation = 1f, lightness = 0.5f))
        assertEquals(0xFFFF0000.toInt(), state.argbInt)
    }

    // ---- Update preserves other components ----

    @Test
    fun updateHuePreservesSaturationAndLightness() {
        val state = createState(HslColor(hue = 0f, saturation = 0.8f, lightness = 0.6f, alpha = 0.784f))
        state.updateHue(120f)
        assertEquals(0.8f, state.hslColor.saturation)
        assertEquals(0.6f, state.hslColor.lightness)
        assertEquals(0.784f, state.hslColor.alpha)
    }

    @Test
    fun updateAlphaPreservesColor() {
        val state = createState(HslColor(hue = 100f, saturation = 0.5f, lightness = 0.7f))
        state.updateAlpha(0.502f)
        assertEquals(100f, state.hslColor.hue)
        assertEquals(0.5f, state.hslColor.saturation)
        assertEquals(0.7f, state.hslColor.lightness)
    }

    @Test
    fun updateSaturationPreservesHueAndLightness() {
        val state = createState(HslColor(hue = 200f, saturation = 0.5f, lightness = 0.6f, alpha = 0.9f))
        state.updateSaturation(0.3f)
        assertEquals(200f, state.hslColor.hue)
        assertEquals(0.6f, state.hslColor.lightness)
        assertEquals(0.9f, state.hslColor.alpha)
    }

    @Test
    fun updateLightnessPreservesHueAndSaturation() {
        val state = createState(HslColor(hue = 200f, saturation = 0.5f, lightness = 0.6f))
        state.updateLightness(0.8f)
        assertEquals(200f, state.hslColor.hue)
        assertEquals(0.5f, state.hslColor.saturation)
    }
}
