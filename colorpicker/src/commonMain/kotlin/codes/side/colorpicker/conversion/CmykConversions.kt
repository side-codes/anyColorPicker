package codes.side.colorpicker.conversion

import codes.side.colorpicker.model.CmykColor
import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.model.RgbColor

/** Converts this CMYK color to RGB. Alpha is carried over unchanged. */
public fun CmykColor.toRgb(): RgbColor {
    val r = (1f - cyan) * (1f - key)
    val g = (1f - magenta) * (1f - key)
    val b = (1f - yellow) * (1f - key)

    return RgbColor(
        red = r.coerceIn(0f, 1f),
        green = g.coerceIn(0f, 1f),
        blue = b.coerceIn(0f, 1f),
        alpha = alpha,
    )
}

/**
 * Converts this RGB color to CMYK. Pure black maps to `key = 1` with zero ink in the
 * other channels. Alpha is carried over unchanged.
 */
public fun RgbColor.toCmyk(): CmykColor {
    val k = 1f - maxOf(red, green, blue)

    if (k >= 1f) {
        return CmykColor(cyan = 0f, magenta = 0f, yellow = 0f, key = 1f, alpha = alpha)
    }

    val c = (1f - red - k) / (1f - k)
    val m = (1f - green - k) / (1f - k)
    val y = (1f - blue - k) / (1f - k)

    return CmykColor(
        cyan = c.coerceIn(0f, 1f),
        magenta = m.coerceIn(0f, 1f),
        yellow = y.coerceIn(0f, 1f),
        key = k.coerceIn(0f, 1f),
        alpha = alpha,
    )
}

/** Converts this CMYK color to HSL by way of RGB. Alpha is carried over unchanged. */
public fun CmykColor.toHsl(): HslColor = toRgb().toHsl()

/** Converts this HSL color to CMYK by way of RGB. Alpha is carried over unchanged. */
public fun HslColor.toCmyk(): CmykColor = toRgb().toCmyk()

/** Packs this CMYK color into an ARGB [Int] (`0xAARRGGBB`); see [RgbColor.toArgbInt]. */
public fun CmykColor.toArgbInt(): Int = toRgb().toArgbInt()

/** Unpacks this ARGB [Int] (`0xAARRGGBB`) into a [CmykColor]; see [Int.toRgbColor]. */
public fun Int.toCmykColor(): CmykColor = toRgbColor().toCmyk()
