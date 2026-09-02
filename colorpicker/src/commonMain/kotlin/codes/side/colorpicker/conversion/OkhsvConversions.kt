package codes.side.colorpicker.conversion

import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.model.OkhsvColor
import codes.side.colorpicker.model.RgbColor
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

// The saturation the cusp is normalized against. Okhsv treats the gamut as a triangle
// anchored at this value and then corrects for the real surface's curvature.
private const val S0 = 0.5

private const val DEGREES_PER_RADIAN = 180.0 / kotlin.math.PI

/**
 * Converts this Okhsv color to sRGB. Every Okhsv coordinate is inside the gamut by
 * construction, so nothing is clipped or mapped. Alpha is carried over unchanged.
 */
public fun OkhsvColor.toRgb(): RgbColor {
    if (value <= 0f) return RgbColor(0f, 0f, 0f, alpha)

    val radians = hue.toDouble() / DEGREES_PER_RADIAN
    val aUnit = cos(radians)
    val bUnit = sin(radians)

    val st = findCusp(aUnit, bUnit).toSaturationTint()
    val k = 1.0 - S0 / st.s
    val s = saturation.toDouble()
    val v = value.toDouble()

    // Lightness and chroma at full value, treating the gamut as a triangle.
    val denominator = S0 + st.t - st.t * k * s
    val lAtFullValue = 1.0 - s * S0 / denominator
    val chromaAtFullValue = s * st.t * S0 / denominator

    var lightness = v * lAtFullValue
    var chroma = v * chromaAtFullValue

    // Undo the toe, then rescale so the triangle meets the gamut's actual curved top.
    val lToed = toeInv(lAtFullValue)
    val chromaToed = chromaAtFullValue * lToed / lAtFullValue

    val lightnessToed = toeInv(lightness)
    chroma *= lightnessToed / lightness
    lightness = lightnessToed

    val scaleRgb = oklabToLinearSrgb(OkLab(lToed, aUnit * chromaToed, bUnit * chromaToed))
    val scale = cbrt(1.0 / max(max(scaleRgb.r, scaleRgb.g), max(scaleRgb.b, 0.0)))

    lightness *= scale
    chroma *= scale

    val linear = oklabToLinearSrgb(OkLab(lightness, chroma * aUnit, chroma * bUnit))

    return RgbColor(
        red = delinearize(linear.r).toFloat().coerceIn(0f, 1f),
        green = delinearize(linear.g).toFloat().coerceIn(0f, 1f),
        blue = delinearize(linear.b).toFloat().coerceIn(0f, 1f),
        alpha = alpha,
    )
}

/**
 * Converts this sRGB color to Okhsv. Grays have no hue to report and come back at hue
 * `0` with zero saturation. Alpha is carried over unchanged.
 */
public fun RgbColor.toOkhsv(): OkhsvColor {
    val lab = linearSrgbToOklab(
        LinearRgb(
            r = linearize(red.toDouble()),
            g = linearize(green.toDouble()),
            b = linearize(blue.toDouble()),
        ),
    )

    val chroma = hypot(lab.a, lab.b)
    val gray = OkhsvColor(
        hue = 0f,
        saturation = 0f,
        value = toe(lab.l).toFloat().coerceIn(0f, 1f),
        alpha = alpha,
    )

    // At the black and white poles the construction below divides by quantities that
    // have gone to zero, so grays are answered before that can happen.
    if (chroma < ACHROMATIC_CHROMA || lab.l <= 0.0 || lab.l >= 1.0) return gray

    val aUnit = lab.a / chroma
    val bUnit = lab.b / chroma
    val degrees = atan2(lab.b, lab.a) * DEGREES_PER_RADIAN

    val st = findCusp(aUnit, bUnit).toSaturationTint()
    val k = 1.0 - S0 / st.s

    val t = st.t / (chroma + lab.l * st.t)
    val lAtFullValue = t * lab.l
    val chromaAtFullValue = t * chroma

    val lToed = toeInv(lAtFullValue)
    val chromaToed = chromaAtFullValue * lToed / lAtFullValue

    val scaleRgb = oklabToLinearSrgb(OkLab(lToed, aUnit * chromaToed, bUnit * chromaToed))
    val scale = cbrt(1.0 / max(max(scaleRgb.r, scaleRgb.g), max(scaleRgb.b, 0.0)))

    val lightness = lab.l / scale
    val value = toe(lightness) / lAtFullValue
    // Saturation is read off the full-value chroma, not the rescaled one: the scaling
    // above exists to place the color on the gamut's curved top, and folding it in here
    // would count that correction twice.
    val saturation = (S0 + st.t) * chromaAtFullValue /
        (st.t * S0 + st.t * k * chromaAtFullValue)

    // The construction collapses at the black pole, where neither saturation nor value
    // has a value to take; the arithmetic goes non-finite rather than wrong.
    if (!saturation.isFinite() || !value.isFinite()) return gray

    return OkhsvColor(
        hue = ((degrees % 360.0 + 360.0) % 360.0).toFloat().coerceIn(0f, 360f),
        saturation = saturation.toFloat().coerceIn(0f, 1f),
        value = value.toFloat().coerceIn(0f, 1f),
        alpha = alpha,
    )
}

/** Converts this Okhsv color to HSL by way of RGB. Alpha is carried over unchanged. */
public fun OkhsvColor.toHsl(): HslColor = toRgb().toHsl()

/** Converts this HSL color to Okhsv by way of RGB. Alpha is carried over unchanged. */
public fun HslColor.toOkhsv(): OkhsvColor = toRgb().toOkhsv()

/** Packs this Okhsv color into an ARGB [Int] (`0xAARRGGBB`); see [RgbColor.toArgbInt]. */
public fun OkhsvColor.toArgbInt(): Int = toRgb().toArgbInt()

/** Unpacks this ARGB [Int] (`0xAARRGGBB`) into an [OkhsvColor]; see [Int.toRgbColor]. */
public fun Int.toOkhsvColor(): OkhsvColor = toRgbColor().toOkhsv()
