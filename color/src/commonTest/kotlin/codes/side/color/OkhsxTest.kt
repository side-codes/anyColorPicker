package codes.side.color

import codes.side.color.internal.cuspLightness
import codes.side.color.internal.maxChroma
import codes.side.color.internal.maxSaturation
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Expected values are color.js 0.7.1 outputs, at hues where its fitted cusp sits within 1e-7 of
// sRGB's edge. From 210° to pure blue it drifts off, by up to 5.9e-4 in a channel just below blue's
// hue, and there the two part by up to 1.2e-2 in s; the tests below that pin the edge are the
// ground truth. color.js also finds Okhsl's chroma limit with one Halley step, and Okhsl's mid
// anchor scales with that limit, which leaves Okhsl up to 2.3e-5 apart even at well-fitted hues.
class OkhsxTest {

    @Test
    fun okhslMatchesColorJs() {
        assertComponents(doubleArrayOf(334.9000820935721, 0.9265867215555352, 0.5948813896218199), Srgb(0.9, 0.2, 0.8).to(Okhsl), 3e-5)
        assertComponents(doubleArrayOf(67.46135280323955, 0.9650451049315959, 0.7171976149756722), Srgb(0.95, 0.6, 0.1).to(Okhsl), 3e-5)
        assertComponents(doubleArrayOf(146.50664610056594, 0.9503132509748233, 0.6217068440177707), Srgb(0.2, 0.7, 0.3).to(Okhsl), 3e-5)
        assertComponents(doubleArrayOf(0.6405985679680205, 0.4437768656315378, 0.9324916949848554), Okhsl(300.0, 0.8, 0.6).to(Srgb), 3e-5)
        assertComponents(doubleArrayOf(0.5362391537424661, 0.2882681215093182, 0.2494341291832996), Okhsl(30.0, 0.5, 0.4).to(Srgb), 3e-5)
    }

    @Test
    fun okhsvMatchesColorJs() {
        assertComponents(doubleArrayOf(334.9000820935721, 0.9160160753799435, 0.9062061749905603), Srgb(0.9, 0.2, 0.8).to(Okhsv), 1e-6)
        assertComponents(doubleArrayOf(67.46135280323955, 0.9548875966581694, 0.9540791287134736), Srgb(0.95, 0.6, 0.1).to(Okhsv), 1e-6)
        assertComponents(doubleArrayOf(146.50664610056594, 0.8527351298632351, 0.7223355489900756), Srgb(0.2, 0.7, 0.3).to(Okhsv), 1e-6)
        assertComponents(doubleArrayOf(0.3725205557465032, 0.20058384079480457, 0.5926548308051491), Okhsv(300.0, 0.8, 0.6).to(Srgb), 1e-6)
        assertComponents(doubleArrayOf(0.3482419421057739, 0.6754425942582462, 0.3673244110823704), Okhsv(145.0, 0.6, 0.7).to(Srgb), 1e-6)
    }

    @Test
    fun theCuspEndsTheLastStretchOfTheHueInsideSrgb() {
        // 264.1° is past pure blue, where the stretch is split and the first exit is not the cusp.
        for (degrees in doubleArrayOf(29.0, 110.0, 142.0, 200.0, 264.1, 300.58, 330.0)) {
            val a = cos(degrees * PI / 180.0)
            val b = sin(degrees * PI / 180.0)
            val scanned = scannedEdge(2.0) { s -> linearSrgb(1.0, s * a, s * b).all { it >= 0.0 } }
            assertNear(scanned, maxSaturation(a, b), 1e-12, "at $degrees°")
        }
    }

    @Test
    fun theEdgeIsTheLastChromaInsideSrgb() {
        // Below the cusp's lightness, at it and above it; just above it at 264.1°, the line crosses the
        // sliver outside sRGB past pure blue.
        for (degrees in doubleArrayOf(29.0, 110.0, 200.0, 264.1, 330.0)) {
            val a = cos(degrees * PI / 180.0)
            val b = sin(degrees * PI / 180.0)
            val sMax = maxSaturation(a, b)
            val lCusp = cuspLightness(a, b, sMax)
            for (l in doubleArrayOf(0.3, lCusp, lCusp + 0.01, 0.99)) {
                val scanned = scannedEdge(0.5) { c -> linearSrgb(l, c * a, c * b).all { it in 0.0..1.0 } }
                assertNear(scanned, maxChroma(l, a, b, sMax, lCusp), 1e-12, "at $degrees°, L $l")
            }
        }
    }

    @Test
    fun theCuspSitsOnTheGamutEdge() {
        for (degrees in 0 until 360 step 15) {
            val a = cos(degrees * PI / 180.0)
            val b = sin(degrees * PI / 180.0)
            val s = maxSaturation(a, b)
            val l = cuspLightness(a, b, s)
            val rgb = linearSrgb(l, l * s * a, l * s * b)
            assertNear(0.0, rgb.min(), 1e-12, "lowest channel at $degrees°")
            assertNear(1.0, rgb.max(), 1e-12, "highest channel at $degrees°")
        }
    }

    @Test
    fun pureBlueIsItsOwnHuesCusp() {
        // Pure blue's hue is where the cusp jumps, so rounding in the hue alone could put it on
        // either side.
        val blue = Srgb(0.0, 0.0, 1.0)
        val lab = blue.to(Oklab)
        val a = lab[Oklab.A]!!
        val b = lab[Oklab.B]!!
        val chroma = hypot(a, b)
        assertNear(chroma / lab[Oklab.L]!!, maxSaturation(a / chroma, b / chroma), 1e-12)
        val hue = atan2(b, a) * 180.0 / PI + 360.0
        assertComponents(doubleArrayOf(hue, 1.0, 1.0), blue.to(Okhsv), 1e-12)
        for (space in listOf(Okhsl, Okhsv)) {
            assertComponents(blue.components(), blue.to(space).to(Srgb), 1e-12)
        }
    }

