package codes.side.colorpicker.foundation

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * [value] with [places] decimals, rounded half away from zero, with no sign on a value that rounds to zero. For test
 * messages, which need fixed-point text on every target and no locale.
 */
internal fun decimals(value: Double, places: Int): String {
    var scale = 1L
    repeat(places) { scale *= 10L }
    val scaled = (abs(value) * scale).roundToLong()
    val sign = if (value < 0.0 && scaled != 0L) "-" else ""
    if (places == 0) return "$sign$scaled"
    val fraction = (scaled % scale).toString().padStart(places, '0')
    return "$sign${scaled / scale}.$fraction"
}
