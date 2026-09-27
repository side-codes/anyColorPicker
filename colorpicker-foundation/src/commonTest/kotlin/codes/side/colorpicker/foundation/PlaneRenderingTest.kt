package codes.side.colorpicker.foundation

import codes.side.color.ColorChannel
import codes.side.color.ColorSpace
import codes.side.color.DisplayP3
import codes.side.color.Hwb
import codes.side.color.Lch
import codes.side.color.Okhsl
import codes.side.color.Okhsv
import codes.side.color.Oklch
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlaneRenderingTest {

    private val p3Hsl = ColorSpace.hsl("--p3-hsl", DisplayP3)

    private fun held(x: ColorChannel, hue: Double): DoubleArray {
        val held = DoubleArray(x.space.channels.size)
        x.space.channels.firstOrNull { it.isHue }?.let { held[it.index] = hue }
        return held
    }

    @Test
    fun fastDrawsWhatCanonicalDrawsToALevel() {
        // HWB and Okhsv never leave sRGB, so nothing differs; where chroma is reduced, the solvers' last bits can
        // move a byte by one.
        val exact = setOf(Hwb.W, Okhsv.S)
        val planes = listOf(Okhsl.S to Okhsl.L, Oklch.C to Oklch.L, Lch.C to Lch.L, Okhsv.S to Okhsv.V, Hwb.W to Hwb.B, p3Hsl.S to p3Hsl.L)
        for ((x, y) in planes) {
            for (hue in listOf(0.0, 110.0, 200.0, 264.1, 330.0)) {
                val grid = PlaneGrid(33, 29)
                val canonical = planePixels(x, y, held(x, hue), grid, PlaneRendering.Canonical)
                val fast = planePixels(x, y, held(x, hue), grid, PlaneRendering.Fast)
                if (x in exact) {
                    assertEquals(canonical.toList(), fast.toList(), "$x × $y at $hue°")
                } else {
                    for (i in canonical.indices) {
                        for (shift in listOf(16, 8, 0)) {
                            val difference = abs(((canonical[i] shr shift) and 0xFF) - ((fast[i] shr shift) and 0xFF))
                            assertTrue(difference <= 1, "$x × $y at $hue°, pixel $i: ${canonical[i].toUInt().toString(16)} against ${fast[i].toUInt().toString(16)}")
                        }
                    }
                }
            }
        }
    }

    @Test
    fun canonicalIsTheDefaultOfTheRows() {
        val held = held(Oklch.C, 200.0)
        assertEquals(planePixels(Oklch.C, Oklch.L, held, PlaneGrid(9, 7), PlaneRendering.Canonical).toList(), planePixels(Oklch.C, Oklch.L, held, PlaneGrid(9, 7)).toList())
    }

    @Test
    fun presetsNameThemselves() {
        assertEquals("Fast", PlaneRendering.Fast.toString())
        assertEquals("Canonical", PlaneRendering.Canonical.toString())
    }
}
