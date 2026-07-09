package codes.side.colorpicker.conversion

import kotlin.test.Test
import kotlin.test.assertEquals

class BlendArgbTest {

    private val black = argb(255, 0, 0, 0)
    private val white = argb(255, 255, 255, 255)
    private val red = argb(255, 255, 0, 0)
    private val blue = argb(255, 0, 0, 255)

    @Test
    fun blendRatioZeroReturnsColor1() {
        val result = blendArgb(black, white, 0f)
        assertEquals(black, result)
    }

    @Test
    fun blendRatioOneReturnsColor2() {
        val result = blendArgb(black, white, 1f)
        assertEquals(white, result)
    }

    @Test
    fun blendHalfway() {
        // 127.5 rounds to 128 (unlike AndroidX ColorUtils.blendARGB, which truncates to 127)
        val result = blendArgb(black, white, 0.5f).toRgbColor()
        assertEquals(128, result.intRed)
        assertEquals(128, result.intGreen)
        assertEquals(128, result.intBlue)
        assertEquals(255, result.intAlpha)
    }

    @Test
    fun blendWithAlpha() {
        val transparent = argb(0, 255, 0, 0)
        val opaque = argb(255, 255, 0, 0)
        val result = blendArgb(transparent, opaque, 0.5f).toRgbColor()
        assertEquals(128, result.intAlpha)
        assertEquals(255, result.intRed)
    }

    @Test
    fun blendQuarterWay() {
        // 63.75 rounds to 64
        val result = blendArgb(black, white, 0.25f).toRgbColor()
        assertEquals(64, result.intRed)
        assertEquals(64, result.intGreen)
        assertEquals(64, result.intBlue)
    }

    @Test
    fun blendDifferentColors() {
        val result = blendArgb(red, blue, 0.5f).toRgbColor()
        assertEquals(128, result.intRed)
        assertEquals(0, result.intGreen)
        assertEquals(128, result.intBlue)
    }

    @Test
    fun blendThreeQuarterWay() {
        // 191.25 rounds to 191
        val result = blendArgb(black, white, 0.75f).toRgbColor()
        assertEquals(191, result.intRed)
        assertEquals(191, result.intGreen)
        assertEquals(191, result.intBlue)
    }

    @Test
    fun blendSameColor() {
        val result = blendArgb(red, red, 0.5f)
        assertEquals(red, result)
    }

    @Test
    fun blendAlphaChannelOnly() {
        val c1 = argb(0, 100, 100, 100)
        val c2 = argb(255, 100, 100, 100)
        val result = blendArgb(c1, c2, 0.5f).toRgbColor()
        assertEquals(128, result.intAlpha)
        assertEquals(100, result.intRed)
        assertEquals(100, result.intGreen)
        assertEquals(100, result.intBlue)
    }

    // ---- Ratio clamping & NaN ----

    @Test
    fun blendRatioAboveOneClampsToColor2() {
        val result = blendArgb(black, white, 1.5f)
        assertEquals(white, result)
    }

    @Test
    fun blendRatioBelowZeroClampsToColor1() {
        val result = blendArgb(black, white, -0.5f)
        assertEquals(black, result)
    }

    @Test
    fun blendNaNRatioReturnsColor1() {
        // Documented behavior: NaN ratio is treated as 0
        val result = blendArgb(red, blue, Float.NaN)
        assertEquals(red, result)
    }
}
