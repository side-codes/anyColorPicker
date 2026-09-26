package codes.side.colorpicker.foundation

import kotlin.test.Test
import kotlin.test.assertEquals

class RoundingTest {

    @Test
    fun tiesRoundAwayFromZero() {
        assertEquals(3L, roundHalfAwayFromZero(2.5))
        assertEquals(-3L, roundHalfAwayFromZero(-2.5))
        assertEquals(128L, roundHalfAwayFromZero(127.5))
        assertEquals(0L, roundHalfAwayFromZero(-0.4))
    }

    @Test
    fun placesRoundAwayFromZero() {
        assertEquals(0.063, roundHalfAwayFromZero(0.0625, 3))
        assertEquals(-1.24, roundHalfAwayFromZero(-1.2351, 2))
        assertEquals(12.05, roundHalfAwayFromZero(12.05, 3))
        assertEquals(3.0, roundHalfAwayFromZero(2.5, 0))
    }

    @Test
    fun aValueThatRoundsToZeroIsPositiveZero() {
        val zero = roundHalfAwayFromZero(-0.0004, 3)
        assertEquals(0.0, zero)
        assertEquals(Double.POSITIVE_INFINITY, 1.0 / zero, "negative zero would make a formatter write a minus")
    }
}
