package codes.side.colorpicker.conversion

import codes.side.colorpicker.model.RgbColor
import kotlin.test.Test
import kotlin.test.assertEquals

class RgbConversionsTest {

    // ---- contrastColor: WCAG relative luminance, black text if luminance > 0.179 ----

    @Test
    fun contrastColorOnBlack() {
        val contrast = RgbColor.Black.contrastColor()
        assertEquals(RgbColor.White, contrast)
    }

    @Test
    fun contrastColorOnWhite() {
        val contrast = RgbColor.White.contrastColor()
        assertEquals(RgbColor.Black, contrast)
    }

    @Test
    fun contrastColorOnDarkRed() {
        // luminance = 0.2126 * linearize(0.392) = ~0.027 -> <= 0.179 -> White
        val contrast = RgbColor(red = 0.392f, green = 0f, blue = 0f).contrastColor()
        assertEquals(RgbColor.White, contrast)
    }

    @Test
    fun contrastColorOnBrightYellow() {
        // luminance = 0.2126 + 0.7152 = 0.9278 -> > 0.179 -> Black
        val contrast = RgbColor(red = 1f, green = 1f, blue = 0f).contrastColor()
        assertEquals(RgbColor.Black, contrast)
    }

    @Test
    fun contrastColorOnMidGray() {
        // luminance = linearize(0.502) = ~0.216 -> > 0.179 -> Black
        val contrast = RgbColor(0.502f, 0.502f, 0.502f).contrastColor()
        assertEquals(RgbColor.Black, contrast)
    }

    @Test
    fun contrastColorOnLightGray() {
        // luminance = linearize(0.784) = ~0.577 -> > 0.179 -> Black
        val contrast = RgbColor(0.784f, 0.784f, 0.784f).contrastColor()
        assertEquals(RgbColor.Black, contrast)
    }

    @Test
    fun contrastColorOnPureRed() {
        // luminance = 0.2126 -> > 0.179 -> Black
        val contrast = RgbColor.Red.contrastColor()
        assertEquals(RgbColor.Black, contrast)
    }

    @Test
    fun contrastColorOnPureGreen() {
        // luminance = 0.7152 -> > 0.179 -> Black
        val contrast = RgbColor.Green.contrastColor()
        assertEquals(RgbColor.Black, contrast)
    }

    @Test
    fun contrastColorOnPureBlue() {
        // luminance = 0.0722 -> <= 0.179 -> White
        val contrast = RgbColor.Blue.contrastColor()
        assertEquals(RgbColor.White, contrast)
    }

    @Test
    fun contrastColorJustBelowThreshold() {
        // luminance = linearize(0.45) = ~0.171 -> <= 0.179 -> White
        val contrast = RgbColor(0.45f, 0.45f, 0.45f).contrastColor()
        assertEquals(RgbColor.White, contrast)
    }

    @Test
    fun contrastColorJustAboveThreshold() {
        // luminance = linearize(0.47) = ~0.187 -> > 0.179 -> Black
        val contrast = RgbColor(0.47f, 0.47f, 0.47f).contrastColor()
        assertEquals(RgbColor.Black, contrast)
    }
}
