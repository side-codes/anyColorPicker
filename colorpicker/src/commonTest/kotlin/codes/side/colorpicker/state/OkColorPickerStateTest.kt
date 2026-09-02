package codes.side.colorpicker.state

import androidx.compose.runtime.saveable.SaverScope
import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.model.LabColor
import codes.side.colorpicker.model.OkhslColor
import codes.side.colorpicker.model.OkhsvColor
import codes.side.colorpicker.model.OklabColor
import codes.side.colorpicker.model.OklchColor
import codes.side.colorpicker.model.RgbColor
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

// The Saver contract asks whether a value can be persisted; nothing here is rejected.
private val AlwaysSaveable = SaverScope { true }

class OkColorPickerStateTest {

    private fun assertNear(expected: Float, actual: Float, tolerance: Float, msg: String = "") {
        assertTrue(
            abs(expected - actual) <= tolerance,
            "$msg expected=$expected actual=$actual diff=${abs(expected - actual)}",
        )
    }

    // ---- Origin space ----

    @Test
    fun pickerColorTracksEveryNewSpace() {
        val state = ColorPickerState()
        assertIs<HslColor>(state.pickerColor)
        state.updateOklabLightness(0.6f)
        assertIs<OklabColor>(state.pickerColor)
        state.updateOklchChroma(0.1f)
        assertIs<OklchColor>(state.pickerColor)
        state.updateOkhslSaturation(0.7f)
        assertIs<OkhslColor>(state.pickerColor)
        state.updateOkhsvValue(0.8f)
        assertIs<OkhsvColor>(state.pickerColor)
        state.updateRed(0.1f)
        assertIs<RgbColor>(state.pickerColor)
    }

    @Test
    fun updateAlphaPreservesEveryNewOriginSpace() {
        for (color in listOf(
            OklabColor(0.5f, 0.1f, 0.1f),
            OklchColor(0.5f, 0.1f, 120f),
            OkhslColor(120f, 0.5f, 0.5f),
            OkhsvColor(120f, 0.5f, 0.5f),
        )) {
            val state = ColorPickerState(color)
            state.updateAlpha(0.25f)
            assertEquals(color::class, state.pickerColor::class, "origin space for $color")
            assertEquals(0.25f, state.pickerColor.alpha, "alpha for $color")
        }
    }

    // ---- Zero drift within each new space ----

    @Test
    fun updateFromReadsBackTheExactInstance() {
        val oklab = OklabColor(0.42f, -0.13f, 0.07f, alpha = 0.6f)
        ColorPickerState().let {
            it.updateFromOklab(oklab)
            assertSame(oklab, it.pickerColor)
            assertSame(oklab, it.oklabColor)
        }

        val oklch = OklchColor(0.42f, 0.13f, 217f, alpha = 0.6f)
        ColorPickerState().let {
            it.updateFromOklch(oklch)
            assertSame(oklch, it.pickerColor)
            assertSame(oklch, it.oklchColor)
        }

        val okhsl = OkhslColor(217f, 0.63f, 0.42f, alpha = 0.6f)
        ColorPickerState().let {
            it.updateFromOkhsl(okhsl)
            assertSame(okhsl, it.pickerColor)
            assertSame(okhsl, it.okhslColor)
        }

        val okhsv = OkhsvColor(217f, 0.63f, 0.42f, alpha = 0.6f)
        ColorPickerState().let {
            it.updateFromOkhsv(okhsv)
            assertSame(okhsv, it.pickerColor)
            assertSame(okhsv, it.okhsvColor)
        }
    }

    @Test
    fun editingOneOkhslChannelLeavesTheOthersUntouched() {
        val state = ColorPickerState(OkhslColor(hue = 200f, saturation = 0.8f, lightness = 0.4f))
        state.updateOkhslHue(275f)
        assertEquals(275f, state.okhslColor.hue)
        assertEquals(0.8f, state.okhslColor.saturation)
        assertEquals(0.4f, state.okhslColor.lightness)
    }

    @Test
    fun editingOneOklchChannelLeavesTheOthersUntouched() {
        val state = ColorPickerState(OklchColor(l = 0.4f, chroma = 0.12f, hue = 200f))
        state.updateOklchHue(275f)
        assertEquals(275f, state.oklchColor.hue)
        assertEquals(0.4f, state.oklchColor.l)
        assertEquals(0.12f, state.oklchColor.chroma)
    }

