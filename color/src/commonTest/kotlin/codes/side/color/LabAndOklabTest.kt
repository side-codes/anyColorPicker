package codes.side.color

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Expected values are color.js 0.7.1 outputs for the same inputs, or CSS Color 4's own examples.
class LabAndOklabTest {

    @Test
    fun theCssWorkedExampleInLab() {
        // CSS Color 4: #7654CD is lab(44.36% 36.05 -58.99).
        assertComponents(doubleArrayOf(44.35772389400392, 36.047904790759866, -58.98589914832903), hex(0x76, 0x54, 0xCD).to(Lab), 1e-9)
        assertComponents(doubleArrayOf(44.35772389400392, 69.12877648375192, 301.4302490907625), hex(0x76, 0x54, 0xCD).to(Lch), 1e-9)
    }

    @Test
    fun labToSrgb() {
        assertComponents(doubleArrayOf(0.5211546420044169, 0.42365695312687185, 0.6685103406759236), Lab(50.0, 20.0, -30.0).to(Srgb), 1e-9)
    }

    @Test
    fun srgbRedInOklabAndOkLch() {
        assertComponents(doubleArrayOf(0.6279553639214311, 0.2248630684262744, 0.125846277330585), Srgb(1.0, 0.0, 0.0).to(Oklab), 1e-12)
        assertComponents(doubleArrayOf(0.6279553639214311, 0.2576833038053608, 29.23388027962784), Srgb(1.0, 0.0, 0.0).to(OkLch), 1e-9)
        assertComponents(doubleArrayOf(0.49931445584520834, 0.09866437712418327, 250.4330574201754), Srgb(0.2, 0.4, 0.6).to(OkLch), 1e-9)
    }

    @Test
    fun okLchToSrgb() {
        assertComponents(doubleArrayOf(0.4070375553869942, 0.7077423917482657, 0.339612790200408), OkLch(0.7, 0.15, 140.0).to(Srgb), 1e-12)
    }

    @Test
    fun chromaBeyondTheOldCapIsHeld() {
        // 1.x rejected chroma above 0.4; CSS takes any non-negative chroma.
        assertEquals(0.45, OkLch(0.7, 0.45, 140.0)[OkLch.C])
    }

    @Test
    fun greysComeOutWithoutAHue() {
        for (grey in doubleArrayOf(0.0, 0.18, 0.5, 1.0)) {
            val oklch = Srgb(grey, grey, grey).to(OkLch)
            assertTrue(oklch.isMissing(OkLch.H), "oklch of grey $grey")
            assertEquals(0.0, oklch[OkLch.C], "oklch chroma of grey $grey")
            val lch = Srgb(grey, grey, grey).to(Lch)
            assertTrue(lch.isMissing(Lch.H), "lch of grey $grey")
            assertEquals(0.0, lch[Lch.C], "lch chroma of grey $grey")
        }
    }

    @Test
    fun powerlessThresholdsSitWhereCssPutsThem() {
        // WPT's boundary cases: at the threshold the hue is powerless, just above it is kept.
        assertTrue(Lab(50.0, 0.0015, 0.0).to(Lch).isMissing(Lch.H))
        assertFalse(Lab(50.0, 0.00151, 0.0).to(Lch).isMissing(Lch.H))
        assertTrue(Oklab(0.5, 0.000004, 0.0).to(OkLch).isMissing(OkLch.H))
        assertFalse(Oklab(0.5, 0.0000041, 0.0).to(OkLch).isMissing(OkLch.H))
    }

    @Test
    fun aMissingLightnessCarriesForward() {
        val converted = Lab(null, 20.0, -30.0).to(Oklab)
        assertTrue(converted.isMissing(Oklab.L))
        assertFalse(converted.isMissing(Oklab.A))
    }

    @Test
    fun missingOpponentAxesBecomeMissingChromaAndHue() {
        // CSS's analogous-sets rule (issue 10210): lab(50% none none) → lch(50% none none).
        val converted = Lab(50.0, null, null).to(Lch)
        assertEquals(50.0, converted[Lch.L])
        assertTrue(converted.isMissing(Lch.C))
        assertTrue(converted.isMissing(Lch.H))
    }

