package codes.side.colorpicker.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CmykColorTest {

    @Test
    fun defaultValues() {
        val c = CmykColor()
        assertEquals(0f, c.cyan)
        assertEquals(0f, c.magenta)
        assertEquals(0f, c.yellow)
        assertEquals(0f, c.key)
        assertEquals(1f, c.alpha)
    }

    // ---- Boundaries ----

    @Test
    fun cyanBoundaries() {
        assertEquals(0f, CmykColor(cyan = 0f).cyan)
        assertEquals(1f, CmykColor(cyan = 1f).cyan)
        assertFailsWith<IllegalArgumentException> { CmykColor(cyan = 1.01f) }
        assertFailsWith<IllegalArgumentException> { CmykColor(cyan = -0.01f) }
    }

    @Test
    fun magentaBoundaries() {
        assertEquals(0f, CmykColor(magenta = 0f).magenta)
        assertEquals(1f, CmykColor(magenta = 1f).magenta)
        assertFailsWith<IllegalArgumentException> { CmykColor(magenta = 1.01f) }
        assertFailsWith<IllegalArgumentException> { CmykColor(magenta = -0.01f) }
    }

    @Test
    fun yellowBoundaries() {
        assertEquals(0f, CmykColor(yellow = 0f).yellow)
        assertEquals(1f, CmykColor(yellow = 1f).yellow)
        assertFailsWith<IllegalArgumentException> { CmykColor(yellow = 1.01f) }
        assertFailsWith<IllegalArgumentException> { CmykColor(yellow = -0.01f) }
    }

    @Test
    fun keyBoundaries() {
        assertEquals(0f, CmykColor(key = 0f).key)
        assertEquals(1f, CmykColor(key = 1f).key)
        assertFailsWith<IllegalArgumentException> { CmykColor(key = 1.01f) }
        assertFailsWith<IllegalArgumentException> { CmykColor(key = -0.01f) }
    }

    @Test
    fun alphaBoundaries() {
        assertEquals(0f, CmykColor(alpha = 0f).alpha)
        assertEquals(1f, CmykColor(alpha = 1f).alpha)
        assertFailsWith<IllegalArgumentException> { CmykColor(alpha = 1.01f) }
        assertFailsWith<IllegalArgumentException> { CmykColor(alpha = -0.01f) }
    }

    // ---- Signed zero normalization ----

    @Test
    fun negativeZeroEqualsZero() {
        val negativeZero = CmykColor(cyan = -0.0f, magenta = -0.0f, yellow = -0.0f, key = -0.0f)
        val zero = CmykColor(cyan = 0f, magenta = 0f, yellow = 0f, key = 0f)
        assertEquals(zero, negativeZero)
        assertEquals(zero.hashCode(), negativeZero.hashCode())
    }

    // ---- Int accessors ----

    @Test
    fun intAccessorsMidValues() {
        val c = CmykColor(cyan = 0.5f, magenta = 0.25f, yellow = 0.75f, key = 0.1f, alpha = 128f / 255f)
        assertEquals(50, c.intCyan)
        assertEquals(25, c.intMagenta)
        assertEquals(75, c.intYellow)
        assertEquals(10, c.intKey)
        assertEquals(128, c.intAlpha)
    }

    @Test
    fun intAccessorsAtZero() {
        val c = CmykColor(cyan = 0f, magenta = 0f, yellow = 0f, key = 0f, alpha = 0f)
        assertEquals(0, c.intCyan)
        assertEquals(0, c.intMagenta)
        assertEquals(0, c.intYellow)
        assertEquals(0, c.intKey)
        assertEquals(0, c.intAlpha)
    }

    @Test
    fun intAccessorsAtMax() {
        val c = CmykColor(cyan = 1f, magenta = 1f, yellow = 1f, key = 1f, alpha = 1f)
        assertEquals(100, c.intCyan)
        assertEquals(100, c.intMagenta)
        assertEquals(100, c.intYellow)
        assertEquals(100, c.intKey)
        assertEquals(255, c.intAlpha)
    }

    // ---- Companion constants ----

    @Test
    fun companionBlack() {
        assertEquals(1f, CmykColor.Black.key)
        assertEquals(0f, CmykColor.Black.cyan)
        assertEquals(0f, CmykColor.Black.magenta)
        assertEquals(0f, CmykColor.Black.yellow)
    }

    @Test
    fun companionWhite() {
        assertEquals(0f, CmykColor.White.key)
        assertEquals(0f, CmykColor.White.cyan)
        assertEquals(0f, CmykColor.White.magenta)
        assertEquals(0f, CmykColor.White.yellow)
    }

    // ---- fromInt factory ----

    @Test
    fun fromIntBasicConversion() {
        val c = CmykColor.fromInt(cyan = 50, magenta = 25, yellow = 75, key = 10, alpha = 200)
        assertEquals(0.5f, c.cyan)
        assertEquals(0.25f, c.magenta)
        assertEquals(0.75f, c.yellow)
        assertEquals(0.1f, c.key)
        assertEquals(200 / 255f, c.alpha)
    }

    @Test
    fun fromIntDefaultAlpha() {
        val c = CmykColor.fromInt(cyan = 0, magenta = 0, yellow = 0, key = 0)
        assertEquals(1f, c.alpha)
    }

    @Test
    fun fromIntClampsValues() {
        val c = CmykColor.fromInt(cyan = 200, magenta = -5, yellow = 150, key = 300, alpha = 999)
        assertEquals(1f, c.cyan)
        assertEquals(0f, c.magenta)
        assertEquals(1f, c.yellow)
        assertEquals(1f, c.key)
        assertEquals(1f, c.alpha)
    }

    // ---- data class semantics ----

    @Test
    fun equalityByValue() {
        val a = CmykColor(cyan = 0.3f, magenta = 0.4f, yellow = 0.5f, key = 0.6f, alpha = 0.7f)
        val b = CmykColor(cyan = 0.3f, magenta = 0.4f, yellow = 0.5f, key = 0.6f, alpha = 0.7f)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }
}
