package codes.side.color

import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// cssMatchesColorJs's expected values are color.js 0.7.1's toGamut({ method: "css" }) of the same
// OkLCh colors converted to srgb and p3.
class GamutMappingTest {

    private val methods = listOf(GamutMapping.Css(), GamutMapping.ChromaReduction, GamutMapping.Clip)

    @Test
    fun aColorTheGamutHoldsComesBackConverted() {
        val color = Srgb(0.2, 0.4, 0.6)
        for (method in methods) {
            assertEquals(color, color.toGamut(Srgb.gamut, method))
            assertEquals(color.to(DisplayP3), color.toGamut(DisplayP3.gamut, method))
        }
    }

    @Test
    fun isInGamutAllowsItsToleranceAndNoMore() {
        assertTrue(Srgb(1.00007, 0.5, 0.5).isInGamut(Srgb.gamut))
        assertFalse(Srgb(1.0001, 0.5, 0.5).isInGamut(Srgb.gamut))
        assertFalse(Srgb(1.00007, 0.5, 0.5).isInGamut(Srgb.gamut, tolerance = 0.0))
        assertFalse(DisplayP3(1.0, 0.0, 0.0).isInGamut(Srgb.gamut))
        assertTrue(DisplayP3(1.0, 0.0, 0.0).isInGamut(DisplayP3.gamut))
        assertTrue(Srgb(1.0, 0.0, 0.0).isInGamut(DisplayP3.gamut))
        assertFailsWith<IllegalArgumentException> { Srgb(0.5, 0.5, 0.5).isInGamut(Srgb.gamut, -1.0) }
    }

    @Test
    fun lightnessPastEitherEndIsWhiteOrBlack() {
        for (method in listOf(GamutMapping.Css(), GamutMapping.ChromaReduction)) {
            assertComponents(doubleArrayOf(1.0, 1.0, 1.0), OkLch(1.2, 0.2, 30.0).toGamut(Srgb.gamut, method), 1e-15)
            assertComponents(doubleArrayOf(0.0, 0.0, 0.0), OkLch(-0.1, 0.2, 30.0).toGamut(Srgb.gamut, method), 0.0)
        }
    }

    @Test
    fun clipClampsEachChannel() {
        assertComponents(doubleArrayOf(1.0, 0.0, 0.0), DisplayP3(1.0, 0.0, 0.0).toGamut(Srgb.gamut, GamutMapping.Clip), 1e-15)
    }

