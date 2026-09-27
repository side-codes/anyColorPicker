package codes.side.color.internal

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// An RGB gamut's shape in Oklab, from nothing but its LMS → linear RGB matrix [t]: the cusp and the
// edge, solved from the channels' cubics. Along a line of constant hue each linear channel is a
// cubic in chroma, so the edge has a closed form. Ottosson's reference code approximates both for
// sRGB (a fitted polynomial per channel region for the cusp, one Halley step for each), which leaves
// its cusp up to 0.015 off in S just below pure blue's hue and its edge low enough near yellow that
// sRGB colors read as Okhsl s > 1. Everything here is scalar, because bulk conversions allocate
// nothing per color.

// How far outside 0..1 a linear channel may land and still count as on the gamut's edge: the
// rounding a polished root leaves, not a gamut margin.
private const val EDGE_TOLERANCE = 1e-9

private const val HALF_SQRT3 = 0.8660254037844386

/** The highest linear channel of an Oklab color, through LMS → linear RGB matrix [t]. */
internal fun highestLinear(t: DoubleArray, l: Double, a: Double, b: Double): Double {
    val m = OKLAB_TO_LMS
    val lRoot = m[0] * l + m[1] * a + m[2] * b
    val mRoot = m[3] * l + m[4] * a + m[5] * b
    val sRoot = m[6] * l + m[7] * a + m[8] * b
    val lms0 = lRoot * lRoot * lRoot
    val lms1 = mRoot * mRoot * mRoot
    val lms2 = sRoot * sRoot * sRoot
    return max(t[0] * lms0 + t[1] * lms1 + t[2] * lms2, max(t[3] * lms0 + t[4] * lms1 + t[5] * lms2, t[6] * lms0 + t[7] * lms1 + t[8] * lms2))
}

/**
 * The largest S = C/L at hue ([a], [b]), a unit vector, that the gamut of [t] still holds: the
 * largest root of a channel reaching 0 along (1, S·a, S·b) at which no channel is negative. Scaling a
 * color toward black keeps every channel's sign, so the whole ray from black at that S is the
 * gamut's lower edge.
 *
 * Not the smallest root: just above pure blue's hue (264.05–264.21° in sRGB) red dips below zero and
 * comes back before green reaches it, so the cusp ends a second in-gamut stretch. At pure blue's own
 * hue that stretch shrinks to one point, pure blue, which only the tolerance keeps from rounding
 * away.
 */
internal fun maxSaturation(t: DoubleArray, a: Double, b: Double): Double =
    largestEdge(t, 1.0, a, b, withCeiling = false, limit = Double.POSITIVE_INFINITY)

/** The cusp's Oklab lightness at hue ([a], [b]), given its S from [maxSaturation]. */
internal fun cuspLightness(t: DoubleArray, a: Double, b: Double, saturation: Double): Double =
    cbrt(1.0 / highestLinear(t, 1.0, saturation * a, saturation * b))

/**
 * The largest chroma the gamut of [t] holds at Oklab lightness [l], strictly between 0 and 1, and
 * hue ([a], [b]), whose cusp is [sMax] from [maxSaturation] at [lCusp] from [cuspLightness].
 *
 * Up to the cusp's lightness that is l·sMax exactly: along a ray from black every channel scales by
 * L³, so the ray at sMax stays inside until its highest channel reaches 1, at the cusp. Above it,
 * the edge is the largest root of a channel reaching 0 or 1 along (l, C·a, C·b) at which every
 * channel is in 0..1; past pure blue's hue the line can cross a sliver outside sRGB and come back
 * in, and the edge is where it leaves for good.
 */
internal fun maxChroma(t: DoubleArray, l: Double, a: Double, b: Double, sMax: Double, lCusp: Double): Double =
    if (l <= lCusp) l * sMax else gamutMemo().outerEdge(t, l, a, b)

/**
 * The largest chroma up to [chroma] that the gamut of [t] holds at Oklab lightness [l], strictly
 * between 0 and 1, and hue ([a], [b]): [chroma] itself when the color is inside, else the nearest
 * edge below it. Inside the sliver past pure blue that is the edge before the sliver, not the one
 * beyond it. [iterative] finds the edges by [walkInward] rather than the closed forms. The two agree
 * to 1e-12, except near black, where the channels are about as small as EDGE_TOLERANCE, so two can
 * reach zero within it of each other and either may be stopped on; the colors then differ by what the
 * tolerance allows.
 */
