package codes.side.color

import codes.side.color.internal.hexColor
import kotlin.math.roundToInt

/**
 * Where a hex string keeps its alpha channel, if it keeps one.
 *
 * Eight hex digits are ambiguous and no parser can resolve them: `#FF000080` is a half-transparent
 * red to a stylesheet and an opaque navy to `android.graphics.Color`. Both conventions are in wide
 * use, so the one in play is named rather than guessed.
 */
public enum class HexAlpha {
    /** `#RRGGBB` and `#RGB`, with no alpha digits. Formats opaque; parses the opaque forms only. */
    None,

    /** `#AARRGGBB` and `#ARGB`, as `android.graphics.Color` writes and reads them. */
    First,

    /** `#RRGGBBAA` and `#RGBA`, as CSS Color 4 writes and reads them. */
    Last,
}

/**
 * This color as an uppercase 8-bit sRGB hex string with a leading `#`: brought into sRGB by
 * [mapping], then each channel rounded to 8 bits. [alpha] picks the shape, `#AARRGGBB`, `#RRGGBBAA`
 * or `#RRGGBB` without it; a missing alpha is written `00`, as CSS reads `none`.
 *
 * [alpha] has no default: eight digits mean different colors to a stylesheet and to
 * `android.graphics.Color`, and a string written without saying which is one some reader will get
 * wrong.
 */
public fun ColorValue.toHexString(alpha: HexAlpha, mapping: GamutMapping = GamutMapping.Css()): String {
    val rgb = toGamut(Srgb.gamut, mapping).components()
    return buildString(9) {
        append('#')
        if (alpha == HexAlpha.First) appendByte(this@toHexString.alpha)
        for (value in rgb) appendByte(value)
        if (alpha == HexAlpha.Last) appendByte(this@toHexString.alpha)
    }
}

/**
 * The hex color [text], as an [Srgb] color. The leading `#` is optional and case does not matter.
 * Three and six digits carry no alpha and mean the same whatever [alpha] says; four and eight carry
 * one at the end [alpha] names, and [HexAlpha.None] refuses them. Three and four digits are each
 * doubled: `#ABC` is `#AABBCC`.
 *
 * @throws IllegalArgumentException if [text] is not a hex color with that alpha.
 */
public fun ColorValue.Companion.parseHex(text: String, alpha: HexAlpha): ColorValue =
    parseHexOrNull(text, alpha) ?: throw IllegalArgumentException("Not a hex color with alpha $alpha: $text")

/** As [parseHex], but null when [text] is not a hex color with that alpha. */
public fun ColorValue.Companion.parseHexOrNull(text: String, alpha: HexAlpha): ColorValue? =
    hexColor(text.removePrefix("#"), alpha)

private fun StringBuilder.appendByte(value: Double) {
    val byte = (value.coerceIn(0.0, 1.0) * 255.0).roundToInt()
    append(HEX_DIGITS[byte shr 4]).append(HEX_DIGITS[byte and 0xF])
}

private const val HEX_DIGITS = "0123456789ABCDEF"
