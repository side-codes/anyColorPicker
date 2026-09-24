package codes.side.color

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Properties over every space, with a seeded generator so a failure reproduces.
class ConversionPropertiesTest {

    private val random = Random(20260924)

    @Test
    fun theLibraryHasFifteenSpacesWithDistinctIds() {
        assertEquals(15, ColorSpaces.all.size)
        assertEquals(15, ColorSpaces.all.map { it.id }.toSet().size)
    }

    private fun randomSrgb(spread: Double): ColorValue = Srgb(
        random.nextDouble(-spread, 1.0 + spread),
        random.nextDouble(-spread, 1.0 + spread),
        random.nextDouble(-spread, 1.0 + spread),
    )

    @Test
    fun exactSpacesRoundTripThroughEachOther() {
        val exact = ColorSpaces.all.filter { it.exactness == Exactness.Exact }
        repeat(200) {
            val color = randomSrgb(0.0)
            for (space in exact) {
                val back = color.to(space).to(Srgb)
                assertComponents(color.components(), back, 1e-9)
            }
        }
    }

    @Test
    fun noFiniteInputMakesAConversionThrow() {
        repeat(300) {
            val color = randomSrgb(2.0)
            for (space in ColorSpaces.all) {
                val converted = color.to(space)
                for (target in ColorSpaces.all) converted.to(target)
            }
        }
    }

    @Test
    fun hugeFiniteComponentsDoNotMakeAConversionThrow() {
        for (sign in doubleArrayOf(1.0, -1.0)) {
            for (space in ColorSpaces.all) {
                val components = DoubleArray(space.channels.size) { i ->
                    val limit = space.channels[i].limit
                    if (limit == null) sign * 1e300 else (sign * 1e300).coerceIn(limit.start, limit.endInclusive)
                }
                val color = space.color(components)
                for (target in ColorSpaces.all) color.to(target)
            }
        }
    }

    @Test
    fun greysIntoPolarSpacesHaveNoHueAndNoColorfulness() {
        val polar = ColorSpaces.all.filter { space -> space.channels.any { it.isHue } }
        for (step in 0..20) {
            val v = step / 20.0
            for (space in polar) {
                val grey = Srgb(v, v, v).to(space)
                val hue = space.channels.first { it.isHue }
                assertTrue(grey.isMissing(hue), "${space.id} of grey $v")
                space.channels.filter { it.analogous == AnalogousCategory.Colorfulness }.forEach {
                    assertTrue(grey[it] == 0.0, "${space.id}.${it.id} of grey $v")
                }
            }
        }
    }
}
