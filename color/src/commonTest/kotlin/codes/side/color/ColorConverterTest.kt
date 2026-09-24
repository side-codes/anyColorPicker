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
    fun inPlaceConversionIntoMoreChannelsIsAllowedWhereNothingUnreadIsOverwritten() {
        val toCmyk = Srgb.converterTo(Cmyk)
        val one = doubleArrayOf(0.1, 0.5, 0.9, 0.0)
        toCmyk.convert(one, 0, one, 0, 1)
        assertComponents(one, Srgb(0.1, 0.5, 0.9).to(Cmyk), 1e-12)
        // Two colors: the destination has to start a channel earlier for each color it grows by.
        val two = doubleArrayOf(0.0, 0.1, 0.5, 0.9, 0.2, 0.4, 0.6, 0.0)
        toCmyk.convert(two, 1, two, 0, 2)
        assertComponents(two.copyOfRange(4, 8), Srgb(0.2, 0.4, 0.6).to(Cmyk), 1e-12)
        // At the same offset the first color's fourth channel lands on the second color's first.
        val same = DoubleArray(8)
        assertFailsWith<IllegalArgumentException> { toCmyk.convert(same, 0, same, 0, 2) }
    }

    @Test
    fun inPlaceConversionIntoFewerChannelsMayWriteAhead() {
        val toSrgb = Cmyk.converterTo(Srgb)
        val pixels = doubleArrayOf(0.8, 0.4, 0.0, 0.1, 0.0, 0.5, 0.5, 0.2, 0.0)
        val second = Cmyk(0.0, 0.5, 0.5, 0.2).to(Srgb).components()
        toSrgb.convert(pixels, 0, pixels, 1, 2)
        assertComponents(second, Srgb(pixels[4], pixels[5], pixels[6]), 1e-12)
    }

    @Test
    fun anAbsurdCountIsRefusedBeforeAnythingIsWritten() {
        val converter = XyzD65.converterTo(XyzD50)
        val dst = DoubleArray(6)
        assertFailsWith<IllegalArgumentException> { converter.convert(DoubleArray(6), 0, dst, 0, Int.MAX_VALUE / 2) }
        assertTrue(dst.all { it == 0.0 })
    }

    @Test
    fun theRoundTripThroughD50IsExactToRounding() {
        val back = XyzD65(0.3, 0.4, 0.5).to(XyzD50).to(XyzD65)
        assertComponents(doubleArrayOf(0.3, 0.4, 0.5), back, 1e-15)
        assertTrue(XyzD65.converterTo(XyzD65).toString().contains("0 steps"))
    }
}
