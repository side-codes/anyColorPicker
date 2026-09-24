package codes.side.color.compose

import androidx.compose.ui.graphics.Color
import codes.side.color.ColorSpace
import codes.side.color.ColorSpaces
import codes.side.color.ColorValue
import codes.side.color.DisplayP3
import codes.side.color.GamutMapping
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
        // Compose converts in Float through its own D50 and returns half floats, so 2e-3 is its precision.
        val steps = listOf(0.0f, 0.25f, 0.5f, 0.75f, 1.0f)
        for (composeSpace in rebuilt + listOf(ComposeSpaces.DisplayP3, ComposeSpaces.LinearSrgb, ComposeSpaces.ExtendedSrgb)) {
            for (r in steps) for (g in steps) for (b in steps) {
                val color = Color(r, g, b, 1f, composeSpace)
                val expected = color.convert(ComposeSpaces.CieXyz)
                val actual = color.toColorValue().to(XyzD50).components()
                val at = "${composeSpace.name} ($r, $g, $b)"
                assertTrue(abs(expected.red - actual[0]) <= 2e-3, "$at x: Compose ${expected.red}, here ${actual[0]}")
                assertTrue(abs(expected.green - actual[1]) <= 2e-3, "$at y: Compose ${expected.green}, here ${actual[1]}")
                assertTrue(abs(expected.blue - actual[2]) <= 2e-3, "$at z: Compose ${expected.blue}, here ${actual[2]}")
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
        val mapped = vivid.toGamut(Srgb.gamut).components()
        assertEquals(Color(mapped[0].toFloat(), mapped[1].toFloat(), mapped[2].toFloat()), vivid.toComposeColor())
        val clipped = vivid.toGamut(Srgb.gamut, GamutMapping.Clip).components()
        assertEquals(Color(clipped[0].toFloat(), clipped[1].toFloat(), clipped[2].toFloat()), vivid.toComposeColor(mapping = GamutMapping.Clip))
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
