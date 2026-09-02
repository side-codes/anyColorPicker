package codes.side.colorpicker.conversion

import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.model.OkhslColor
import codes.side.colorpicker.model.RgbColor
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

// Okhsl's saturation is piecewise: below this it interpolates between the neutral and
// mid chroma anchors, above it between mid and the gamut boundary. 0.8 is Ottosson's
// split, chosen so the two pieces meet with matching slope.
private const val SATURATION_SPLIT = 0.8
private const val SATURATION_SPLIT_INV = 1.25

private const val DEGREES_PER_RADIAN = 180.0 / kotlin.math.PI

/**
 * Converts this Okhsl color to sRGB. Every Okhsl coordinate is inside the gamut by
 * construction, so nothing is clipped or mapped. Alpha is carried over unchanged.
 */
public fun OkhslColor.toRgb(): RgbColor {
    if (lightness >= 1f) return RgbColor(1f, 1f, 1f, alpha)
    if (lightness <= 0f) return RgbColor(0f, 0f, 0f, alpha)

    val radians = hue.toDouble() / DEGREES_PER_RADIAN
    val aUnit = cos(radians)
    val bUnit = sin(radians)
    val okLightness = toeInv(lightness.toDouble())

    val anchors = getChromaAnchors(okLightness, aUnit, bUnit)
    val chroma = chromaForSaturation(saturation.toDouble(), anchors)

    val linear = oklabToLinearSrgb(OkLab(okLightness, chroma * aUnit, chroma * bUnit))

    return RgbColor(
        red = delinearize(linear.r).toFloat().coerceIn(0f, 1f),
        green = delinearize(linear.g).toFloat().coerceIn(0f, 1f),
        blue = delinearize(linear.b).toFloat().coerceIn(0f, 1f),
        alpha = alpha,
    )
}

/**
 * Converts this sRGB color to Okhsl. Grays have no hue to report and come back at hue
 * `0` with zero saturation. Alpha is carried over unchanged.
 */
public fun RgbColor.toOkhsl(): OkhslColor {
    val lab = linearSrgbToOklab(
        LinearRgb(
            r = linearize(red.toDouble()),
            g = linearize(green.toDouble()),
            b = linearize(blue.toDouble()),
        ),
    )

    val chroma = hypot(lab.a, lab.b)
    val lightness = toe(lab.l).toFloat().coerceIn(0f, 1f)
    val gray = OkhslColor(hue = 0f, saturation = 0f, lightness = lightness, alpha = alpha)

    // At the black and white poles the chroma anchors below collapse to zero and the
    // interpolation divides by them, so grays are answered before that can happen.
    if (chroma < ACHROMATIC_CHROMA || lab.l <= 0.0 || lab.l >= 1.0) return gray

    val aUnit = lab.a / chroma
    val bUnit = lab.b / chroma
    val degrees = atan2(lab.b, lab.a) * DEGREES_PER_RADIAN

    val anchors = getChromaAnchors(lab.l, aUnit, bUnit)
    val saturation = saturationForChroma(chroma, anchors)

    if (!saturation.isFinite()) return gray

    return OkhslColor(
        hue = ((degrees % 360.0 + 360.0) % 360.0).toFloat().coerceIn(0f, 360f),
        saturation = saturation.toFloat().coerceIn(0f, 1f),
        lightness = lightness,
        alpha = alpha,
    )
}

private fun chromaForSaturation(saturation: Double, anchors: ChromaAnchors): Double {
    if (saturation < SATURATION_SPLIT) {
        val t = SATURATION_SPLIT_INV * saturation
        val k1 = SATURATION_SPLIT * anchors.c0
        val k2 = 1.0 - k1 / anchors.cMid
        return t * k1 / (1.0 - k2 * t)
    }

    val t = (saturation - SATURATION_SPLIT) / (1.0 - SATURATION_SPLIT)
    val k0 = anchors.cMid
    val k1 = (1.0 - SATURATION_SPLIT) * anchors.cMid * anchors.cMid *
        SATURATION_SPLIT_INV * SATURATION_SPLIT_INV / anchors.c0
    val k2 = 1.0 - k1 / (anchors.cMax - anchors.cMid)
    return k0 + t * k1 / (1.0 - k2 * t)
}

private fun saturationForChroma(chroma: Double, anchors: ChromaAnchors): Double {
    if (chroma < anchors.cMid) {
        val k1 = SATURATION_SPLIT * anchors.c0
        val k2 = 1.0 - k1 / anchors.cMid
        val t = chroma / (k1 + k2 * chroma)
        return t * SATURATION_SPLIT
    }

    val k0 = anchors.cMid
    val k1 = (1.0 - SATURATION_SPLIT) * anchors.cMid * anchors.cMid *
        SATURATION_SPLIT_INV * SATURATION_SPLIT_INV / anchors.c0
    val k2 = 1.0 - k1 / (anchors.cMax - anchors.cMid)
    val t = (chroma - k0) / (k1 + k2 * (chroma - k0))
    return SATURATION_SPLIT + (1.0 - SATURATION_SPLIT) * t
}

/** Converts this Okhsl color to HSL by way of RGB. Alpha is carried over unchanged. */
public fun OkhslColor.toHsl(): HslColor = toRgb().toHsl()

/** Converts this HSL color to Okhsl by way of RGB. Alpha is carried over unchanged. */
public fun HslColor.toOkhsl(): OkhslColor = toRgb().toOkhsl()

/** Packs this Okhsl color into an ARGB [Int] (`0xAARRGGBB`); see [RgbColor.toArgbInt]. */
public fun OkhslColor.toArgbInt(): Int = toRgb().toArgbInt()

/** Unpacks this ARGB [Int] (`0xAARRGGBB`) into an [OkhslColor]; see [Int.toRgbColor]. */
public fun Int.toOkhslColor(): OkhslColor = toRgbColor().toOkhsl()
