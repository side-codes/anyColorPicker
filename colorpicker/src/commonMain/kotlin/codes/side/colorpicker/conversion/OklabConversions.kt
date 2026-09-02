package codes.side.colorpicker.conversion

import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.model.OKLAB_AB_RANGE
import codes.side.colorpicker.model.OklabColor
import codes.side.colorpicker.model.OklchColor
import codes.side.colorpicker.model.RgbColor
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private const val DEGREES_PER_RADIAN = 180.0 / kotlin.math.PI

/** Converts this sRGB color to Oklab. Alpha is carried over unchanged. */
public fun RgbColor.toOklab(): OklabColor {
    val lab = linearSrgbToOklab(
        LinearRgb(
            r = linearize(red.toDouble()),
            g = linearize(green.toDouble()),
            b = linearize(blue.toDouble()),
        ),
    )

    return OklabColor(
        l = lab.l.toFloat().coerceIn(0f, 1f),
        a = lab.a.toFloat().coerceIn(-OKLAB_AB_RANGE, OKLAB_AB_RANGE),
        b = lab.b.toFloat().coerceIn(-OKLAB_AB_RANGE, OKLAB_AB_RANGE),
        alpha = alpha,
    )
}

/**
 * Converts this Oklab color to sRGB. Colors outside the display gamut are mapped by the
 * CSS Color 4 algorithm, which holds lightness and hue and gives up chroma — see
 * [gamutMapToSrgb]. Alpha is carried over unchanged.
 */
public fun OklabColor.toRgb(): RgbColor {
    val linear = gamutMapToSrgb(OkLab(l.toDouble(), a.toDouble(), b.toDouble()))

    return RgbColor(
        red = delinearize(linear.r).toFloat().coerceIn(0f, 1f),
        green = delinearize(linear.g).toFloat().coerceIn(0f, 1f),
        blue = delinearize(linear.b).toFloat().coerceIn(0f, 1f),
        alpha = alpha,
    )
}

/**
 * Converts this Oklab color to its cylindrical form. A neutral color has no meaningful
 * hue angle and reports `0`. Alpha is carried over unchanged.
 */
public fun OklabColor.toOklch(): OklchColor {
    val chroma = hypot(a.toDouble(), b.toDouble())
    val degrees = atan2(b.toDouble(), a.toDouble()) * DEGREES_PER_RADIAN

    return OklchColor(
        l = l,
        chroma = chroma.toFloat().coerceIn(0f, OKLAB_AB_RANGE),
        hue = ((degrees % 360.0 + 360.0) % 360.0).toFloat().coerceIn(0f, 360f),
        alpha = alpha,
    )
}

/** Converts this OkLCh color to its rectangular form. Alpha is carried over unchanged. */
public fun OklchColor.toOklab(): OklabColor {
    val radians = hue.toDouble() / DEGREES_PER_RADIAN
    val chroma = this.chroma.toDouble()

    return OklabColor(
        l = l,
        a = (chroma * cos(radians)).toFloat().coerceIn(-OKLAB_AB_RANGE, OKLAB_AB_RANGE),
        b = (chroma * sin(radians)).toFloat().coerceIn(-OKLAB_AB_RANGE, OKLAB_AB_RANGE),
        alpha = alpha,
    )
}

/** Converts this sRGB color to OkLCh. Alpha is carried over unchanged. */
public fun RgbColor.toOklch(): OklchColor = toOklab().toOklch()

/**
 * Converts this OkLCh color to sRGB, gamut-mapping colors the display cannot show. Alpha
 * is carried over unchanged.
 */
public fun OklchColor.toRgb(): RgbColor = toOklab().toRgb()

/** Converts this Oklab color to HSL by way of RGB. Alpha is carried over unchanged. */
public fun OklabColor.toHsl(): HslColor = toRgb().toHsl()

/** Converts this HSL color to Oklab by way of RGB. Alpha is carried over unchanged. */
public fun HslColor.toOklab(): OklabColor = toRgb().toOklab()

/** Converts this OkLCh color to HSL by way of RGB. Alpha is carried over unchanged. */
public fun OklchColor.toHsl(): HslColor = toRgb().toHsl()

/** Converts this HSL color to OkLCh by way of RGB. Alpha is carried over unchanged. */
public fun HslColor.toOklch(): OklchColor = toRgb().toOklch()

/** Packs this Oklab color into an ARGB [Int] (`0xAARRGGBB`); see [RgbColor.toArgbInt]. */
public fun OklabColor.toArgbInt(): Int = toRgb().toArgbInt()

/** Unpacks this ARGB [Int] (`0xAARRGGBB`) into an [OklabColor]; see [Int.toRgbColor]. */
public fun Int.toOklabColor(): OklabColor = toRgbColor().toOklab()

/** Packs this OkLCh color into an ARGB [Int] (`0xAARRGGBB`); see [RgbColor.toArgbInt]. */
public fun OklchColor.toArgbInt(): Int = toRgb().toArgbInt()

/** Unpacks this ARGB [Int] (`0xAARRGGBB`) into an [OklchColor]; see [Int.toRgbColor]. */
public fun Int.toOklchColor(): OklchColor = toRgbColor().toOklch()