internal fun chromaWithin(t: DoubleArray, l: Double, a: Double, b: Double, chroma: Double, iterative: Boolean = false): Double {
    if (inside(t, l, chroma * a, chroma * b, 1.0 + EDGE_TOLERANCE)) return chroma
    // At or past the outer edge, the nearest edge below is the outer edge itself, which is what the search would
    // find. Only a chroma inside the sliver, short of the outer edge, needs the search for the edge before it.
    val outer = gamutMemo().outerEdge(t, l, a, b, iterative)
    return when {
        chroma >= outer -> outer
        iterative -> walkInward(t, l, a, b, chroma)
        else -> largestEdge(t, l, a, b, withCeiling = true, limit = chroma)
    }
}

/** This thread's [GamutMemo]. */
internal expect fun gamutMemo(): GamutMemo

/**
 * One thread's last cusp, last few outer edges, last hue's cosine and sine, and last Okhsl row. A plane or a gradient
 * asks the same question once per color, and the answer depends on the matrix, the hue and, for an edge or a row, the
 * lightness alone. Keyed by the exact bits asked with, so a remembered answer is the one a fresh solve gives. Each
 * thread keeps its own and overwrites it in place, so remembering allocates nothing, and no thread reads an entry
 * another is halfway through writing.
 */
internal class GamutMemo {
    private var cuspT: DoubleArray? = null
    private var cuspA = 0.0
    private var cuspB = 0.0

    /** The S of the cusp last asked for by [cusp]. */
    var cuspSaturation: Double = 0.0
        private set

    /** The Oklab lightness of the cusp last asked for by [cusp]. */
    var cuspLightness: Double = 0.0
        private set

    private var trigHue = Double.NaN

    /** The cosine of the hue last asked for by [hue]. */
    var hueCos: Double = Double.NaN
        private set

    /** The sine of the hue last asked for by [hue]. */
    var hueSin: Double = Double.NaN
        private set

    /** The cosine and sine of [degrees], into [hueCos] and [hueSin]. */
    fun hue(degrees: Double): GamutMemo {
        if (!trigHue.sameBits(degrees)) {
            val radians = degrees * PI / 180.0
            hueCos = cos(radians)
            hueSin = sin(radians)
            trigHue = degrees
        }
        return this
    }

    private var okhslRowKnown = false
    private var okhslRowLightness = 0.0
    private var okhslRowA = 0.0
    private var okhslRowB = 0.0

    /** Oklab lightness of the Okhsl row last remembered. */
    var okhslL: Double = 0.0
        private set

    /** Okhsl's C_0 of the row last remembered. */
    var okhslC0: Double = 0.0
        private set

    /** Okhsl's C_mid of the row last remembered. */
    var okhslCMid: Double = 0.0
        private set

    /** Okhsl's C_max of the row last remembered. */
    var okhslCMax: Double = 0.0
        private set

    /** Whether the terms remembered are those of Okhsl [lightness] at hue ([a], [b]), by their exact bits. */
    fun hasOkhslRow(lightness: Double, a: Double, b: Double): Boolean =
        okhslRowKnown && okhslRowLightness.sameBits(lightness) && okhslRowA.sameBits(a) && okhslRowB.sameBits(b)

    /** Remembers Okhsl's terms at [lightness] and hue ([a], [b]): along a plane's row only the saturation changes. */
    fun rememberOkhslRow(lightness: Double, a: Double, b: Double, l: Double, c0: Double, cMid: Double, cMax: Double) {
        okhslRowKnown = true
        okhslRowLightness = lightness
        okhslRowA = a
        okhslRowB = b
        okhslL = l
        okhslC0 = c0
        okhslCMid = cMid
        okhslCMax = cMax
    }

    private var boundT: DoubleArray? = null
    private var bound = 0.0

