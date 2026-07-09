package codes.side.colorpicker.conversion

import codes.side.colorpicker.model.CmykColor
import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.model.RgbColor

fun CmykColor.toRgb(): RgbColor {
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

fun RgbColor.toCmyk(): CmykColor {
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

fun CmykColor.toHsl(): HslColor = toRgb().toHsl()

fun HslColor.toCmyk(): CmykColor = toRgb().toCmyk()

fun CmykColor.toArgbInt(): Int = toRgb().toArgbInt()

fun Int.toCmykColor(): CmykColor = toRgbColor().toCmyk()
