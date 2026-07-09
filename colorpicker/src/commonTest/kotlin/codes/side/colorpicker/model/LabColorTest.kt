package codes.side.colorpicker.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LabColorTest {

    @Test
    fun defaultValues() {
        val lab = LabColor()
        assertEquals(50f, lab.l)
        assertEquals(0f, lab.a)
        assertEquals(0f, lab.b)
        assertEquals(1f, lab.alpha)
    }

    // ---- L boundaries ----

    @Test
    fun lBoundaries() {
        assertEquals(0f, LabColor(l = 0f).l)
        assertEquals(100f, LabColor(l = 100f).l)
        assertFailsWith<IllegalArgumentException> { LabColor(l = 100.1f) }
        assertFailsWith<IllegalArgumentException> { LabColor(l = -0.1f) }
    }

    @Test
    fun lFractionalValue() {
        val lab = LabColor(l = 53.23f)
        assertEquals(53.23f, lab.l)
    }

    // ---- A boundaries ----

    @Test
    fun aBoundaries() {
        assertEquals(-128f, LabColor(a = -128f).a)
        assertEquals(127f, LabColor(a = 127f).a)
        assertFailsWith<IllegalArgumentException> { LabColor(a = -128.1f) }
        assertFailsWith<IllegalArgumentException> { LabColor(a = 127.1f) }
    }

    @Test
    fun aFractionalValue() {
        val lab = LabColor(a = -30.5f)
        assertEquals(-30.5f, lab.a)
    }

    // ---- B boundaries ----

    @Test
    fun bBoundaries() {
        assertEquals(-128f, LabColor(b = -128f).b)
        assertEquals(127f, LabColor(b = 127f).b)
        assertFailsWith<IllegalArgumentException> { LabColor(b = -128.1f) }
        assertFailsWith<IllegalArgumentException> { LabColor(b = 127.1f) }
    }

    @Test
    fun bFractionalValue() {
        val lab = LabColor(b = 79.84f)
        assertEquals(79.84f, lab.b)
    }

    // ---- Alpha boundaries ----

    @Test
    fun alphaBoundaries() {
        assertEquals(0f, LabColor(alpha = 0f).alpha)
        assertEquals(1f, LabColor(alpha = 1f).alpha)
        assertFailsWith<IllegalArgumentException> { LabColor(alpha = 1.01f) }
        assertFailsWith<IllegalArgumentException> { LabColor(alpha = -0.01f) }
    }

    // ---- Int accessors ----

    @Test
    fun intAccessors() {
        val lab = LabColor(l = 53.23f, a = -30.5f, b = 79.84f, alpha = 0.75f)
        assertEquals(53, lab.intL)
        assertEquals(-30, lab.intA) // -30.5 rounds to -30 (banker's rounding: half to even)
        assertEquals(80, lab.intB)
        assertEquals(191, lab.intAlpha) // 0.75 * 255 = 191.25 -> 191
    }

    @Test
    fun intAccessorsAtZero() {
        val lab = LabColor(l = 0f, a = 0f, b = 0f, alpha = 0f)
        assertEquals(0, lab.intL)
        assertEquals(0, lab.intA)
        assertEquals(0, lab.intB)
        assertEquals(0, lab.intAlpha)
    }

    @Test
    fun intAccessorsAtExtremes() {
        val lab = LabColor(l = 100f, a = -128f, b = 127f, alpha = 1f)
        assertEquals(100, lab.intL)
        assertEquals(-128, lab.intA)
        assertEquals(127, lab.intB)
        assertEquals(255, lab.intAlpha)
    }

    // ---- Companion constants ----

    @Test
    fun companionBlack() {
        assertEquals(0f, LabColor.Black.l)
        assertEquals(0f, LabColor.Black.a)
        assertEquals(0f, LabColor.Black.b)
    }

    @Test
    fun companionWhite() {
        assertEquals(100f, LabColor.White.l)
        assertEquals(0f, LabColor.White.a)
        assertEquals(0f, LabColor.White.b)
    }

    // ---- fromInt factory ----

    @Test
    fun fromIntBasicConversion() {
        val lab = LabColor.fromInt(l = 50, a = -60, b = 80, alpha = 200)
        assertEquals(50f, lab.l)
        assertEquals(-60f, lab.a)
        assertEquals(80f, lab.b)
        assertEquals(200 / 255f, lab.alpha)
    }

    @Test
    fun fromIntDefaultAlpha() {
        val lab = LabColor.fromInt(l = 50, a = 0, b = 0)
        assertEquals(1f, lab.alpha)
    }

    @Test
    fun fromIntClampsValues() {
        val lab = LabColor.fromInt(l = 200, a = -200, b = 200, alpha = 300)
        assertEquals(100f, lab.l)
        assertEquals(-128f, lab.a)
        assertEquals(127f, lab.b)
        assertEquals(1f, lab.alpha)
    }

    @Test
    fun fromIntClampsNegatives() {
        val lab = LabColor.fromInt(l = -10, a = 0, b = 0, alpha = -5)
        assertEquals(0f, lab.l)
        assertEquals(0f, lab.alpha)
    }

    // ---- data class semantics ----

    @Test
    fun equalityByValue() {
        val a = LabColor(l = 53.23f, a = -30.5f, b = 79.84f, alpha = 0.5f)
        val b = LabColor(l = 53.23f, a = -30.5f, b = 79.84f, alpha = 0.5f)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }
}
