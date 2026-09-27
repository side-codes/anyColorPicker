package codes.side.color

import codes.side.color.internal.GamutMemo
import codes.side.color.internal.chromaWithin
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// The walk in from a color's own chroma lands where the closed forms do: the largest chroma up to the color's own
// that the gamut holds, inside the sliver past pure blue as elsewhere.
class EdgeWalkTest {

    companion object {
        // How far apart the solvers' answers may be: 1e-12 in chroma, except near black, where the channels are about
        // as small as the 1e-9 edge tolerance, so two can reach zero within it of each other and either solver may
        // stop on either. There the two colors may differ by what the tolerance allows, well under 1e-8 in linear light.
        fun disagreement(t: DoubleArray, l: Double, a: Double, b: Double, closed: Double, walked: Double): String? {
            if (abs(closed - walked) <= 1e-12) return null
            val one = DoubleArray(3)
            val other = DoubleArray(3)
            toLinear(t, l, closed * a, closed * b, one)
            toLinear(t, l, walked * a, walked * b, other)
            val linear = (0..2).maxOf { abs(one[it].coerceIn(0.0, 1.0) - other[it].coerceIn(0.0, 1.0)) }
            return if (l < 0.05 && linear <= 1e-8) null else "chroma ${abs(closed - walked)} apart, linear light $linear"
        }
    }

    private val rec2020 = ColorSpace.rgb(
        "--rec2020-walk",
        RgbPrimaries(0.708, 0.292, 0.170, 0.797, 0.131, 0.046),
        WhitePoint.D65,
        TransferFunction.Srgb,
    )
    private val gamuts = listOf(Srgb.gamut, DisplayP3.gamut, rec2020.gamut)
    private val pureBlue = Srgb(0.0, 0.0, 1.0).to(Oklch)[Oklch.H]!!

    // Every 1.5°, and the hues around sRGB's and Rec. 2020's slivers, pure blue's own among them.
    private val hues = List(240) { it * 1.5 } + listOf(245.1, 245.2, 264.05, 264.1, 264.2, pureBlue)

    @Test
    fun theWalkLandsWhereTheClosedFormsDo() {
        for (gamut in gamuts) {
            val t = gamut.lmsToLinear
            for (hue in hues) {
                val a = cos(hue * PI / 180.0)
                val b = sin(hue * PI / 180.0)
                for (step in 1 until 50) {
                    val l = step / 50.0
                    val edge = chromaWithin(t, l, a, b, 10.0)
                    // Just past the edge, well past it, far out, and 0.27, inside the sliver where there is one.
                    for (chroma in listOf(edge * (1.0 + 1e-7), edge * 1.3 + 1e-3, 0.6, 0.27)) {
                        val closed = chromaWithin(t, l, a, b, chroma)
                        val walked = chromaWithin(t, l, a, b, chroma, iterative = true)
                        val apart = disagreement(t, l, a, b, closed, walked)
                        assertTrue(apart == null, "$gamut at L $l, $hue°, chroma $chroma: closed $closed, walked $walked, $apart")
                    }
                }
            }
        }
    }

    @Test
    fun aRememberedEdgeAnswersOnlyItsOwnSolver() {
        // Among these, lines whose two answers differ in their last bits: a memo that ignored the solver would hand
        // one solver's edge to the other there.
        var differing = 0
        for (hue in hues) {
            val a = cos(hue * PI / 180.0)
            val b = sin(hue * PI / 180.0)
            for (l in listOf(0.3, 0.6, 0.9)) {
                val closed = GamutMemo().outerEdge(Srgb.gamut.lmsToLinear, l, a, b)
                val walked = GamutMemo().outerEdge(Srgb.gamut.lmsToLinear, l, a, b, iterative = true)
                if (closed.toRawBits() == walked.toRawBits()) continue
                differing++
                val memo = GamutMemo()
                memo.outerEdge(Srgb.gamut.lmsToLinear, l, a, b)
                assertEquals(walked.toRawBits(), memo.outerEdge(Srgb.gamut.lmsToLinear, l, a, b, iterative = true).toRawBits(), "L $l, $hue°")
                assertEquals(closed.toRawBits(), memo.outerEdge(Srgb.gamut.lmsToLinear, l, a, b).toRawBits(), "L $l, $hue°")
            }
        }
        assertTrue(differing > 0, "no line whose answers differ, so the check checked nothing")
    }
}
