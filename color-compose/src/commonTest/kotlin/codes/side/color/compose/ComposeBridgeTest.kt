package codes.side.color.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.Rgb
import androidx.compose.ui.graphics.colorspace.connect
import codes.side.color.ColorSpace
import codes.side.color.ColorSpaces
import codes.side.color.ColorValue
import codes.side.color.DisplayP3
import codes.side.color.GamutMapping
import codes.side.color.HexAlpha
import codes.side.color.Lab
import codes.side.color.OkLch
import codes.side.color.Oklab
import codes.side.color.RgbPrimaries
import codes.side.color.Srgb
import codes.side.color.SrgbLinear
import codes.side.color.TransferFunction
import codes.side.color.WhitePoint
import codes.side.color.XyzD50
import codes.side.color.parseCss
import codes.side.color.toCssString
import codes.side.color.toGamut
import codes.side.color.toHexString
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import androidx.compose.ui.graphics.colorspace.ColorSpace as ComposeSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces as ComposeSpaces

class ComposeBridgeTest {

    private val rebuilt = listOf(
        ComposeSpaces.Bt709, ComposeSpaces.Bt2020, ComposeSpaces.DciP3, ComposeSpaces.Ntsc1953, ComposeSpaces.SmpteC,
        ComposeSpaces.AdobeRgb, ComposeSpaces.ProPhotoRgb, ComposeSpaces.Aces, ComposeSpaces.Acescg,
    )

    @Test
    fun srgbBytesComeBackExact() {
        assertEquals(Srgb(0.2, 0.4, 0.6), Color(0xFF336699).toColorValue())
        assertEquals(Srgb(0.2, 0.4, 0.6, 0x80 / 255.0), Color(0x80336699).toColorValue())
    }

    @Test
    fun eachComposeSpaceIsCopiedIntoItsMatch() {
        val cases = listOf(
            Triple(ComposeSpaces.ExtendedSrgb, Srgb, doubleArrayOf(-0.25, 0.5, 1.5)),
            Triple(ComposeSpaces.LinearSrgb, SrgbLinear, doubleArrayOf(0.25, 0.5, 0.75)),
            Triple(ComposeSpaces.LinearExtendedSrgb, SrgbLinear, doubleArrayOf(-0.25, 0.5, 3.0)),
            Triple(ComposeSpaces.DisplayP3, DisplayP3, doubleArrayOf(0.25, 0.5, 0.75)),
            Triple(ComposeSpaces.CieXyz, XyzD50, doubleArrayOf(0.25, 0.5, 0.75)),
            Triple(ComposeSpaces.CieLab, Lab, doubleArrayOf(50.0, -20.0, 64.0)),
            Triple(ComposeSpaces.Oklab, Oklab, doubleArrayOf(0.5, -0.125, 0.25)),
            Triple(ComposeSpaces.AdobeRgb, ComposeColorSpaces.AdobeRgb, doubleArrayOf(0.25, 0.5, 0.75)),
        )
        for ((composeSpace, space, values) in cases) {
            val color = Color(values[0].toFloat(), values[1].toFloat(), values[2].toFloat(), 0.5f, composeSpace)
            // Alpha is 10 bits outside sRGB: 0.5 is stored as 512/1023.
            assertEquals(space.color(values, 512 / 1023.0), color.toColorValue(), composeSpace.name)
        }
    }

    @Test
    fun rebuiltSpacesConvertAsComposeDoes() {
        // Compose converts in Float through its own D50: at most 1.15e-4 from these conversions.
        val steps = listOf(0.0f, 0.25f, 0.5f, 0.75f, 1.0f)
        for (composeSpace in rebuilt + listOf(ComposeSpaces.DisplayP3, ComposeSpaces.LinearSrgb, ComposeSpaces.ExtendedSrgb)) {
            val connector = composeSpace.connect(ComposeSpaces.CieXyz)
            for (r in steps) for (g in steps) for (b in steps) {
                val expected = connector.transform(r, g, b)
                val actual = Color(r, g, b, 1f, composeSpace).toColorValue().to(XyzD50).components()
                val at = "${composeSpace.name} ($r, $g, $b)"
                for (i in 0..2) assertTrue(abs(expected[i] - actual[i]) <= 2e-4, "$at [$i]: Compose ${expected[i]}, here ${actual[i]}")
            }
        }
    }

    @Test
    fun greysUnderD65OrD50StayExactlyGrey() {
        val spaces = listOf(
            ComposeSpaces.Srgb, ComposeSpaces.LinearSrgb, ComposeSpaces.DisplayP3, ComposeSpaces.Bt709, ComposeSpaces.Bt2020,
            ComposeSpaces.SmpteC, ComposeSpaces.AdobeRgb, ComposeSpaces.ProPhotoRgb,
        )
        for (composeSpace in spaces) {
            val grey = Color(0.5f, 0.5f, 0.5f, 1f, composeSpace).toColorValue().to(Oklab).components()
            assertTrue(abs(grey[1]) <= 1e-12 && abs(grey[2]) <= 1e-12, "${composeSpace.name}: a ${grey[1]}, b ${grey[2]}")
            assertNull(Color(0.5f, 0.5f, 0.5f, 1f, composeSpace).toColorValue().to(OkLch)[OkLch.H], composeSpace.name)
        }
    }

