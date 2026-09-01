package codes.side.colorpicker.conversion

import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.model.LabColor
import codes.side.colorpicker.model.RgbColor
import kotlin.math.cbrt
import kotlin.math.pow

// D65 reference white matching the sRGB conversion matrices below.
private const val XN = 0.95047
private const val YN = 1.0
private const val ZN = 1.08883

// CIE standard constants: EPSILON = (6/29)^3, KAPPA = (29/3)^3.
private const val EPSILON = 216.0 / 24389.0
private const val KAPPA = 24389.0 / 27.0

/**
 * Converts this CIELAB color (D65 reference white) to sRGB. Out-of-gamut results are
 * clamped channel-wise into `0..1`. Alpha is carried over unchanged.
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

    val x = xr * XN
    val y = yr * YN
    val z = zr * ZN

    val rLinear = 3.2404542 * x - 1.5371385 * y - 0.4985314 * z
    val gLinear = -0.9692660 * x + 1.8760108 * y + 0.0415560 * z
    val bLinear = 0.0556434 * x - 0.2040259 * y + 1.0572252 * z

    return RgbColor(
        red = delinearize(rLinear).toFloat().coerceIn(0f, 1f),
        green = delinearize(gLinear).toFloat().coerceIn(0f, 1f),
        blue = delinearize(bLinear).toFloat().coerceIn(0f, 1f),
        alpha = alpha,
    )
}

/** Converts this sRGB color to CIELAB (D65 reference white). Alpha is carried over unchanged. */
public fun RgbColor.toLab(): LabColor {
    val rLinear = linearize(red.toDouble())
    val gLinear = linearize(green.toDouble())
    val bLinear = linearize(blue.toDouble())

    val x = 0.4124564 * rLinear + 0.3575761 * gLinear + 0.1804375 * bLinear
    val y = 0.2126729 * rLinear + 0.7151522 * gLinear + 0.0721750 * bLinear
    val z = 0.0193339 * rLinear + 0.1191920 * gLinear + 0.9503041 * bLinear

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

private fun delinearize(c: Double): Double =
    if (c <= 0.0031308) c * 12.92 else 1.055 * c.pow(1.0 / 2.4) - 0.055

/** Converts this CIELAB color to HSL by way of RGB. Alpha is carried over unchanged. */
public fun LabColor.toHsl(): HslColor = toRgb().toHsl()

/** Converts this HSL color to CIELAB by way of RGB. Alpha is carried over unchanged. */
public fun HslColor.toLab(): LabColor = toRgb().toLab()

/** Packs this CIELAB color into an ARGB [Int] (`0xAARRGGBB`); see [RgbColor.toArgbInt]. */
public fun LabColor.toArgbInt(): Int = toRgb().toArgbInt()

/** Unpacks this ARGB [Int] (`0xAARRGGBB`) into a [LabColor]; see [Int.toRgbColor]. */
public fun Int.toLabColor(): LabColor = toRgbColor().toLab()
