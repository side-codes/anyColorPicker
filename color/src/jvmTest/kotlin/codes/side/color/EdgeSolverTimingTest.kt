package codes.side.color

import codes.side.color.internal.chromaWithin
import kotlin.math.sqrt
import kotlin.test.Test

class EdgeSolverTimingTest {

    @Test
    fun recordChromaReductionOnPlanes() {
        // Recorded, not gated: an LCH C × L and an OkLCh C × L plane at hue 200 on their 256 × 256 grids, taken to
        // Oklab once, then each color brought into sRGB by each solver. The median of 15 runs after 5.
        record("LCH C × L", Lch.converterTo(Oklab)) { row, column -> doubleArrayOf(100.0 * (255 - row) / 255, 150.0 * column / 255, 200.0) }
        record("OkLCh C × L", OkLch.converterTo(Oklab)) { row, column -> doubleArrayOf((255 - row) / 255.0, 0.4 * column / 255, 200.0) }
    }

    private fun record(label: String, toOklab: ColorConverter, color: (Int, Int) -> DoubleArray) {
        val t = Srgb.gamut.lmsToLinear
        val lab = DoubleArray(3)
        val ls = ArrayList<Double>()
        val hueAs = ArrayList<Double>()
        val hueBs = ArrayList<Double>()
        val chromas = ArrayList<Double>()
        for (row in 0 until 256) for (column in 0 until 256) {
            toOklab.convert(color(row, column), lab)
            val chroma = sqrt(lab[1] * lab[1] + lab[2] * lab[2])
            if (lab[0] <= 0.0 || lab[0] >= 1.0 || chroma == 0.0) continue
            ls += lab[0]
            hueAs += lab[1] / chroma
            hueBs += lab[2] / chroma
            chromas += chroma
        }
        fun run(iterative: Boolean): Double {
            var sum = 0.0
            for (i in ls.indices) sum += chromaWithin(t, ls[i], hueAs[i], hueBs[i], chromas[i], iterative)
            return sum
        }
        fun median(iterative: Boolean): Double {
            repeat(5) { run(iterative) }
            val times = LongArray(15) {
                val start = System.nanoTime()
                run(iterative)
                System.nanoTime() - start
            }
            times.sort()
            return times[times.size / 2].toDouble() / ls.size
        }
        val closed = median(iterative = false)
        val walked = median(iterative = true)
        println("%s: closed forms %.1f ns a color, walk %.1f ns a color, %.2f× faster".format(label, closed, walked, closed / walked))
    }
}
