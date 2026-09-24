package codes.side.color

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ColorConverterTest {

    @Test
    fun xyzD65ToD50MatchesColorJs() {
        // color.js 0.7.1: color(xyz-d65 0.3 0.4 0.5).to("xyz-d50")
        assertComponents(
            doubleArrayOf(0.2984615528595403, 0.39652521380085937, 0.37918630511632645),
            XyzD65(0.3, 0.4, 0.5).to(XyzD50),
            1e-12,
        )
    }

    @Test
    fun convertersAreCachedPerTarget() {
        assertSame(XyzD65.converterTo(XyzD50), XyzD65.converterTo(XyzD50))
    }

    @Test
    fun bulkConversionMatchesOneAtATime() {
        val converter = XyzD65.converterTo(XyzD50)
        val source = doubleArrayOf(0.3, 0.4, 0.5, 0.1, 0.2, 0.3)
        val bulk = DoubleArray(6)
        converter.convert(source, 0, bulk, 0, 2)
        val single = DoubleArray(4)
        converter.convert(doubleArrayOf(0.1, 0.2, 0.3, 0.0), single)
        assertEquals(single[0], bulk[3])
        assertEquals(single[2], bulk[5])

        val floats = FloatArray(6)
        converter.convert(FloatArray(6) { source[it].toFloat() }, 0, floats, 0, 2)
        assertNear(bulk[4], floats[4].toDouble(), 1e-6)
    }

    @Test
    fun bulkConversionChecksItsBounds() {
        val converter = XyzD65.converterTo(XyzD50)
        assertFailsWith<IllegalArgumentException> { converter.convert(DoubleArray(5), 0, DoubleArray(6), 0, 2) }
        assertFailsWith<IllegalArgumentException> { converter.convert(DoubleArray(6), 0, DoubleArray(5), 0, 2) }
    }

    @Test
    fun convertingInPlaceIsRefusedWhereItWouldOverwriteUnreadColors() {
        val converter = XyzD65.converterTo(XyzD50)
        val pixels = doubleArrayOf(0.3, 0.4, 0.5, 0.1, 0.2, 0.3, 0.0, 0.0, 0.0)
        val expected = XyzD65(0.1, 0.2, 0.3).to(XyzD50).components()
        converter.convert(pixels, 0, pixels, 0, 2)
        assertEquals(expected[0], pixels[3])
        converter.convert(pixels, 0, pixels, 6, 1)
        assertFailsWith<IllegalArgumentException> { converter.convert(pixels, 0, pixels, 3, 2) }
    }

    @Test
    fun theRoundTripThroughD50IsExactToRounding() {
        val back = XyzD65(0.3, 0.4, 0.5).to(XyzD50).to(XyzD65)
        assertComponents(doubleArrayOf(0.3, 0.4, 0.5), back, 1e-15)
        assertTrue(XyzD65.converterTo(XyzD65).toString().contains("0 steps"))
    }
}
