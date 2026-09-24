package codes.side.color.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.toArgb
import codes.side.color.ColorValue
import codes.side.color.DisplayP3
import codes.side.color.GamutMapping
import codes.side.color.Lab
import codes.side.color.Oklab
import codes.side.color.RgbGamut
import codes.side.color.Srgb
import codes.side.color.SrgbLinear
import codes.side.color.XyzD50
import codes.side.color.toGamut
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.colorspace.ColorSpaces as ComposeSpaces

/**
 * This Compose color as a [ColorValue], its components copied into the matching space, never
 * converted through Compose's Float arithmetic:
 *
 * | Compose | ColorValue |
 * |---|---|
 * | `Srgb`, `ExtendedSrgb` | [Srgb] |
 * | `LinearSrgb`, `LinearExtendedSrgb` | [SrgbLinear] |
 * | `DisplayP3` | [DisplayP3] |
 * | `CieXyz` | [XyzD50], Compose's connection space |
 * | `CieLab` | [Lab] |
 * | `Oklab` | [Oklab] |
 * | any other RGB space | its [ComposeColorSpaces] space |
 *
 * Compose packs sRGB into 8 bits a channel, so `Color(0xFF336699)` is exactly 0.2, 0.4, 0.6.
 *
 * @throws IllegalArgumentException for [Color.Unspecified], and for Compose's HDR spaces
 * `Bt2020Hlg` and `Bt2020Pq`, which ColorValue does not model.
 */
public fun Color.toColorValue(): ColorValue = toColorValueOrNull() ?: throw IllegalArgumentException(
    if (isUnspecified) "Color.Unspecified has no color" else "${colorSpace.name} is an HDR space, which ColorValue does not model",
)

/** As [toColorValue], but null for [Color.Unspecified] and Compose's HDR spaces. */
public fun Color.toColorValueOrNull(): ColorValue? {
    if (isUnspecified) return null
    if (colorSpace == ComposeSpaces.Srgb) {
        val argb = toArgb()
        return Srgb((argb shr 16 and 0xFF) / 255.0, (argb shr 8 and 0xFF) / 255.0, (argb and 0xFF) / 255.0, (argb ushr 24) / 255.0)
    }
    val space = when (colorSpace) {
        ComposeSpaces.ExtendedSrgb -> Srgb
        ComposeSpaces.LinearSrgb, ComposeSpaces.LinearExtendedSrgb -> SrgbLinear
        ComposeSpaces.DisplayP3 -> DisplayP3
        ComposeSpaces.CieXyz -> XyzD50
        ComposeSpaces.CieLab -> Lab
        ComposeSpaces.Oklab -> Oklab
        else -> ComposeColorSpaces.of(colorSpace) ?: return null
    }
    // Every space but sRGB packs half floats, which a Double holds exactly, and alpha in 10 bits.
    val opacity = (alpha * 1023f).roundToInt() / 1023.0
    return space.color(doubleArrayOf(red.toDouble(), green.toDouble(), blue.toDouble()), opacity)
}

/**
 * This color as a Compose [Color], brought into [gamut] by [mapping] first. An [Srgb] gamut, the
 * default and what desktop, iOS and the web draw, gives a Compose sRGB color of 8 bits a channel;
 * [DisplayP3] gives half floats, which keep 10-bit values, for an Android window in wide-gamut mode.
 * Linear sRGB and the [ComposeColorSpaces] give their Compose spaces. A missing alpha is 0, as CSS
 * reads `none`.
 *
 * @throws IllegalArgumentException if Compose has no space for [gamut]'s.
 */
public fun ColorValue.toComposeColor(gamut: RgbGamut = Srgb.gamut, mapping: GamutMapping = GamutMapping.Css()): Color {
    val space = requireNotNull(ComposeColorSpaces.composeOf(gamut.space)) { "Compose has no color space for ${gamut.space.id}" }
    val rgb = toGamut(gamut, mapping).components()
    return Color(rgb[0].toFloat(), rgb[1].toFloat(), rgb[2].toFloat(), alpha.toFloat(), space)
}
