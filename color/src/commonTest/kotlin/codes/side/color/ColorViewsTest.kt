package codes.side.color

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ColorViewsTest {

    @Test
    fun srgbNamesItsComponents() {
        val color = Srgb(0.1, 0.2, 0.3, 0.4).asSrgb()
        assertEquals(listOf(0.1, 0.2, 0.3, 0.4), listOf(color.r, color.g, color.b, color.alpha))
        assertEquals(Srgb(0.1, 0.5, 0.3, 0.4), color.with(g = 0.5).value)
    }

    @Test
    fun srgbLinearNamesItsComponents() {
        val color = SrgbLinear(0.1, 0.2, 0.3, 0.4).asSrgbLinear()
        assertEquals(listOf(0.1, 0.2, 0.3, 0.4), listOf(color.r, color.g, color.b, color.alpha))
        assertEquals(SrgbLinear(0.5, 0.2, 0.3, 0.4), color.with(r = 0.5).value)
    }

    @Test
    fun displayP3NamesItsComponents() {
        val color = DisplayP3(0.1, 0.2, 0.3, 0.4).asDisplayP3()
        assertEquals(listOf(0.1, 0.2, 0.3, 0.4), listOf(color.r, color.g, color.b, color.alpha))
        assertEquals(DisplayP3(0.1, 0.2, 0.5, 0.4), color.with(b = 0.5).value)
    }

    @Test
    fun xyzD65NamesItsComponents() {
        val color = XyzD65(0.2, 0.3, 0.4, 0.5).asXyzD65()
        assertEquals(listOf(0.2, 0.3, 0.4, 0.5), listOf(color.x, color.y, color.z, color.alpha))
        assertEquals(XyzD65(0.2, 0.6, 0.4, 0.5), color.with(y = 0.6).value)
    }

    @Test
    fun xyzD50NamesItsComponents() {
        val color = XyzD50(0.2, 0.3, 0.4, 0.5).asXyzD50()
        assertEquals(listOf(0.2, 0.3, 0.4, 0.5), listOf(color.x, color.y, color.z, color.alpha))
        assertEquals(XyzD50(0.2, 0.3, 0.6, 0.5), color.with(z = 0.6).value)
    }

    @Test
    fun labNamesItsComponents() {
        val color = Lab(50.0, 20.0, -30.0, 0.5).asLab()
        assertEquals(listOf(50.0, 20.0, -30.0, 0.5), listOf(color.l, color.a, color.b, color.alpha))
        assertEquals(Lab(50.0, 10.0, -30.0, 0.5), color.with(a = 10.0).value)
    }

    @Test
    fun lchNamesItsComponents() {
        val color = Lch(50.0, 30.0, 120.0, 0.5).asLch()
        assertEquals(listOf(50.0, 30.0, 120.0, 0.5), listOf(color.l, color.c, color.h, color.alpha))
        assertEquals(Lch(50.0, 40.0, 120.0, 0.5), color.with(c = 40.0).value)
    }

    @Test
    fun oklabNamesItsComponents() {
        val color = Oklab(0.5, 0.1, -0.1, 0.5).asOklab()
        assertEquals(listOf(0.5, 0.1, -0.1, 0.5), listOf(color.l, color.a, color.b, color.alpha))
        assertEquals(Oklab(0.5, 0.1, 0.05, 0.5), color.with(b = 0.05).value)
    }

    @Test
    fun okLchNamesItsComponents() {
        val color = OkLch(0.5, 0.1, 120.0, 0.5).asOkLch()
        assertEquals(listOf(0.5, 0.1, 120.0, 0.5), listOf(color.l, color.c, color.h, color.alpha))
        assertEquals(OkLch(0.5, 0.1, 240.0, 0.5), color.with(h = 240.0).value)
    }

    @Test
    fun hslNamesItsComponents() {
        val color = Hsl(200.0, 50.0, 40.0, 0.5).asHsl()
        assertEquals(listOf(200.0, 50.0, 40.0, 0.5), listOf(color.h, color.s, color.l, color.alpha))
        assertEquals(Hsl(200.0, 50.0, 60.0, 0.5), color.with(l = 60.0).value)
    }

    @Test
    fun hwbNamesItsComponents() {
        val color = Hwb(200.0, 10.0, 20.0, 0.5).asHwb()
        assertEquals(listOf(200.0, 10.0, 20.0, 0.5), listOf(color.h, color.w, color.b, color.alpha))
        assertEquals(Hwb(200.0, 30.0, 20.0, 0.5), color.with(w = 30.0).value)
    }

    @Test
    fun hsvNamesItsComponents() {
        val color = Hsv(200.0, 50.0, 40.0, 0.5).asHsv()
        assertEquals(listOf(200.0, 50.0, 40.0, 0.5), listOf(color.h, color.s, color.v, color.alpha))
        assertEquals(Hsv(200.0, 50.0, 60.0, 0.5), color.with(v = 60.0).value)
    }

    @Test
    fun okhslNamesItsComponents() {
        val color = Okhsl(200.0, 0.5, 0.4, 0.5).asOkhsl()
        assertEquals(listOf(200.0, 0.5, 0.4, 0.5), listOf(color.h, color.s, color.l, color.alpha))
        assertEquals(Okhsl(200.0, 0.6, 0.4, 0.5), color.with(s = 0.6).value)
    }

    @Test
    fun okhsvNamesItsComponents() {
        val color = Okhsv(200.0, 0.5, 0.4, 0.5).asOkhsv()
        assertEquals(listOf(200.0, 0.5, 0.4, 0.5), listOf(color.h, color.s, color.v, color.alpha))
        assertEquals(Okhsv(200.0, 0.5, 0.6, 0.5), color.with(v = 0.6).value)
    }

    @Test
    fun cmykNamesItsComponents() {
        val color = Cmyk(0.1, 0.2, 0.3, 0.4, 0.5).asCmyk()
        assertEquals(listOf(0.1, 0.2, 0.3, 0.4, 0.5), listOf(color.c, color.m, color.y, color.k, color.alpha))
        assertEquals(Cmyk(0.1, 0.2, 0.3, 0.6, 0.5), color.with(k = 0.6).value)
    }

    @Test
    fun withKeepsWhatItIsNotGivenNoneIncluded() {
        val color = Hsl(null, 50.0, 40.0, null).asHsl()
        assertEquals(Hsl(null, 60.0, 40.0, null), color.with(s = 60.0).value)
    }

    @Test
    fun withNullWritesNone() {
        val color = Hsl(200.0, 50.0, 40.0).asHsl()
        assertNull(color.with(h = null).h)
        assertNull(color.with(alpha = null).alpha)
    }

    @Test
    fun asRefusesAColorInAnotherSpace() {
        assertFailsWith<IllegalArgumentException> { Srgb(1.0, 0.0, 0.0).asHsl() }
        assertFailsWith<IllegalArgumentException> { Hsl(0.0, 100.0, 50.0).asSrgb() }
    }

    @Test
    fun aViewPrintsAsItsValue() {
        val color = OkLch(0.5, 0.1, 120.0)
        assertEquals(color.toString(), color.asOkLch().toString())
    }

    @Test
    fun everyLibrarySpaceHasAViewThatConvertsIntoIt() {
        val color = OkLch(0.6, 0.1, 200.0)
        val views: Map<ColorSpace, ColorValue> = mapOf(
            Srgb to color.toSrgb().value,
            SrgbLinear to color.toSrgbLinear().value,
            DisplayP3 to color.toDisplayP3().value,
            XyzD65 to color.toXyzD65().value,
            XyzD50 to color.toXyzD50().value,
            Lab to color.toLab().value,
            Lch to color.toLch().value,
            Oklab to color.toOklab().value,
            OkLch to color.toOkLch().value,
            Hsl to color.toHsl().value,
            Hwb to color.toHwb().value,
            Hsv to color.toHsv().value,
            Okhsl to color.toOkhsl().value,
            Okhsv to color.toOkhsv().value,
            Cmyk to color.toCmyk().value,
        )
        assertEquals(ColorSpaces.all.map { it.id }.sorted(), views.keys.map { it.id }.sorted())
        for ((space, converted) in views) assertEquals(color.to(space), converted, space.id)
    }
}
