package codes.side.colorpicker.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class PlaneMappingTest {

    @Test
    fun saturationRunsLeftToRight() {
        assertEquals(0f, saturationAt(0f, 200))
        assertEquals(0.5f, saturationAt(100f, 200))
        assertEquals(1f, saturationAt(200f, 200))
    }

    @Test
    fun lightnessRunsBottomToTop() {
        assertEquals(1f, lightnessAt(0f, 200), "the top edge is white")
        assertEquals(0.5f, lightnessAt(100f, 200))
        assertEquals(0f, lightnessAt(200f, 200), "the bottom edge is black")
    }

    @Test
    fun pointersOutsideTheSurfaceAreClamped() {
        // A drag that starts inside and leaves keeps reporting the nearest edge rather than
        // running the channels past their range.
        assertEquals(0f, saturationAt(-40f, 200))
        assertEquals(1f, saturationAt(260f, 200))
        assertEquals(1f, lightnessAt(-40f, 200))
        assertEquals(0f, lightnessAt(260f, 200))
    }

    @Test
    fun anUnmeasuredSurfaceDoesNotDivideByZero() {
        assertEquals(0f, saturationAt(10f, 0))
        assertEquals(0.5f, lightnessAt(10f, 0))
    }
}
