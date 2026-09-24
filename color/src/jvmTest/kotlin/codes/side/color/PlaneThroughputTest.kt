package codes.side.color

import kotlin.test.Test

class PlaneThroughputTest {

    @Test
    fun recordPlaneThroughput() {
        // Recorded, not gated: a 256 × 256 picker plane through the bulk calls, the median of 15 runs
        // after 5 to warm up, printed to the test's output. OkLCh at L 0.7 over C 0–0.4 and every hue
        // leaves about half the plane outside sRGB.
        val side = 256
        val count = side * side
        val plane = DoubleArray(count * 3)
        for (y in 0 until side) for (x in 0 until side) {
            val i = (y * side + x) * 3
            plane[i] = 0.7
            plane[i + 1] = 0.4 * y / (side - 1)
            plane[i + 2] = 360.0 * x / side
        }
        val out = DoubleArray(count * 4)
        val converters = listOf(OkLch to Srgb, OkLch to DisplayP3, OkLch to Okhsl, Srgb to OkLch, Hsl to Srgb)
        for ((from, to) in converters) {
            val source = if (from == OkLch) plane else DoubleArray(count * 3).also { OkLch.converterTo(from).convert(plane, 0, it, 0, count) }
            val converter = from.converterTo(to)
            report("${from.id} → ${to.id}", count) { converter.convert(source, 0, out, 0, count) }
        }
        for (method in listOf(GamutMapping.ChromaReduction, GamutMapping.Css(), GamutMapping.Clip)) {
            val mapper = Srgb.gamut.mapper(OkLch, method)
            report("oklch into sRGB, $method", count) { mapper.convert(plane, 0, out, 0, count) }
        }
    }

    private fun report(label: String, count: Int, run: () -> Unit) {
        repeat(5) { run() }
        val times = LongArray(15) {
            val start = System.nanoTime()
            run()
            System.nanoTime() - start
        }
        times.sort()
        println("%-40s %6.1f ns a color".format(label, times[times.size / 2].toDouble() / count))
    }
}
