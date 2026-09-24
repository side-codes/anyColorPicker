package codes.side.color.internal

import codes.side.color.ColorValue
import codes.side.color.HexAlpha
import codes.side.color.Srgb

/** The sRGB color of hex [digits] (no `#`) with alpha where [alpha] says, or null if they are not one. */
internal fun hexColor(digits: String, alpha: HexAlpha): ColorValue? {
    val width = when (digits.length) {
        3, 4 -> 1
        6, 8 -> 2
        else -> return null
    }
    val count = digits.length / width
    if (count == 4 && alpha == HexAlpha.None) return null
    val bytes = IntArray(count)
    for (i in 0 until count) {
        var byte = 0
        for (j in 0 until width) {
            // Not digitToIntOrNull, which takes other scripts' digits and fullwidth letters too.
            val digit = when (val c = digits[i * width + j]) {
                in '0'..'9' -> c - '0'
                in 'a'..'f' -> c - 'a' + 10
                in 'A'..'F' -> c - 'A' + 10
                else -> return null
            }
            byte = byte * 16 + digit
        }
        bytes[i] = if (width == 1) byte * 17 else byte
    }
    val first = if (count == 4 && alpha == HexAlpha.First) 1 else 0
    val alphaByte = when {
        count == 3 -> 255
        alpha == HexAlpha.First -> bytes[0]
        else -> bytes[3]
    }
    return Srgb(bytes[first] / 255.0, bytes[first + 1] / 255.0, bytes[first + 2] / 255.0, alphaByte / 255.0)
}
