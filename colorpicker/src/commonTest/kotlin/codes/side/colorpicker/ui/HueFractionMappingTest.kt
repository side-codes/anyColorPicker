package codes.side.colorpicker.ui

import codes.side.colorpicker.state.ColorPickerState
import kotlin.math.nextDown
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression tests for the HueSlider value mapping: `HslColor` normalizes hue 360 to
 * the equivalent 0, so the slider must never write exactly 360 — otherwise dragging
 * the thumb to the right end of the track would snap it back to the far left.
 */
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
        assertEquals(360f.nextDown(), hue)
    }

    @Test
    fun fullFractionIsNotNormalizedToZeroByState() {
        // Dragging the M3 Slider to the right edge emits exactly 1.0f; the written
        // hue must read back unchanged instead of wrapping to 0.
        val state = ColorPickerState()
        state.updateHue(hueFromFraction(1f))
        assertEquals(360f.nextDown(), state.hslColor.hue)
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
        assertEquals(360f.nextDown(), state.hslColor.hue)
    }
}
