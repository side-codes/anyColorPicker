package codes.side.colorpicker.conversion

import codes.side.colorpicker.model.RgbColor
import kotlin.math.roundToInt

/**
 * Packs this color into an ARGB [Int] (`0xAARRGGBB`), quantizing each `0..1` channel
 * to 8 bits by rounding to the nearest integer.
 */
public fun RgbColor.toArgbInt(): Int {
    val a = (alpha * 255f).roundToInt() and 0xFF
    val r = (red * 255f).roundToInt() and 0xFF
    val g = (green * 255f).roundToInt() and 0xFF
    val b = (blue * 255f).roundToInt() and 0xFF
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}

/**
 * Unpacks this ARGB [Int] (`0xAARRGGBB`) into an [RgbColor] with `0..1` channels.
 * Alpha is taken from the top byte; every input is valid, so this never throws.
 */
public fun Int.toRgbColor(): RgbColor = RgbColor(
    red = ((this shr 16) and 0xFF) / 255f,
    green = ((this shr 8) and 0xFF) / 255f,
    blue = (this and 0xFF) / 255f,
    alpha = ((this ushr 24) and 0xFF) / 255f,
)

internal fun argb(alpha: Int, red: Int, green: Int, blue: Int): Int =
    ((alpha.coerceIn(0, 255) and 0xFF) shl 24) or
            ((red.coerceIn(0, 255) and 0xFF) shl 16) or
            ((green.coerceIn(0, 255) and 0xFF) shl 8) or
            (blue.coerceIn(0, 255) and 0xFF)

internal fun setAlphaComponent(color: Int, alpha: Int): Int =
    (color and 0x00FFFFFF) or ((alpha.coerceIn(0, 255) and 0xFF) shl 24)

/**
 * Blends [color1] and [color2] using the given [ratio]: `0.0` returns [color1] and `1.0`
 * returns [color2]. Ratios outside `0..1` are clamped to that range, and a `NaN` [ratio]
 * is treated as `0` (returning [color1]).
 *
 * Note: this deviates from AndroidX `ColorUtils.blendARGB`, which truncates each blended
 * channel; here every channel is rounded to the nearest integer for unbiased results.
 */
internal fun blendArgb(color1: Int, color2: Int, ratio: Float): Int {
    val r = if (ratio.isNaN()) 0f else ratio.coerceIn(0f, 1f)
    val inverseRatio = 1f - r
    val a = ((color1 ushr 24) and 0xFF) * inverseRatio + ((color2 ushr 24) and 0xFF) * r
    val red = ((color1 shr 16) and 0xFF) * inverseRatio + ((color2 shr 16) and 0xFF) * r
    val g = ((color1 shr 8) and 0xFF) * inverseRatio + ((color2 shr 8) and 0xFF) * r
    val b = (color1 and 0xFF) * inverseRatio + (color2 and 0xFF) * r
    return argb(a.roundToInt(), red.roundToInt(), g.roundToInt(), b.roundToInt())
}
