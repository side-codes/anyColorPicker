package codes.side.color

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ChannelStepTest {

    private fun assertSteps(step: Double, pageStep: Double, vararg channels: ColorChannel) {
        for (channel in channels) {
            assertNear(step, channel.step, 1e-15, "$channel step")
            assertNear(pageStep, channel.pageStep, 1e-14, "$channel page step")
        }
    }

    @Test
    fun hueStepsByADegree() {
        assertSteps(1.0, 10.0, Hsl.H, Hwb.H, Hsv.H, Lch.H, OkLch.H, Okhsl.H, Okhsv.H)
    }

    @Test
    fun rgbStepsByAnEightBitLevel() {
        val channels = Srgb.channels + SrgbLinear.channels + DisplayP3.channels
        assertSteps(1.0 / 255.0, 17.0 / 255.0, *channels.toTypedArray())
    }

    @Test
    fun hundredWideChannelsStepByOne() {
        assertSteps(1.0, 10.0, Hsl.S, Hsl.L, Hwb.W, Hwb.B, Hsv.S, Hsv.V, Lab.L, Lab.A, Lab.B, Lch.L, Lch.C)
    }

    @Test
    fun unitChannelsStepByAHundredth() {
        assertSteps(
            0.01,
            0.1,
            Okhsl.S, Okhsl.L, Okhsv.S, Okhsv.V, Oklab.L, OkLch.L,
            Cmyk.C, Cmyk.M, Cmyk.Y, Cmyk.K,
            XyzD65.X, XyzD65.Y, XyzD65.Z, XyzD50.X, XyzD50.Y, XyzD50.Z,
        )
    }

    @Test
    fun okChromaAndOpponentAxesStepByAThousandth() {
        assertSteps(0.001, 0.01, Oklab.A, Oklab.B, OkLch.C)
    }

    @Test
    fun aDerivedStepIsTheLiteralItNames() {
        assertEquals(0.01, ColorChannel("x", 0.0..1.0).step)
        assertEquals(0.001, ColorChannel("x", 0.0..0.4).step)
    }

    @Test
    fun anAppChannelDerivesItsSteps() {
        assertSteps(0.01, 0.1, ColorChannel("x", -2.0..2.0))
    }

    @Test
    fun aChannelMayNameItsOwnSteps() {
        val channel = ColorChannel("x", 0.0..1.0, step = 0.25, pageStep = 0.5)
        assertEquals(0.25, channel.step)
        assertEquals(0.5, channel.pageStep)
    }

    @Test
    fun aStepMustBePositive() {
        assertFailsWith<IllegalArgumentException> { ColorChannel("x", 0.0..1.0, step = 0.0) }
        assertFailsWith<IllegalArgumentException> { ColorChannel("x", 0.0..1.0, step = Double.NaN) }
    }

    @Test
    fun aPageStepIsAtLeastOneStep() {
        assertFailsWith<IllegalArgumentException> { ColorChannel("x", 0.0..1.0, step = 0.1, pageStep = 0.05) }
    }

    @Test
    fun aReferenceRangeWithoutSpanIsRefused() {
        assertFailsWith<IllegalArgumentException> { ColorChannel("x", 1.0..1.0) }
    }

    @Test
    fun aReferenceRangeWithoutAFiniteSpanIsRefusedWhateverTheStep() {
        assertFailsWith<IllegalArgumentException> { ColorChannel("x", 1.0..1.0, step = 0.1) }
        assertFailsWith<IllegalArgumentException> { ColorChannel("x", 0.0..Double.POSITIVE_INFINITY, step = 0.1) }
    }
}
