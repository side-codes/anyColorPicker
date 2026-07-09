package codes.side.colorpicker.conversion

import codes.side.colorpicker.model.RgbColor
import kotlin.math.roundToInt

fun RgbColor.toArgbInt(): Int {
    val a = (alpha * 255f).roundToInt() and 0xFF
    val r = (red * 255f).roundToInt() and 0xFF
    val g = (green * 255f).roundToInt() and 0xFF
    val b = (blue * 255f).roundToInt() and 0xFF
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}

fun Int.toRgbColor(): RgbColor = RgbColor(
    red = ((this shr 16) and 0xFF) / 255f,
    green = ((this shr 8) and 0xFF) / 255f,
    blue = (this and 0xFF) / 255f,
    alpha = ((this ushr 24) and 0xFF) / 255f,
)

fun argb(alpha: Int, red: Int, green: Int, blue: Int): Int =
    ((alpha and 0xFF) shl 24) or ((red and 0xFF) shl 16) or ((green and 0xFF) shl 8) or (blue and 0xFF)

fun setAlphaComponent(color: Int, alpha: Int): Int =
    (color and 0x00FFFFFF) or ((alpha and 0xFF) shl 24)

fun blendArgb(color1: Int, color2: Int, ratio: Float): Int {
    val inverseRatio = 1f - ratio
    val a = ((color1 ushr 24) and 0xFF) * inverseRatio + ((color2 ushr 24) and 0xFF) * ratio
    val r = ((color1 shr 16) and 0xFF) * inverseRatio + ((color2 shr 16) and 0xFF) * ratio
    val g = ((color1 shr 8) and 0xFF) * inverseRatio + ((color2 shr 8) and 0xFF) * ratio
    val b = (color1 and 0xFF) * inverseRatio + (color2 and 0xFF) * ratio
    return argb(a.toInt(), r.toInt(), g.toInt(), b.toInt())
}