    /**
     * A chroma no color of the gamut of [t] reaches, at any lightness and hue, its channels allowed EDGE_TOLERANCE
     * past 0..1. The last gamut's is remembered.
     */
    fun chromaBound(t: DoubleArray): Double {
        if (boundT !== t) {
            bound = chromaReach(t)
            boundT = t
        }
        return bound
    }

    // Enough for a plane's row, whose colors reach the gamut along a few hue directions that differ in their last
    // bits. Overwritten oldest first.
    private val edgeT = arrayOfNulls<DoubleArray>(EDGES_KEPT)
    private val edgeIterative = BooleanArray(EDGES_KEPT)
    private val edgeKeys = DoubleArray(EDGES_KEPT * 3)
    private val edgeChroma = DoubleArray(EDGES_KEPT)
    private var nextEdge = 0

    /** The cusp of hue ([a], [b]) in the gamut of [t], as [maxSaturation] and [cuspLightness] give it, into [cuspSaturation] and [cuspLightness]. */
    fun cusp(t: DoubleArray, a: Double, b: Double): GamutMemo {
        if (cuspT !== t || !cuspA.sameBits(a) || !cuspB.sameBits(b)) {
            val saturation = maxSaturation(t, a, b)
            cuspLightness = cuspLightness(t, a, b, saturation)
            cuspSaturation = saturation
            cuspT = t
            cuspA = a
            cuspB = b
        }
        return this
    }

    /**
     * The largest chroma at which the line of lightness [l] and hue ([a], [b]) leaves the gamut of [t] for good, by
     * the closed forms or, [iterative], by [walkInward]. Each is remembered apart from the other.
     */
    fun outerEdge(t: DoubleArray, l: Double, a: Double, b: Double, iterative: Boolean = false): Double {
        for (i in 0 until EDGES_KEPT) {
            val key = i * 3
            if (edgeT[i] === t && edgeIterative[i] == iterative && edgeKeys[key].sameBits(l) && edgeKeys[key + 1].sameBits(a) && edgeKeys[key + 2].sameBits(b)) {
                return edgeChroma[i]
            }
        }
        val chroma = if (iterative) outerEdgeByWalk(t, l, a, b) else largestEdge(t, l, a, b, withCeiling = true, limit = Double.POSITIVE_INFINITY)
        val i = nextEdge
        edgeT[i] = t
        edgeIterative[i] = iterative
        edgeKeys[i * 3] = l
        edgeKeys[i * 3 + 1] = a
        edgeKeys[i * 3 + 2] = b
        edgeChroma[i] = chroma
        nextEdge = (i + 1) % EDGES_KEPT
        return chroma
    }

    private companion object {
        const val EDGES_KEPT = 16
    }
}

private fun Double.sameBits(other: Double): Boolean = toRawBits() == other.toRawBits()

// The largest positive x up to [limit] at which a channel along (l, x·a, x·b) reaches 0, or 1
// [withCeiling], while every channel stays in 0..1 (0..∞ without the ceiling), give or take
// EDGE_TOLERANCE. Every line out of grey leaves the gamut somewhere, so some root always qualifies.
private fun largestEdge(t: DoubleArray, l: Double, a: Double, b: Double, withCeiling: Boolean, limit: Double): Double {
    val m = OKLAB_TO_LMS
    // Cube-rooted LMS is u + x·q, with u the matrix's first column times l.
    val u0 = m[0] * l
    val u1 = m[3] * l
    val u2 = m[6] * l
    val q0 = m[1] * a + m[2] * b
    val q1 = m[4] * a + m[5] * b
    val q2 = m[7] * a + m[8] * b
    val ceiling = if (withCeiling) 1.0 + EDGE_TOLERANCE else Double.POSITIVE_INFINITY
    val levels = if (withCeiling) 2 else 1
    var edge = 0.0
    for (row in 0..2) {
        val t0 = t[row * 3]
        val t1 = t[row * 3 + 1]
        val t2 = t[row * 3 + 2]
        // Σ t_k (u_k + q_k x)³ expanded in powers of x.
        val cubic = t0 * q0 * q0 * q0 + t1 * q1 * q1 * q1 + t2 * q2 * q2 * q2
        val square = 3.0 * (t0 * u0 * q0 * q0 + t1 * u1 * q1 * q1 + t2 * u2 * q2 * q2)
        val linear = 3.0 * (t0 * u0 * u0 * q0 + t1 * u1 * u1 * q1 + t2 * u2 * u2 * q2)
        val constant = t0 * u0 * u0 * u0 + t1 * u1 * u1 * u1 + t2 * u2 * u2 * u2
        for (level in 0 until levels) {
            val shifted = constant - level
            forEachRealRoot(cubic, square, linear, shifted) { seed ->
                // Only a root that could pass the edge is worth polishing and checking.
                if (seed > edge - 1e-6) {
                    val x = polish(cubic, square, linear, shifted, seed)
                    if (x > edge && x <= limit && inside(t, l, x * a, x * b, ceiling)) edge = x
                }
            }
        }
    }
    return edge
}

