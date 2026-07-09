package codes.side.colorpicker.conversion

import codes.side.colorpicker.model.RgbColor
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArgbIntTest {

    private fun assertIntRgbEqual(expected: RgbColor, actual: RgbColor) {
        assertEquals(expected.intRed, actual.intRed, "red: ${expected.intRed} vs ${actual.intRed}")
        assertEquals(expected.intGreen, actual.intGreen, "green: ${expected.intGreen} vs ${actual.intGreen}")
        assertEquals(expected.intBlue, actual.intBlue, "blue: ${expected.intBlue} vs ${actual.intBlue}")
        assertEquals(expected.intAlpha, actual.intAlpha, "alpha: ${expected.intAlpha} vs ${actual.intAlpha}")
    }

    // ---- Pack & unpack ----

    @Test
    fun packAndUnpack() {
        val original = RgbColor.fromInt(red = 128, green = 64, blue = 32, alpha = 200)
        val packed = original.toArgbInt()
        val unpacked = packed.toRgbColor()
        assertIntRgbEqual(original, unpacked)
    }

    @Test
    fun packAndUnpackBlack() {
        val rgb = RgbColor(0f, 0f, 0f, alpha = 1f)
        val packed = rgb.toArgbInt()
        val unpacked = packed.toRgbColor()
        assertEquals(0, unpacked.intRed)
        assertEquals(0, unpacked.intGreen)
        assertEquals(0, unpacked.intBlue)
        assertEquals(255, unpacked.intAlpha)
    }

    @Test
    fun packAndUnpackWhite() {
        val rgb = RgbColor(1f, 1f, 1f, alpha = 1f)
        val packed = rgb.toArgbInt()
        val unpacked = packed.toRgbColor()
        assertEquals(255, unpacked.intRed)
        assertEquals(255, unpacked.intGreen)
        assertEquals(255, unpacked.intBlue)
        assertEquals(255, unpacked.intAlpha)
    }

    @Test
    fun packAndUnpackTransparent() {
        val rgb = RgbColor(1f, 0f, 0f, alpha = 0f)
        val packed = rgb.toArgbInt()
        val unpacked = packed.toRgbColor()
        assertEquals(255, unpacked.intRed)
        assertEquals(0, unpacked.intGreen)
        assertEquals(0, unpacked.intBlue)
        assertEquals(0, unpacked.intAlpha)
    }

    // ---- argb() helper ----

    @Test
    fun opaqueRed() {
        val packed = argb(255, 255, 0, 0)
        assertEquals(0xFFFF0000.toInt(), packed)
    }

    @Test
    fun opaqueGreen() {
        val packed = argb(255, 0, 255, 0)
        assertEquals(0xFF00FF00.toInt(), packed)
    }

    @Test
    fun opaqueBlue() {
        val packed = argb(255, 0, 0, 255)
        assertEquals(0xFF0000FF.toInt(), packed)
    }

    @Test
    fun transparentBlack() {
        val packed = argb(0, 0, 0, 0)
        assertEquals(0x00000000, packed)
    }

    @Test
    fun argbClampsOutOfRangeComponents() {
        // 256 clamps to 255, -1 clamps to 0 (instead of wrapping through the 0xFF mask)
        val packed = argb(256, -1, 0, 0)
        assertEquals(argb(255, 0, 0, 0), packed)
    }

    @Test
    fun argbClampsLargeValues() {
        val packed = argb(1000, 512, -300, 300)
        assertEquals(argb(255, 255, 0, 255), packed)
    }

    // ---- setAlphaComponent ----

    @Test
    fun setAlphaOnOpaqueColor() {
        val opaque = argb(255, 100, 150, 200)
        val halfTransparent = setAlphaComponent(opaque, 128)
        val rgb = halfTransparent.toRgbColor()
        assertEquals(128, rgb.intAlpha)
        assertEquals(100, rgb.intRed)
        assertEquals(150, rgb.intGreen)
        assertEquals(200, rgb.intBlue)
    }

    @Test
    fun setAlphaToZero() {
        val opaque = argb(255, 50, 100, 150)
        val transparent = setAlphaComponent(opaque, 0)
        val rgb = transparent.toRgbColor()
        assertEquals(0, rgb.intAlpha)
        assertEquals(50, rgb.intRed)
    }

    @Test
    fun setAlphaToMax() {
        val transparent = argb(0, 50, 100, 150)
        val opaque = setAlphaComponent(transparent, 255)
        val rgb = opaque.toRgbColor()
        assertEquals(255, rgb.intAlpha)
    }

    @Test
    fun setAlphaClampsOutOfRangeValues() {
        val color = argb(255, 50, 100, 150)
        assertEquals(255, setAlphaComponent(color, 300).toRgbColor().intAlpha)
        assertEquals(0, setAlphaComponent(color, -5).toRgbColor().intAlpha)
    }

    // ---- blendArgb ----

    @Test
    fun blendBlackWhite() {
        val black = argb(255, 0, 0, 0)
        val white = argb(255, 255, 255, 255)
        val mid = blendArgb(black, white, 0.5f)
        val rgb = mid.toRgbColor()
        // 127.5 rounds to 128 (blendArgb rounds instead of truncating)
        assertEquals(128, rgb.intRed)
        assertEquals(128, rgb.intGreen)
        assertEquals(128, rgb.intBlue)
        assertEquals(255, rgb.intAlpha)
    }

    // ---- Float -> Int -> Float precision ----

    @Test
    fun floatToIntRoundTrip() {
        // Verify that packing to ARGB int and unpacking preserves 8-bit precision
        val original = RgbColor(red = 0.5f, green = 0.25f, blue = 0.75f, alpha = 0.9f)
        val packed = original.toArgbInt()
        val unpacked = packed.toRgbColor()
        // After quantization to 8-bit, values should be close
        assertTrue(abs(original.red - unpacked.red) < 1f / 255f + 1e-5f, "red precision")
        assertTrue(abs(original.green - unpacked.green) < 1f / 255f + 1e-5f, "green precision")
        assertTrue(abs(original.blue - unpacked.blue) < 1f / 255f + 1e-5f, "blue precision")
        assertTrue(abs(original.alpha - unpacked.alpha) < 1f / 255f + 1e-5f, "alpha precision")
    }
}