    @Test
    fun cssMatchesColorJs() {
        val cases = listOf(
            Srgb.gamut to listOf(
                doubleArrayOf(0.7, 0.35, 30.0) to doubleArrayOf(1.0, 0.34516243102018995, 0.2646030706712666),
                doubleArrayOf(0.5, 0.3, 140.0) to doubleArrayOf(0.0, 0.47899174481732815, 0.0),
                doubleArrayOf(0.9, 0.3, 100.0) to doubleArrayOf(0.9989869632752219, 0.8734696298380631, 0.0),
                doubleArrayOf(0.4, 0.4, 264.1) to doubleArrayOf(0.021516783980893038, 0.0, 0.8944801381501645),
                doubleArrayOf(0.3717, 0.3, 261.13) to doubleArrayOf(0.0, 0.0, 0.8029556722109672),
                doubleArrayOf(0.6, 0.25, 200.0) to doubleArrayOf(0.0, 0.5837551743125512, 0.6141174628528082),
                doubleArrayOf(0.8, 0.4, 330.0) to doubleArrayOf(1.0, 0.49581749932247005, 1.0),
                doubleArrayOf(0.95, 0.2, 60.0) to doubleArrayOf(1.0, 0.9022755271061652, 0.7828597532421461),
                doubleArrayOf(0.2, 0.2, 300.0) to doubleArrayOf(0.13199988336769203, 0.0, 0.273362252191656),
                doubleArrayOf(0.55, 0.5, 20.0) to doubleArrayOf(0.8571618655108819, 0.0, 0.1882214503062765),
                doubleArrayOf(0.98, 0.05, 250.0) to doubleArrayOf(0.9141847493856686, 0.9832942093846239, 1.0),
                doubleArrayOf(0.05, 0.1, 30.0) to doubleArrayOf(0.013078589049334073, 0.0, 0.0),
                // Near enough that clipping the color itself is already within the JND.
                doubleArrayOf(0.853, 0.19, 84.46) to doubleArrayOf(1.0, 0.7591067638762454, 0.0),
                doubleArrayOf(0.468, 0.205, 8.502) to doubleArrayOf(0.6812373103694702, 0.0, 0.2618393680444845),
            ),
            DisplayP3.gamut to listOf(
                doubleArrayOf(0.7, 0.35, 30.0) to doubleArrayOf(1.0, 0.2850955096115956, 0.19277696636614394),
                doubleArrayOf(0.5, 0.3, 140.0) to doubleArrayOf(0.1521733490480411, 0.48083816696172005, 0.0),
                doubleArrayOf(0.9, 0.3, 100.0) to doubleArrayOf(0.9942952174901488, 0.8750580968636537, 0.0),
                doubleArrayOf(0.4, 0.4, 264.1) to doubleArrayOf(0.0, 0.0, 0.8605280668825523),
                doubleArrayOf(0.3717, 0.3, 261.13) to doubleArrayOf(0.0, 0.0, 0.7701026419540096),
                doubleArrayOf(0.6, 0.25, 200.0) to doubleArrayOf(0.0, 0.5902650541820983, 0.6366096575649905),
                doubleArrayOf(0.8, 0.4, 330.0) to doubleArrayOf(1.0, 0.44380677241347505, 1.0),
                doubleArrayOf(0.95, 0.2, 60.0) to doubleArrayOf(1.0, 0.902604979119858, 0.7732469976640679),
                doubleArrayOf(0.2, 0.2, 300.0) to doubleArrayOf(0.11597967352491317, 0.0, 0.26676165943762764),
                doubleArrayOf(0.55, 0.5, 20.0) to doubleArrayOf(0.8185949406553992, 0.0, 0.17784955998250687),
                doubleArrayOf(0.98, 0.05, 250.0) to doubleArrayOf(0.9254772889924076, 0.9813060881305332, 1.0),
                doubleArrayOf(0.05, 0.1, 30.0) to doubleArrayOf(0.011352691590150558, 0.0, 0.0),
                doubleArrayOf(0.864, 0.257, 164.874) to doubleArrayOf(0.0, 0.9978693373001034, 0.6789866006331982),
                doubleArrayOf(0.458, 0.193, 152.475) to doubleArrayOf(0.0, 0.43851571020544966, 0.13238483567727294),
            ),
        )
        for ((gamut, pairs) in cases) {
            for ((input, expected) in pairs) {
                val color = OkLch(input[0], input[1], input[2])
                assertFalse(color.isInGamut(gamut, 0.0))
                assertComponents(expected, color.toGamut(gamut), 1e-9)
            }
        }
    }

    @Test
    fun chromaReductionKeepsLightnessAndHueAndStopsAtTheEdge() {
        val random = Random(20260925)
        for (gamut in listOf(Srgb.gamut, DisplayP3.gamut)) {
            repeat(300) {
                val lightness = random.nextDouble(0.05, 0.95)
                val hue = random.nextDouble(0.0, 360.0)
                val edge = gamut.maxChroma(lightness, hue)
                val mapped = OkLch(lightness, edge + random.nextDouble(0.001, 0.3), hue).toGamut(gamut, GamutMapping.ChromaReduction)
                assertTrue(mapped.isInGamut(gamut, 0.0))
                val back = mapped.to(OkLch)
                val at = "$gamut at L $lightness, $hue°"
                assertNear(lightness, back[OkLch.L]!!, 1e-9, "$at lightness")
                assertNear(edge, back[OkLch.C]!!, 1e-9, "$at chroma")
                assertNear(0.0, hueDifference(hue, back[OkLch.H]!!), 1e-7, "$at hue")
            }
        }
    }