// Crossings a walk follows before handing over to the closed forms; a line crosses 0 or 1 at most eighteen times.
private const val WALK_CROSSINGS = 24

/**
 * [largestEdge]'s answer without its closed forms: the largest x up to [from], where the line (l, x·a, x·b) is
 * outside the gamut of [t], at which the line is inside, give or take EDGE_TOLERANCE. From [from] it follows a
 * channel that is outside down to where that channel last crossed back over its level, and goes on from there
 * while any channel is still outside. Every point it passes has a channel outside, so the first point where none
 * is, is the answer. No cube root or trigonometry: each crossing is Newton's method inside a stretch where the
 * channel's cubic is monotone.
 */
internal fun walkInward(t: DoubleArray, l: Double, a: Double, b: Double, from: Double): Double {
    val m = OKLAB_TO_LMS
    // Cube-rooted LMS is u + x·q, with u the matrix's first column times l.
    val u0 = m[0] * l
    val u1 = m[3] * l
    val u2 = m[6] * l
    val q0 = m[1] * a + m[2] * b
    val q1 = m[4] * a + m[5] * b
    val q2 = m[7] * a + m[8] * b
    var x = from
    for (crossing in 0 until WALK_CROSSINGS) {
        var row = -1
        var level = 0.0
        for (r in 0..2) {
            val value = channelAt(t, r, u0, u1, u2, q0, q1, q2, x)
            if (value !in -EDGE_TOLERANCE..1.0 + EDGE_TOLERANCE) {
                row = r
                level = if (value < 0.0) 0.0 else 1.0
                break
            }
        }
        if (row < 0) return x
        x = lastCrossing(t, row, u0, u1, u2, q0, q1, q2, level, x)
    }
    return largestEdge(t, l, a, b, withCeiling = true, limit = from)
}

// The outer edge by walking in from just past the chroma no color of the gamut reaches, where the line is outside and
// stays outside.
private fun outerEdgeByWalk(t: DoubleArray, l: Double, a: Double, b: Double): Double =
    walkInward(t, l, a, b, 1.01 * gamutMemo().chromaBound(t))

// GamutMemo.chromaBound's answer: each cube-rooted LMS channel bounded over the widened cube, and Oklab's a and b
// bounded over those.
private fun chromaReach(t: DoubleArray): Double {
    val toLms = invert(t)
    val low = DoubleArray(3)
    val high = DoubleArray(3)
    for (k in 0..2) {
        var lo = 0.0
        var hi = 0.0
        for (j in 0..2) {
            val entry = toLms[k * 3 + j]
            lo += min(entry * -EDGE_TOLERANCE, entry * (1.0 + EDGE_TOLERANCE))
            hi += max(entry * -EDGE_TOLERANCE, entry * (1.0 + EDGE_TOLERANCE))
        }
        low[k] = cbrt(lo)
        high[k] = cbrt(hi)
    }
    val m = LMS_TO_OKLAB
    var reach = 0.0
    for (row in 1..2) {
        var lo = 0.0
        var hi = 0.0
        for (k in 0..2) {
            lo += min(m[row * 3 + k] * low[k], m[row * 3 + k] * high[k])
            hi += max(m[row * 3 + k] * low[k], m[row * 3 + k] * high[k])
        }
        val most = max(abs(lo), abs(hi))
        reach += most * most
    }
    return sqrt(reach)
}

