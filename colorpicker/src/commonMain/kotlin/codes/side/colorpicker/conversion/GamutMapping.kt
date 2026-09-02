package codes.side.colorpicker.conversion

import kotlin.math.hypot

/**
 * The CSS Color 4 gamut mapping algorithm: binary search on chroma with local-MINDE
 * clipping. See https://www.w3.org/TR/css-color-4/#binsearch.
 *
 * Two simpler approaches are wrong in opposite directions, which is why this one is
 * neither. Clipping each RGB channel on its own shifts lightness and hue as a side
 * effect — push only a* out of range and all three coordinates come back changed.
 * Reducing chroma until the color fits, holding lightness and hue, fixes that but
 * over-corrects badly on yellows, where the gamut surface curves away from the search
 * line and the color arrives visibly washed out.
 *
 * So chroma is searched down, and at each step the candidate is compared against its own
 * clipped version. Once those two are within a just-noticeable difference, clipping has
 * become perceptually free and the clipped color wins — which keeps most of the chroma
 * that pure reduction would have thrown away.
 */

// Just-noticeable difference in ΔE-OK, and the search's stopping width. Both from the
// spec; JND is what makes the clipped and searched colors interchangeable.
private const val JND = 0.02
private const val EPSILON = 0.0001

/**
 * Maps [origin] to the closest color sRGB can display, in linear light. Colors already
 * inside the gamut pass through untouched.
 */
internal fun gamutMapToSrgb(origin: OkLab): LinearRgb {
    if (origin.l >= 1.0) return LinearRgb(1.0, 1.0, 1.0)
    if (origin.l <= 0.0) return LinearRgb(0.0, 0.0, 0.0)

    val direct = oklabToLinearSrgb(origin)
    if (direct.isInGamut()) return direct.clipToUnit()

    val originChroma = hypot(origin.a, origin.b)
    val aUnit = origin.a / originChroma
    val bUnit = origin.b / originChroma

    var clipped = direct.clipToUnit()
    if (deltaEOk(linearSrgbToOklab(clipped), origin) < JND) return clipped

    var min = 0.0
    var max = originChroma
    var minInGamut = true

    while (max - min > EPSILON) {
        val chroma = (min + max) / 2.0
        val current = OkLab(origin.l, aUnit * chroma, bUnit * chroma)
        val currentRgb = oklabToLinearSrgb(current)

        if (minInGamut && currentRgb.isInGamut()) {
            min = chroma
            continue
        }

        clipped = currentRgb.clipToUnit()
        val error = deltaEOk(linearSrgbToOklab(clipped), current)

        when {
            error >= JND -> max = chroma
            JND - error < EPSILON -> return clipped
            else -> {
                // Close enough that clipping is nearly free, but there is still chroma to
                // recover; keep searching upward with clipping now allowed.
                minInGamut = false
                min = chroma
            }
        }
    }

    return clipped
}