    // ---- Clamping ----

    @Test
    fun channelsClampRatherThanThrow() {
        val state = ColorPickerState()
        state.updateOklabA(5f)
        assertEquals(0.4f, state.oklabColor.a)
        state.updateOklabB(-5f)
        assertEquals(-0.4f, state.oklabColor.b)
        state.updateOklchChroma(5f)
        assertEquals(0.4f, state.oklchColor.chroma)
        state.updateOkhslSaturation(5f)
        assertEquals(1f, state.okhslColor.saturation)
        state.updateOkhsvValue(-5f)
        assertEquals(0f, state.okhsvColor.value)
    }

    @Test
    fun nanIsIgnoredOnEveryNewChannel() {
        val state = ColorPickerState(OkhslColor(hue = 200f, saturation = 0.8f, lightness = 0.4f))
        val before = state.pickerColor
        state.updateOkhslHue(Float.NaN)
        state.updateOkhslSaturation(Float.NaN)
        state.updateOkhslLightness(Float.NaN)
        state.updateOkhsvHue(Float.NaN)
        state.updateOkhsvSaturation(Float.NaN)
        state.updateOkhsvValue(Float.NaN)
        state.updateOklabLightness(Float.NaN)
        state.updateOklabA(Float.NaN)
        state.updateOklabB(Float.NaN)
        state.updateOklchLightness(Float.NaN)
        state.updateOklchChroma(Float.NaN)
        state.updateOklchHue(Float.NaN)
        assertSame(before, state.pickerColor)
    }

    // ---- Derived views agree with each other ----

    @Test
    fun everySpaceDescribesTheSameColor() {
        val state = ColorPickerState(RgbColor(0.2f, 0.6f, 0.85f))
        val argb = state.argbInt

        for (color in listOf(
            state.oklabColor,
            state.oklchColor,
            state.okhslColor,
            state.okhsvColor,
        )) {
            val roundTripped = ColorPickerState(color).argbInt
            assertEquals(argb, roundTripped, "argb via $color")
        }
    }

    @Test
    fun switchingOriginSpaceKeepsTheColorStable() {
        val state = ColorPickerState(OkhslColor(hue = 217f, saturation = 0.7f, lightness = 0.45f))
        val before = state.rgbColor

        // Touch each space in turn without changing the color it represents.
        state.updateFromOklab(state.oklabColor)
        state.updateFromOklch(state.oklchColor)
        state.updateFromOkhsv(state.okhsvColor)
        state.updateFromLab(state.labColor)

        val after = state.rgbColor
        assertNear(before.red, after.red, 2e-2f, "red")
        assertNear(before.green, after.green, 2e-2f, "green")
        assertNear(before.blue, after.blue, 2e-2f, "blue")
    }

    // ---- Saved state ----

    @Test
    fun everyNewSpaceSurvivesSaveAndRestore() {
        for (color in listOf(
            OklabColor(0.42f, -0.13f, 0.07f, alpha = 0.6f),
            OklchColor(0.42f, 0.13f, 217f, alpha = 0.6f),
            OkhslColor(217f, 0.63f, 0.42f, alpha = 0.6f),
            OkhsvColor(217f, 0.63f, 0.42f, alpha = 0.6f),
        )) {
            val saved = with(ColorPickerStateSaver) {
                AlwaysSaveable.save(ColorPickerState(color))
            }
            val restored = ColorPickerStateSaver.restore(saved!!)
            assertEquals(color, restored?.pickerColor, "restored $color")
        }
    }

    @Test
    fun theSpaceKeysOfTheExistingSpacesAreUnchanged() {
        // The saved format is persisted across process death, so these keys are a
        // compatibility surface: a state saved by 1.x has to restore under 2.x.
        val cases = listOf(
            0f to HslColor(200f, 0.5f, 0.5f),
            1f to RgbColor(0.1f, 0.2f, 0.3f),
            3f to LabColor(50f, 10f, -10f),
        )
        for ((key, color) in cases) {
            val saved = with(ColorPickerStateSaver) {
                AlwaysSaveable.save(ColorPickerState(color))
            }
            assertEquals(key, saved!![0], "space key for $color")
        }
    }
}
