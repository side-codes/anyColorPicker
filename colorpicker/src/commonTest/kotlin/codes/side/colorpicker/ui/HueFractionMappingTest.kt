package codes.side.colorpicker.ui

import codes.side.colorpicker.state.ColorPickerState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression tests for the HueSlider value mapping: `HslColor` normalizes hue 360 to
 * the equivalent 0, so the slider must never write exactly 360 — otherwise dragging
 * the thumb to the right end of the track would snap it back to the far left.
 */
// kotlin.math.nextDown is unavailable on wasmJs, and this test runs in commonTest, so it
// compiles for every target. Computed here from the bit pattern rather than reusing the
// implementation's own constant, which would make these assertions circular.
private val JUST_BELOW_360 = Float.fromBits(360f.toRawBits() - 1)

class HueFractionMappingTest {

    @Test
    fun zeroFractionMapsToZeroHue() {
        assertEquals(0f, hueFromFraction(0f))
    }

    @Test
    fun midFractionMapsProportionally() {
        assertEquals(180f, hueFromFraction(0.5f))
    }

    @Test
    fun fullFractionMapsJustBelow360() {
        val hue = hueFromFraction(1f)
        assertTrue(hue < 360f, "hue at the track end must stay below 360, was $hue")
        assertEquals(JUST_BELOW_360, hue)
    }

    @Test
    fun fullFractionIsNotNormalizedToZeroByState() {
        // Dragging the M3 Slider to the right edge emits exactly 1.0f; the written
        // hue must read back unchanged instead of wrapping to 0.
        val state = ColorPickerState()
        state.updateHue(hueFromFraction(1f))
        assertEquals(JUST_BELOW_360, state.hslColor.hue)
    }

    @Test
    fun thumbStaysAtRightEndAfterFullFraction() {
        val state = ColorPickerState()
        state.updateHue(hueFromFraction(1f))
        val thumbFraction = state.hslColor.hue / 360f
        assertTrue(
            thumbFraction > 0.9999f && thumbFraction <= 1f,
            "thumb must stay at the right end, was $thumbFraction",
        )
    }

    @Test
    fun rightEndIsAStablePosition() {
        // Re-deriving the fraction from the stored hue and writing it back must be a
        // fixed point, so continued dragging at the right edge does not jitter.
        val state = ColorPickerState()
        state.updateHue(hueFromFraction(1f))
        val redisplayedFraction = state.hslColor.hue / 360f
        state.updateHue(hueFromFraction(redisplayedFraction))
        assertEquals(JUST_BELOW_360, state.hslColor.hue)
    }
}