// Linear channel [row] of the gamut of [t] at chroma x along the line whose cube-rooted LMS is u + x·q.
private fun channelAt(t: DoubleArray, row: Int, u0: Double, u1: Double, u2: Double, q0: Double, q1: Double, q2: Double, x: Double): Double {
    val s0 = u0 + x * q0
    val s1 = u1 + x * q1
    val s2 = u2 + x * q2
    return t[row * 3] * s0 * s0 * s0 + t[row * 3 + 1] * s1 * s1 * s1 + t[row * 3 + 2] * s2 * s2 * s2
}

// The largest y below [x] at which channel [row] crosses [level], where the channel is past [level] at x and inside
// at 0, the grey. The channel's cubic is monotone between the roots of its derivative, so the stretches between
// them are tried from x down, and the crossing lies in the first whose ends straddle the level.
private fun lastCrossing(t: DoubleArray, row: Int, u0: Double, u1: Double, u2: Double, q0: Double, q1: Double, q2: Double, level: Double, x: Double): Double {
    val t0 = t[row * 3]
    val t1 = t[row * 3 + 1]
    val t2 = t[row * 3 + 2]
    // A third of the derivative: A·y² + 2B·y + C.
    val a3 = t0 * q0 * q0 * q0 + t1 * q1 * q1 * q1 + t2 * q2 * q2 * q2
    val b2 = t0 * u0 * q0 * q0 + t1 * u1 * q1 * q1 + t2 * u2 * q2 * q2
    val c1 = t0 * u0 * u0 * q0 + t1 * u1 * u1 * q1 + t2 * u2 * u2 * q2
    var high = Double.NaN
    var low = Double.NaN
    if (abs(a3) <= 1e-14 * (abs(b2) + abs(c1))) {
        if (b2 != 0.0) high = -c1 / (2.0 * b2)
    } else {
        val discriminant = b2 * b2 - a3 * c1
        if (discriminant >= 0.0) {
            val root = sqrt(discriminant)
            val first = (-b2 + root) / a3
            val second = (-b2 - root) / a3
            high = max(first, second)
            low = min(first, second)
        }
    }
    val outsideBelow = channelAt(t, row, u0, u1, u2, q0, q1, q2, x) < level
    var top = x
    for (turn in 0..1) {
        val p = if (turn == 0) high else low
        if (!(p > 0.0 && p < top)) continue
        val g = channelAt(t, row, u0, u1, u2, q0, q1, q2, p) - level
        if (g == 0.0) return p
        if ((g < 0.0) != outsideBelow) return crossingIn(t, row, u0, u1, u2, q0, q1, q2, level, p, top, outsideBelow)
        top = p
    }
    return crossingIn(t, row, u0, u1, u2, q0, q1, q2, level, 0.0, top, outsideBelow)
}

// The one crossing of [level] by channel [row] in [low]..[high], where the channel is monotone, inside at [low] and
// outside at [high]: Newton's steps from [high], each kept in the bracket that still holds the crossing, halving it
// instead where a step would leave it.
private fun crossingIn(t: DoubleArray, row: Int, u0: Double, u1: Double, u2: Double, q0: Double, q1: Double, q2: Double, level: Double, low: Double, high: Double, outsideBelow: Boolean): Double {
    val t0 = t[row * 3]
    val t1 = t[row * 3 + 1]
    val t2 = t[row * 3 + 2]
    var inner = low
    var outer = high
    var y = high
    repeat(64) {
        val s0 = u0 + y * q0
        val s1 = u1 + y * q1
        val s2 = u2 + y * q2
        val g = t0 * s0 * s0 * s0 + t1 * s1 * s1 * s1 + t2 * s2 * s2 * s2 - level
        if (g == 0.0) return y
        if ((g < 0.0) == outsideBelow) outer = y else inner = y
        val slope = 3.0 * (t0 * q0 * s0 * s0 + t1 * q1 * s1 * s1 + t2 * q2 * s2 * s2)
        val step = if (slope != 0.0) g / slope else Double.NaN
        // A step this small is the crossing, even where it lands on the bracket's own end, which is y itself.
        if (abs(step) <= 1e-15 * abs(y)) return y - step
        val lo = min(inner, outer)
        val hi = max(inner, outer)
        var next = y - step
        if (!(next > lo && next < hi)) next = 0.5 * (lo + hi)
        if (hi - lo <= 1e-15 * hi) return next
        y = next
    }
    return y
}

