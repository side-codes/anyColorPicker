package codes.side.color

import codes.side.color.internal.ColorRules
import codes.side.color.internal.LMS_TO_OKLAB
import codes.side.color.internal.OKLAB_TO_LMS
import codes.side.color.internal.chromaWithin
import kotlin.math.cbrt
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * How a color outside an [RgbGamut] is brought inside. [Css] and [ChromaReduction] reduce chroma at
 * constant OkLCh lightness and hue, as CSS Color 4 §13.2 has it, and give white at lightness 1 or
 * more and black at 0 or less; [Clip] clamps each channel.
 */
public abstract class GamutMapping internal constructor() {

    // Maps the Oklab color (l, a, b) into [gamut], leaving linear RGB in out[0..2].
    internal fun map(gamut: RgbGamut, l: Double, a: Double, b: Double, out: DoubleArray) {
        toLinear(gamut.lmsToLinear, l, a, b, out)
        if (!inCube(out)) reduce(gamut, l, a, b, out)
    }

    // As map, for a color whose linear RGB, already in out[0..2], lies outside the cube.
    internal abstract fun reduce(gamut: RgbGamut, l: Double, a: Double, b: Double, out: DoubleArray)

    /**
     * CSS Color 4's binary search with local MINDE, the default, as in CSS and color.js: chroma is
     * halved toward the gamut until the color clipped into it lies within [jnd] ΔEOK of the unclipped
     * one, to [epsilon] in chroma. By design it keeps a little more chroma than the exact edge, up to
     * [jnd] of accuracy traded for staying vivid.
     */
    public class Css(
        public val jnd: Double = ColorRules.GAMUT_MAPPING_JND,
        public val epsilon: Double = ColorRules.GAMUT_MAPPING_EPSILON,
    ) : GamutMapping() {

        init {
            require(jnd >= 0.0 && jnd.isFinite()) { "The JND must be finite and not negative, was $jnd" }
            require(epsilon > 0.0 && epsilon.isFinite()) { "Epsilon must be finite and positive, was $epsilon" }
        }

        override fun reduce(gamut: RgbGamut, l: Double, a: Double, b: Double, out: DoubleArray) {
            if (whiteOrBlack(l, out)) return
            val chroma = hypot(a, b)
            val hueA = a / chroma
            val hueB = b / chroma
            val toLms = gamut.linearToLms
            clamp(out)
            var clippedR = out[0]
            var clippedG = out[1]
            var clippedB = out[2]
            if (deltaEok(toLms, clippedR, clippedG, clippedB, l, a, b) < jnd) return
            var min = 0.0
            var max = chroma
            var minInGamut = true
            while (max - min > epsilon) {
                val current = (min + max) / 2.0
                val currentA = current * hueA
                val currentB = current * hueB
                toLinear(gamut.lmsToLinear, l, currentA, currentB, out)
                if (minInGamut && inCube(out)) {
                    min = current
                    continue
                }
                clamp(out)
                clippedR = out[0]
                clippedG = out[1]
                clippedB = out[2]
                val error = deltaEok(toLms, clippedR, clippedG, clippedB, l, currentA, currentB)
                if (error < jnd) {
                    if (jnd - error < epsilon) break
                    minInGamut = false
                    min = current
                } else {
                    max = current
                }
            }
            // CSS returns the last color it clipped, not the last one it tried.
            out[0] = clippedR
            out[1] = clippedG
            out[2] = clippedB
        }

        override fun equals(other: Any?): Boolean = other is Css && other.jnd == jnd && other.epsilon == epsilon

        override fun hashCode(): Int = 31 * jnd.hashCode() + epsilon.hashCode()

        override fun toString(): String = "Css(jnd = $jnd, epsilon = $epsilon)"
    }

    /**
     * Exact chroma reduction at constant OkLCh lightness and hue: the most chroma the gamut holds
     * there up to the color's own, solved from each channel's cubic, then a clip of the last rounding.
     * The method for planes, gradients and boundaries.
     */
    public object ChromaReduction : GamutMapping() {
        override fun reduce(gamut: RgbGamut, l: Double, a: Double, b: Double, out: DoubleArray) {
            if (whiteOrBlack(l, out)) return
            val chroma = hypot(a, b)
            val hueA = a / chroma
            val hueB = b / chroma
            val kept = chromaWithin(gamut.lmsToLinear, l, hueA, hueB, chroma)
            toLinear(gamut.lmsToLinear, l, kept * hueA, kept * hueB, out)
            clamp(out)
        }

