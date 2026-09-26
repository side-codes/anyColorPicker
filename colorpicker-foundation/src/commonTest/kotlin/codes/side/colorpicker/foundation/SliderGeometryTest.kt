package codes.side.colorpicker.foundation

import kotlin.test.Test
import kotlin.test.assertEquals

class SliderGeometryTest {

    // A 220 px slider with a 20 px thumb: the track is 200 px, 10 px in from each end.
    private fun at(x: Float, rtl: Boolean = false): Float =
        sliderFractionAt(x, width = 220, thumbWidth = 20, trackWidth = 200, rtl = rtl)

    @Test
    fun theThumbsCentreIsZeroAtTheStartAndOneAtTheEnd() {
        assertEquals(0f, at(10f))
        assertEquals(0.25f, at(60f))
        assertEquals(1f, at(210f))
    }

    @Test
    fun rightToLeftStartsAtTheRight() {
        assertEquals(0f, at(210f, rtl = true))
        assertEquals(0.25f, at(160f, rtl = true))
        assertEquals(1f, at(10f, rtl = true))
    }

    @Test
    fun aPointerPastAnEndReadsThatEnd() {
        assertEquals(0f, at(-40f))
        assertEquals(1f, at(400f))
    }

    @Test
    fun aTrackWithNoWidthReadsTheStart() {
        assertEquals(0f, sliderFractionAt(30f, width = 20, thumbWidth = 20, trackWidth = 0, rtl = false))
    }
}
