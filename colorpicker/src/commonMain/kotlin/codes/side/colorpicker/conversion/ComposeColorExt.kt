package codes.side.colorpicker.conversion

import androidx.compose.ui.graphics.Color
import codes.side.colorpicker.model.CmykColor
import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.model.LabColor
import codes.side.colorpicker.model.RgbColor

fun RgbColor.toComposeColor(): Color =
    Color(red = red, green = green, blue = blue, alpha = alpha)

fun HslColor.toComposeColor(): Color = toRgb().toComposeColor()

fun CmykColor.toComposeColor(): Color = toRgb().toComposeColor()

fun LabColor.toComposeColor(): Color = toRgb().toComposeColor()

fun Color.toRgbColor(): RgbColor = RgbColor(
    red = red.coerceIn(0f, 1f),
    green = green.coerceIn(0f, 1f),
    blue = blue.coerceIn(0f, 1f),
    alpha = alpha.coerceIn(0f, 1f),
)
