package codes.side.colorpicker.foundation

import kotlin.math.abs
import kotlin.math.roundToLong

/** [value] rounded to a whole number, half away from zero, as every value the components show is. */
internal fun roundHalfAwayFromZero(value: Double): Long {
    val magnitude = abs(value).roundToLong()
    return if (value < 0.0) -magnitude else magnitude
}

/**
 * [value] rounded to [places] decimals, half away from zero. A value that rounds to zero comes back as positive zero,
 * so no formatter writes `-0.000`.
 */
internal fun roundHalfAwayFromZero(value: Double, places: Int): Double {
    var scale = 1L
    repeat(places) { scale *= 10L }
    val scaled = (abs(value) * scale).roundToLong()
    if (scaled == 0L) return 0.0
    return (if (value < 0.0) -scaled else scaled).toDouble() / scale
}
