package codes.side.color

import codes.side.color.internal.argb
import codes.side.color.internal.encodedByte
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
            Oklch to { doubleArrayOf(random.nextDouble(0.0, 1.1), random.nextDouble(0.0, 0.4), random.nextDouble(0.0, 360.0)) },
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
        Srgb.gamut.mapper(Oklch, GamutMapping.Css()).convert(color, color)
        assertComponents(Oklch(0.7, 0.35, 30.0).toGamut(Srgb.gamut).components(), Srgb(color[0], color[1], color[2]), 0.0)
    }

    @Test
    fun aMapperReducesChromaUnlessToldOtherwise() {
        assertEquals(GamutMapping.ChromaReduction(), Srgb.gamut.mapper(Oklch).method)
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
                for (scale in listOf(1.0 - 1e-15, 1.0, 1.0 + 1e-15, 1.0 + 1e-12, 1.0 + 1e-9)) colors += Oklch(lightness, edge * scale, step.toDouble())
            }
        }
        for (gamut in listOf(Srgb.gamut, DisplayP3.gamut)) {
            val mapper = gamut.mapper(Oklch, GamutMapping.ChromaReduction())
            val mapped = DoubleArray(3)
            for (color in colors) {
                mapper.convert(color.components(), mapped)
                assertEquals(color.toGamut(gamut, GamutMapping.ChromaReduction()).components().toList(), mapped.toList(), "$color into $gamut")
            }
        }
    }

    @Test
    fun pixelsAreTheMappedColorsRounded() {
        // On sRGB's curve through the table, on a gamma curve through encoding: either way the bytes of convert.
        val gamma = ColorSpace.rgb("--gamma-pixels", RgbPrimaries.Srgb, WhitePoint.D65, TransferFunction.gamma(2.2))
        val random = Random(20261001)
        val count = 300
        val sources: List<Pair<ColorSpace, () -> DoubleArray>> = listOf(
            Oklch to { doubleArrayOf(random.nextDouble(0.0, 1.05), random.nextDouble(0.0, 0.4), random.nextDouble(0.0, 360.0)) },
            Okhsl to { doubleArrayOf(random.nextDouble(0.0, 360.0), random.nextDouble(), random.nextDouble()) },
            Lch to { doubleArrayOf(random.nextDouble(0.0, 105.0), random.nextDouble(0.0, 150.0), random.nextDouble(0.0, 360.0)) },
            Hwb to { doubleArrayOf(random.nextDouble(0.0, 360.0), random.nextDouble(0.0, 100.0), random.nextDouble(0.0, 100.0)) },
            Srgb to { doubleArrayOf(random.nextDouble(-0.2, 1.2), random.nextDouble(-0.2, 1.2), random.nextDouble(-0.2, 1.2)) },
        )
        for ((space, next) in sources) {
            val source = DoubleArray(count * 3)
            for (k in 0 until count) next().copyInto(source, k * 3)
            for (gamut in listOf(Srgb.gamut, DisplayP3.gamut, gamma.gamut)) {
                for (method in methods) {
                    val mapper = gamut.mapper(space, method)
                    val doubles = DoubleArray(count * 3)
                    mapper.convert(source, 0, doubles, 0, count)
                    val pixels = IntArray(count + 2)
                    mapper.convertToArgb(source, 0, pixels, 1, count)
                    for (k in 0 until count) {
                        val expected = argb(encodedByte(doubles[k * 3]), encodedByte(doubles[k * 3 + 1]), encodedByte(doubles[k * 3 + 2]))
                        assertEquals(expected, pixels[k + 1], "$method from $space to $gamut, color $k")
                    }
                    assertEquals(0, pixels[0], "before dstOffset")
                    assertEquals(0, pixels[count + 1], "after the last color")
                }
            }
        }
    }

    @Test
    fun colorsOnTheCubesSurfacePackAsTheyMap() {
        val colors = ArrayList<ColorValue>()
        for (step in 0 until 360) {
            for (lightness in listOf(0.1, 0.3, 0.5, 0.7, 0.9, 0.99)) {
                val edge = Srgb.gamut.maxChroma(lightness, step.toDouble())
                for (scale in listOf(1.0 - 1e-15, 1.0, 1.0 + 1e-15, 1.0 + 1e-12, 1.0 + 1e-9)) colors += Oklch(lightness, edge * scale, step.toDouble())
            }
        }
        val mapper = Srgb.gamut.mapper(Oklch)
        val mapped = DoubleArray(3)
        val pixel = IntArray(1)
        for (color in colors) {
            mapper.convert(color.components(), mapped)
            mapper.convertToArgb(color.components(), 0, pixel, 0, 1)
            assertEquals(argb(encodedByte(mapped[0]), encodedByte(mapped[1]), encodedByte(mapped[2])), pixel[0], "$color")
        }
    }

    @Test
    fun packingChecksItsBounds() {
        val mapper = Srgb.gamut.mapper(Oklch)
        assertFailsWith<IllegalArgumentException> { mapper.convertToArgb(DoubleArray(6), 0, IntArray(1), 0, 2) }
        assertFailsWith<IllegalArgumentException> { mapper.convertToArgb(DoubleArray(5), 0, IntArray(2), 0, 2) }
    }

    @Test
    fun aNanComponentPacksAsBlack() {
        // Through the table and through encoding alike: from these colors convert carries the NaN into every channel,
        // and each packs as 0.
        val gamma = ColorSpace.rgb("--gamma-nan", RgbPrimaries.Srgb, WhitePoint.D65, TransferFunction.gamma(2.2))
        val sources = listOf(Oklch to doubleArrayOf(0.5, 0.1, 120.0), Srgb to doubleArrayOf(0.5, 0.4, 0.3))
        for ((space, color) in sources) {
            for (gamut in listOf(Srgb.gamut, gamma.gamut)) {
                for (method in methods) {
                    for (index in 0..2) {
                        val src = color.copyOf().also { it[index] = Double.NaN }
                        val pixel = IntArray(1)
                        gamut.mapper(space, method).convertToArgb(src, 0, pixel, 0, 1)
                        assertEquals(0xFF000000.toInt(), pixel[0], "$method from $space to $gamut, NaN at $index")
                    }
                }
            }
        }
    }
}
