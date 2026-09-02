package codes.side.colorpicker.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RgbColorTest {

    @Test
    fun defaultValues() {
        val rgb = RgbColor()
        assertEquals(0f, rgb.red)
        assertEquals(0f, rgb.green)
        assertEquals(0f, rgb.blue)
        assertEquals(1f, rgb.alpha)
    }

    // ---- Boundaries ----

    @Test
    fun redBoundaries() {
        assertEquals(0f, RgbColor(red = 0f).red)
        assertEquals(1f, RgbColor(red = 1f).red)
        assertFailsWith<IllegalArgumentException> { RgbColor(red = 1.01f) }
        assertFailsWith<IllegalArgumentException> { RgbColor(red = -0.01f) }
    }

    @Test
    fun greenBoundaries() {
        assertEquals(0f, RgbColor(green = 0f).green)
        assertEquals(1f, RgbColor(green = 1f).green)
        assertFailsWith<IllegalArgumentException> { RgbColor(green = 1.01f) }
        assertFailsWith<IllegalArgumentException> { RgbColor(green = -0.01f) }
    }

    @Test
    fun blueBoundaries() {
        assertEquals(0f, RgbColor(blue = 0f).blue)
        assertEquals(1f, RgbColor(blue = 1f).blue)
        assertFailsWith<IllegalArgumentException> { RgbColor(blue = 1.01f) }
        assertFailsWith<IllegalArgumentException> { RgbColor(blue = -0.01f) }
    }

    @Test
    fun alphaBoundaries() {
        assertEquals(0f, RgbColor(alpha = 0f).alpha)
        assertEquals(1f, RgbColor(alpha = 1f).alpha)
        assertFailsWith<IllegalArgumentException> { RgbColor(alpha = 1.01f) }
        assertFailsWith<IllegalArgumentException> { RgbColor(alpha = -0.01f) }
    }

    // ---- Int accessors ----

    @Test
    fun intAccessors() {
        val rgb =
            RgbColor(red = 128f / 255f, green = 64f / 255f, blue = 32f / 255f, alpha = 200f / 255f)
        assertEquals(128, rgb.intRed)
        assertEquals(64, rgb.intGreen)
        assertEquals(32, rgb.intBlue)
        assertEquals(200, rgb.intAlpha)
    }

    @Test
    fun intAccessorsAtZero() {
        val rgb = RgbColor(red = 0f, green = 0f, blue = 0f, alpha = 0f)
        assertEquals(0, rgb.intRed)
        assertEquals(0, rgb.intGreen)
        assertEquals(0, rgb.intBlue)
        assertEquals(0, rgb.intAlpha)
    }

    @Test
    fun intAccessorsAtMax() {
        val rgb = RgbColor(red = 1f, green = 1f, blue = 1f, alpha = 1f)
        assertEquals(255, rgb.intRed)
        assertEquals(255, rgb.intGreen)
        assertEquals(255, rgb.intBlue)
        assertEquals(255, rgb.intAlpha)
    }

    @Test
    fun intAccessorMidpoint() {
        val rgb = RgbColor(red = 0.5f)
        assertEquals(128, rgb.intRed) // 0.5 * 255 = 127.5 -> rounds to 128
    }

    // ---- Companion constants ----

    @Test
    fun companionBlack() {
        val c = RgbColor.Black
        assertEquals(0f, c.red)
        assertEquals(0f, c.green)
        assertEquals(0f, c.blue)
        assertEquals(1f, c.alpha)
    }

    @Test
    fun companionWhite() {
        val c = RgbColor.White
        assertEquals(1f, c.red)
        assertEquals(1f, c.green)
        assertEquals(1f, c.blue)
    }

    @Test
    fun companionPrimaryColors() {
        assertEquals(1f, RgbColor.Red.red)
        assertEquals(0f, RgbColor.Red.green)
        assertEquals(0f, RgbColor.Red.blue)

        assertEquals(0f, RgbColor.Green.red)
        assertEquals(1f, RgbColor.Green.green)
        assertEquals(0f, RgbColor.Green.blue)

        assertEquals(0f, RgbColor.Blue.red)
        assertEquals(0f, RgbColor.Blue.green)
        assertEquals(1f, RgbColor.Blue.blue)
    }

    // ---- fromInt factory ----

    @Test
    fun fromIntBasicConversion() {
        val rgb = RgbColor.fromInt(red = 128, green = 64, blue = 32, alpha = 200)
        assertEquals(128 / 255f, rgb.red)
        assertEquals(64 / 255f, rgb.green)
        assertEquals(32 / 255f, rgb.blue)
        assertEquals(200 / 255f, rgb.alpha)
    }

    @Test
    fun fromIntDefaultAlpha() {
        val rgb = RgbColor.fromInt(red = 0, green = 0, blue = 0)
        assertEquals(1f, rgb.alpha)
    }

    @Test
    fun fromIntClampsValues() {
        val rgb = RgbColor.fromInt(red = 300, green = -10, blue = 999, alpha = 256)
        assertEquals(1f, rgb.red)
        assertEquals(0f, rgb.green)
        assertEquals(1f, rgb.blue)
        assertEquals(1f, rgb.alpha)
    }

    // ---- value semantics ----

    @Test
    fun equalityByValue() {
        val a = RgbColor(red = 0.3f, green = 0.5f, blue = 0.7f, alpha = 0.9f)
        val b = RgbColor(red = 0.3f, green = 0.5f, blue = 0.7f, alpha = 0.9f)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun negativeZeroEqualsZero() {
        val negativeZero = RgbColor(red = -0.0f, green = -0.0f, blue = -0.0f, alpha = 1f)
        val zero = RgbColor(red = 0f, green = 0f, blue = 0f, alpha = 1f)
        assertEquals(zero, negativeZero)
        assertEquals(zero.hashCode(), negativeZero.hashCode())
        assertEquals(0f, negativeZero.red)
    }

    @Test
    fun copyNormalizesNegativeZero() {
        val copy = RgbColor(red = 0.5f).copy(red = -0.0f)
        assertEquals(RgbColor(red = 0f), copy)
    }

    @Test
    fun fractionalRedValue() {
        val rgb = RgbColor(red = 0.392f)
        assertEquals(0.392f, rgb.red)
    }
}
