package codes.side.color

import codes.side.color.internal.MAX_COMPONENTS
import codes.side.color.internal.Step
import codes.side.color.internal.checkBulk
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
        checkBulk(sourceSize, targetSize, src.size, srcOffset, dst.size, dstOffset, count, src === dst, source.id, target.id)
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
        checkBulk(sourceSize, targetSize, src.size, srcOffset, dst.size, dstOffset, count, src === dst, source.id, target.id)
        val v = DoubleArray(MAX_COMPONENTS)
        for (k in 0 until count) {
            val from = srcOffset + k * sourceSize
            for (i in 0 until sourceSize) v[i] = src[from + i].toDouble()
            for (step in steps) step.apply(v)
            val to = dstOffset + k * targetSize
            for (i in 0 until targetSize) dst[to + i] = v[i].toFloat()
        }
    }

    /** Runs the conversion on [v], a buffer of [MAX_COMPONENTS] holding a source color, which it leaves holding the target color. */
    internal fun convertInPlace(v: DoubleArray) {
        for (step in steps) step.apply(v)
    }

    override fun toString(): String = "ColorConverter(${source.id} → ${target.id}, ${steps.size} steps)"

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
