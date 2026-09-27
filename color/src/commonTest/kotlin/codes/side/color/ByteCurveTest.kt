package codes.side.color

import codes.side.color.internal.encodedByte
import codes.side.color.internal.srgbBytes
import kotlin.test.Test
import kotlin.test.assertEquals

class ByteCurveTest {

    private fun rounded(linear: Double): Int = encodedByte(TransferFunction.Srgb.encode(linear))

    @Test
    fun eachStepIsWhereTheRoundedCurveSteps() {
        for (k in 1..255) {
            val step = srgbBytes.threshold(k)
            assertEquals(k, rounded(step), "at step $k, $step")
            assertEquals(k - 1, rounded(Double.fromBits(step.toRawBits() - 1)), "just below step $k")
        }
    }

    @Test
    fun theTableGivesTheRoundedCurve() {
        // Four doubles either side of every step, a dense sweep, the ends, and past them: clamping in linear light
        // then looking up gives what encoding, clamping and rounding give.
        val values = ArrayList<Double>()
        for (k in 1..255) {
            val bits = srgbBytes.threshold(k).toRawBits()
            for (d in -4L..4L) values += Double.fromBits(bits + d)
        }
        for (i in 0..100_000) values += i / 100_000.0
        values += listOf(-1.0, -1e-300, -0.0, 0.0, Double.MIN_VALUE, 0.9999999999999999, 1.0, 1.0 + 1e-15, 1.0 + 1e-12, 1.0 + 1e-9, 2.0)
        for (x in values) assertEquals(rounded(x), srgbBytes.byteOf(x), "$x")
    }
}
