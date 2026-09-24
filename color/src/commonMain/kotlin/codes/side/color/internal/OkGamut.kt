package codes.side.color.internal

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

// The sRGB gamut's shape in Oklab, as Okhsl and Okhsv need it: the cusp and the edge, solved from
// the channels' cubics. Ottosson's reference code approximates both (a fitted polynomial per
// channel region for the cusp, one Halley step for each), which leaves its cusp up to 0.015 off in
// S just below pure blue's hue and its edge low enough near yellow that sRGB colors read as Okhsl
// s > 1. Everything here is scalar, because bulk conversions allocate nothing per color.

// How far outside 0..1 a linear channel may land and still count as on sRGB's edge: the rounding a
// polished root leaves, not a gamut margin.
private const val EDGE_TOLERANCE = 1e-9

private const val HALF_SQRT3 = 0.8660254037844386

/** The highest linear sRGB channel of an Oklab color, via [LMS_TO_SRGB_LINEAR]. */
internal fun highestLinearSrgb(l: Double, a: Double, b: Double): Double {
    val m = OKLAB_TO_LMS
    val lRoot = m[0] * l + m[1] * a + m[2] * b
    val mRoot = m[3] * l + m[4] * a + m[5] * b
    val sRoot = m[6] * l + m[7] * a + m[8] * b
    val lms0 = lRoot * lRoot * lRoot
    val lms1 = mRoot * mRoot * mRoot
    val lms2 = sRoot * sRoot * sRoot
    val t = LMS_TO_SRGB_LINEAR
    return max(t[0] * lms0 + t[1] * lms1 + t[2] * lms2, max(t[3] * lms0 + t[4] * lms1 + t[5] * lms2, t[6] * lms0 + t[7] * lms1 + t[8] * lms2))
}

/**
 * The largest S = C/L at hue ([a], [b]), a unit vector, that sRGB still holds: the largest root of
 * a channel reaching 0 along (1, S·a, S·b) at which no channel is negative. Scaling a color toward
 * black keeps every channel's sign, so the whole ray from black at that S is sRGB's lower edge.
 *
 * Not the smallest root: just above pure blue's hue (264.05–264.21°) red dips below zero and comes
 * back before green reaches it, so the cusp ends a second in-gamut stretch. At pure blue's own hue
 * that stretch shrinks to one point, pure blue, which only the tolerance keeps from rounding away.
 */
internal fun maxSaturation(a: Double, b: Double): Double = largestEdge(1.0, a, b, withCeiling = false)

/** The cusp's Oklab lightness at hue ([a], [b]), given its S from [maxSaturation]. */
internal fun cuspLightness(a: Double, b: Double, saturation: Double): Double =
    cbrt(1.0 / highestLinearSrgb(1.0, saturation * a, saturation * b))

/**
 * The largest chroma sRGB holds at Oklab lightness [l], strictly between 0 and 1, and hue ([a],
 * [b]), whose cusp is [sMax] from [maxSaturation] at [lCusp] from [cuspLightness].
 *
 * Up to the cusp's lightness that is l·sMax exactly: along a ray from black every channel scales by
 * L³, so the ray at sMax stays inside until its highest channel reaches 1, at the cusp. Above it,
 * the edge is the largest root of a channel reaching 0 or 1 along (l, C·a, C·b) at which every
 * channel is in 0..1; past pure blue's hue the line can cross a sliver outside sRGB and come back
 * in, and the edge is where it leaves for good.
 */
internal fun maxChroma(l: Double, a: Double, b: Double, sMax: Double, lCusp: Double): Double =
    if (l <= lCusp) l * sMax else largestEdge(l, a, b, withCeiling = true)

// The largest positive x at which a channel along (l, x·a, x·b) reaches 0, or 1 [withCeiling], while
// every channel stays in 0..1 (0..∞ without the ceiling), give or take EDGE_TOLERANCE. Every line out
// of grey leaves sRGB somewhere, so some root always qualifies.
private fun largestEdge(l: Double, a: Double, b: Double, withCeiling: Boolean): Double {
    val m = OKLAB_TO_LMS
    val t = LMS_TO_SRGB_LINEAR
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
                    if (x > edge && inside(l, x * a, x * b, ceiling)) edge = x
                }
            }
        }
    }
    return edge
}

private fun inside(l: Double, a: Double, b: Double, ceiling: Double): Boolean {
    val m = OKLAB_TO_LMS
    val lRoot = m[0] * l + m[1] * a + m[2] * b
    val mRoot = m[3] * l + m[4] * a + m[5] * b
    val sRoot = m[6] * l + m[7] * a + m[8] * b
    val lms0 = lRoot * lRoot * lRoot
    val lms1 = mRoot * mRoot * mRoot
    val lms2 = sRoot * sRoot * sRoot
    val t = LMS_TO_SRGB_LINEAR
    return t[0] * lms0 + t[1] * lms1 + t[2] * lms2 in -EDGE_TOLERANCE..ceiling &&
        t[3] * lms0 + t[4] * lms1 + t[5] * lms2 in -EDGE_TOLERANCE..ceiling &&
        t[6] * lms0 + t[7] * lms1 + t[8] * lms2 in -EDGE_TOLERANCE..ceiling
}

// Calls [action] with each real root of a·x³ + b·x² + c·x + d as the closed forms leave it: close
// enough to tell the roots apart, not to land on the edge, which is what [polish] is for.
private inline fun forEachRealRoot(a: Double, b: Double, c: Double, d: Double, action: (Double) -> Unit) {
    if (abs(a) <= 1e-14 * (abs(b) + abs(c) + abs(d))) {
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
