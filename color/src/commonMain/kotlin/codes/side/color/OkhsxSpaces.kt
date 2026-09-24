package codes.side.color

import codes.side.color.internal.cuspLightness
import codes.side.color.internal.highestLinearSrgb
import codes.side.color.internal.maxChroma
import codes.side.color.internal.maxSaturation
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// The Oklab chroma below which Okhsl's and Okhsv's hue is powerless: OkLCh's threshold.
private const val POWERLESS_CHROMA = 0.000004

/**
 * Okhsl, Björn Ottosson's perceptual HSL over [Oklab], normalized to the sRGB gamut. S and L run
 * 0–1 and s = 1 is as colorful as sRGB allows. Serializes as `color(--okhsl …)`.
 *
 * It describes sRGB and nothing beyond it: its saturation formula has no finite value far outside
 * the gamut. A color converted in from outside sRGB has its chroma reduced to sRGB's edge at the
 * same Oklab lightness and hue, and its lightness held to 0..1. Fitted rather than exact: its
 * mid-chroma anchor is Ottosson's polynomial.
 */
@OptIn(ExperimentalColorSpaceApi::class)
public object Okhsl : ColorSpace(
    "okhsl",
    listOf(
        okHueChannel(),
        ColorChannel("s", 0.0..1.0, gamutBound = 0.0..1.0, limit = 0.0..1.0, analogous = AnalogousCategory.Colorfulness),
        ColorChannel("l", 0.0..1.0, gamutBound = 0.0..1.0, limit = 0.0..1.0, analogous = AnalogousCategory.Lightness),
    ),
    Oklab,
) {
    public val H: ColorChannel get() = channels[0]
    public val S: ColorChannel get() = channels[1]
    public val L: ColorChannel get() = channels[2]

    private const val MID = 0.8
    private const val MID_INVERSE = 1.25

    override val gamut: RgbGamut get() = Srgb.gamut

    override val exactness: Exactness get() = Exactness.Approximate

    override fun toBase(src: DoubleArray, dst: DoubleArray) {
        val hue = src[0]
        val saturation = src[1]
        val lightness = src[2]
        if (lightness >= 1.0 || lightness <= 0.0) {
            dst[0] = if (lightness >= 1.0) 1.0 else 0.0
            dst[1] = 0.0
            dst[2] = 0.0
            return
        }
        val radians = hue * PI / 180.0
        val a = cos(radians)
        val b = sin(radians)
        val l = toeInverse(lightness)
        val sMax = maxSaturation(a, b)
        val lCusp = cuspLightness(a, b, sMax)
        val cMax = maxChroma(l, a, b, sMax, lCusp)
        val c0 = lowChroma(l)
        val cMid = midChroma(l, a, b, sMax, lCusp, cMax)
        val chroma = if (saturation < MID) {
            val t = MID_INVERSE * saturation
            val k1 = MID * c0
            val k2 = 1.0 - k1 / cMid
            t * k1 / (1.0 - k2 * t)
        } else {
            val t = (saturation - MID) / (1.0 - MID)
            val k0 = cMid
            val k1 = (1.0 - MID) * cMid * cMid * MID_INVERSE * MID_INVERSE / c0
            val k2 = 1.0 - k1 / (cMax - cMid)
            k0 + t * k1 / (1.0 - k2 * t)
        }
        dst[0] = l
        dst[1] = chroma * a
        dst[2] = chroma * b
    }

    override fun fromBase(src: DoubleArray, dst: DoubleArray) {
        val l = src[0]
        var chroma = hypot(src[1], src[2])
        var hue = atan2(src[2], src[1]) * 180.0 / PI
        if (hue < 0.0) hue += 360.0
        if (l >= 1.0 || l <= 0.0 || chroma == 0.0) {
            dst[0] = if (chroma == 0.0) 0.0 else hue
            dst[1] = 0.0
            dst[2] = if (l >= 1.0) 1.0 else if (l <= 0.0) 0.0 else toe(l)
            return
        }
        val a = src[1] / chroma
        val b = src[2] / chroma
        val sMax = maxSaturation(a, b)
        val lCusp = cuspLightness(a, b, sMax)
        val cMax = maxChroma(l, a, b, sMax, lCusp)
        val c0 = lowChroma(l)
        val cMid = midChroma(l, a, b, sMax, lCusp, cMax)
        if (chroma > cMax) chroma = cMax
        val saturation = if (chroma < cMid) {
            val k1 = MID * c0
            val k2 = 1.0 - k1 / cMid
            chroma / (k1 + k2 * chroma) * MID
        } else {
            val k0 = cMid
            val k1 = (1.0 - MID) * cMid * cMid * MID_INVERSE * MID_INVERSE / c0
            val k2 = 1.0 - k1 / (cMax - cMid)
            MID + (1.0 - MID) * ((chroma - k0) / (k1 + k2 * (chroma - k0)))
        }
        dst[0] = hue
        dst[1] = saturation.coerceIn(0.0, 1.0)
        dst[2] = toe(l).coerceIn(0.0, 1.0)
    }

    /** The hue is powerless where the Oklab chroma is at or below OkLCh's 0.000004. */
    override fun powerless(components: DoubleArray): Int = okPowerless(this, components)

    public operator fun invoke(h: Double?, s: Double?, l: Double?, alpha: Double? = 1.0): ColorValue =
        colorOf(arrayOf(h, s, l), alpha)

    // C_0 at [l]: a hue-independent soft minimum that sets how fast chroma grows at low saturation.
    private fun lowChroma(l: Double): Double {
        val a = l * 0.4
        val b = (1.0 - l) * 0.8
        return sqrt(1.0 / (1.0 / (a * a) + 1.0 / (b * b)))
    }

    // C_mid at [l] and hue ([a], [b]): Ottosson's fitted S and T for a middling saturation, scaled by
    // how far [cMax] reaches past the triangle through the cusp. The fit is data, not derivable.
    private fun midChroma(l: Double, a: Double, b: Double, sMax: Double, lCusp: Double, cMax: Double): Double {
        val tMax = cuspT(sMax, lCusp)
        val k = cMax / min(l * sMax, (1.0 - l) * tMax)
        val sMid = 0.11516993 + 1.0 / (7.44778970 + 4.15901240 * b + a * (-2.19557347 + 1.75198401 * b +
            a * (-2.13704948 - 10.02301043 * b + a * (-4.24894561 + 5.38770819 * b + 4.69891013 * a))))
        val tMid = 0.11239642 + 1.0 / (1.61320320 - 0.68124379 * b + a * (0.40370612 + 0.90148123 * b +
            a * (-0.27087943 + 0.61223990 * b + a * (0.00299215 - 0.45399568 * b - 0.14661872 * a))))
        val midA = l * sMid
        val midB = (1.0 - l) * tMid
        return 0.9 * k * sqrt(sqrt(1.0 / (1.0 / (midA * midA * midA * midA) + 1.0 / (midB * midB * midB * midB))))
    }
}