    @Test
    fun srgbIsTheDefaultAndMapsFirst() {
        assertEquals(Color(0xFF336699), Srgb(0.2, 0.4, 0.6).toComposeColor())
        val vivid = OkLch(0.7, 0.4, 30.0)
        assertEquals(Color(vivid.toHexString(HexAlpha.First).substring(1).toLong(16)), vivid.toComposeColor())
        assertEquals(
            Color(vivid.toHexString(HexAlpha.First, GamutMapping.Clip).substring(1).toLong(16)),
            vivid.toComposeColor(mapping = GamutMapping.Clip),
        )
    }

    @Test
    fun aComposeColorHoldingNaNHasNoValue() {
        // Compose clamps components into their space's range, which a NaN passes through.
        val color = Color(Float.NaN, 0.5f, 0.5f, 1f, ComposeSpaces.LinearSrgb)
        assertNull(color.toColorValueOrNull())
        val error = assertFailsWith<IllegalArgumentException> { color.toColorValue() }
        assertTrue("NaN" in error.message.orEmpty(), error.message)
    }

    @Test
    fun srgbRoundsToEightBitsAsTheHexStringDoes() {
        // Beside each half step, where rounding in Float and in Double part ways.
        for (step in 0 until 255) {
            for (nudge in listOf(-1e-9, 1e-9)) {
                val v = (step + 0.5 + nudge) / 255.0
                val color = Srgb(v, v, v, alpha = v)
                assertEquals(Color(color.toHexString(HexAlpha.First).substring(1).toLong(16)), color.toComposeColor(), "$v")
            }
        }
    }

    @Test
    fun parametricTransferMatchesSrgbAndMirrors() {
        // Either side of d but not at it: ICC takes d itself on the power segment, CSS's sRGB on the
        // linear one, 2.3e-9 apart.
        val curve = ParametricTransfer(checkNotNull((ComposeSpaces.Srgb as Rgb).transferParameters))
        for (x in listOf(0.02, 0.04, 0.041, 0.2, 0.5, 1.0, 1.5)) {
            assertEquals(TransferFunction.Srgb.decode(x), curve.decode(x), 1e-12, "decode $x")
            assertEquals(-curve.decode(x), curve.decode(-x), "decode -$x")
            assertEquals(-curve.encode(x), curve.encode(-x), "encode -$x")
            assertEquals(x, curve.encode(curve.decode(x)), 1e-12, "round trip $x")
        }
    }

    @Test
    fun displayP3KeepsTenBitValues() {
        for (k in 0..1023) {
            val color = DisplayP3(k / 1023.0, (1023 - k) / 1023.0, 0.5).toComposeColor(DisplayP3.gamut)
            assertSame(ComposeSpaces.DisplayP3, color.colorSpace)
            val back = color.toColorValue().components()
            assertEquals(k, (back[0] * 1023.0).roundToInt(), "red $k")
            assertEquals(1023 - k, (back[1] * 1023.0).roundToInt(), "green ${1023 - k}")
        }
    }

    @Test
    fun everyRgbSpaceComposeHasRoundTrips() {
        val spaces: List<Pair<ColorSpace, ComposeSpace>> = listOf(
            SrgbLinear to ComposeSpaces.LinearSrgb,
            DisplayP3 to ComposeSpaces.DisplayP3,
        ) + ComposeColorSpaces.all.zip(rebuilt)
        for ((space, composeSpace) in spaces) {
            val color = space.color(doubleArrayOf(0.25, 0.5, 0.75))
            val compose = color.toComposeColor(checkNotNull(space.gamut))
            assertSame(composeSpace, compose.colorSpace, space.id)
            assertEquals(color, compose.toColorValue(), space.id)
        }
    }

    @Test
    fun unspecifiedAndHdrColorsHaveNoValue() {
        assertNull(Color.Unspecified.toColorValueOrNull())
        assertFailsWith<IllegalArgumentException> { Color.Unspecified.toColorValue() }
        for (hdr in listOf(ComposeSpaces.Bt2020Hlg, ComposeSpaces.Bt2020Pq)) {
            assertNull(Color(0.5f, 0.5f, 0.5f, 1f, hdr).toColorValueOrNull(), hdr.name)
            assertFailsWith<IllegalArgumentException> { Color(0.5f, 0.5f, 0.5f, 1f, hdr).toColorValue() }
        }
    }

    @Test
    fun aGamutComposeHasNoSpaceForIsRefused() {
        val mine = ColorSpace.rgb("--mine", RgbPrimaries.Srgb, WhitePoint.D65, TransferFunction.gamma(2.0))
        assertFailsWith<IllegalArgumentException> { Srgb(0.5, 0.5, 0.5).toComposeColor(mine.gamut) }
    }

    @Test
    fun aMissingAlphaIsTransparent() {
        assertEquals(0f, Srgb(1.0, 0.0, 0.0, alpha = null).toComposeColor().alpha)
        assertEquals(0.5f, Srgb(1.0, 0.0, 0.0, alpha = 0.5).toComposeColor().alpha, 1f / 255f)
    }

    @Test
    fun rebuiltSpacesParseBackWhenKnown() {
        val color = ComposeColorSpaces.AdobeRgb.color(doubleArrayOf(0.25, 0.5, 0.75))
        val text = color.toCssString()
        assertEquals("color(--compose-adobe-rgb 0.25 0.5 0.75)", text)
        assertEquals(color, ColorValue.parseCss(text, ColorSpaces.all + ComposeColorSpaces.all))
    }
}
