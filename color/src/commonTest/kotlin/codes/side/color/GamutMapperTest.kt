package codes.side.color

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GamutMapperTest {

    private val methods = listOf(GamutMapping.Css(), GamutMapping.ChromaReduction(), GamutMapping.ChromaReduction(EdgeSolver.Iterative), GamutMapping.Clip)

    @Test
    fun bulkMappingGivesWhatToGamutGives() {
        // Routes through Oklab (OkLCh, Okhsl, Okhsv, Oklab itself) and around it (LCH, HWB, sRGB), inside and far out.
        val random = Random(20260927)
        val count = 200
        val sources: List<Pair<ColorSpace, () -> DoubleArray>> = listOf(
            OkLch to { doubleArrayOf(random.nextDouble(0.0, 1.1), random.nextDouble(0.0, 0.4), random.nextDouble(0.0, 360.0)) },
            Okhsl to { doubleArrayOf(random.nextDouble(0.0, 360.0), random.nextDouble(), random.nextDouble()) },
            Okhsv to { doubleArrayOf(random.nextDouble(0.0, 360.0), random.nextDouble(), random.nextDouble()) },
            Oklab to { doubleArrayOf(random.nextDouble(0.0, 1.1), random.nextDouble(-0.4, 0.4), random.nextDouble(-0.4, 0.4)) },
            Lch to { doubleArrayOf(random.nextDouble(0.0, 110.0), random.nextDouble(0.0, 150.0), random.nextDouble(0.0, 360.0)) },
            Hwb to { doubleArrayOf(random.nextDouble(0.0, 360.0), random.nextDouble(0.0, 100.0), random.nextDouble(0.0, 100.0)) },
            Srgb to { doubleArrayOf(random.nextDouble(-0.2, 1.2), random.nextDouble(-0.2, 1.2), random.nextDouble(-0.2, 1.2)) },
        )
        for ((space, next) in sources) {
            val source = DoubleArray(count * 3)
            for (k in 0 until count) next().copyInto(source, k * 3)
            for (gamut in listOf(Srgb.gamut, DisplayP3.gamut)) {
                for (method in methods) {
                    val mapped = DoubleArray(count * 3)
                    gamut.mapper(space, method).convert(source, 0, mapped, 0, count)
                    val floats = FloatArray(count * 3)
                    gamut.mapper(space, method).convert(FloatArray(count * 3) { source[it].toFloat() }, 0, floats, 0, count)
                    for (k in 0 until count) {
                        val color = space.color(source.copyOfRange(k * 3, k * 3 + 3))
                        val expected = color.toGamut(gamut, method).components()
                        for (i in 0..2) {
                            assertNear(expected[i], mapped[k * 3 + i], 0.0, "$method from $space to $gamut, color $k")
                            assertNear(expected[i], floats[k * 3 + i].toDouble(), 1e-3, "$method from $space to $gamut in floats, color $k")
                        }
                    }
                }
            }
        }
    }

    @Test
    fun oneColorMapsInPlace() {
        val color = doubleArrayOf(0.7, 0.35, 30.0)
        Srgb.gamut.mapper(OkLch, GamutMapping.Css()).convert(color, color)
        assertComponents(OkLch(0.7, 0.35, 30.0).toGamut(Srgb.gamut).components(), Srgb(color[0], color[1], color[2]), 0.0)
    }

    @Test
    fun aMapperReducesChromaUnlessToldOtherwise() {
        assertEquals(GamutMapping.ChromaReduction(), Srgb.gamut.mapper(OkLch).method)
    }

    @Test
    fun bulkMappingChecksItsBounds() {
        val mapper = Srgb.gamut.mapper(Cmyk)
        assertFailsWith<IllegalArgumentException> { mapper.convert(DoubleArray(7), 0, DoubleArray(6), 0, 2) }
        assertFailsWith<IllegalArgumentException> { mapper.convert(DoubleArray(8), 0, DoubleArray(5), 0, 2) }
        val same = DoubleArray(8)
        mapper.convert(same, 0, same, 0, 2)
    }

    @Test
    fun colorsOnTheCubesSurfaceMapAsToGamutMapsThem() {
        // On sRGB's surface a linear channel lands within a few ulps of 0 or 1, where encoding can round a hair past
        // 1 back inside: however the mapper tells inside from out, such a color must come back as toGamut gives it.
        val colors = ArrayList<ColorValue>()
        for (step in 0 until 360) {
            for (lightness in listOf(0.1, 0.3, 0.5, 0.7, 0.9, 0.99)) {
                val edge = Srgb.gamut.maxChroma(lightness, step.toDouble())
                for (scale in listOf(1.0 - 1e-15, 1.0, 1.0 + 1e-15, 1.0 + 1e-12, 1.0 + 1e-9)) colors += OkLch(lightness, edge * scale, step.toDouble())
            }
        }
        for (gamut in listOf(Srgb.gamut, DisplayP3.gamut)) {
            val mapper = gamut.mapper(OkLch, GamutMapping.ChromaReduction())
            val mapped = DoubleArray(3)
            for (color in colors) {
                mapper.convert(color.components(), mapped)
                assertEquals(color.toGamut(gamut, GamutMapping.ChromaReduction()).components().toList(), mapped.toList(), "$color into $gamut")
            }
        }
    }
}
