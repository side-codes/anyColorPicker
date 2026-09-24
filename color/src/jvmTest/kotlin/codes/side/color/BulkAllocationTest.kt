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
}
