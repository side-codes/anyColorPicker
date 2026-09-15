package codes.side.colorpicker.conversion

/**
 * The linear value at which each encoded byte takes over: byte `b` wins from
 * `linearize((b + 0.5) / 255)` upward, because that is the linear value whose encoding
 * rounds to `b` from below. [linearize] is [delinearize]'s inverse, so these are the exact
 * boundaries rather than a sampling of the curve.
 */
private val THRESHOLDS = DoubleArray(255) { linearize((it + 0.5) / 255.0) }

/**
 * Linear-light sRGB in `0..1` as the `0..255` byte a packed pixel holds.
 *
 * Identical in every case to rounding [delinearize], and reached by locating [linear]
 * between the boundaries above instead of raising it to a power. Encoding a plane means
 * three of these per pixel — 49152 for an [codes.side.colorpicker.ui.OkhslPlane] — and on
 * Android `pow` costs 5.1 ms of a 17.8 ms rasterization where this costs a tenth of that.
 */
internal fun linearToSrgbByte(linear: Double): Int {
    if (linear <= THRESHOLDS[0]) return 0
    if (linear >= THRESHOLDS[254]) return 255
    var low = 0
    var high = 254
    while (low < high) {
        val mid = (low + high) ushr 1
        if (linear < THRESHOLDS[mid]) high = mid else low = mid + 1
    }
    return low
}
