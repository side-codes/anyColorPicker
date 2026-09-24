package codes.side.color

import codes.side.color.internal.MAX_COMPONENTS
import codes.side.color.internal.checkBulk

/**
 * A prepared mapping from [source] into [gamut]'s encoded RGB with [method], for drawing: one color
 * gives what [toGamut] gives, many give a slider track or a plane. Immutable and safe to share
 * between threads.
 *
 * Like [ColorConverter], it takes complete components, with no `none`, and allocates nothing per
 * color: one scratch buffer per call. Bulk calls take colors packed at [source]'s channel count and
 * write three channels each.
 */
public class GamutMapper internal constructor(
    public val source: ColorSpace,
    public val gamut: RgbGamut,
    public val method: GamutMapping,
) {
    private val sourceSize = source.channels.size
    private val toSpace = source.converterTo(gamut.space)
    private val toOklab = source.converterTo(Oklab)

    /** Maps one color from [src] into [dst], which may be the same array. */
    public fun convert(src: DoubleArray, dst: DoubleArray) {
        val buffer = DoubleArray(2 * MAX_COMPONENTS)
        src.copyInto(buffer, 0, 0, sourceSize)
        src.copyInto(buffer, MAX_COMPONENTS, 0, sourceSize)
        map(buffer)
        buffer.copyInto(dst, 0, 0, 3)
    }

    /**
     * Maps [count] colors packed in [src] from [srcOffset] into [dst] from [dstOffset], three channels
     * each. [dst] may be [src] as long as no color is written over one not yet read.
     */
    public fun convert(src: DoubleArray, srcOffset: Int, dst: DoubleArray, dstOffset: Int, count: Int) {
        checkBulk(sourceSize, 3, src.size, srcOffset, dst.size, dstOffset, count, src === dst, source.id, gamut.space.id)
        val buffer = DoubleArray(2 * MAX_COMPONENTS)
        for (k in 0 until count) {
            val from = srcOffset + k * sourceSize
            for (i in 0 until sourceSize) {
                buffer[i] = src[from + i]
                buffer[MAX_COMPONENTS + i] = src[from + i]
            }
            map(buffer)
            val to = dstOffset + k * 3
            for (i in 0..2) dst[to + i] = buffer[i]
        }
    }

    /** As the [DoubleArray] form, for raster loops; the arithmetic is still in Double. */
    public fun convert(src: FloatArray, srcOffset: Int, dst: FloatArray, dstOffset: Int, count: Int) {
        checkBulk(sourceSize, 3, src.size, srcOffset, dst.size, dstOffset, count, src === dst, source.id, gamut.space.id)
        val buffer = DoubleArray(2 * MAX_COMPONENTS)
        for (k in 0 until count) {
            val from = srcOffset + k * sourceSize
            for (i in 0 until sourceSize) {
                val component = src[from + i].toDouble()
                buffer[i] = component
                buffer[MAX_COMPONENTS + i] = component
            }
            map(buffer)
            val to = dstOffset + k * 3
            for (i in 0..2) dst[to + i] = buffer[i].toFloat()
        }
    }

    override fun toString(): String = "GamutMapper(${source.id} → $gamut, $method)"

    // Maps the source color held twice in [buffer], at 0 and at MAX_COMPONENTS, to encoded RGB in
    // buffer[0..2]: straight through when the gamut holds it, as toGamut does, else through Oklab.
    private fun map(buffer: DoubleArray) {
        toSpace.convertInPlace(buffer)
        if (inCube(buffer)) return
        buffer.copyInto(buffer, 0, MAX_COMPONENTS, 2 * MAX_COMPONENTS)
        toOklab.convertInPlace(buffer)
        method.map(gamut, buffer[0], buffer[1], buffer[2], buffer)
        val transfer = gamut.space.transfer
        for (i in 0..2) buffer[i] = transfer.encode(buffer[i])
    }
}
