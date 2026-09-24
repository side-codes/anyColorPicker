package codes.side.colorpicker.conversion

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.colorspace.Illuminant
import androidx.compose.ui.graphics.colorspace.adapt
import codes.side.colorpicker.model.CmykColor
import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.model.LabColor
import codes.side.colorpicker.model.OkhslColor
import codes.side.colorpicker.model.OkhsvColor
import codes.side.colorpicker.model.OklabColor
import codes.side.colorpicker.model.OklchColor
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
 * Converts this color to a Compose [Color] in the sRGB color space, preserving alpha.
 * Colors outside the display gamut are mapped, not clipped; see [OklabColor.toRgb].
 */
public fun OklabColor.toComposeColor(): Color = toRgb().toComposeColor()

/**
 * Converts this color to a Compose [Color] in the sRGB color space, preserving alpha.
 * Colors outside the display gamut are mapped, not clipped; see [OklchColor.toRgb].
 */
public fun OklchColor.toComposeColor(): Color = toRgb().toComposeColor()

/** Converts this color to a Compose [Color] in the sRGB color space, preserving alpha. */
public fun OkhslColor.toComposeColor(): Color = toRgb().toComposeColor()

/** Converts this color to a Compose [Color] in the sRGB color space, preserving alpha. */
public fun OkhsvColor.toComposeColor(): Color = toRgb().toComposeColor()

/**
 * Converts this Compose [Color] to an [RgbColor].
 *
 * An sRGB color is read as it is. One in any other space goes through CIE XYZ, and if sRGB cannot
 * show it, it is mapped as [LabColor.toRgb] maps: lightness and hue hold and chroma gives way,
 * where clipping each channel would move all three.
 *
 * @throws IllegalArgumentException if this is [Color.Unspecified].
 */
public fun Color.toRgbColor(): RgbColor {
    require(this != Color.Unspecified) { "Cannot convert Color.Unspecified to RgbColor" }
    if (colorSpace == ColorSpaces.Srgb) {
        return RgbColor(red = red, green = green, blue = blue, alpha = alpha)
    }
    // Float XYZ from the space itself. convert(ColorSpaces.CieXyz) would hand it back as a Color,
    // which keeps components in half floats and clamps them to ±2.
    val xyz = colorSpace.adapt(Illuminant.D50).toXyz(red, green, blue)
    return gamutMappedRgbColor(
        linear = relativeXyzToLinearSrgb(
            xr = xyz[0] / ComposeD50X,
            yr = xyz[1].toDouble(),
            zr = xyz[2] / ComposeD50Z,
        ),
        alpha = alpha.coerceIn(0f, 1f),
    )
}

// The D50 white Compose adapts to, from the chromaticity it defines, as XYZ with Y = 1.
private val ComposeD50X: Double = Illuminant.D50.x.toDouble() / Illuminant.D50.y
private val ComposeD50Z: Double = (1.0 - Illuminant.D50.x - Illuminant.D50.y) / Illuminant.D50.y

/** Converts this Compose [Color] to an [HslColor]; see [Color.toRgbColor] for sRGB handling. */
public fun Color.toHslColor(): HslColor = toRgbColor().toHsl()

/** Converts this Compose [Color] to a [CmykColor]; see [Color.toRgbColor] for sRGB handling. */
public fun Color.toCmykColor(): CmykColor = toRgbColor().toCmyk()

/** Converts this Compose [Color] to a [LabColor]; see [Color.toRgbColor] for sRGB handling. */
public fun Color.toLabColor(): LabColor = toRgbColor().toLab()

/** Converts this Compose [Color] to an [OklabColor]; see [Color.toRgbColor] for sRGB handling. */
public fun Color.toOklabColor(): OklabColor = toRgbColor().toOklab()

/** Converts this Compose [Color] to an [OklchColor]; see [Color.toRgbColor] for sRGB handling. */
public fun Color.toOklchColor(): OklchColor = toRgbColor().toOklch()

/** Converts this Compose [Color] to an [OkhslColor]; see [Color.toRgbColor] for sRGB handling. */
public fun Color.toOkhslColor(): OkhslColor = toRgbColor().toOkhsl()

/** Converts this Compose [Color] to an [OkhsvColor]; see [Color.toRgbColor] for sRGB handling. */
public fun Color.toOkhsvColor(): OkhsvColor = toRgbColor().toOkhsv()
