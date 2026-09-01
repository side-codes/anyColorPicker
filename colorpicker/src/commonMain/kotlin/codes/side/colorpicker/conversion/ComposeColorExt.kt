package codes.side.colorpicker.conversion

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import codes.side.colorpicker.model.CmykColor
import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.model.LabColor
import codes.side.colorpicker.model.RgbColor

/** Converts this color to a Compose [Color] in the sRGB color space, preserving alpha. */
public fun RgbColor.toComposeColor(): Color =
    Color(red = red, green = green, blue = blue, alpha = alpha)

/** Converts this color to a Compose [Color] in the sRGB color space, preserving alpha. */
public fun HslColor.toComposeColor(): Color = toRgb().toComposeColor()

/** Converts this color to a Compose [Color] in the sRGB color space, preserving alpha. */
public fun CmykColor.toComposeColor(): Color = toRgb().toComposeColor()

/** Converts this color to a Compose [Color] in the sRGB color space, preserving alpha. */
public fun LabColor.toComposeColor(): Color = toRgb().toComposeColor()

/**
 * Converts this Compose [Color] to an [RgbColor], converting to the sRGB color space
 * first if needed (clamping any out-of-gamut channels to `0..1`).
 *
 * @throws IllegalArgumentException if this is [Color.Unspecified].
 */
public fun Color.toRgbColor(): RgbColor {
    require(this != Color.Unspecified) { "Cannot convert Color.Unspecified to RgbColor" }
    val srgb = convert(ColorSpaces.Srgb)
    return RgbColor(
        red = srgb.red.coerceIn(0f, 1f),
        green = srgb.green.coerceIn(0f, 1f),
        blue = srgb.blue.coerceIn(0f, 1f),
        alpha = srgb.alpha.coerceIn(0f, 1f),
    )
}

/** Converts this Compose [Color] to an [HslColor]; see [Color.toRgbColor] for sRGB handling. */
public fun Color.toHslColor(): HslColor = toRgbColor().toHsl()

/** Converts this Compose [Color] to a [CmykColor]; see [Color.toRgbColor] for sRGB handling. */
public fun Color.toCmykColor(): CmykColor = toRgbColor().toCmyk()

/** Converts this Compose [Color] to a [LabColor]; see [Color.toRgbColor] for sRGB handling. */
public fun Color.toLabColor(): LabColor = toRgbColor().toLab()
