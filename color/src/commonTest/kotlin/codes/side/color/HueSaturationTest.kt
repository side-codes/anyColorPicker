package codes.side.color

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Expected values are color.js 0.7.1 outputs for the same inputs.
class HueSaturationTest {

    @Test
    fun hslBothWays() {
        assertComponents(doubleArrayOf(210.0, 80.0, 50.0), Srgb(0.1, 0.5, 0.9).to(Hsl), 1e-12)
        assertComponents(doubleArrayOf(0.09999999999999998, 0.6333333333333329, 0.9), Hsl(200.0, 80.0, 50.0).to(Srgb), 1e-12)
    }

    @Test
    fun hwbBothWays() {
        assertComponents(doubleArrayOf(210.0, 9.99999999999999, 10.0), Srgb(0.1, 0.5, 0.9).to(Hwb), 1e-12)
        assertComponents(doubleArrayOf(0.20000000000000007, 0.7, 0.20000000000000007), Hwb(120.0, 20.0, 30.0).to(Srgb), 1e-12)
    }

    @Test
    fun hsvBothWays() {
        assertComponents(doubleArrayOf(210.0, 88.8888888888889, 90.0), Srgb(0.1, 0.5, 0.9).to(Hsv), 1e-12)
        assertComponents(doubleArrayOf(0.1, 0.5, 0.9), Hsv(210.0, 88.8888888888889, 90.0).to(Srgb), 1e-12)
    }

    @Test
    fun aWideGamutColorIsHeldOutsideHslsRanges() {
        assertComponents(
            doubleArrayOf(127.88028827951307, 301.9517233025623, 25.333033767622904),
            DisplayP3(0.0, 1.0, 0.0).to(Hsl),
            1e-9,
        )
    }

    @Test
    fun hslTurnsANegativeSaturationIntoAHalfTurnOfHue() {
        // All three channels negative: the formula's saturation goes below zero and CSS turns the
        // hue half a turn instead (issue 9222).
        val color = Srgb(-0.2, -0.5, -0.4)
        val hsl = color.to(Hsl)
        assertComponents(doubleArrayOf(160.0, 42.857142857142854, -35.0), hsl, 1e-12)
        assertComponents(color.components(), hsl.to(Srgb), 1e-12)
    }

    @Test
    fun hsvKeepsANegativeSaturation() {
        // HSV's hexcone has no half-turn symmetry, so the negative S itself is what round-trips.
        val color = Srgb(-0.2, -0.5, -0.4)
        val hsv = color.to(Hsv)
        assertComponents(doubleArrayOf(340.0, -150.0, -20.0), hsv, 1e-12)
        assertComponents(color.components(), hsv.to(Srgb), 1e-12)
    }

    @Test
    fun hwbTakesHslsHueForANegativeSaturation() {
        // The same color as the HSL case: HWB reads its hue through HSL, half turn included.
        assertComponents(doubleArrayOf(160.0, -50.0, 120.0), Srgb(-0.2, -0.5, -0.4).to(Hwb), 1e-12)
    }

    @Test
    fun greysHaveNoHue() {
        for (space in listOf(Hsl, Hwb, Hsv)) {
            val grey = Srgb(0.4, 0.4, 0.4).to(space)
            assertTrue(grey.isMissing(space.channels[0]), space.id)
        }
        assertEquals(0.0, Srgb(0.4, 0.4, 0.4).to(Hsl)[Hsl.S])
    }

    @Test
    fun powerlessThresholds() {
        assertTrue(Hsl(120.0, 0.001, 50.0).to(Hsl).let { it[Hsl.H] == 120.0 }, "a hue given directly is kept")
        assertTrue(Hsl(120.0, 0.001, 50.0).to(Hwb).isMissing(Hwb.H))
        assertFalse(Hsl(120.0, 5.0, 50.0).to(Hwb).isMissing(Hwb.H))
        assertTrue(Hwb(120.0, 60.0, 39.9995).to(Hsl).isMissing(Hsl.H))
        assertTrue(Hsv(120.0, 0.3, 0.3).to(Hwb).isMissing(Hwb.H))
        assertTrue(Hwb(120.0, 60.0, 39.9995).to(Hsv).isMissing(Hsv.H))
        assertFalse(Hwb(120.0, 60.0, 39.9).to(Hsv).isMissing(Hsv.H))
    }

    @Test
    fun whitenessAndBlacknessPastFullNormalize() {
        // CSS: W + B ≥ 100% is a grey of W / (W + B).
        assertComponents(doubleArrayOf(0.6, 0.6, 0.6), Hwb(40.0, 60.0, 40.0).to(Srgb), 1e-12)
        assertComponents(doubleArrayOf(0.5, 0.5, 0.5), Hwb(40.0, 80.0, 80.0).to(Srgb), 1e-12)
    }

    @Test
    fun preparedConvertersTakeHuesOutsideOneTurn() {
        // A converter gets components unwrapped; −90° is 270°, −360° is 0° and 450° is 90°.
        for (space in listOf(Hsl, Hwb, Hsv)) {
            val converter = space.converterTo(Srgb)
            for ((given, wrapped) in listOf(-90.0 to 270.0, -360.0 to 0.0, 450.0 to 90.0)) {
                val out = DoubleArray(4)
                val expected = DoubleArray(4)
                converter.convert(doubleArrayOf(given, 20.0, 30.0, 0.0), out)
                converter.convert(doubleArrayOf(wrapped, 20.0, 30.0, 0.0), expected)
                for (i in 0..2) assertNear(expected[i], out[i], 1e-12, "${space.id} at $given°")
            }
        }
    }

    @Test
    fun hueFamiliesFollowTheRgbSpace() {
        assertEquals(Hsl.H.kind, Hsv.H.kind)
        assertEquals(Hsl.H.kind, Hwb.H.kind)
        val p3Hsl = ColorSpace.hsl("--hsl-p3", DisplayP3)
        assertFalse(Hsl.H.kind == p3Hsl.H.kind)
        assertFailsWith<IllegalArgumentException> { ColorSpace.hsv("hsv", DisplayP3) }
    }
}
