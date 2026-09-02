package codes.side.colorpicker.conversion

import kotlin.math.cbrt
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * The Oklab machinery shared by the Ok* color spaces: the Oklab transform itself, and
 * the sRGB gamut boundary that Okhsl, Okhsv and gamut mapping are all defined against.
 *
 * Transcribed from Björn Ottosson's reference implementation (`ok_color.h`, MIT), with
 * the arithmetic widened to [Double]. The polynomial coefficients below were produced by
 * an optimization process against the sRGB gamut and are not derivable by hand — treat
 * them as data, not as expressions to simplify.
 *
 * See https://bottosson.github.io/posts/oklab/ and
 * https://bottosson.github.io/posts/gamutclipping/.
 */

/** A color in Oklab, in the space's own `0..1` lightness scale. */
internal data class OkLab(val l: Double, val a: Double, val b: Double)

/** A color in linear-light sRGB, unclamped so out-of-gamut values survive. */
internal data class LinearRgb(val r: Double, val g: Double, val b: Double)

/** The lightness and chroma of a hue's cusp — its most colorful point in sRGB. */
internal data class Cusp(val l: Double, val c: Double)

/** The cusp expressed as saturation and "tint" gradients, per Ottosson's `to_ST`. */
internal data class SaturationTint(val s: Double, val t: Double)

/** The three chroma anchors Okhsl interpolates between at a given lightness. */
internal data class ChromaAnchors(val c0: Double, val cMid: Double, val cMax: Double)

internal fun linearSrgbToOklab(c: LinearRgb): OkLab {
    val l = 0.4122214708 * c.r + 0.5363325363 * c.g + 0.0514459929 * c.b
    val m = 0.2119034982 * c.r + 0.6806995451 * c.g + 0.1073969566 * c.b
    val s = 0.0883024619 * c.r + 0.2817188376 * c.g + 0.6299787005 * c.b

    val lRoot = cbrt(l)
    val mRoot = cbrt(m)
    val sRoot = cbrt(s)

    return OkLab(
        l = 0.2104542553 * lRoot + 0.7936177850 * mRoot - 0.0040720468 * sRoot,
        a = 1.9779984951 * lRoot - 2.4285922050 * mRoot + 0.4505937099 * sRoot,
        b = 0.0259040371 * lRoot + 0.7827717662 * mRoot - 0.8086757660 * sRoot,
    )
}

internal fun oklabToLinearSrgb(c: OkLab): LinearRgb {
    val lRoot = c.l + 0.3963377774 * c.a + 0.2158037573 * c.b
    val mRoot = c.l - 0.1055613458 * c.a - 0.0638541728 * c.b
    val sRoot = c.l - 0.0894841775 * c.a - 1.2914855480 * c.b

    val l = lRoot * lRoot * lRoot
    val m = mRoot * mRoot * mRoot
    val s = sRoot * sRoot * sRoot

    return LinearRgb(
        r = 4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
        g = -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
        b = -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s,
    )
}

// Okhsl's lightness is Oklab's L passed through this curve, which stretches the dark end
// so that mid-gray lands near 0.5 the way HSL users expect.
private const val TOE_K1 = 0.206
private const val TOE_K2 = 0.03
private const val TOE_K3 = (1.0 + TOE_K1) / (1.0 + TOE_K2)

internal fun toe(x: Double): Double {
    val inner = TOE_K3 * x - TOE_K1
    return 0.5 * (inner + sqrt(inner * inner + 4.0 * TOE_K2 * TOE_K3 * x))
}

internal fun toeInv(x: Double): Double = (x * x + TOE_K1 * x) / (TOE_K3 * (x + TOE_K2))

/**
 * The saturation at which the hue `(aNorm, bNorm)` leaves the sRGB gamut, for the
 * channel that goes out first. A polynomial approximation refined by one Halley step,
 * which lands within a rounding error of the true boundary.
 */
