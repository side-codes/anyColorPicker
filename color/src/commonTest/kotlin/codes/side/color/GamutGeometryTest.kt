package codes.side.color

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GamutGeometryTest {

    // Rec. 2020's primaries in linear light: an app-built gamut, and one with its own sliver near
    // pure blue (245.067–245.284°).
    private val rec2020 = ColorSpace.rgb(
        "--rec2020-linear",
        RgbPrimaries(0.708, 0.292, 0.170, 0.797, 0.131, 0.046),
        WhitePoint.D65,
        TransferFunction.Linear,
    ).gamut

    // ProPhoto's primaries around a D50 white: the white an app's space is adapted from matters.
    private val proPhoto = ColorSpace.rgb(
        "--prophoto-linear",
        RgbPrimaries(0.734699, 0.265301, 0.159597, 0.840403, 0.036598, 0.000105),
        WhitePoint.D50,
        TransferFunction.Linear,
    ).gamut

    private val gamuts = listOf(Srgb.gamut, DisplayP3.gamut, rec2020, proPhoto)

    @Test
    fun maxChromaIsTheLastChromaTheGamutHolds() {
        // 264.1° splits sRGB's chroma in two at L 0.42, and 245.2° Rec. 2020's.
        val cases = listOf(
            Triple(Srgb.gamut, listOf(29.0 to 0.3, 110.0 to 0.9, 200.0 to 0.6, 264.1 to 0.42, 264.1 to 0.6, 330.0 to 0.5), 0.6),
            Triple(DisplayP3.gamut, listOf(29.0 to 0.3, 142.0 to 0.8, 264.1 to 0.42, 330.0 to 0.7), 0.6),
            Triple(rec2020, listOf(60.0 to 0.8, 245.2 to 0.3, 245.2 to 0.5, 300.0 to 0.4), 0.6),
            // ProPhoto reaches far past the others' chroma, so its scan runs further.
            Triple(proPhoto, listOf(30.0 to 0.5, 150.0 to 0.7, 264.1 to 0.3, 330.0 to 0.6), 2.0),
        )
        for ((gamut, points, limit) in cases) {
            for ((hue, lightness) in points) {
                val scanned = scannedEdge(limit) { c -> Oklch(lightness, c, hue).to(gamut.space).components().all { it in 0.0..1.0 } }
                assertNear(scanned, gamut.maxChroma(lightness, hue), 1e-9, "$gamut at $hue°, L $lightness")
            }
        }
    }

    @Test
    fun theEdgeHoldsWhereAChannelsCubicTermVanishes() {
        // At each of these hues one channel's cubic term, along a line of constant hue, passes through zero. Within a
        // few millionths of a degree of one, the closed form lost the real roots and mapped a vivid red to grey. Each
        // hue is held to the iterative solver, which uses no closed form, and to a colorful cusp.
        val hues = listOf(
            Srgb.gamut to listOf(29.2229263, 52.5546158, 232.5546158),
            DisplayP3.gamut to listOf(28.1173809, 57.9919545, 237.9919545),
        )
        val closedForm = GamutMapping.ChromaReduction(EdgeSolver.ClosedForm)
        val iterative = GamutMapping.ChromaReduction(EdgeSolver.Iterative)
        val offsets = listOf(-2e-6, -1e-6, -1e-7, -1e-9, 0.0, 1e-9, 1e-7, 1e-6, 2e-6)
        for ((gamut, centres) in hues) for (centre in centres) for (offset in offsets) {
            val hue = centre + offset
            assertTrue(gamut.cusp(hue)[Oklch.C]!! > 0.1, "$gamut's cusp at $hue°: ${gamut.cusp(hue)}")
            for (lightness in listOf(0.2, 0.4, 0.6, 0.8, 0.95)) {
                val color = Oklch(lightness, 0.4, hue)
                val byClosedForm = color.toGamut(gamut, closedForm).to(Oklch)[Oklch.C]!!
                val byWalk = color.toGamut(gamut, iterative).to(Oklch)[Oklch.C]!!
                assertNear(byWalk, byClosedForm, 1e-9, "$gamut at $hue°, L $lightness")
            }
        }
    }

    @Test
    fun maxChromaIsZeroAtBlackAndWhiteAndBeyond() {
        for (lightness in doubleArrayOf(-0.5, 0.0, 1.0, 1.5)) {
            assertEquals(0.0, Srgb.gamut.maxChroma(lightness, 30.0), "at L $lightness")
        }
        assertFailsWith<IllegalArgumentException> { Srgb.gamut.maxChroma(0.5, Double.NaN) }
    }

    @Test
    fun lightnessNextToBlackOrWhiteGivesSmallFiniteChroma() {
        for (gamut in gamuts) {
            for (lightness in doubleArrayOf(1e-9, 1.0 - 1e-9)) {
                for (step in 0 until 72) {
                    val hue = step * 5.0
                    val chroma = gamut.maxChroma(lightness, hue)
                    assertTrue(chroma.isFinite() && chroma > 0.0 && chroma <= 1e-3, "$gamut at L $lightness, $hue°: $chroma")
                }
            }
        }
    }

    @Test
    fun huesOutsideOneTurnAnswerAsTheirWrappedHue() {
        assertEquals(Srgb.gamut.maxChroma(0.6, 330.0), Srgb.gamut.maxChroma(0.6, -30.0), 1e-12)
        assertEquals(Srgb.gamut.maxChroma(0.6, 30.0), Srgb.gamut.maxChroma(0.6, 390.0), 1e-12)
        assertNear(330.0, Srgb.gamut.cusp(-30.0)[Oklch.H]!!, 1e-9)
    }

    @Test
    fun hugeFiniteHuesAnswerAsTheSameAngleStoredInAColor() {
        for (hue in listOf(1e20, -1e20, Double.MAX_VALUE, -Double.MAX_VALUE)) {
            val wrapped = Oklch(0.6, 0.1, hue)[Oklch.H]!!
            for (gamut in gamuts) {
                assertEquals(gamut.maxChroma(0.6, wrapped), gamut.maxChroma(0.6, hue), 1e-12, "$gamut at $hue")
                assertComponents(gamut.cusp(wrapped).components(), gamut.cusp(hue), 1e-12)
            }
        }
    }

    @Test
    fun theCuspSitsOnTheGamutsEdge() {
        for (gamut in gamuts) {
            for (hue in (0 until 360 step 15).map { it.toDouble() } + listOf(245.2, 264.1)) {
                val cusp = gamut.cusp(hue)
                assertNear(hue, cusp[Oklch.H]!!, 1e-9, "$gamut cusp hue at $hue°")
                val linear = linear(cusp, gamut)
                assertNear(0.0, linear.min(), 1e-12, "$gamut lowest channel at $hue°")
                assertNear(1.0, linear.max(), 1e-12, "$gamut highest channel at $hue°")
            }
        }
    }

    @Test
    fun theCuspIsTheMostChromaOfItsHue() {
        for (gamut in gamuts) {
            for (hue in doubleArrayOf(30.0, 140.0, 250.0)) {
                val cusp = gamut.cusp(hue)
                val lightness = cusp[Oklch.L]!!
                assertNear(cusp[Oklch.C]!!, gamut.maxChroma(lightness, hue), 1e-9, "$gamut at $hue°")
                assertTrue(gamut.maxChroma(lightness - 0.01, hue) < cusp[Oklch.C]!!)
                assertTrue(gamut.maxChroma(lightness + 0.01, hue) < cusp[Oklch.C]!!)
            }
        }
    }

    private fun linear(color: ColorValue, gamut: RgbGamut): DoubleArray {
        val encoded = color.to(gamut.space).components()
        return DoubleArray(3) { gamut.space.transfer.decode(encoded[it]) }
    }

    // The end of the last stretch of 0..[limit] that [inside] holds: sampled finely enough to see the
    // split stretch near pure blue, then bisected.
    private fun scannedEdge(limit: Double, inside: (Double) -> Boolean): Double {
        val step = 1e-4
        var last = 0.0
        for (i in 1..(limit / step).toInt()) {
            if (inside(i * step)) last = i * step
        }
        var low = last
        var high = last + step
        repeat(100) {
            val mid = (low + high) / 2.0
            if (inside(mid)) low = mid else high = mid
        }
        return low
    }
}
