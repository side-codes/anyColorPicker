package codes.side.colorpicker.conversion

import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.model.RgbColor
import kotlin.math.abs

/** Converts this HSL color to RGB. Alpha is carried over unchanged. */
public fun HslColor.toRgb(): RgbColor {
    val h = hue
    val s = saturation
    val l = lightness

    val c = (1f - abs(2f * l - 1f)) * s
    val m = l - 0.5f * c
    val x = c * (1f - abs((h / 60f) % 2f - 1f))

    val hueSegment = (h / 60f).toInt()

    val r: Float
    val g: Float
    val b: Float

    when (hueSegment) {
        0 -> { r = c + m; g = x + m; b = m }
        1 -> { r = x + m; g = c + m; b = m }
        2 -> { r = m; g = c + m; b = x + m }
        3 -> { r = m; g = x + m; b = c + m }
        4 -> { r = x + m; g = m; b = c + m }
        else -> { r = c + m; g = m; b = x + m }
    }

    return RgbColor(
        red = r.coerceIn(0f, 1f),
        green = g.coerceIn(0f, 1f),
        blue = b.coerceIn(0f, 1f),
        alpha = alpha,
    )
}

/**
 * Converts this RGB color to HSL. Achromatic colors (grays) map to hue 0 and
 * saturation 0. Alpha is carried over unchanged.
 */
public fun RgbColor.toHsl(): HslColor {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val delta = max - min

    val l = (max + min) / 2f

    val h: Float
    val s: Float

    if (max == min) {
        h = 0f
        s = 0f
    } else {
        h = when (max) {
            red -> ((green - blue) / delta) % 6f
            green -> ((blue - red) / delta) + 2f
            else -> ((red - green) / delta) + 4f
        } * 60f

        s = delta / (1f - abs(2f * l - 1f))
    }

    return HslColor(
        hue = ((h % 360f + 360f) % 360f).coerceIn(0f, 360f),
        saturation = s.coerceIn(0f, 1f),
        lightness = l.coerceIn(0f, 1f),
        alpha = alpha,
    )
}

/** Packs this HSL color into an ARGB [Int] (`0xAARRGGBB`); see [RgbColor.toArgbInt]. */
public fun HslColor.toArgbInt(): Int = toRgb().toArgbInt()

/** Unpacks this ARGB [Int] (`0xAARRGGBB`) into an [HslColor]; see [Int.toRgbColor]. */
public fun Int.toHslColor(): HslColor = toRgbColor().toHsl()
