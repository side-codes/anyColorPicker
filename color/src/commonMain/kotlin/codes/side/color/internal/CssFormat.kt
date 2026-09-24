package codes.side.color.internal

import codes.side.color.ColorValue
import codes.side.color.Hsl
import codes.side.color.Hwb
import codes.side.color.Srgb
import kotlin.math.abs
import kotlin.math.floor

/** [color] as CSS; [codes.side.color.toCssString] says how. */
internal fun cssString(color: ColorValue, precision: Int, legacy: Boolean): String {
    val space = color.space
    val components = color.components()
    if (legacy && (space == Srgb || space == Hsl)) {
        // A missing component, and a missing alpha, store 0.0: the comma syntax has no none.
        val values = if (space == Srgb) {
            components.map { cssNumber(finiteOrLargest(it * 255.0), precision) }
        } else {
            listOf(cssNumber(components[0], precision), "${cssNumber(components[1], precision)}%", "${cssNumber(components[2], precision)}%")
        }
        val name = if (space == Srgb) "rgb" else "hsl"
        val alpha = cssNumber(color.alpha, precision)
        return if (alpha == "1") "$name(${values.joinToString(", ")})" else "${name}a(${values.joinToString(", ")}, $alpha)"
    }
    // hsl() and hwb() take numbers too, but browsers only from 2024; CSS writes percentages.
    val percentages = space == Hsl || space == Hwb
    val body = components.indices.joinToString(" ") { i ->
        when {
            color.missingMask and (1 shl i) != 0 -> "none"
            percentages && i > 0 -> "${cssNumber(components[i], precision)}%"
            else -> cssNumber(components[i], precision)
        }
    }
    val alpha = if (color.isAlphaMissing) " / none" else cssNumber(color.alpha, precision).let { if (it == "1") "" else " / $it" }
    val name = cssName(space)
    return if (name in FUNCTION_SPACES) "$name($body$alpha)" else "color($name $body$alpha)"
}

/**
 * [value] rounded to [precision] digits counted from its first integer digit, or from the decimal
 * point when it is below 1, as color.js rounds, and written without an exponent.
 */
internal fun cssNumber(value: Double, precision: Int): String {
    val magnitude = abs(value)
    var integerDigits = 0
    while (integerDigits < POWERS_OF_TEN.size && magnitude >= POWERS_OF_TEN[integerDigits]) integerDigits++
    val decimals = precision - integerDigits
    val digits = if (decimals >= 0) {
        val units = floor(magnitude * POWERS_OF_TEN[decimals] + 0.5).toLong()
        if (units == 0L) return "0"
        val padded = units.toString().padStart(decimals + 1, '0')
        val whole = padded.substring(0, padded.length - decimals)
        val fraction = padded.substring(padded.length - decimals).trimEnd('0')
        if (fraction.isEmpty()) whole else "$whole.$fraction"
    } else {
        val units = floor(magnitude / POWERS_OF_TEN[-decimals] + 0.5).toLong()
        "$units${"0".repeat(-decimals)}"
    }
    return if (value < 0.0) "-$digits" else digits
}

// 10^0 to 10^308, each the previous times ten: exact to 10^22, and the same on every platform past it.
private val POWERS_OF_TEN = DoubleArray(309).also {
    var power = 1.0
    for (i in it.indices) {
        it[i] = power
        power *= 10.0
    }
}
