package codes.side.color.compose

import androidx.compose.ui.graphics.colorspace.Illuminant
import androidx.compose.ui.graphics.colorspace.Rgb
import codes.side.color.ColorSpace
import codes.side.color.DisplayP3
import codes.side.color.RgbColorSpace
import codes.side.color.RgbPrimaries
import codes.side.color.Srgb
import codes.side.color.SrgbLinear
import codes.side.color.WhitePoint
import kotlin.math.roundToLong
import androidx.compose.ui.graphics.colorspace.ColorSpace as ComposeSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces as ComposeSpaces
import androidx.compose.ui.graphics.colorspace.WhitePoint as ComposeWhite

/**
 * Compose's named RGB spaces that this library has no space for, rebuilt from Compose's own
 * primaries, white and transfer curve, which is what [toColorValue] gives their colors in.
 *
 * Compose's definitions are its own: its Bt2020 is not CSS's rec2020, for one. Its D65 and D50,
 * written to five digits, are taken as CSS's, so a grey stays exactly grey; its primaries are read to
 * the decimals it writes them with. The ids start with `--compose-`; [all] passed to `parseCss` reads
 * back what `toCssString` writes for these spaces.
 */
public object ComposeColorSpaces {
    /** Rec. ITU-R BT.709-5: sRGB's primaries under the BT.709 curve. */
    public val Bt709: RgbColorSpace = rebuilt("--compose-bt709", ComposeSpaces.Bt709)

    /** Rec. ITU-R BT.2020-1, with its SDR curve. */
    public val Bt2020: RgbColorSpace = rebuilt("--compose-bt2020", ComposeSpaces.Bt2020)

    /** SMPTE RP 431-2-2007 DCI-P3: the theatre white and gamma 2.6. */
    public val DciP3: RgbColorSpace = rebuilt("--compose-dci-p3", ComposeSpaces.DciP3)

    /** NTSC (1953), under illuminant C. */
    public val Ntsc1953: RgbColorSpace = rebuilt("--compose-ntsc-1953", ComposeSpaces.Ntsc1953)

    /** SMPTE C. */
    public val SmpteC: RgbColorSpace = rebuilt("--compose-smpte-c", ComposeSpaces.SmpteC)

    /** Adobe RGB (1998), with gamma 2.2. */
    public val AdobeRgb: RgbColorSpace = rebuilt("--compose-adobe-rgb", ComposeSpaces.AdobeRgb)

    /** ROMM RGB (ProPhoto), under D50. */
    public val ProPhotoRgb: RgbColorSpace = rebuilt("--compose-prophoto-rgb", ComposeSpaces.ProPhotoRgb)

    /** SMPTE ST 2065-1:2012 ACES, linear, under D60. */
    public val Aces: RgbColorSpace = rebuilt("--compose-aces", ComposeSpaces.Aces)

    /** Academy S-2014-004 ACEScg, linear, under D60. */
    public val Acescg: RgbColorSpace = rebuilt("--compose-acescg", ComposeSpaces.Acescg)

    /** All nine. */
    public val all: List<RgbColorSpace> = listOf(Bt709, Bt2020, DciP3, Ntsc1953, SmpteC, AdobeRgb, ProPhotoRgb, Aces, Acescg)

    private val byCompose: Map<ComposeSpace, RgbColorSpace> = mapOf(
        ComposeSpaces.Bt709 to Bt709,
        ComposeSpaces.Bt2020 to Bt2020,
        ComposeSpaces.DciP3 to DciP3,
        ComposeSpaces.Ntsc1953 to Ntsc1953,
        ComposeSpaces.SmpteC to SmpteC,
        ComposeSpaces.AdobeRgb to AdobeRgb,
        ComposeSpaces.ProPhotoRgb to ProPhotoRgb,
        ComposeSpaces.Aces to Aces,
        ComposeSpaces.Acescg to Acescg,
    )

    /** This library's space for Compose's RGB [space], or null for one it has none for. */
    internal fun of(space: ComposeSpace): RgbColorSpace? = byCompose[space]

    /** Compose's space for this library's RGB [space], or null for one Compose has none for. */
    internal fun composeOf(space: ColorSpace): ComposeSpace? = when (space) {
        Srgb -> ComposeSpaces.Srgb
        SrgbLinear -> ComposeSpaces.LinearSrgb
        DisplayP3 -> ComposeSpaces.DisplayP3
        else -> byCompose.entries.firstOrNull { it.value == space }?.key
    }
}

private fun rebuilt(id: String, space: ComposeSpace): RgbColorSpace {
    val rgb = space as Rgb
    val p = rgb.getPrimaries(FloatArray(6)).map { decimal(it) }
    return ColorSpace.rgb(
        id,
        RgbPrimaries(p[0], p[1], p[2], p[3], p[4], p[5]),
        white(rgb.whitePoint),
        ParametricTransfer(checkNotNull(rgb.transferParameters) { "${rgb.name} has no parametric curve" }),
    )
}

private fun white(point: ComposeWhite): WhitePoint = when {
    point.x == Illuminant.D65.x && point.y == Illuminant.D65.y -> WhitePoint.D65
    point.x == Illuminant.D50.x && point.y == Illuminant.D50.y -> WhitePoint.D50
    else -> WhitePoint(decimal(point.x), decimal(point.y))
}

// Compose writes its chromaticities as Float literals of at most five decimals; six recover them.
private fun decimal(value: Float): Double = (value.toDouble() * 1e6).roundToLong() / 1e6