    @Test
    fun allMissingRgbBecomesAllMissingOklab() {
        // rgb(none none none / 50%) → oklab(none none none / 50%).
        val converted = Srgb(null, null, null, alpha = 0.5).to(Oklab)
        assertTrue(converted.isMissing(Oklab.L) && converted.isMissing(Oklab.A) && converted.isMissing(Oklab.B))
        assertEquals(0.5, converted.alpha)
    }

    @Test
    fun aMissingHueCountsAsZeroOutsideItsFamily() {
        // CSS treats none as 0 in the arithmetic: oklch(0.6 0.1 none) is oklch(0.6 0.1 0).
        val withoutHue = OkLch(0.6, 0.1, null).to(Srgb)
        val atZero = OkLch(0.6, 0.1, 0.0).to(Srgb)
        assertComponents(atZero.components(), withoutHue, 1e-15)
    }

    @Test
    fun aMissingHueCarriesToAnotherPolarSpace() {
        assertTrue(OkLch(0.6, 0.1, null).to(Lch).isMissing(Lch.H))
    }

    @Test
    fun equivalentColorsInDifferentSpaces() {
        assertTrue(Srgb(1.0, 0.0, 0.0).isEquivalentTo(Srgb(1.0, 0.0, 0.0).to(OkLch)))
        assertTrue(Srgb(1.0, 0.0, 0.0).isEquivalentTo(Srgb(1.0, 0.0, 0.0).to(Lab)))
        assertFalse(Srgb(1.0, 0.0, 0.0).isEquivalentTo(Srgb(1.0, 0.001, 0.0)))
        assertFalse(Srgb(1.0, 0.0, 0.0).isEquivalentTo(Srgb(1.0, 0.0, 0.0, alpha = 0.5)))
    }

    @Test
    fun aPolarFactoryBuildsLch() {
        val built = ColorSpace.polar("--my-lch", Lab, 150.0, 0.0015, HueFamily.CieLab)
        assertComponents(Lab(44.0, 36.0, -59.0).to(Lch).components(), Lab(44.0, 36.0, -59.0).to(built), 1e-12)
        assertFailsWith<IllegalArgumentException> { ColorSpace.polar("lch", Lab, 150.0, 0.0015, HueFamily.CieLab) }
    }

    @Test
    fun anOverflowThatComesOutNaNReadsAsZero() {
        // Cubing 1e200 overflows, and the matrix back to XYZ then subtracts infinities.
        assertTrue(Oklab(1e200, 0.0, 0.0).to(XyzD65).components().contentEquals(doubleArrayOf(0.0, 0.0, 0.0)))
    }

    @Test
    fun aPolarSpaceNeedsAUsableChromaReferenceAndThreshold() {
        for (reference in listOf(Double.POSITIVE_INFINITY, Double.NaN, 0.0, -1.0)) {
            assertFailsWith<IllegalArgumentException>("$reference") { ColorSpace.polar("--p", Lab, reference, 0.0015, HueFamily.CieLab) }
        }
        for (threshold in listOf(Double.POSITIVE_INFINITY, Double.NaN, -1.0)) {
            assertFailsWith<IllegalArgumentException>("$threshold") { ColorSpace.polar("--p", Lab, 150.0, threshold, HueFamily.CieLab) }
        }
    }

    @Test
    fun aPolarSpaceKeepsItsLightnessKind() {
        val polar = ColorSpace.polar("--angled-polar", AngledLightness, 1.0, 0.0, HueFamily("--angled"))
        assertEquals(AngledLightness.channels[0].kind, polar.L.kind)
    }

    private fun hex(r: Int, g: Int, b: Int): ColorValue = Srgb(r / 255.0, g / 255.0, b / 255.0)

    // A space whose first channel is not a plain quantity, to see its kind carried into the polar form.
    @OptIn(ExperimentalColorSpaceApi::class)
    private object AngledLightness : ColorSpace(
        "--angled",
        listOf(
            ColorChannel("l", 0.0..360.0, kind = ChannelKind.Hue(HueFamily("--angled"))),
            ColorChannel("a", -1.0..1.0),
            ColorChannel("b", -1.0..1.0),
        ),
        XyzD65,
    ) {
        override fun toBase(src: DoubleArray, dst: DoubleArray) {
            src.copyInto(dst, 0, 0, 3)
        }

        override fun fromBase(src: DoubleArray, dst: DoubleArray) {
            src.copyInto(dst, 0, 0, 3)
        }
    }
}
