package codes.side.colorpicker.conversion

import codes.side.colorpicker.model.RgbColor
import kotlin.test.Test
import kotlin.test.assertEquals

class RgbConversionsTest {

    // ---- contrastColor threshold: luminance > 0.729f ----

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
        // luminance = 0.392 * 0.299 = ~0.117 -> < 0.729 -> White
        val contrast = RgbColor(red = 0.392f, green = 0f, blue = 0f).contrastColor()
        assertEquals(RgbColor.White, contrast)
    }

    @Test
    fun contrastColorOnBrightYellow() {
        // luminance = 1.0 * 0.299 + 1.0 * 0.587 + 0 * 0.114 = 0.886 -> > 0.729 -> Black
        val contrast = RgbColor(red = 1f, green = 1f, blue = 0f).contrastColor()
        assertEquals(RgbColor.Black, contrast)
    }

    @Test
    fun contrastColorOnMidGray() {
        // luminance = 0.502 * (0.299 + 0.587 + 0.114) = 0.502 -> < 0.729 -> White
        val contrast = RgbColor(0.502f, 0.502f, 0.502f).contrastColor()
        assertEquals(RgbColor.White, contrast)
    }

    @Test
    fun contrastColorOnLightGray() {
        // luminance = 0.784 * 1.0 = 0.784 -> > 0.729 -> Black
        val contrast = RgbColor(0.784f, 0.784f, 0.784f).contrastColor()
        assertEquals(RgbColor.Black, contrast)
    }

    @Test
    fun contrastColorOnPureRed() {
        // luminance = 1.0 * 0.299 = 0.299 -> < 0.729 -> White
        val contrast = RgbColor.Red.contrastColor()
        assertEquals(RgbColor.White, contrast)
    }

    @Test
    fun contrastColorOnPureGreen() {
        // luminance = 1.0 * 0.587 = 0.587 -> < 0.729 -> White
        val contrast = RgbColor.Green.contrastColor()
        assertEquals(RgbColor.White, contrast)
    }

    @Test
    fun contrastColorOnPureBlue() {
        // luminance = 1.0 * 0.114 = 0.114 -> < 0.729 -> White
        val contrast = RgbColor.Blue.contrastColor()
        assertEquals(RgbColor.White, contrast)
    }

    @Test
    fun contrastColorNearThreshold() {
        // luminance exactly 0.729 -> NOT > 0.729 -> White
        // R * 0.299 + G * 0.587 + B * 0.114 = 0.729
        // Use achromatic: 0.729 * (0.299+0.587+0.114) = 0.729, so R=G=B=0.729
        val contrast = RgbColor(0.729f, 0.729f, 0.729f).contrastColor()
        assertEquals(RgbColor.White, contrast)
    }

    @Test
    fun contrastColorJustAboveThreshold() {
        // 0.73 > 0.729 -> Black
        val contrast = RgbColor(0.73f, 0.73f, 0.73f).contrastColor()
        assertEquals(RgbColor.Black, contrast)
    }
}