    @Test
    fun chromaReductionInsidePureBluesSliverStopsBeforeIt() {
        // At L 0.42 and 264.1° sRGB holds chroma up to 0.2504 and again from 0.2872 to 0.2909. A color
        // in the gap comes down to the first stretch's end, not up to the second's.
        val edge = Srgb.gamut.maxChroma(0.42, 264.1)
        assertNear(0.2909, edge, 1e-4)
        val mapped = OkLch(0.42, 0.27, 264.1).toGamut(Srgb.gamut, GamutMapping.ChromaReduction).to(OkLch)
        assertNear(0.2504, mapped[OkLch.C]!!, 1e-4)
        assertNear(edge, OkLch(0.42, 0.4, 264.1).toGamut(Srgb.gamut, GamutMapping.ChromaReduction).to(OkLch)[OkLch.C]!!, 1e-9)
    }

    @Test
    fun everyMethodLandsInTheGamutAndCssStaysCloseToTheEdge() {
        // The limits of issue 14521's shape: lightness and hue kept almost exactly, and chroma not
        // cut more than it has to be. The binary search keeps a little chroma past the exact edge by
        // design, so its lightness and hue move by up to its JND.
        val random = Random(20260926)
        for (gamut in listOf(Srgb.gamut, DisplayP3.gamut)) {
            repeat(300) {
                val lightness = random.nextDouble(0.02, 0.98)
                val hue = random.nextDouble(0.0, 360.0)
                val edge = gamut.maxChroma(lightness, hue)
                val color = OkLch(lightness, edge + random.nextDouble(0.001, 0.4), hue)
                for (method in methods) assertTrue(color.toGamut(gamut, method).isInGamut(gamut, 0.0), "$method, $gamut")
                val back = color.toGamut(gamut).to(OkLch)
                val at = "css to $gamut at L $lightness, $hue°"
                assertNear(lightness, back[OkLch.L]!!, 0.02, "$at lightness")
                assertTrue(back[OkLch.C]!! >= edge - 0.02, "$at chroma ${back[OkLch.C]} against the edge $edge")
            }
        }
    }

    @Test
    fun aSearchWithNoJndLandsOnTheExactEdge() {
        // With no JND to stop at, the binary search runs down to its epsilon beside the exact answer.
        val random = Random(20260928)
        val search = GamutMapping.Css(jnd = 0.0)
        repeat(100) {
            val color = OkLch(random.nextDouble(0.1, 0.9), random.nextDouble(0.3, 0.5), random.nextDouble(0.0, 360.0))
            val exact = color.toGamut(Srgb.gamut, GamutMapping.ChromaReduction).to(OkLch)[OkLch.C]!!
            assertNear(exact, color.toGamut(Srgb.gamut, search).to(OkLch)[OkLch.C]!!, 2e-4, "$color")
        }
    }

    @Test
    fun aMissingLightnessMapsAsBlack() {
        assertComponents(doubleArrayOf(0.0, 0.0, 0.0), Lab(null, 20.0, -30.0).toGamut(Srgb.gamut), 0.0)
    }

    @Test
    fun missingComponentsCountAsZeroAndAMissingAlphaStays() {
        val mapped = OkLch(0.7, 0.4, null, alpha = null).toGamut(Srgb.gamut)
        assertEquals(ColorValue.MISSING_ALPHA, mapped.missingMask)
        assertComponents(OkLch(0.7, 0.4, 0.0).toGamut(Srgb.gamut).components(), mapped, 0.0)
        assertEquals(0.5, OkLch(0.7, 0.4, 30.0, alpha = 0.5).toGamut(Srgb.gamut).alpha)
    }

    @Test
    fun cssParametersAreChecked() {
        assertEquals(GamutMapping.Css(), GamutMapping.Css(jnd = 0.02, epsilon = 1e-4))
        assertFailsWith<IllegalArgumentException> { GamutMapping.Css(jnd = -1.0) }
        assertFailsWith<IllegalArgumentException> { GamutMapping.Css(epsilon = 0.0) }
    }

    private fun hueDifference(a: Double, b: Double): Double {
        val difference = abs(a - b) % 360.0
        return if (difference > 180.0) 360.0 - difference else difference
    }
}
