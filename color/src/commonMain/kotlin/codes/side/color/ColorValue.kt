package codes.side.color

import androidx.compose.runtime.Immutable
import codes.side.color.internal.MAX_COMPONENTS

/**
 * A color: components in one [space], an [alpha], and which of them are `none`.
 *
 * Components are unclamped, so a color outside every display gamut holds exactly what it was
 * given; showing it is a separate, explicit mapping. A missing (`none`) component stores `0.0`.
 * Hues are wrapped into `0..<360`.
 *
 * Equality is exact: same space, same component bits (−0.0 counts as 0.0), same missing
 * components, same alpha. [isEquivalentTo] answers whether two values show the same color.
 */
@Immutable
public class ColorValue internal constructor(
    public val space: ColorSpace,
    private val c0: Double,
    private val c1: Double,
    private val c2: Double,
    private val c3: Double,
    private val missing: Int,
    /** Opacity in `0..1`; `0.0` when missing. */
    public val alpha: Double,
) {
    /** One bit per channel set when that component is `none`, plus [MISSING_ALPHA]. */
    public val missingMask: Int get() = missing

    /** True when alpha is `none`. */
    public val isAlphaMissing: Boolean get() = missing and MISSING_ALPHA != 0

    /** [channel]'s value, or null when it is `none`. */
    public operator fun get(channel: ColorChannel): Double? {
        requireOwn(channel)
        return if (missing and (1 shl channel.index) != 0) null else component(channel.index)
    }

    /** True when [channel] is `none`. */
    public fun isMissing(channel: ColorChannel): Boolean {
        requireOwn(channel)
        return missing and (1 shl channel.index) != 0
    }

    /** A copy with [channel] set to [value], or to `none` when [value] is null. */
    public fun with(channel: ColorChannel, value: Double?): ColorValue {
        requireOwn(channel)
        val components = components()
        val bit = 1 shl channel.index
        components[channel.index] = value ?: 0.0
        val newMissing = if (value == null) missing or bit else missing and bit.inv()
        return create(space, components, alpha, newMissing)
    }

    /** A copy with alpha set to [value], or to `none` when [value] is null. */
    public fun withAlpha(value: Double?): ColorValue {
        val newMissing = if (value == null) missing or MISSING_ALPHA else missing and MISSING_ALPHA.inv()
        return create(space, components(), value ?: 0.0, newMissing)
    }

    /** The components in channel order; a missing one reads `0.0`. */
    public fun components(): DoubleArray = DoubleArray(space.channels.size) { component(it) }

    /**
     * This color in [target].
     *
     * A missing component counts as 0 in the arithmetic, and stays missing where [target] has an
     * analogous channel. A hue that comes out powerless becomes missing and its colorfulness 0.
     * Nothing is clamped or mapped into a gamut, except into Okhsl and Okhsv, which describe sRGB
     * alone. A component the arithmetic overflows comes out as CSS makes an overflowing `calc()`:
     * infinity as the largest finite value of its sign, NaN as 0.
     */
    public fun to(target: ColorSpace): ColorValue {
        if (target == space) return this
        val buffer = DoubleArray(MAX_COMPONENTS)
        for (i in space.channels.indices) buffer[i] = component(i)
        space.converterTo(target).convert(buffer, buffer)
        val out = DoubleArray(target.channels.size) { finite(buffer[it]) }
        var outMissing = carriedMissing(target)
        val powerless = target.powerless(out)
        if (powerless != 0) {
            outMissing = outMissing or powerless
            target.channels.forEachIndexed { j, channel ->
                if (channel.analogous == AnalogousCategory.Colorfulness) out[j] = 0.0
            }
        }
        for (j in out.indices) if (outMissing and (1 shl j) != 0) out[j] = 0.0
        return create(target, out, alpha, outMissing or (missing and MISSING_ALPHA))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ColorValue) return false
        return space == other.space &&
            missing == other.missing &&
            c0 == other.c0 &&
            c1 == other.c1 &&
            c2 == other.c2 &&
            c3 == other.c3 &&
            alpha == other.alpha
    }

    override fun hashCode(): Int {
        var result = space.hashCode()
        result = 31 * result + missing
        result = 31 * result + c0.hashCode()
        result = 31 * result + c1.hashCode()
        result = 31 * result + c2.hashCode()
        result = 31 * result + c3.hashCode()
        result = 31 * result + alpha.hashCode()
        return result
    }

    /** For debugging: `oklch(0.7 0.15 none / 1.0)`. Not CSS output. */
    override fun toString(): String = buildString {
        append(space.id).append('(')
        for (i in space.channels.indices) {
            if (i > 0) append(' ')
            append(if (missing and (1 shl i) != 0) "none" else component(i).toString())
        }
        append(" / ").append(if (isAlphaMissing) "none" else alpha.toString()).append(')')
    }

    private fun component(index: Int): Double = when (index) {
        0 -> c0
        1 -> c1
        2 -> c2
        else -> c3
    }

    private fun requireOwn(channel: ColorChannel) {
        require(channel.space == space) { "$channel is not a channel of ${space.id}" }
    }

    // CSS's carrying forward: a missing component stays missing where the target has an analogous
    // channel. And when every source component without an analog in the target is missing, every
    // target component without one in the source is missing too (issue 10210).
    private fun carriedMissing(target: ColorSpace): Int {
        val sourceChannels = space.channels
        val sourceMissing = missing and ((1 shl sourceChannels.size) - 1)
        if (sourceMissing == 0) return 0
        val targetCategories = target.channels.mapNotNull { it.analogous }.toSet()
        val sourceCategories = sourceChannels.mapNotNull { it.analogous }.toSet()
        var out = 0
        sourceChannels.forEachIndexed { i, channel ->
            val category = channel.analogous
            if (sourceMissing and (1 shl i) != 0 && category != null) {
                target.channels.forEachIndexed { j, t -> if (t.analogous == category) out = out or (1 shl j) }
            }
        }
        val unmatched = sourceChannels.indices.filter { sourceChannels[it].analogous !in targetCategories }
        if (unmatched.isNotEmpty() && unmatched.all { sourceMissing and (1 shl it) != 0 }) {
            target.channels.forEachIndexed { j, t ->
                if (t.analogous == null || t.analogous !in sourceCategories) out = out or (1 shl j)
            }
        }
        return out
    }

    public companion object {
        /** The bit in [missingMask] marking alpha as `none`. */
        public const val MISSING_ALPHA: Int = 1 shl 4

        // Every ColorValue comes through here: the constructor stores what it is given, unchecked.
        // It is internal rather than private only so the companion needs no synthetic accessor,
        // which would otherwise show up in the public ABI.
        internal fun create(space: ColorSpace, components: DoubleArray, alpha: Double, missing: Int): ColorValue {
            val count = space.channels.size
            require(components.size == count) { "${space.id} takes $count components, got ${components.size}" }
            require(missing and ((1 shl count) - 1 or MISSING_ALPHA).inv() == 0) { "Missing mask $missing names no channel of ${space.id}" }
            val values = DoubleArray(MAX_COMPONENTS)
            for (i in 0 until count) {
                if (missing and (1 shl i) != 0) continue
                val channel = space.channels[i]
                var value = components[i]
                require(value.isFinite()) { "${space.id}.${channel.id} must be finite, was $value" }
                val limit = channel.limit
                require(limit == null || value in limit) { "${space.id}.${channel.id} must be in $limit, was $value" }
                if (channel.isHue) value = wrapHue(value)
                values[i] = value + 0.0
            }
            val storedAlpha = if (missing and MISSING_ALPHA != 0) {
                0.0
            } else {
                require(alpha.isFinite() && alpha in 0.0..1.0) { "Alpha must be in 0..1, was $alpha" }
                alpha + 0.0
            }
            return ColorValue(space, values[0], values[1], values[2], values[3], missing, storedAlpha)
        }

        private fun finite(value: Double): Double = when {
            value.isNaN() -> 0.0
            value == Double.POSITIVE_INFINITY -> Double.MAX_VALUE
            value == Double.NEGATIVE_INFINITY -> -Double.MAX_VALUE
            else -> value
        }

        private fun wrapHue(degrees: Double): Double {
            var wrapped = degrees % 360.0
            if (wrapped < 0.0) wrapped += 360.0
            return if (wrapped >= 360.0) 0.0 else wrapped
        }
    }
}
