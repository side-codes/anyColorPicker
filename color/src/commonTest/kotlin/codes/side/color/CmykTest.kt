package codes.side.color

import kotlin.test.Test
import kotlin.test.assertEquals

class CmykTest {

    @Test
    fun cmykBothWays() {
        assertComponents(doubleArrayOf(0.8888888888888888, 0.4444444444444444, 0.0, 0.1), Srgb(0.1, 0.5, 0.9).to(Cmyk), 1e-12)
        assertComponents(doubleArrayOf(0.1, 0.5, 0.9), Cmyk(0.8888888888888888, 0.4444444444444444, 0.0, 0.1).to(Srgb), 1e-12)
    }

    @Test
    fun blackIsAllKey() {
        assertComponents(doubleArrayOf(0.0, 0.0, 0.0, 1.0), Srgb(0.0, 0.0, 0.0).to(Cmyk), 0.0)
    }

    @Test
    fun cmykIsNotColorimetric() {
        assertEquals(Exactness.NonColorimetric, Cmyk.exactness)
    }
}
