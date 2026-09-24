package codes.side.colorpicker.conversion

import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.model.LabColor
import codes.side.colorpicker.model.RgbColor
import kotlin.math.cbrt
import kotlin.math.pow

// D50 reference white. CIELAB is quoted at D50 wherever it travels between tools — CSS
// lab(), Photoshop and Compose's own ColorSpaces.CieLab all use it — and reading a D50
// number as D65 costs a median ΔE of 2.7, past the threshold where a difference shows.
//
// The sRGB matrices below carry the Bradford adaptation to D50 folded in rather than
// applied as a separate step. That is what keeps the neutral axis exact: each matrix row
// sums to its own component of this white, so white lands on a* = b* = 0 by construction.
// Adapting as a second step with a matrix rounded against a different D50 leaves the two
// disagreeing in the last digits, and white picks up a visible b* of its own.
private const val XN = 0.96422
private const val YN = 1.0
private const val ZN = 0.82521

// CIE standard constants: EPSILON = (6/29)^3, KAPPA = (29/3)^3.
private const val EPSILON = 216.0 / 24389.0
private const val KAPPA = 24389.0 / 27.0

/**
 * Converts this CIELAB color (D50 reference white) to sRGB. Colors outside the display
 * gamut are mapped by the CSS Color 4 algorithm, which holds lightness and hue and gives
 * up chroma — see [gamutMapToSrgb]. Alpha is carried over unchanged.
 *
 * Only about an eighth of the `L* 0..100`, `a*`/`b* -128..127` box is inside sRGB, so
 * most of the space this type can hold arrives mapped.
 */
public fun LabColor.toRgb(): RgbColor {
    val lD = l.toDouble()
    val aD = a.toDouble()
    val bD = b.toDouble()

    val fy = (lD + 16.0) / 116.0
    val fx = aD / 500.0 + fy
    val fz = fy - bD / 200.0

    val fx3 = fx.pow(3)
    val fz3 = fz.pow(3)

    val xr = if (fx3 > EPSILON) fx3 else (116.0 * fx - 16.0) / KAPPA
    val yr = if (lD > 8.0) fy.pow(3) else lD / KAPPA
    val zr = if (fz3 > EPSILON) fz3 else (116.0 * fz - 16.0) / KAPPA

    return gamutMappedRgbColor(relativeXyzToLinearSrgb(xr, yr, zr), alpha)
}

/**
 * D50 CIE XYZ divided by its white, so the white is `1, 1, 1`, as linear-light sRGB; unclamped.
 *
 * Taken relative so that a caller whose D50 was computed to other digits divides by its own white
 * and still lands a neutral on the neutral axis. Compose derives D50 from its chromaticity and
 * gets 0.964212 and 0.825188 where the matrices below were built against [XN] and [ZN].
 */
internal fun relativeXyzToLinearSrgb(xr: Double, yr: Double, zr: Double): LinearRgb {
    val x = xr * XN
    val y = yr * YN
    val z = zr * ZN
    return LinearRgb(
        r = 3.1338561 * x - 1.6168667 * y - 0.4906146 * z,
        g = -0.9787684 * x + 1.9161415 * y + 0.0334540 * z,
        b = 0.0719453 * x - 0.2289914 * y + 1.4052427 * z,
    )
}

/** Converts this sRGB color to CIELAB (D50 reference white). Alpha is carried over unchanged. */
public fun RgbColor.toLab(): LabColor {
    val rLinear = linearize(red.toDouble())
    val gLinear = linearize(green.toDouble())
    val bLinear = linearize(blue.toDouble())

    val x = 0.4360747 * rLinear + 0.3850649 * gLinear + 0.1430804 * bLinear
    val y = 0.2225045 * rLinear + 0.7168786 * gLinear + 0.0606169 * bLinear
    val z = 0.0139322 * rLinear + 0.0971045 * gLinear + 0.7141733 * bLinear

    val fx = labF(x / XN)
    val fy = labF(y / YN)
    val fz = labF(z / ZN)

    val l = (116.0 * fy - 16.0).toFloat()
    val a = (500.0 * (fx - fy)).toFloat()
    val b = (200.0 * (fy - fz)).toFloat()

    return LabColor(
        l = l.coerceIn(0f, 100f),
        a = a.coerceIn(-128f, 127f),
        b = b.coerceIn(-128f, 127f),
        alpha = alpha,
    )
}

private fun labF(t: Double): Double =
    if (t > EPSILON) cbrt(t) else (KAPPA * t + 16.0) / 116.0

internal fun linearize(c: Double): Double =
    if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

internal fun delinearize(c: Double): Double =
    if (c <= 0.0031308) c * 12.92 else 1.055 * c.pow(1.0 / 2.4) - 0.055

/** Converts this CIELAB color to HSL by way of RGB. Alpha is carried over unchanged. */
public fun LabColor.toHsl(): HslColor = toRgb().toHsl()

/** Converts this HSL color to CIELAB by way of RGB. Alpha is carried over unchanged. */
public fun HslColor.toLab(): LabColor = toRgb().toLab()

/** Packs this CIELAB color into an ARGB [Int] (`0xAARRGGBB`); see [RgbColor.toArgbInt]. */
public fun LabColor.toArgbInt(): Int = toRgb().toArgbInt()

/** Unpacks this ARGB [Int] (`0xAARRGGBB`) into a [LabColor]; see [Int.toRgbColor]. */
public fun Int.toLabColor(): LabColor = toRgbColor().toLab()
