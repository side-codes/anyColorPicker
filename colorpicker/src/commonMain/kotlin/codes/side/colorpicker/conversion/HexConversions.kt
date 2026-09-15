package codes.side.colorpicker.conversion

import codes.side.colorpicker.model.PickerColor
import codes.side.colorpicker.model.RgbColor

/**
 * Where a hex string keeps its alpha channel, if it keeps one.
 *
 * Eight hex digits are ambiguous and no parser can resolve them: `#FF000080` is a
 * half-transparent red to a stylesheet and an opaque navy to `android.graphics.Color`.
 * Both conventions are in wide use, so the one in play is named rather than guessed.
 */
public enum class HexAlpha {
    /** `#RRGGBB` and `#RGB`, with no alpha digits. Formats opaque; parses opaque-only. */
    None,

    /** `#AARRGGBB` and `#ARGB`, as `android.graphics.Color` writes and reads them. */
    First,

    /** `#RRGGBBAA` and `#RGBA`, as CSS Color 4 writes and reads them. */
    Last,
}

/**
 * Formats this color as an uppercase hex string with a leading `#`.
 *
 * The color is first packed into an ARGB [Int], quantizing each channel to 8 bits. [alpha]
 * picks the shape: `#AARRGGBB`, `#RRGGBBAA`, or `#RRGGBB` with the channel dropped.
 */
public fun PickerColor.toHexString(alpha: HexAlpha = HexAlpha.First): String =
    toRgbColor().toArgbInt().toHexColorString(alpha)

/**
 * Formats this packed ARGB [Int] as an uppercase hex string with a leading `#`; see
 * [PickerColor.toHexString].
 */
public fun Int.toHexColorString(alpha: HexAlpha = HexAlpha.First): String {
    val rgb = (this and 0xFFFFFF).toString(16).uppercase().padStart(6, '0')
    if (alpha == HexAlpha.None) return "#$rgb"
    val alphaDigits = ((this ushr 24) and 0xFF).toString(16).uppercase().padStart(2, '0')
    return when (alpha) {
        HexAlpha.First -> "#$alphaDigits$rgb"
        else -> "#$rgb$alphaDigits"
    }
}

/**
 * Parses this string as a hex color, or returns `null` if it is not one.
 *
 * The leading `#` is optional and parsing is case-insensitive. Three and six digits carry
 * no alpha and mean the same thing whatever [alpha] says; four and eight read theirs from
 * the end it names, so a string copied out of a stylesheet needs [HexAlpha.Last] or it comes
 * back a different color rather than `null`. [HexAlpha.None] accepts only the forms that
 * carry no alpha, for a caller that wants an opaque color or nothing.
 *
 * - `RGB` (3 digits, `#ABC` expands to `#AABBCC`), alpha defaults to `FF`
 * - `ARGB` or `RGBA` (4 digits), each digit doubled as above
 * - `RRGGBB` (6 digits), alpha defaults to `FF`
 * - `AARRGGBB` or `RRGGBBAA` (8 digits)
 *
 * Any other length or any non-hex character yields `null`; this function never throws.
 */
public fun String.toRgbColorOrNull(alpha: HexAlpha = HexAlpha.First): RgbColor? {
    val hex = removePrefix("#")
    if (hex.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) {
        return null
    }
    val argb = when (hex.length) {
        3 -> doubledDigits("F$hex")
        4 -> if (alpha == HexAlpha.None) return null else doubledDigits(alphaFirst(hex, alpha))
        6 -> "FF$hex"
        8 -> if (alpha == HexAlpha.None) return null else alphaFirst(hex, alpha)
        else -> return null
    }
    return argb.toLong(16).toInt().toRgbColor()
}

/** Expands a shorthand form by doubling every digit: `ARGB` becomes `AARRGGBB`. */
private fun doubledDigits(hex: String): String = buildString(hex.length * 2) {
    for (char in hex) {
        append(char)
        append(char)
    }
}

/** Moves the alpha digits to the front when the caller says they are at the back. */
private fun alphaFirst(hex: String, alpha: HexAlpha): String = when (alpha) {
    HexAlpha.Last -> {
        // One alpha digit in the shorthand form, two in the long one.
        val alphaDigits = hex.length / 4
        hex.takeLast(alphaDigits) + hex.dropLast(alphaDigits)
    }

    else -> hex
}

/**
 * Parses this string as a hex color (see [toRgbColorOrNull] for the accepted formats).
 *
 * @throws IllegalArgumentException if the string is not a valid hex color.
 */
public fun String.toRgbColor(alpha: HexAlpha = HexAlpha.First): RgbColor =
    toRgbColorOrNull(alpha) ?: throw IllegalArgumentException("Invalid hex color string: '$this'")
