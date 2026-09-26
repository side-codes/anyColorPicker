package codes.side.colorpicker.foundation

import kotlin.test.Test
import kotlin.test.assertEquals

class PlaneMappingTest {

    @Test
    fun theXAxisRunsLeftToRight() {
        assertEquals(0f, planeXFraction(0f, 200))
        assertEquals(0.5f, planeXFraction(100f, 200))
        assertEquals(1f, planeXFraction(200f, 200))
    }

    @Test
    fun theYAxisRunsBottomToTop() {
        assertEquals(1f, planeYFraction(0f, 200), "the top edge reads 1")
        assertEquals(0.5f, planeYFraction(100f, 200))
        assertEquals(0f, planeYFraction(200f, 200), "the bottom edge reads 0")
    }

    @Test
    fun pointersOutsideTheSurfaceAreClamped() {
        // A drag that starts inside and leaves keeps reporting the nearest edge rather than
        // running the channels past their range.
        assertEquals(0f, planeXFraction(-40f, 200))
        assertEquals(1f, planeXFraction(260f, 200))
        assertEquals(1f, planeYFraction(-40f, 200))
        assertEquals(0f, planeYFraction(260f, 200))
    }

    @Test
    fun anUnmeasuredSurfaceDoesNotDivideByZero() {
        assertEquals(0f, planeXFraction(10f, 0))
        assertEquals(0.5f, planeYFraction(10f, 0))
    }
}
