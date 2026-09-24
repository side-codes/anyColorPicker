package codes.side.color

import com.sun.management.ThreadMXBean
import java.lang.management.ManagementFactory
import kotlin.test.Test
import kotlin.test.assertTrue

class BulkAllocationTest {

    @Test
    fun bulkConversionsAllocateNothingPerColor() {
        // Measured on the second call, before the JIT has run long enough to optimize allocations
        // away, so what is counted is what the code asks for. The first call pays for lazy setup;
        // under a byte per color leaves room for a call's fixed costs and none for a per-color one.
        val threads = ManagementFactory.getThreadMXBean() as ThreadMXBean
        val count = 2_000
        val report = StringBuilder()
        for (from in ColorSpaces.all) {
            for (to in ColorSpaces.all) {
                val converter = from.converterTo(to)
                val src = DoubleArray(count * from.channels.size) { i -> 0.1 + i % 7 * 0.1 }
                val dst = DoubleArray(count * to.channels.size)
                converter.convert(src, 0, dst, 0, 1)
                val before = threads.currentThreadAllocatedBytes
                converter.convert(src, 0, dst, 0, count)
                val allocated = threads.currentThreadAllocatedBytes - before
                if (allocated >= count) report.appendLine("${from.id} → ${to.id}: $allocated bytes for $count colors")
            }
        }
        assertTrue(report.isEmpty(), report.toString())
    }

    @Test
    fun gamutMappersAllocateNothingPerColor() {
        // Mostly far outside both gamuts, so every method runs its reduction. Ten thousand colors,
        // because the first mapper measured can carry a one-off of up to 2 KB when other tests have
        // run in the same JVM, which settles to the call's 80 bytes on the calls after it.
        val threads = ManagementFactory.getThreadMXBean() as ThreadMXBean
        val count = 10_000
        val report = StringBuilder()
        for (from in listOf(OkLch, Srgb, Okhsl, Cmyk)) {
            for (gamut in listOf(Srgb.gamut, DisplayP3.gamut)) {
                for (method in listOf(GamutMapping.Css(), GamutMapping.ChromaReduction, GamutMapping.Clip)) {
                    val mapper = gamut.mapper(from, method)
                    val src = DoubleArray(count * from.channels.size) { i -> if (from === Srgb) 1.2 - i % 7 * 0.2 else 0.1 + i % 7 * 0.1 }
                    val dst = DoubleArray(count * 3)
                    mapper.convert(src, 0, dst, 0, 1)
                    val before = threads.currentThreadAllocatedBytes
                    mapper.convert(src, 0, dst, 0, count)
                    val allocated = threads.currentThreadAllocatedBytes - before
                    if (allocated >= count) report.appendLine("$mapper: $allocated bytes for $count colors")
                }
            }
        }
        assertTrue(report.isEmpty(), report.toString())
    }
}