        override fun toString(): String = "ChromaReduction"
    }

    /** Each channel clamped to `0..1` in the gamut's space: fast, and free to shift hue and lightness. */
    public object Clip : GamutMapping() {
        override fun reduce(gamut: RgbGamut, l: Double, a: Double, b: Double, out: DoubleArray) {
            clamp(out)
        }

        override fun toString(): String = "Clip"
    }
}

/**
 * Whether [gamut] holds this color: every channel of it in [gamut]'s space within [tolerance] of
 * `0..1`. The default, 7.5e-5, is color.js's and ColorAide's; CSS leaves it open. Missing components
 * count as 0.
 */
public fun ColorValue.isInGamut(gamut: RgbGamut, tolerance: Double = ColorRules.IN_GAMUT_TOLERANCE): Boolean {
    require(tolerance >= 0.0) { "Tolerance must not be negative, was $tolerance" }
    return to(gamut.space).components().all { it in -tolerance..1.0 + tolerance }
}

/**
 * This color brought into [gamut] by [method], as a color of the gamut's own RGB space. A color the
 * gamut holds comes back converted and nothing else. Missing components count as 0 first, and the
 * result has none, except a missing alpha.
 */
public fun ColorValue.toGamut(gamut: RgbGamut, method: GamutMapping = GamutMapping.Css()): ColorValue {
    val space = gamut.space
    val alphaMask = if (isAlphaMissing) ColorValue.MISSING_ALPHA else 0
    val direct = to(space).components()
    if (direct.all { it in 0.0..1.0 }) return space.color(direct, alpha, alphaMask)
    val lab = to(Oklab).components()
    val linear = DoubleArray(3)
    method.map(gamut, lab[0], lab[1], lab[2], linear)
    return space.color(DoubleArray(3) { space.transfer.encode(linear[it]) }, alpha, alphaMask)
}

// Linear RGB of the Oklab color (l, a, b) through LMS → linear RGB matrix [t], into out[0..2].
internal fun toLinear(t: DoubleArray, l: Double, a: Double, b: Double, out: DoubleArray) {
    val m = OKLAB_TO_LMS
    val lRoot = m[0] * l + m[1] * a + m[2] * b
    val mRoot = m[3] * l + m[4] * a + m[5] * b
    val sRoot = m[6] * l + m[7] * a + m[8] * b
    val lms0 = lRoot * lRoot * lRoot
    val lms1 = mRoot * mRoot * mRoot
    val lms2 = sRoot * sRoot * sRoot
    out[0] = t[0] * lms0 + t[1] * lms1 + t[2] * lms2
    out[1] = t[3] * lms0 + t[4] * lms1 + t[5] * lms2
    out[2] = t[6] * lms0 + t[7] * lms1 + t[8] * lms2
}

internal fun inCube(v: DoubleArray): Boolean = v[0] in 0.0..1.0 && v[1] in 0.0..1.0 && v[2] in 0.0..1.0

private fun clamp(v: DoubleArray) {
    for (i in 0..2) v[i] = v[i].coerceIn(0.0, 1.0)
}

// SDR's ends: lightness 1 or more is white, 0 or less black. True when [l] was one of them.
private fun whiteOrBlack(l: Double, out: DoubleArray): Boolean {
    val end = when {
        l >= 1.0 -> 1.0
        l <= 0.0 -> 0.0
        else -> return false
    }
    out[0] = end
    out[1] = end
    out[2] = end
    return true
}

// ΔEOK between the linear RGB (r, g, b), taken to Oklab through linear RGB → LMS matrix [toLms], and
// the Oklab color (l, a, b).
private fun deltaEok(toLms: DoubleArray, r: Double, g: Double, b: Double, l: Double, a: Double, bb: Double): Double {
    val lms0 = cbrt(toLms[0] * r + toLms[1] * g + toLms[2] * b)
    val lms1 = cbrt(toLms[3] * r + toLms[4] * g + toLms[5] * b)
    val lms2 = cbrt(toLms[6] * r + toLms[7] * g + toLms[8] * b)
    val m = LMS_TO_OKLAB
    val dl = m[0] * lms0 + m[1] * lms1 + m[2] * lms2 - l
    val da = m[3] * lms0 + m[4] * lms1 + m[5] * lms2 - a
    val db = m[6] * lms0 + m[7] * lms1 + m[8] * lms2 - bb
    return sqrt(dl * dl + da * da + db * db)
}
