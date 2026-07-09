package codes.side.colorpicker.conversion

import codes.side.colorpicker.model.RgbColor

fun RgbColor.contrastColor(): RgbColor {
    val luminance = red * 0.299f + green * 0.587f + blue * 0.114f
    return if (luminance > 0.729f) RgbColor.Black else RgbColor.White
}
