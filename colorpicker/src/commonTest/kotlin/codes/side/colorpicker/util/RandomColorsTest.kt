package codes.side.colorpicker.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RandomColorsTest {

    @Test
    fun randomHslColorInRange() {
        repeat(50) {
            val color = randomHslColor()
            assertTrue(color.hue in 0f..360f, "Hue out of range: ${color.hue}")
            assertTrue(color.saturation in 0f..1f, "Saturation out of range: ${color.saturation}")
            assertTrue(color.lightness in 0f..1f, "Lightness out of range: ${color.lightness}")
            assertEquals(1f, color.alpha)
        }
    }

    @Test
    fun randomHslColorPure() {
        repeat(50) {
            val color = randomHslColor(pure = true)
            assertTrue(color.hue in 0f..360f, "Hue out of range: ${color.hue}")
            assertEquals(1f, color.saturation)
            assertEquals(0.5f, color.lightness)
            assertEquals(1f, color.alpha)
        }
    }

    @Test
    fun randomHslColorProducesVariation() {
        val colors = (1..20).map { randomHslColor() }
        val uniqueHues = colors.map { it.hue }.toSet()
        assertTrue(uniqueHues.size > 1, "Expected variation in hues")
    }

    @Test
    fun randomHslColorPureVariation() {
        val colors = (1..20).map { randomHslColor(pure = true) }
        val uniqueHues = colors.map { it.hue }.toSet()
        assertTrue(uniqueHues.size > 1, "Expected variation in pure-mode hues")
        // All should have sat=1, light=0.5
        colors.forEach {
            assertEquals(1f, it.saturation)
            assertEquals(0.5f, it.lightness)
        }
    }

    @Test
    fun randomHslColorDefaultAlpha() {
        repeat(10) {
            val color = randomHslColor()
            assertEquals(1f, color.alpha, "Default alpha should be 1f (opaque)")
        }
    }
}
