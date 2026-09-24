package codes.side.color

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ConverterCacheTest {

    @Test
    fun aTargetIsCachedOnce() {
        val cache = ConverterCache(Srgb)
        assertSame(cache.converterTo(OkLch), cache.converterTo(OkLch))
        assertEquals(1, cache.size)
    }

    @Test
    fun anEqualTargetBuiltAgainReplacesItsEntry() {
        val cache = ConverterCache(Srgb)
        repeat(100) { cache.converterTo(ColorSpace.hsl("--same-id", Srgb)) }
        assertEquals(1, cache.size)
        cache.converterTo(ColorSpace.hsl("--other-id", Srgb))
        assertEquals(2, cache.size)
    }
}
