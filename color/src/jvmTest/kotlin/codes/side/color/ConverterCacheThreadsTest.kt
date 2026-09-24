package codes.side.color

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// XYZ-D65 shifted by 0.1, a space no other test has prepared converters for.
@OptIn(ExperimentalColorSpaceApi::class)
private class Shifted : ColorSpace(
    "--shifted",
    listOf(ColorChannel("x", 0.0..1.0), ColorChannel("y", 0.0..1.0), ColorChannel("z", 0.0..1.0)),
    XyzD65,
) {
    override fun toBase(src: DoubleArray, dst: DoubleArray) {
        for (i in 0..2) dst[i] = src[i] + 0.1
    }

    override fun fromBase(src: DoubleArray, dst: DoubleArray) {
        for (i in 0..2) dst[i] = src[i] - 0.1
    }
}

class ConverterCacheThreadsTest {

    @Test
    fun convertersPreparedFromManyThreadsAtOnceAllWork() {
        val space = Shifted()
        val expected = XyzD65(0.4, 0.5, 0.6).to(XyzD50).components()
        val start = CountDownLatch(1)
        val failures = ConcurrentLinkedQueue<Throwable>()
        val workers = List(8) {
            thread {
                start.await()
                try {
                    val out = DoubleArray(4)
                    space.converterTo(XyzD50).convert(doubleArrayOf(0.3, 0.4, 0.5, 0.0), out)
                    for (i in 0..2) assertEquals(expected[i], out[i])
                    space.converterTo(XyzD65).convert(doubleArrayOf(0.3, 0.4, 0.5, 0.0), out)
                    assertEquals(0.4, out[0])
                } catch (failure: Throwable) {
                    failures += failure
                }
            }
        }
        start.countDown()
        workers.forEach { it.join() }
        assertTrue(failures.isEmpty(), failures.joinToString())
    }
}