/**
 * Okhsv, Björn Ottosson's perceptual HSV over [Oklab], normalized to the sRGB gamut. S and V run
 * 0–1; s = 1 with v = 1 is the most vivid form of a hue. Serializes as `color(--okhsv …)`.
 *
 * Like [Okhsl], it describes sRGB only: a color converted in from outside sRGB has its chroma
 * reduced to sRGB's edge at the same Oklab lightness and hue, and its lightness held to 0..1.
 */
@OptIn(ExperimentalColorSpaceApi::class)
public object Okhsv : ColorSpace(
    "okhsv",
    listOf(
        okHueChannel(),
        ColorChannel("s", 0.0..1.0, gamutBound = 0.0..1.0, limit = 0.0..1.0, analogous = AnalogousCategory.Colorfulness),
        ColorChannel("v", 0.0..1.0, gamutBound = 0.0..1.0, limit = 0.0..1.0),
    ),
    Oklab,
) {
    public val H: ColorChannel get() = channels[0]
    public val S: ColorChannel get() = channels[1]
    public val V: ColorChannel get() = channels[2]

    private const val S0 = 0.5

    override val gamut: RgbGamut get() = Srgb.gamut

    override val exactness: Exactness get() = Exactness.Approximate

    override fun toBase(src: DoubleArray, dst: DoubleArray) {
        val hue = src[0]
        val saturation = src[1]
        val value = src[2]
        if (value <= 0.0) {
            dst[0] = 0.0
            dst[1] = 0.0
            dst[2] = 0.0
            return
        }
        val radians = hue * PI / 180.0
        val a = cos(radians)
        val b = sin(radians)
        val sMax = maxSaturation(a, b)
        val tMax = cuspT(sMax, cuspLightness(a, b, sMax))
        val k = 1.0 - S0 / sMax
        val lv = 1.0 - saturation * S0 / (S0 + tMax - tMax * k * saturation)
        val cv = saturation * tMax * S0 / (S0 + tMax - tMax * k * saturation)
        var l = value * lv
        var c = value * cv
        val lvt = toeInverse(lv)
        val cvt = cv * lvt / lv
        val lNew = toeInverse(l)
        c = c * lNew / l
        l = lNew
        val scaleL = cbrt(1.0 / max(highestLinearSrgb(lvt, a * cvt, b * cvt), 0.0))
        l *= scaleL
        c *= scaleL
        dst[0] = l
        dst[1] = c * a
        dst[2] = c * b
    }

    override fun fromBase(src: DoubleArray, dst: DoubleArray) {
        var l = src[0]
        var chroma = hypot(src[1], src[2])
        var hue = atan2(src[2], src[1]) * 180.0 / PI
        if (hue < 0.0) hue += 360.0
        if (l <= 0.0 || l >= 1.0) {
            dst[0] = if (chroma == 0.0) 0.0 else hue
            dst[1] = 0.0
            dst[2] = if (l <= 0.0) 0.0 else 1.0
            return
        }
        val a = if (chroma == 0.0) 1.0 else src[1] / chroma
        val b = if (chroma == 0.0) 0.0 else src[2] / chroma
        val sMax = maxSaturation(a, b)
        val lCusp = cuspLightness(a, b, sMax)
        val cMax = maxChroma(l, a, b, sMax, lCusp)
        if (chroma > cMax) chroma = cMax
        val tMax = cuspT(sMax, lCusp)
        val k = 1.0 - S0 / sMax
        val t = tMax / (chroma + l * tMax)
        val lv = t * l
        val cv = t * chroma
        val lvt = toeInverse(lv)
        val cvt = cv * lvt / lv
        val scaleL = cbrt(1.0 / max(highestLinearSrgb(lvt, a * cvt, b * cvt), 0.0))
        l = toe(l / scaleL)
        dst[0] = if (chroma == 0.0) 0.0 else hue
        dst[1] = ((S0 + tMax) * cv / (tMax * S0 + tMax * k * cv)).coerceIn(0.0, 1.0)
        dst[2] = (l / lv).coerceIn(0.0, 1.0)
    }

    /** The hue is powerless where the Oklab chroma is at or below OkLCh's 0.000004. */
    override fun powerless(components: DoubleArray): Int = okPowerless(this, components)

    public operator fun invoke(h: Double?, s: Double?, v: Double?, alpha: Double? = 1.0): ColorValue =
        colorOf(arrayOf(h, s, v), alpha)
}