private fun inside(t: DoubleArray, l: Double, a: Double, b: Double, ceiling: Double): Boolean {
    val m = OKLAB_TO_LMS
    val lRoot = m[0] * l + m[1] * a + m[2] * b
    val mRoot = m[3] * l + m[4] * a + m[5] * b
    val sRoot = m[6] * l + m[7] * a + m[8] * b
    val lms0 = lRoot * lRoot * lRoot
    val lms1 = mRoot * mRoot * mRoot
    val lms2 = sRoot * sRoot * sRoot
    return t[0] * lms0 + t[1] * lms1 + t[2] * lms2 in -EDGE_TOLERANCE..ceiling &&
        t[3] * lms0 + t[4] * lms1 + t[5] * lms2 in -EDGE_TOLERANCE..ceiling &&
        t[6] * lms0 + t[7] * lms1 + t[8] * lms2 in -EDGE_TOLERANCE..ceiling
}

// A cubic term this small beside the others is taken for zero. At each hue where a channel's cubic term passes
// through zero, Cardano's discriminant is the difference of two terms each about (b/a)⁶ in size, so near that hue it
// cancels to noise and the real roots at the gamut's edge are lost, and the cusp with them. The quadratic loses only
// the roots the cubic term makes, where a·x³ matches the rest, at |x| of 1e5^(1/3), 46, or more: far past any gamut's
// chroma. [polish] on the whole cubic makes up the little the term moves the roots kept.
private const val NEGLIGIBLE_CUBIC = 1e-5

// Calls [action] with each real root of a·x³ + b·x² + c·x + d as the closed forms leave it: close
// enough to tell the roots apart, not to land on the edge, which is what [polish] is for.
private inline fun forEachRealRoot(a: Double, b: Double, c: Double, d: Double, action: (Double) -> Unit) {
    if (abs(a) <= NEGLIGIBLE_CUBIC * (abs(b) + abs(c) + abs(d))) {
        forEachQuadraticRoot(b, c, d, action)
        return
    }
    val p0 = b / a
    val p1 = c / a
    val p2 = d / a
    val shift = -p0 / 3.0
    val p = p1 - p0 * p0 / 3.0
    val q = 2.0 * p0 * p0 * p0 / 27.0 - p0 * p1 / 3.0 + p2
    val discriminant = q * q / 4.0 + p * p * p / 27.0
    when {
        discriminant > 0.0 -> {
            val root = sqrt(discriminant)
            action(cbrt(-q / 2.0 + root) + cbrt(-q / 2.0 - root) + shift)
        }
        p == 0.0 -> action(shift)
        else -> {
            // Three real roots at radius·cos(θ − 2πk/3), the last two from cos θ and sin θ.
            val radius = 2.0 * sqrt(-p / 3.0)
            val angle = acos((3.0 * q / (2.0 * p) * sqrt(-3.0 / p)).coerceIn(-1.0, 1.0)) / 3.0
            val cosine = cos(angle)
            val sine = sin(angle)
            action(radius * cosine + shift)
            action(radius * (-0.5 * cosine + HALF_SQRT3 * sine) + shift)
            action(radius * (-0.5 * cosine - HALF_SQRT3 * sine) + shift)
        }
    }
}

private inline fun forEachQuadraticRoot(a: Double, b: Double, c: Double, action: (Double) -> Unit) {
    if (abs(a) <= 1e-14 * (abs(b) + abs(c))) {
        if (b != 0.0) action(-c / b)
        return
    }
    val discriminant = b * b - 4.0 * a * c
    if (discriminant < 0.0) return
    val root = sqrt(discriminant)
    action((-b + root) / (2.0 * a))
    action((-b - root) / (2.0 * a))
}

// Two Newton steps on a·x³ + b·x² + c·x + d from [seed].
private fun polish(a: Double, b: Double, c: Double, d: Double, seed: Double): Double {
    var x = seed
    repeat(2) {
        val value = ((a * x + b) * x + c) * x + d
        val slope = (3.0 * a * x + 2.0 * b) * x + c
        if (slope != 0.0) x -= value / slope
    }
    return x
}