internal fun computeMaxSaturation(aNorm: Double, bNorm: Double): Double {
    val k0: Double
    val k1: Double
    val k2: Double
    val k3: Double
    val k4: Double
    val wl: Double
    val wm: Double
    val ws: Double

    if (-1.88170328 * aNorm - 0.80936493 * bNorm > 1.0) {
        // Red channel leaves the gamut first.
        k0 = 1.19086277; k1 = 1.76576728; k2 = 0.59662641; k3 = 0.75515197; k4 = 0.56771245
        wl = 4.0767416621; wm = -3.3077115913; ws = 0.2309699292
    } else if (1.81444104 * aNorm - 1.19445276 * bNorm > 1.0) {
        // Green channel leaves the gamut first.
        k0 = 0.73956515; k1 = -0.45954404; k2 = 0.08285427; k3 = 0.12541070; k4 = 0.14503204
        wl = -1.2684380046; wm = 2.6097574011; ws = -0.3413193965
    } else {
        // Blue channel leaves the gamut first.
        k0 = 1.35733652; k1 = -0.00915799; k2 = -1.15130210; k3 = -0.50559606; k4 = 0.00692167
        wl = -0.0041960863; wm = -0.7034186147; ws = 1.7076147010
    }

    val approx = k0 + k1 * aNorm + k2 * bNorm + k3 * aNorm * aNorm + k4 * aNorm * bNorm

    val kL = 0.3963377774 * aNorm + 0.2158037573 * bNorm
    val kM = -0.1055613458 * aNorm - 0.0638541728 * bNorm
    val kS = -0.0894841775 * aNorm - 1.2914855480 * bNorm

    val lRoot = 1.0 + approx * kL
    val mRoot = 1.0 + approx * kM
    val sRoot = 1.0 + approx * kS

    val l = lRoot * lRoot * lRoot
    val m = mRoot * mRoot * mRoot
    val s = sRoot * sRoot * sRoot

    val lds = 3.0 * kL * lRoot * lRoot
    val mds = 3.0 * kM * mRoot * mRoot
    val sds = 3.0 * kS * sRoot * sRoot

    val lds2 = 6.0 * kL * kL * lRoot
    val mds2 = 6.0 * kM * kM * mRoot
    val sds2 = 6.0 * kS * kS * sRoot

    val f = wl * l + wm * m + ws * s
    val f1 = wl * lds + wm * mds + ws * sds
    val f2 = wl * lds2 + wm * mds2 + ws * sds2

    return approx - f * f1 / (f1 * f1 - 0.5 * f * f2)
}

/** The most colorful point of a hue in sRGB, where two channels are at their limit. */
internal fun findCusp(aNorm: Double, bNorm: Double): Cusp {
    val sCusp = computeMaxSaturation(aNorm, bNorm)
    val rgbAtMax = oklabToLinearSrgb(OkLab(1.0, sCusp * aNorm, sCusp * bNorm))
    val lCusp = cbrt(1.0 / max(max(rgbAtMax.r, rgbAtMax.g), rgbAtMax.b))
    return Cusp(l = lCusp, c = lCusp * sCusp)
}

internal fun Cusp.toSaturationTint(): SaturationTint = SaturationTint(s = c / l, t = c / (1.0 - l))

/**
 * How far along the segment from `(l0, 0)` to `(l1, c1)` the sRGB gamut boundary lies,
 * as a fraction in `0..1`. Below the cusp the boundary is a straight line and the answer
 * is exact; above it the surface curves, so the linear guess is refined by one Halley
 * step per channel and the nearest crossing wins.
 */
internal fun findGamutIntersection(
    aNorm: Double,
    bNorm: Double,
    l1: Double,
    c1: Double,
    l0: Double,
    cusp: Cusp,
): Double {
    if ((l1 - l0) * cusp.c - (cusp.l - l0) * c1 <= 0.0) {
        return cusp.c * l0 / (c1 * cusp.l + cusp.c * (l0 - l1))
    }

    var t = cusp.c * (l0 - 1.0) / (c1 * (cusp.l - 1.0) + cusp.c * (l0 - l1))

    val dL = l1 - l0
    val dC = c1
    val kL = 0.3963377774 * aNorm + 0.2158037573 * bNorm
    val kM = -0.1055613458 * aNorm - 0.0638541728 * bNorm
    val kS = -0.0894841775 * aNorm - 1.2914855480 * bNorm

    val lDt = dL + dC * kL
    val mDt = dL + dC * kM
    val sDt = dL + dC * kS

    val l = l0 * (1.0 - t) + t * l1
    val c = t * c1

    val lRoot = l + c * kL
    val mRoot = l + c * kM
    val sRoot = l + c * kS

    val lCubed = lRoot * lRoot * lRoot
    val mCubed = mRoot * mRoot * mRoot
    val sCubed = sRoot * sRoot * sRoot

    val ldt = 3.0 * lDt * lRoot * lRoot
    val mdt = 3.0 * mDt * mRoot * mRoot
    val sdt = 3.0 * sDt * sRoot * sRoot

    val ldt2 = 6.0 * lDt * lDt * lRoot
    val mdt2 = 6.0 * mDt * mDt * mRoot
    val sdt2 = 6.0 * sDt * sDt * sRoot

    val r = 4.0767416621 * lCubed - 3.3077115913 * mCubed + 0.2309699292 * sCubed - 1.0
    val r1 = 4.0767416621 * ldt - 3.3077115913 * mdt + 0.2309699292 * sdt
    val r2 = 4.0767416621 * ldt2 - 3.3077115913 * mdt2 + 0.2309699292 * sdt2
    val uR = r1 / (r1 * r1 - 0.5 * r * r2)
    val tR = if (uR >= 0.0) -r * uR else Double.MAX_VALUE

    val g = -1.2684380046 * lCubed + 2.6097574011 * mCubed - 0.3413193965 * sCubed - 1.0
    val g1 = -1.2684380046 * ldt + 2.6097574011 * mdt - 0.3413193965 * sdt
    val g2 = -1.2684380046 * ldt2 + 2.6097574011 * mdt2 - 0.3413193965 * sdt2
    val uG = g1 / (g1 * g1 - 0.5 * g * g2)
    val tG = if (uG >= 0.0) -g * uG else Double.MAX_VALUE

    val b = -0.0041960863 * lCubed - 0.7034186147 * mCubed + 1.7076147010 * sCubed - 1.0
    val b1 = -0.0041960863 * ldt - 0.7034186147 * mdt + 1.7076147010 * sdt
    val b2 = -0.0041960863 * ldt2 - 0.7034186147 * mdt2 + 1.7076147010 * sdt2
    val uB = b1 / (b1 * b1 - 0.5 * b * b2)
    val tB = if (uB >= 0.0) -b * uB else Double.MAX_VALUE

    t += min(tR, min(tG, tB))
    return t
}