private const val TOE_K1 = 0.206
private const val TOE_K2 = 0.03
private const val TOE_K3 = (1.0 + TOE_K1) / (1.0 + TOE_K2)

// Ottosson's toe: Oklab L to a lightness that spaces greys the way HSL users expect.
private fun toe(x: Double): Double {
    val inner = TOE_K3 * x - TOE_K1
    return 0.5 * (inner + sqrt(inner * inner + 4.0 * TOE_K2 * TOE_K3 * x))
}

private fun toeInverse(x: Double): Double = (x * x + TOE_K1 * x) / (TOE_K3 * (x + TOE_K2))

// The cusp's T = C/(1 − L), from its S = C/L and its lightness.
private fun cuspT(sMax: Double, lCusp: Double): Double = lCusp * sMax / (1.0 - lCusp)

private fun okHueChannel(): ColorChannel = ColorChannel(
    id = "h",
    referenceRange = 0.0..360.0,
    kind = ChannelKind.Hue(HueFamily.Oklab),
    analogous = AnalogousCategory.Hue,
)

@OptIn(ExperimentalColorSpaceApi::class)
private fun okPowerless(space: ColorSpace, components: DoubleArray): Int {
    val lab = DoubleArray(4)
    components.copyInto(lab, 0, 0, 3)
    space.toBase(lab, lab)
    return if (hypot(lab[1], lab[2]) <= POWERLESS_CHROMA) 1 else 0
}
