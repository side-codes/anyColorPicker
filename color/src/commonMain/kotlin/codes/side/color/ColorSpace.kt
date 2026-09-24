package codes.side.color

import codes.side.color.internal.MAX_COMPONENTS
import codes.side.color.internal.Step
import kotlin.concurrent.Volatile

/** How faithfully a space's conversions describe color. */
public enum class Exactness {
    /** Conversions are exact up to floating-point rounding. */
    Exact,

    /** Conversions rest on a fitted approximation; Okhsl and Okhsv. */
    Approximate,

    /** The components are not colorimetric; the naive CMYK formula. */
    NonColorimetric,
}

/**
 * A color space: its channels, and how to reach its [base] space.
 *
 * Every space hangs from XYZ-D65, the only one without a base, and a conversion climbs to the
 * nearest space both ends share before descending. [toBase] and [fromBase] take components in
 * arrays of at least four elements, which may be the same array.
 *
 * Two spaces are equal when their ids are. The library's spaces are singletons; an app builds its
 * own with the factories on [Companion].
 */
@SubclassOptInRequired(ExperimentalColorSpaceApi::class)
public abstract class ColorSpace protected constructor(
    public val id: String,
    channels: List<ColorChannel>,
    public val base: ColorSpace?,
) {
    /** This space's components, in order. */
    public val channels: List<ColorChannel> = channels.toList()

    init {
        require(id.isNotBlank()) { "A color space needs an id" }
        require(this.channels.size in 1..MAX_COMPONENTS) {
            "A color space has 1 to $MAX_COMPONENTS channels; $id has ${this.channels.size}"
        }
        var root: ColorSpace? = base
        while (root?.base != null) root = root.base
        require(if (root == null) id == XYZ_D65_ID else root.id == XYZ_D65_ID) {
            "$id does not descend from $XYZ_D65_ID"
        }
        this.channels.forEachIndexed { index, channel -> channel.attach(this, index) }
    }

    /** Converts [src], this space's components, to [base]'s, into [dst]. */
    public abstract fun toBase(src: DoubleArray, dst: DoubleArray)

    /** Converts [src], [base]'s components, to this space's, into [dst]. */
    public abstract fun fromBase(src: DoubleArray, dst: DoubleArray)

    /** The reference white; D65 unless the space says otherwise. */
    public open val whitePoint: WhitePoint get() = base?.whitePoint ?: WhitePoint.D65

    /** How faithfully this space's conversions describe color. */
    public open val exactness: Exactness get() = Exactness.Exact

    /**
     * The components that are powerless for these values, as a bit mask over [channels]: a hue
     * whose colorfulness is at or below the space's threshold. A conversion into this space makes
     * them missing; a value constructed with them keeps them.
     */
    public open fun powerless(components: DoubleArray): Int = 0

    /**
     * A color in this space. [missing] marks components that are `none`, one bit per channel,
     * plus [ColorValue.MISSING_ALPHA] for alpha.
     *
     * @throws IllegalArgumentException if a component is not finite or lies outside its channel's
     * [ColorChannel.limit], or alpha is outside `0..1`.
     */
    public fun color(components: DoubleArray, alpha: Double = 1.0, missing: Int = 0): ColorValue =
        ColorValue.create(this, components, alpha, missing)

    @Volatile
    private var converters: Array<ColorConverter> = emptyArray()

    /** A prepared converter from this space to [target]. Converters are cached per target. */
    public fun converterTo(target: ColorSpace): ColorConverter {
        val cached = converters
        for (converter in cached) if (converter.target === target) return converter
        val made = ColorConverter.build(this, target)
        converters = cached + made
        return made
    }

    internal open fun stepsToBase(): List<Step> = listOf(Step { v -> toBase(v, v) })

    internal open fun stepsFromBase(): List<Step> = listOf(Step { v -> fromBase(v, v) })

    internal fun colorOf(components: Array<Double?>, alpha: Double?): ColorValue {
        var missing = if (alpha == null) ColorValue.MISSING_ALPHA else 0
        val values = DoubleArray(components.size) { i ->
            val value = components[i]
            if (value == null) missing = missing or (1 shl i)
            value ?: 0.0
        }
        return ColorValue.create(this, values, alpha ?: 0.0, missing)
    }

    final override fun equals(other: Any?): Boolean = other is ColorSpace && other.id == id

    final override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = id

}

internal const val XYZ_D65_ID: String = "xyz-d65"
