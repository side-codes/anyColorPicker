package codes.side.color

import kotlin.concurrent.Volatile

/**
 * The converters a space has prepared, one per target id. A target that equals a cached one but is
 * another instance replaces it, so a space built afresh for every conversion keeps one entry rather
 * than one per call. Copy-on-write behind a volatile array: readers never lock, and losing a race
 * only builds a converter twice.
 */
internal class ConverterCache(private val source: ColorSpace) {
    @Volatile
    private var converters: Array<ColorConverter> = emptyArray()

    val size: Int get() = converters.size

    fun converterTo(target: ColorSpace): ColorConverter {
        val cached = converters
        for (converter in cached) if (converter.target === target) return converter
        val made = ColorConverter.build(source, target)
        val stale = cached.indexOfFirst { it.target == target }
        converters = if (stale < 0) cached + made else cached.copyOf().also { it[stale] = made }
        return made
    }
}
