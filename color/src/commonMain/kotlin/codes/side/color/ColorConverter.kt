package codes.side.color

import codes.side.color.internal.MAX_COMPONENTS
import codes.side.color.internal.Step
import codes.side.color.internal.fuse

/**
 * A prepared conversion from [source] to [target]: the route through their nearest shared base,
 * with consecutive matrices multiplied into one. Immutable and safe to share between threads.
 *
 * Components are complete here, with no `none`; [ColorValue.to] handles missing ones. Bulk calls
 * take colors packed one after another, each at its space's channel count.
 */
public class ColorConverter internal constructor(
    public val source: ColorSpace,
    public val target: ColorSpace,
    private val steps: Array<Step>,
) {
    private val sourceSize = source.channels.size
    private val targetSize = target.channels.size

    /** Converts one color from [src] into [dst], which may be the same array. */
    public fun convert(src: DoubleArray, dst: DoubleArray) {
        val v = DoubleArray(MAX_COMPONENTS)
        src.copyInto(v, 0, 0, sourceSize)
        for (step in steps) step.apply(v)
        v.copyInto(dst, 0, 0, targetSize)
    }

    /**
     * Converts [count] colors packed in [src] from [srcOffset] into [dst] from [dstOffset]. [dst] may
     * be [src] as long as no color is written over one not yet read.
     */
    public fun convert(src: DoubleArray, srcOffset: Int, dst: DoubleArray, dstOffset: Int, count: Int) {
        checkBounds(src.size, srcOffset, dst.size, dstOffset, count, src === dst)
        val v = DoubleArray(MAX_COMPONENTS)
        for (k in 0 until count) {
            val from = srcOffset + k * sourceSize
            for (i in 0 until sourceSize) v[i] = src[from + i]
            for (step in steps) step.apply(v)
            val to = dstOffset + k * targetSize
            for (i in 0 until targetSize) dst[to + i] = v[i]
        }
    }

    /** As the [DoubleArray] form, for raster loops; the arithmetic is still in Double. */
    public fun convert(src: FloatArray, srcOffset: Int, dst: FloatArray, dstOffset: Int, count: Int) {
        checkBounds(src.size, srcOffset, dst.size, dstOffset, count, src === dst)
        val v = DoubleArray(MAX_COMPONENTS)
        for (k in 0 until count) {
            val from = srcOffset + k * sourceSize
            for (i in 0 until sourceSize) v[i] = src[from + i].toDouble()
            for (step in steps) step.apply(v)
            val to = dstOffset + k * targetSize
            for (i in 0 until targetSize) dst[to + i] = v[i].toFloat()
        }
    }

    private fun checkBounds(srcSize: Int, srcOffset: Int, dstSize: Int, dstOffset: Int, count: Int, sameArray: Boolean) {
        require(count >= 0 && srcOffset >= 0 && dstOffset >= 0) { "Negative count or offset" }
        // Long, so an absurd count cannot wrap around and pass.
        val srcEnd = srcOffset + count.toLong() * sourceSize
        val dstEnd = dstOffset + count.toLong() * targetSize
        require(srcEnd <= srcSize) { "Source holds fewer than $count ${source.id} colors" }
        require(dstEnd <= dstSize) { "Destination has room for fewer than $count ${target.id} colors" }
        if (sameArray && count > 1 && dstEnd > srcOffset && srcEnd > dstOffset) {
            // Each color is read whole before it is written, so a color's write may reach no further
            // than the next color's read: dstOffset − srcOffset ≤ (k + 1)(sourceSize − targetSize) for
            // every k up to count − 2, tightest at the first k or the last.
            val step = (sourceSize - targetSize).toLong()
            val bound = if (step >= 0) step else (count - 1) * step
            require(dstOffset - srcOffset <= bound) {
                "Converting in place from $srcOffset to $dstOffset would overwrite ${source.id} colors not yet read"
            }
        }
    }

    override fun toString(): String = "ColorConverter(${source.id} → ${target.id}, ${steps.size} steps)"

    internal val stepCount: Int get() = steps.size

    internal companion object {
        fun build(from: ColorSpace, to: ColorSpace): ColorConverter {
            val up = ancestry(from)
            val down = ancestry(to)
            val shared = up.first { space -> down.any { it === space } }
            val steps = ArrayList<Step>()
            for (space in up) {
                if (space === shared) break
                steps += space.stepsToBase()
            }
            for (space in down.takeWhile { it !== shared }.asReversed()) {
                steps += space.stepsFromBase()
            }
            return ColorConverter(from, to, fuse(steps).toTypedArray())
        }

        private fun ancestry(space: ColorSpace): List<ColorSpace> {
            val chain = ArrayList<ColorSpace>()
            var current: ColorSpace? = space
            while (current != null) {
                chain += current
                current = current.base
            }
            return chain
        }
    }
}
