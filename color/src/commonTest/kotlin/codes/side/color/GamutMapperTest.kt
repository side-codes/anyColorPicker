package codes.side.color

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class GamutMapperTest {

    private val methods = listOf(GamutMapping.Css(), GamutMapping.ChromaReduction, GamutMapping.Clip)

    @Test
    fun bulkMappingGivesWhatToGamutGives() {
        val random = Random(20260927)
        val count = 200
        val source = DoubleArray(count * 3)
        for (k in 0 until count) {
            source[k * 3] = random.nextDouble(0.0, 1.1)
            source[k * 3 + 1] = random.nextDouble(0.0, 0.4)
            source[k * 3 + 2] = random.nextDouble(0.0, 360.0)
        }
        for (gamut in listOf(Srgb.gamut, DisplayP3.gamut)) {
            for (method in methods) {
                val mapped = DoubleArray(count * 3)
                gamut.mapper(OkLch, method).convert(source, 0, mapped, 0, count)
                val floats = FloatArray(count * 3)
                gamut.mapper(OkLch, method).convert(FloatArray(count * 3) { source[it].toFloat() }, 0, floats, 0, count)
                for (k in 0 until count) {
                    val expected = OkLch(source[k * 3], source[k * 3 + 1], source[k * 3 + 2]).toGamut(gamut, method).components()
                    for (i in 0..2) {
                        assertNear(expected[i], mapped[k * 3 + i], 0.0, "$method to $gamut, color $k")
                        assertNear(expected[i], floats[k * 3 + i].toDouble(), 1e-3, "$method to $gamut in floats, color $k")
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
        assertSame(GamutMapping.ChromaReduction, Srgb.gamut.mapper(OkLch).method)
    }

    @Test
    fun bulkMappingChecksItsBounds() {
        val mapper = Srgb.gamut.mapper(Cmyk)
        assertFailsWith<IllegalArgumentException> { mapper.convert(DoubleArray(7), 0, DoubleArray(6), 0, 2) }
        assertFailsWith<IllegalArgumentException> { mapper.convert(DoubleArray(8), 0, DoubleArray(5), 0, 2) }
        val same = DoubleArray(8)
        mapper.convert(same, 0, same, 0, 2)
    }
}
