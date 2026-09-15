package codes.side.colorpicker.conversion

import codes.side.colorpicker.model.PickerColor
import codes.side.colorpicker.model.RgbColor

/**
 * Formats this color as an uppercase hex string with a leading `#`.
 *
 * The color is first packed into an ARGB [Int] (quantizing each channel to 8 bits), so the
 * alpha channel comes first: the result is `#AARRGGBB` when [includeAlpha] is `true`
 * (the default) and `#RRGGBB` otherwise.
 */
public fun PickerColor.toHexString(includeAlpha: Boolean = true): String =
    toRgbColor().toArgbInt().toHexColorString(includeAlpha = includeAlpha)

/**
 * Formats this packed ARGB [Int] as an uppercase hex string with a leading `#`.
 *
 * The alpha channel comes first: the result is `#AARRGGBB` when [includeAlpha] is `true`
 * (the default) and `#RRGGBB` otherwise.
 */
public fun Int.toHexColorString(includeAlpha: Boolean = true): String {
    val rgb = (this and 0xFFFFFF).toString(16).uppercase().padStart(6, '0')
    if (!includeAlpha) return "#$rgb"
    val alpha = ((this ushr 24) and 0xFF).toString(16).uppercase().padStart(2, '0')
    return "#$alpha$rgb"
}

/**
 * Parses this string as a hex color, or returns `null` if it is not a valid hex color.
 *
 * The leading `#` is optional and parsing is case-insensitive. Supported forms:
 * - `RGB` shorthand (3 digits, e.g. `#ABC` expands to `#AABBCC`), alpha defaults to `FF`
 * - `RRGGBB` (6 digits), alpha defaults to `FF`
 * - `AARRGGBB` (8 digits, alpha first)
 *
 * Any other length or any non-hex character yields `null`; this function never throws.
 */
public fun String.toRgbColorOrNull(): RgbColor? {
    val hex = removePrefix("#")
    if (hex.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) {
        return null
    }
    val argb = when (hex.length) {
        3 -> buildString(8) {
            append("FF")
            for (char in hex) {
                append(char)
                append(char)
            }
        }

        6 -> "FF$hex"
        8 -> hex
        else -> return null
    }
    return argb.toLong(16).toInt().toRgbColor()
}

/**
 * Parses this string as a hex color (see [toRgbColorOrNull] for the accepted formats).
 *
 * @throws IllegalArgumentException if the string is not a valid hex color.
 */
public fun String.toRgbColor(): RgbColor =
    toRgbColorOrNull() ?: throw IllegalArgumentException("Invalid hex color string: '$this'")
