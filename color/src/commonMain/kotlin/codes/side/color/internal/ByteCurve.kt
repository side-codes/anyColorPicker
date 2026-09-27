package codes.side.color.internal

import codes.side.color.TransferFunction
import kotlin.math.roundToInt

/** The byte of an encoded channel: clamped to `0..1`, times 255, rounded half up; 0 for NaN. */
internal fun encodedByte(encoded: Double): Int = if (encoded.isNaN()) 0 else (encoded.coerceIn(0.0, 1.0) * 255.0).roundToInt()

/** An opaque `0xAARRGGBB` pixel. */
internal fun argb(r: Int, g: Int, b: Int): Int = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

/** The sRGB curve's table, built on first use from this platform's own curve. */
internal val srgbBytes: ByteCurve by lazy { ByteCurve(TransferFunction.Srgb::encode) }

/**
 * [encodedByte] of a linear channel through [encode], without calling [encode]: from the 255 linear
 * values where the byte steps up, found once by bisection over the doubles in `0..1`, whose bit
 * patterns order as they do. A lookup starts from the step at the start of the value's bucket of
 * 1/4096 and moves up past the steps it has reached, never more than a few.
 *
 * Only for a curve that rises with its input, keeps signs and fixes 0 and 1, as the sRGB curve does:
 * then clamping in linear light before the lookup gives the byte that encoding, clamping and rounding
 * give.
 */
internal class ByteCurve(private val encode: (Double) -> Double) {
    // steps[k]: the smallest linear value in 0..1 whose byte is at least k; steps[0] is 0.
    private val steps = DoubleArray(256)
    private val start = IntArray(BUCKETS + 1)

    init {
        for (k in 1..255) steps[k] = smallestReaching(k)
        var code = 0
        for (i in 0..BUCKETS) {
            val x = i.toDouble() / BUCKETS
            while (code < 255 && x >= steps[code + 1]) code++
            start[i] = code
        }
    }

    /** The linear value where the byte steps up to [k]. */
    fun threshold(k: Int): Double = steps[k]

    /** The byte of [linear]: 0 at or below 0, and for NaN; 255 at or above 1. */
    fun byteOf(linear: Double): Int {
        if (!(linear > 0.0)) return 0
        if (linear >= 1.0) return 255
        var code = start[(linear * BUCKETS).toInt()]
        while (code < 255 && linear >= steps[code + 1]) code++
        return code
    }

    private fun smallestReaching(k: Int): Double {
        var below = 0.0.toRawBits()
        var reaching = 1.0.toRawBits()
        while (reaching - below > 1) {
            val middle = below + (reaching - below) / 2
            if (encodedByte(encode(Double.fromBits(middle))) >= k) reaching = middle else below = middle
        }
        return Double.fromBits(reaching)
    }

    private companion object {
        const val BUCKETS = 4096
    }
}
