package codes.side.color

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

// Expected values are color.js 0.7.1 outputs for the same inputs.
class RgbSpacesTest {

    @Test
    fun srgbWhiteIsD65() {
        assertComponents(doubleArrayOf(0.9504559270516717, 1.0, 1.0890577507598784), Srgb(1.0, 1.0, 1.0).to(XyzD65), 1e-15)
    }

    @Test
    fun srgbWhiteInD50IsTheD50White() {
        assertComponents(doubleArrayOf(0.9642956764295678, 1.0, 0.8251046025104604), Srgb(1.0, 1.0, 1.0).to(XyzD50), 1e-15)
    }

    @Test
    fun srgbDecodesToLinear() {
        assertComponents(
            doubleArrayOf(0.033104766570885055, 0.13286832155381798, 0.31854677812509186),
            Srgb(0.2, 0.4, 0.6).to(SrgbLinear),
            1e-15,
        )
    }

    @Test
    fun displayP3RedIsOutsideSrgb() {
        assertComponents(
            doubleArrayOf(1.0930663624351615, -0.22674197356975417, -0.15013458093711937),
            DisplayP3(1.0, 0.0, 0.0).to(Srgb),
            1e-12,
        )
    }

    @Test
    fun srgbRedInDisplayP3() {
        assertComponents(
            doubleArrayOf(0.9174875573251656, 0.20028680774084717, 0.13856059121111408),
            Srgb(1.0, 0.0, 0.0).to(DisplayP3),
            1e-12,
        )
    }

    @Test
    fun transferCurvesMirrorNegativeValues() {
        for (x in doubleArrayOf(0.001, 0.03, 0.2, 0.7, 1.4)) {
            val curve = TransferFunction.Srgb
            assertEquals(-curve.decode(x), curve.decode(-x))
            assertEquals(-curve.encode(x), curve.encode(-x))
            assertNear(x, curve.encode(curve.decode(x)), 1e-15, "round trip at $x")
        }
    }

    @Test
    fun aFactoryBuiltSrgbAgreesWithTheLibrarys() {
        val built = ColorSpace.rgb("--my-srgb", RgbPrimaries.Srgb, WhitePoint.D65, TransferFunction.Srgb)
        val expected = Srgb(0.2, 0.4, 0.6).to(XyzD65).components()
        assertComponents(expected, built(0.2, 0.4, 0.6).to(XyzD65), 1e-15)
    }

    @Test
    fun aFactoryBuiltSpaceWithAnotherWhiteIsAdapted() {
        // An sRGB-primaried space whose white is D50 still maps its own white to D50, which XYZ-D65
        // holds as the Bradford image of D50.
        val built = ColorSpace.rgb("--srgb-d50", RgbPrimaries.Srgb, WhitePoint.D50, TransferFunction.Linear)
        val white = built(1.0, 1.0, 1.0).to(XyzD50)
        assertComponents(doubleArrayOf(0.9642956764295676, 1.0, 0.8251046025104602), white, 1e-4)
    }

    @Test
    fun anAppSpaceCannotTakeALibraryId() {
        // Spaces are equal by id, so one called srgb would be taken for Srgb and never converted.
        assertFailsWith<IllegalArgumentException> { ColorSpace.rgb("srgb", RgbPrimaries.Srgb, WhitePoint.D65, TransferFunction.Srgb) }
        assertFailsWith<IllegalArgumentException> { ColorSpace.rgb("--", RgbPrimaries.Srgb, WhitePoint.D65, TransferFunction.Srgb) }
    }

    @Test
    fun p3ToSrgbFusesIntoOneMatrixBetweenTheCurves() {
        assertTrue(DisplayP3.converterTo(Srgb).toString().contains("3 steps"), DisplayP3.converterTo(Srgb).toString())
    }

    @Test
    fun rgbGamutBelongsToItsSpace() {
        assertEquals(Srgb, Srgb.gamut.space)
        assertEquals(1.0, Srgb.gamut.peakLuminance)
        assertEquals(DisplayP3, DisplayP3.gamut.space)
    }
}
