package codes.side.color

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

// A gamut's cusp and edges are remembered between calls. What a call returns must not depend on what was asked
// before it: the same colors, asked again in another order among other hues, lightnesses and gamuts, come back
// bit for bit alike.
class RememberedGamutTest {

    // Among them hues in sRGB's sliver past pure blue, where one lightness has two edges.
    private val hues = listOf(0.0, 42.0, 110.0, 200.0, 264.05, 264.1, 264.2, 330.0)

    @Test
    fun okhslAndOkhsvGiveTheSameInAnyOrder() {
        val random = Random(20260927)
        val colors = List(800) { k ->
            val hue = hues[k % hues.size]
            if (k % 2 == 0) Okhsl(hue, random.nextDouble(), random.nextDouble()) else Okhsv(hue, random.nextDouble(), random.nextDouble())
        }
        val labs = colors.map { it.to(Oklab) }
        val inOrder = colors.sortedBy { it.components()[0] }.associateWith { it.to(Oklab).components().toList() }
        val back = labs.sortedBy { it.components()[1] }.associateWith { listOf(it.to(Okhsl).components().toList(), it.to(Okhsv).components().toList()) }
        for (k in colors.indices.shuffled(random)) {
            // Another gamut's cusp and edge at the same hue and lightness, asked in between.
            DisplayP3.gamut.maxChroma(random.nextDouble(0.05, 0.95), hues[k % hues.size])
            assertEquals(inOrder.getValue(colors[k]), colors[k].to(Oklab).components().toList(), "${colors[k]}")
            assertEquals(back.getValue(labs[k]), listOf(labs[k].to(Okhsl).components().toList(), labs[k].to(Okhsv).components().toList()), "${labs[k]}")
        }
    }

    @Test
    fun aGamutsEdgeAndCuspAreItsOwn() {
        val random = Random(20260928)
        val asked = List(400) { random.nextDouble(0.02, 0.98) to hues[it % hues.size] }
        val alone = listOf(Srgb.gamut, DisplayP3.gamut).associateWith { gamut ->
            asked.map { (lightness, hue) -> gamut.maxChroma(lightness, hue) to gamut.cusp(hue).components().toList() }
        }
        for (k in asked.indices.shuffled(random)) {
            val (lightness, hue) = asked[k]
            for (gamut in listOf(DisplayP3.gamut, Srgb.gamut)) {
                assertEquals(alone.getValue(gamut)[k], gamut.maxChroma(lightness, hue) to gamut.cusp(hue).components().toList(), "$gamut at $lightness, $hue")
            }
        }
    }

    @Test
    fun chromaReductionGivesTheSameInAnyOrder() {
        // A plane's rows, chroma across and lightness down, beyond sRGB for most of them.
        val random = Random(20260929)
        val colors = hues.flatMap { hue -> List(24) { row -> List(32) { column -> OkLch(0.04 + row * 0.04, column * 0.4 / 31, hue) } }.flatten() }
        val inRows = colors.associateWith { it.toGamut(Srgb.gamut, GamutMapping.ChromaReduction).components().toList() }
        for (k in colors.indices.shuffled(random)) {
            DisplayP3.gamut.maxChroma(random.nextDouble(0.05, 0.95), hues[k % hues.size])
            val color = colors[k]
            assertEquals(inRows.getValue(color), color.toGamut(Srgb.gamut, GamutMapping.ChromaReduction).components().toList(), "$color")
        }
    }
}