/**
 * A smooth approximation of the cusp's location. Deliberately biased low — `S_mid` stays
 * under `S_max` and `T_mid` under `T_max` — so Okhsl's interpolation never overshoots the
 * gamut.
 */
internal fun getSaturationTintMid(aNorm: Double, bNorm: Double): SaturationTint {
    val s = 0.11516993 + 1.0 / (
        7.44778970 + 4.15901240 * bNorm +
            aNorm * (
                -2.19557347 + 1.75198401 * bNorm +
                    aNorm * (
                        -2.13704948 - 10.02301043 * bNorm +
                            aNorm * (-4.24894561 + 5.38770819 * bNorm + 4.69891013 * aNorm)
                        )
                )
        )

    val t = 0.11239642 + 1.0 / (
        1.61320320 - 0.68124379 * bNorm +
            aNorm * (
                0.40370612 + 0.90148123 * bNorm +
                    aNorm * (
                        -0.27087943 + 0.61223990 * bNorm +
                            aNorm * (0.00299215 - 0.45399568 * bNorm - 0.14661872 * aNorm)
                        )
                )
        )

    return SaturationTint(s = s, t = t)
}

/**
 * The chroma at three reference saturations for a given lightness and hue: neutral-ish
 * `c0`, the smooth midpoint `cMid`, and the gamut boundary `cMax`. Okhsl's saturation is
 * a two-piece interpolation between them.
 */
internal fun getChromaAnchors(l: Double, aNorm: Double, bNorm: Double): ChromaAnchors {
    val cusp = findCusp(aNorm, bNorm)
    val cMax = findGamutIntersection(aNorm, bNorm, l, 1.0, l, cusp)
    val stMax = cusp.toSaturationTint()

    // Compensates for the curvature of the gamut surface, which the triangle below ignores.
    val k = cMax / min(l * stMax.s, (1.0 - l) * stMax.t)

    val stMid = getSaturationTintMid(aNorm, bNorm)
    val midA = l * stMid.s
    val midB = (1.0 - l) * stMid.t
    // A soft minimum rather than a sharp triangle corner, so chroma varies smoothly.
    val cMid = 0.9 * k * sqrt(sqrt(1.0 / (1.0 / (midA * midA * midA * midA) + 1.0 / (midB * midB * midB * midB))))

    // The c0 shape is hue-independent, so these stand in for the average S and T.
    val zeroA = l * 0.4
    val zeroB = (1.0 - l) * 0.8
    val c0 = sqrt(1.0 / (1.0 / (zeroA * zeroA) + 1.0 / (zeroB * zeroB)))

    return ChromaAnchors(c0 = c0, cMid = cMid, cMax = cMax)
}

/**
 * Below this chroma a color is a gray for every purpose that matters: it is five orders
 * of magnitude under a just-noticeable difference, and it is where the Okhsl and Okhsv
 * constructions divide by quantities that have gone to zero.
 */
internal const val ACHROMATIC_CHROMA = 1e-6

/** True when every channel of [rgb] is within `0..1`, allowing for float slop. */
internal fun LinearRgb.isInGamut(epsilon: Double = 1e-6): Boolean =
    r >= -epsilon && r <= 1.0 + epsilon &&
        g >= -epsilon && g <= 1.0 + epsilon &&
        b >= -epsilon && b <= 1.0 + epsilon

/** Perceptual distance between two Oklab colors, the ΔE the CSS gamut mapping uses. */
internal fun deltaEOk(first: OkLab, second: OkLab): Double {
    val dL = first.l - second.l
    val dA = first.a - second.a
    val dB = first.b - second.b
    return sqrt(dL * dL + dA * dA + dB * dB)
}

internal fun LinearRgb.clipToUnit(): LinearRgb = LinearRgb(
    r = r.coerceIn(0.0, 1.0),
    g = g.coerceIn(0.0, 1.0),
    b = b.coerceIn(0.0, 1.0),
)