    @Test
    fun onSrgbsEdgeTheSaturationIsFull() {
        // Each has a channel at 0 or 1; color.js reads the first three as s > 1 and the last as
        // 0.99992.
        for (color in listOf(Srgb(0.0, 34 / 255.0, 170 / 255.0), Srgb(0.0, 51 / 255.0, 238 / 255.0), Srgb(1.0, 238 / 255.0, 119 / 255.0), Srgb(170 / 255.0, 170 / 255.0, 1.0))) {
            assertNear(1.0, color.to(Okhsl)[Okhsl.S]!!, 1e-9, "$color")
        }
        for (color in listOf(Srgb(0.0, 34 / 255.0, 170 / 255.0), Srgb(0.0, 0.4, 0.8), Srgb(0.9, 0.0, 0.3))) {
            assertNear(1.0, color.to(Okhsv)[Okhsv.S]!!, 1e-9, "$color")
        }
        for (color in listOf(Srgb(1.0, 238 / 255.0, 119 / 255.0), Srgb(0.1, 0.5, 1.0))) {
            assertNear(1.0, color.to(Okhsv)[Okhsv.V]!!, 1e-9, "$color")
        }
    }

    @Test
    fun okhsxRoundTripsInsideSrgb() {
        val colors = listOf(
            Srgb(0.1, 0.5, 0.9),
            Srgb(0.9, 0.2, 0.8),
            Srgb(1.0, 0.95, 0.1),
            Srgb(0.3, 0.3, 0.31),
            Srgb(0.002, 0.0, 0.001),
            Srgb(1.0, 0.999, 0.998),
        )
        for (color in colors) {
            assertComponents(color.components(), color.to(Okhsl).to(Srgb), 1e-9)
            assertComponents(color.components(), color.to(Okhsv).to(Srgb), 1e-9)
        }
    }

    @Test
    fun outsideSrgbTheChromaStopsAtSrgbsEdge() {
        val p3Red = DisplayP3(1.0, 0.0, 0.0)
        val original = p3Red.to(OkLch)
        assertNear(1.0, p3Red.to(Okhsl)[Okhsl.S]!!, 1e-9)
        // It reaches sRGB's edge through red's ceiling, and Okhsv's s = 1 is the floor's edge.
        assertNear(1.0, p3Red.to(Okhsv)[Okhsv.V]!!, 1e-9)
        for (space in listOf(Okhsl, Okhsv)) {
            val reduced = p3Red.to(space)
            val rgb = reduced.to(Srgb).components()
            assertTrue(rgb.all { it in -1e-9..1.0 + 1e-9 }, "${space.id} lands in sRGB: ${rgb.toList()}")
            assertNear(1.0, rgb.max(), 1e-9, "${space.id} keeps red at full")
            val back = reduced.to(OkLch)
            assertNear(original[OkLch.L]!!, back[OkLch.L]!!, 1e-9, "${space.id} lightness")
            assertNear(original[OkLch.H]!!, back[OkLch.H]!!, 1e-9, "${space.id} hue")
        }
    }

    @Test
    fun farPastThePoleTheChromaStillStopsAtSrgbsEdge() {
        // Unreduced, a chroma past the saturation formula's pole reads as a negative saturation,
        // which the 0..1 range turns into grey rather than the most colorful sRGB color there.
        for (chroma in doubleArrayOf(0.3, 0.5, 1.0, 3.0)) {
            for (hue in doubleArrayOf(30.0, 100.0, 110.0, 250.0, 264.1)) {
                val color = OkLch(0.97, chroma, hue)
                val at = "chroma $chroma, $hue°"
                assertNear(1.0, color.to(Okhsl)[Okhsl.S]!!, 1e-9, "okhsl s at $at")
                for (space in listOf(Okhsl, Okhsv)) {
                    val reduced = color.to(space)
                    val back = reduced.to(OkLch)
                    assertNear(0.97, back[OkLch.L]!!, 1e-9, "${space.id} lightness at $at")
                    assertNear(hue, back[OkLch.H]!!, 1e-9, "${space.id} hue at $at")
                    val rgb = reduced.to(Srgb).components()
                    assertTrue(rgb.all { it in -1e-9..1.0 + 1e-9 }, "${space.id} at $at lands in sRGB: ${rgb.toList()}")
                    assertTrue(rgb.any { abs(it) <= 1e-9 || abs(it - 1.0) <= 1e-9 }, "${space.id} at $at sits on sRGB's edge: ${rgb.toList()}")
                }
            }
        }
    }

    @Test
    fun okhsxHueIsPowerlessAtOkLchsThreshold() {
        for (space in listOf(Okhsl, Okhsv)) {
            assertTrue(Oklab(0.5, 0.0000039, 0.0).to(space).isMissing(space.channels[0]), space.id)
            assertFalse(Oklab(0.5, 0.0000041, 0.0).to(space).isMissing(space.channels[0]), space.id)
        }
    }

    @Test
    fun okGreysHaveNoHue() {
        for (space in listOf(Okhsl, Okhsv)) {
            val grey = Srgb(0.4, 0.4, 0.4).to(space)
            assertTrue(grey.isMissing(space.channels[0]), space.id)
            assertEquals(0.0, grey[space.channels[1]], space.id)
        }
    }

    private fun linearSrgb(l: Double, a: Double, b: Double): DoubleArray = Oklab(l, a, b).to(SrgbLinear).components()

    // The end of the last stretch of 0..[limit] that [inside] holds: sampled finely enough to see
    // the split stretch past pure blue, then bisected.
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
