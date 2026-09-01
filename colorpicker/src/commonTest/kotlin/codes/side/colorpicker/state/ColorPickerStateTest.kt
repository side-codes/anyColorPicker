package codes.side.colorpicker.state

import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import codes.side.colorpicker.model.CmykColor
import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.model.LabColor
import codes.side.colorpicker.model.RgbColor
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

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
        // Clamped to 360, which HslColor normalizes to the equivalent 0.
        assertEquals(0f, state.hslColor.hue)
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

    // ---- pickerColor (authoritative space) ----

    @Test
    fun pickerColorReturnsInitialColorUnconverted() {
        val hsl = HslColor(hue = 123.456f, saturation = 0.789f, lightness = 0.321f, alpha = 0.5f)
        val state = createState(hsl)
        assertEquals(hsl, state.pickerColor)
    }

    @Test
    fun pickerColorTracksAuthoritativeSpace() {
        val state = createState()
        assertIs<HslColor>(state.pickerColor)
        state.updateRed(0.5f)
        assertIs<RgbColor>(state.pickerColor)
        state.updateCyan(0.5f)
        assertIs<CmykColor>(state.pickerColor)
        state.updateLabA(10f)
        assertIs<LabColor>(state.pickerColor)
        state.updateHue(180f)
        assertIs<HslColor>(state.pickerColor)
    }

    @Test
    fun updateAlphaPreservesOriginSpace() {
        val state = createState()
        state.updateFromRgb(RgbColor(red = 0.2f, green = 0.4f, blue = 0.6f))
        state.updateAlpha(0.5f)
        assertIs<RgbColor>(state.pickerColor)
        assertEquals(0.5f, state.pickerColor.alpha)
    }

    // ---- Zero-drift write-read round-trip per origin space ----

    @Test
    fun updateFromHslReadsBackTheExactInstance() {
        val state = createState()
        val hsl = HslColor(hue = 123.456f, saturation = 0.1f, lightness = 1f / 3f, alpha = 0.7f)
        state.updateFromHsl(hsl)
        assertSame(hsl, state.hslColor)
        assertSame(hsl, state.pickerColor)
    }

    @Test
    fun updateFromRgbReadsBackTheExactInstance() {
        val state = createState()
        val rgb = RgbColor(red = 0.1f, green = 1f / 3f, blue = 0.7f, alpha = 0.9f)
        state.updateFromRgb(rgb)
        assertSame(rgb, state.rgbColor)
        assertSame(rgb, state.pickerColor)
    }

    @Test
    fun updateFromCmykReadsBackTheExactInstance() {
        val state = createState()
        val cmyk = CmykColor(cyan = 0.1f, magenta = 1f / 3f, yellow = 0.7f, key = 0.9f, alpha = 0.3f)
        state.updateFromCmyk(cmyk)
        assertSame(cmyk, state.cmykColor)
        assertSame(cmyk, state.pickerColor)
    }

    @Test
    fun updateFromLabReadsBackTheExactInstance() {
        val state = createState()
        val lab = LabColor(l = 33.333f, a = -12.7f, b = 64.1f, alpha = 0.6f)
        state.updateFromLab(lab)
        assertSame(lab, state.labColor)
        assertSame(lab, state.pickerColor)
    }

    // ---- Snapshot safety (reads must not write) ----

    @Test
    fun readInsideReadOnlySnapshotDoesNotThrowAndIsFresh() {
        val state = createState()
        state.updateHue(120f)
        val snapshot = Snapshot.takeSnapshot()
        try {
            snapshot.enter {
                assertEquals(120f, state.hslColor.hue)
                assertNear(1f, state.rgbColor.green, msg = "green")
                assertNear(0f, state.rgbColor.red, msg = "red")
                state.cmykColor
                state.labColor
                state.argbInt
                assertIs<HslColor>(state.pickerColor)
            }
        } finally {
            snapshot.dispose()
        }
    }

    // ---- snapshotFlow observation (regression: reads must not write) ----

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun snapshotFlowOnDerivedRgbEmitsAfterUpdateRed() = runTest {
        // Regression test: observing a derived space via snapshotFlow used to throw
        // IllegalStateException when reads performed snapshot writes. Collecting an
        // emission after updateRed proves reads are pure.
        val state = createState()
        val emissions = mutableListOf<RgbColor>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            snapshotFlow { state.rgbColor }.take(2).toList(emissions)
        }
        state.updateRed(0.25f)
        Snapshot.sendApplyNotifications()
        job.join()
        assertEquals(2, emissions.size)
        assertEquals(1f, emissions[0].red)
        assertEquals(0.25f, emissions[1].red)
    }

    // ---- Idempotency (identical write does not invalidate) ----

    @Test
    fun updateWithIdenticalValueDoesNotInvalidateState() {
        val state = createState()
        state.updateRed(0.25f)
        Snapshot.sendApplyNotifications()
        val before = state.pickerColor
        var applyNotifications = 0
        val observer = Snapshot.registerApplyObserver { changed, _ ->
            if (changed.isNotEmpty()) applyNotifications++
        }
        try {
            state.updateRed(0.25f)
            state.updateRed(0.25f)
            Snapshot.sendApplyNotifications()
        } finally {
            observer.dispose()
        }
        assertEquals(0, applyNotifications, "identical writes must not produce apply notifications")
        // Structural-equality policy skips the write entirely, so the authoritative
        // instance is untouched.
        assertSame(before, state.pickerColor)
    }

    // ---- NaN handling ----

    @Test
    fun updateHueNaNLeavesStateUnchanged() {
        val state = createState(HslColor(hue = 200f, saturation = 0.8f, lightness = 0.5f))
        state.updateHue(Float.NaN)
        assertEquals(200f, state.hslColor.hue)
        assertEquals(0.8f, state.hslColor.saturation)
        assertEquals(0.5f, state.hslColor.lightness)
    }

    @Test
    fun updateAlphaNaNLeavesStateUnchanged() {
        val state = createState()
        state.updateAlpha(Float.NaN)
        assertEquals(1f, state.hslColor.alpha)
    }

    // ---- Per-channel RGB updates ----

    @Test
    fun updateRedClampsHigh() {
        val state = createState()
        state.updateRed(2f)
        assertEquals(1f, state.rgbColor.red)
    }

    @Test
    fun updateRedThenGreenPreservesRed() {
        val state = createState()
        state.updateRed(0.25f)
        state.updateGreen(0.5f)
        assertEquals(0.25f, state.rgbColor.red)
        assertEquals(0.5f, state.rgbColor.green)
    }

    @Test
    fun updateBlueClampsLow() {
        val state = createState()
        state.updateBlue(-1f)
        assertEquals(0f, state.rgbColor.blue)
    }

    // ---- Per-channel CMYK updates ----

    @Test
    fun updateCmykChannelsPreserveEachOther() {
        val state = createState()
        state.updateCyan(0.1f)
        state.updateMagenta(0.2f)
        state.updateYellow(0.3f)
        state.updateKey(0.4f)
        assertEquals(0.1f, state.cmykColor.cyan)
        assertEquals(0.2f, state.cmykColor.magenta)
        assertEquals(0.3f, state.cmykColor.yellow)
        assertEquals(0.4f, state.cmykColor.key)
    }

    // ---- Per-channel LAB updates ----

    @Test
    fun updateLabChannelsClampAndPreserve() {
        val state = createState()
        state.updateLabLightness(150f)
        state.updateLabA(-200f)
        state.updateLabB(50f)
        assertEquals(100f, state.labColor.l)
        assertEquals(-128f, state.labColor.a)
        assertEquals(50f, state.labColor.b)
    }

    // ---- isInteracting ----

    @Test
    fun isInteractingIsSettableFromInternalCode() {
        val state = createState()
        assertFalse(state.isInteracting)
        state.isInteracting = true
        assertTrue(state.isInteracting)
        state.isInteracting = false
        assertFalse(state.isInteracting)
    }

    // ---- Saver ----

    private fun saveToArray(state: ColorPickerState): FloatArray =
        with(ColorPickerStateSaver) {
            with(SaverScope { true }) {
                assertNotNull(save(state))
            }
        }

    @Test
    fun saverRoundTripHsl() {
        val original = ColorPickerState(HslColor(hue = 210f, saturation = 0.4f, lightness = 0.6f, alpha = 0.5f))
        val restored = assertNotNull(ColorPickerStateSaver.restore(saveToArray(original)))
        assertIs<HslColor>(restored.pickerColor)
        assertEquals(original.pickerColor, restored.pickerColor)
    }

    @Test
    fun saverRoundTripRgb() {
        val original = ColorPickerState(RgbColor(red = 0.1f, green = 0.2f, blue = 0.3f, alpha = 0.4f))
        val restored = assertNotNull(ColorPickerStateSaver.restore(saveToArray(original)))
        assertIs<RgbColor>(restored.pickerColor)
        assertEquals(original.pickerColor, restored.pickerColor)
    }

    @Test
    fun saverRoundTripCmyk() {
        val original = ColorPickerState(CmykColor(cyan = 0.1f, magenta = 0.2f, yellow = 0.3f, key = 0.4f, alpha = 0.5f))
        val restored = assertNotNull(ColorPickerStateSaver.restore(saveToArray(original)))
        assertIs<CmykColor>(restored.pickerColor)
        assertEquals(original.pickerColor, restored.pickerColor)
    }

    @Test
    fun saverRoundTripLab() {
        val original = ColorPickerState(LabColor(l = 42f, a = -30f, b = 60f, alpha = 0.7f))
        val restored = assertNotNull(ColorPickerStateSaver.restore(saveToArray(original)))
        assertIs<LabColor>(restored.pickerColor)
        assertEquals(original.pickerColor, restored.pickerColor)
    }

    private fun assertBitIdentical(expected: Float, actual: Float, msg: String) {
        assertEquals(expected.toRawBits(), actual.toRawBits(), "$msg expected=$expected actual=$actual")
    }

    @Test
    fun saverRoundTripPreservesExactBitsHsl() {
        val hsl = HslColor(hue = 123.456f, saturation = 0.1f, lightness = 1f / 3f, alpha = 0.7f)
        val restored = assertNotNull(ColorPickerStateSaver.restore(saveToArray(ColorPickerState(hsl))))
        val color = assertIs<HslColor>(restored.pickerColor)
        assertBitIdentical(hsl.hue, color.hue, "hue")
        assertBitIdentical(hsl.saturation, color.saturation, "saturation")
        assertBitIdentical(hsl.lightness, color.lightness, "lightness")
        assertBitIdentical(hsl.alpha, color.alpha, "alpha")
    }

    @Test
    fun saverRoundTripPreservesExactBitsRgb() {
        val rgb = RgbColor(red = 0.1f, green = 1f / 3f, blue = 0.7f, alpha = 0.9f)
        val restored = assertNotNull(ColorPickerStateSaver.restore(saveToArray(ColorPickerState(rgb))))
        val color = assertIs<RgbColor>(restored.pickerColor)
        assertBitIdentical(rgb.red, color.red, "red")
        assertBitIdentical(rgb.green, color.green, "green")
        assertBitIdentical(rgb.blue, color.blue, "blue")
        assertBitIdentical(rgb.alpha, color.alpha, "alpha")
    }

    @Test
    fun saverRoundTripPreservesExactBitsCmyk() {
        val cmyk = CmykColor(cyan = 0.1f, magenta = 1f / 3f, yellow = 0.7f, key = 0.9f, alpha = 0.3f)
        val restored = assertNotNull(ColorPickerStateSaver.restore(saveToArray(ColorPickerState(cmyk))))
        val color = assertIs<CmykColor>(restored.pickerColor)
        assertBitIdentical(cmyk.cyan, color.cyan, "cyan")
        assertBitIdentical(cmyk.magenta, color.magenta, "magenta")
        assertBitIdentical(cmyk.yellow, color.yellow, "yellow")
        assertBitIdentical(cmyk.key, color.key, "key")
        assertBitIdentical(cmyk.alpha, color.alpha, "alpha")
    }

    @Test
    fun saverRoundTripPreservesExactBitsLab() {
        val lab = LabColor(l = 33.333f, a = -12.7f, b = 64.1f, alpha = 0.6f)
        val restored = assertNotNull(ColorPickerStateSaver.restore(saveToArray(ColorPickerState(lab))))
        val color = assertIs<LabColor>(restored.pickerColor)
        assertBitIdentical(lab.l, color.l, "l")
        assertBitIdentical(lab.a, color.a, "a")
        assertBitIdentical(lab.b, color.b, "b")
        assertBitIdentical(lab.alpha, color.alpha, "alpha")
    }

    @Test
    fun saverRestoreUnknownSpaceKeyReturnsNull() {
        assertNull(ColorPickerStateSaver.restore(floatArrayOf(99f, 0f, 0f, 0f, 1f, 0f)))
    }

    @Test
    fun saverRestoreOutOfRangeChannelReturnsNull() {
        // Hue 999 is outside 0..360 — restore must return null, not throw.
        assertNull(ColorPickerStateSaver.restore(floatArrayOf(0f, 999f, 0.5f, 0.5f, 1f, 0f)))
    }

    @Test
    fun saverRestoreWrongSizeReturnsNull() {
        assertNull(ColorPickerStateSaver.restore(floatArrayOf(0f, 120f)))
    }
}
