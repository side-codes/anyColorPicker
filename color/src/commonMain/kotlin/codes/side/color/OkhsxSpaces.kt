package codes.side.color

import codes.side.color.internal.ColorRules
import codes.side.color.internal.LMS_TO_SRGB_LINEAR
import codes.side.color.internal.gamutMemo
import codes.side.color.internal.highestLinear
import codes.side.color.internal.maxChroma
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Okhsl, Björn Ottosson's perceptual HSL over [Oklab], normalized to the sRGB gamut. S and L run
 * 0–1 and s = 1 is as colorful as sRGB allows. Serializes as `color(--okhsl …)`.
 *
 * It describes sRGB and nothing beyond it: its saturation formula has no finite value far outside
 * the gamut. A color converted in from outside sRGB has its chroma reduced to sRGB's edge at the
 * same Oklab lightness and hue, and its lightness held to 0..1. Fitted rather than exact: its
 * mid-chroma anchor is Ottosson's polynomial.
 *
 * Just past pure blue's hue, 264.05–264.21°, sRGB's chroma at some lightnesses has a gap, a sliver
 * outside sRGB between two stretches inside it. The edge the chroma is reduced to is the outer one,
 * so a color in the sliver keeps its chroma, and a saturation just below 1 there lands in it, by
 * under a thousandth of a linear channel.
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
        val memo = gamutMemo().hue(hue)
        val a = memo.hueCos
        val b = memo.hueSin
        if (!memo.hasOkhslRow(lightness, a, b)) {
            val l = toeInverse(lightness)
            val cusp = memo.cusp(LMS_TO_SRGB_LINEAR, a, b)
            val sMax = cusp.cuspSaturation
            val lCusp = cusp.cuspLightness
            val cMax = maxChroma(LMS_TO_SRGB_LINEAR, l, a, b, sMax, lCusp)
            memo.rememberOkhslRow(lightness, a, b, l, lowChroma(l), midChroma(l, a, b, sMax, lCusp, cMax), cMax)
        }
        val l = memo.okhslL
        val c0 = memo.okhslC0
        val cMid = memo.okhslCMid
        val cMax = memo.okhslCMax
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
        val cusp = gamutMemo().cusp(LMS_TO_SRGB_LINEAR, a, b)
        val sMax = cusp.cuspSaturation
        val lCusp = cusp.cuspLightness
        val cMax = maxChroma(LMS_TO_SRGB_LINEAR, l, a, b, sMax, lCusp)
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

    override val powerlessFromBase: Boolean get() = true

    override fun powerlessOfBase(base: DoubleArray): Int = okPowerlessOfLab(base)

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
 *
 * Just past pure blue's hue, 264.05–264.21°, its square reaches a little past sRGB, by under a
 * thousandth of a linear channel: over the sliver there, and past the outer edge near the cusp. A
 * color it puts there converts back to the S and V it came from.
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

    // How far past 0..1 a color's own S or V may land and still be inside the square: what the round trip rounds off.
    private const val SQUARE_ROUNDING = 1e-9

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
        val memo = gamutMemo().hue(hue)
        val a = memo.hueCos
        val b = memo.hueSin
        val cusp = memo.cusp(LMS_TO_SRGB_LINEAR, a, b)
        val sMax = cusp.cuspSaturation
        val tMax = cuspT(sMax, cusp.cuspLightness)
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
        val scaleL = cbrt(1.0 / max(highestLinear(LMS_TO_SRGB_LINEAR, lvt, a * cvt, b * cvt), 0.0))
        l *= scaleL
        c *= scaleL
        dst[0] = l
        dst[1] = c * a
        dst[2] = c * b
    }

    override fun fromBase(src: DoubleArray, dst: DoubleArray) {
        val l = src[0]
        val chroma = hypot(src[1], src[2])
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
        val cusp = gamutMemo().cusp(LMS_TO_SRGB_LINEAR, a, b)
        val sMax = cusp.cuspSaturation
        val lCusp = cusp.cuspLightness
        dst[0] = if (chroma == 0.0) 0.0 else hue
        // The model's own inverse first, and the chroma drawn in to sRGB's edge only for a color outside its square,
        // give or take the rounding at the square's own edges. Just past pure blue's hue the square reaches a little
        // past sRGB's outer edge, and a color it put there must come back as it went.
        saturationAndValue(l, chroma, a, b, sMax, lCusp, dst)
        if (dst[1] !in -SQUARE_ROUNDING..1.0 + SQUARE_ROUNDING || dst[2] !in -SQUARE_ROUNDING..1.0 + SQUARE_ROUNDING) {
            val cMax = maxChroma(LMS_TO_SRGB_LINEAR, l, a, b, sMax, lCusp)
            if (chroma > cMax) saturationAndValue(l, cMax, a, b, sMax, lCusp, dst)
        }
        dst[1] = dst[1].coerceIn(0.0, 1.0)
        dst[2] = dst[2].coerceIn(0.0, 1.0)
    }

    // S and V at Oklab lightness [l] and [chroma] along hue ([a], [b]), whose cusp is [sMax] at [lCusp], into dst[1]
    // and dst[2], outside 0..1 for a color outside the model's square.
    private fun saturationAndValue(l: Double, chroma: Double, a: Double, b: Double, sMax: Double, lCusp: Double, dst: DoubleArray) {
        val tMax = cuspT(sMax, lCusp)
        val k = 1.0 - S0 / sMax
        val t = tMax / (chroma + l * tMax)
        val lv = t * l
        val cv = t * chroma
        val lvt = toeInverse(lv)
        val cvt = cv * lvt / lv
        val scaleL = cbrt(1.0 / max(highestLinear(LMS_TO_SRGB_LINEAR, lvt, a * cvt, b * cvt), 0.0))
        dst[1] = (S0 + tMax) * cv / (tMax * S0 + tMax * k * cv)
        dst[2] = toe(l / scaleL) / lv
    }

    /** The hue is powerless where the Oklab chroma is at or below OkLCh's 0.000004. */
    override fun powerless(components: DoubleArray): Int = okPowerless(this, components)

    override val powerlessFromBase: Boolean get() = true

    override fun powerlessOfBase(base: DoubleArray): Int = okPowerlessOfLab(base)

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

@OptIn(ExperimentalColorSpaceApi::class)
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
    return okPowerlessOfLab(lab)
}

// Where OkLCh's hue would be powerless: Oklab chroma at or below OkLCh's threshold.
private fun okPowerlessOfLab(lab: DoubleArray): Int = if (hypot(lab[1], lab[2]) <= ColorRules.OKLCH_POWERLESS_CHROMA) 1 else 0
