package codes.side.color

import androidx.compose.runtime.Immutable
import codes.side.color.internal.ColorRules
import codes.side.color.internal.MAX_COMPONENTS
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// Where the stored mask keeps a missing alpha: past any channel's bit, and never in
// [ColorValue.missingMask]. File-private, so Java sees no field for it.
private const val ALPHA_MISSING: Int = 1 shl 31

/**
 * A color: components in one [space], an [alpha], and which of them are `none`.
 *
 * Components are unclamped, so a color outside every display gamut holds exactly what it was
 * given; showing it is a separate, explicit mapping. A missing (`none`) component stores `0.0`.
 * Hues are wrapped into `0..<360`.
 *
 * Equality is exact: same space, same component bits (−0.0 counts as 0.0), same missing
 * components, same alpha. [isEquivalentTo] is CSS Color 4's test for equivalent colors.
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
    /** One bit per channel, set when that component is `none`. Alpha's is [isAlphaMissing]. */
    public val missingMask: Int get() = missing and ALPHA_MISSING.inv()

    /** True when alpha is `none`. */
    public val isAlphaMissing: Boolean get() = missing and ALPHA_MISSING != 0

    // Alpha as the factories take it: null when it is `none`.
    internal val alphaOrNull: Double? get() = if (isAlphaMissing) null else alpha

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
        val newMissing = if (value == null) missingMask or bit else missingMask and bit.inv()
        return create(space, components, alphaOrNull, newMissing)
    }

    /** A copy with alpha set to [value], or to `none` when [value] is null. */
    public fun withAlpha(value: Double?): ColorValue = create(space, components(), value, missingMask)

    /** The components in channel order; a missing one reads `0.0`. */
    public fun components(): DoubleArray = DoubleArray(space.channels.size) { component(it) }

    /**
     * This color in [target].
     *
     * As CSS Color 4 §11.2 prepares a color for conversion, a powerless hue of this color counts as
     * missing and its colorfulness as 0 first, so a near-grey converts as the grey it is taken for.
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
        // Only what was missing to begin with carries forward: CSS carries before it handles powerless
        // components (§13.3), and a polar target makes the grey's hue missing on its own.
        convertInto(target, buffer)
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
        return create(target, out, alphaOrNull, outMissing)
    }

    // [buffer], this color's components, converted into [target] after CSS Color 4 §11.2's preparation.
    private fun convertInto(target: ColorSpace, buffer: DoubleArray) {
        if (space.powerlessFromBase) {
            val base = buffer.copyOf()
            space.toBase(base, base)
            if (space.powerlessOfBase(base) and missing.inv() == 0) {
                base.copyInto(buffer)
                checkNotNull(space.base).converterTo(target).convert(buffer, buffer)
                return
            }
        }
        val powerlessHere = space.powerless(buffer) and missing.inv()
        if (powerlessHere != 0) space.makeAchromatic(buffer, powerlessHere, missing)
        space.converterTo(target).convert(buffer, buffer)
    }

    /**
     * True when this and [other] are CSS Color 4's equivalent colors (§12). A powerless hue counts as
     * missing first, and [Hsl] and [Hwb] colors are compared as [Srgb], as CSS reads `hsl()` and
     * `hwb()`. In one space every component must match: a missing one only another missing one, a
     * number within 1e-5 of its channel's reference range, a hue around the circle, alpha within 1e-5.
     * Across spaces any missing component makes two colors different; otherwise both are compared in
     * Oklab, L, a, b and alpha within 1e-5.
     */
    public fun isEquivalentTo(other: ColorValue): Boolean {
        val a = comparable()
        val b = other.comparable()
        if (a.space == b.space) return a.matches(b)
        if (a.missing != 0 || b.missing != 0) return false
        val labA = a.to(Oklab)
        val labB = b.to(Oklab)
        val epsilon = ColorRules.EQUIVALENCE_EPSILON
        return abs(labA.c0 - labB.c0) <= epsilon &&
            abs(labA.c1 - labB.c1) <= epsilon &&
            abs(labA.c2 - labB.c2) <= epsilon &&
            abs(a.alpha - b.alpha) <= epsilon
    }

    // §12's first step, powerless components made missing, and its reading of hsl() and hwb() as sRGB.
    private fun comparable(): ColorValue {
        if (space == Hsl || space == Hwb) return to(Srgb)
        val buffer = DoubleArray(MAX_COMPONENTS)
        for (i in space.channels.indices) buffer[i] = component(i)
        val powerless = space.powerless(buffer) and missing.inv()
        if (powerless == 0) return this
        space.makeAchromatic(buffer, powerless, missing)
        return create(space, buffer.copyOf(space.channels.size), alphaOrNull, missingMask or powerless)
    }

    // Component by component, in one space: a missing component equals only another missing one.
    private fun matches(other: ColorValue): Boolean {
        if (missing != other.missing) return false
        val epsilon = ColorRules.EQUIVALENCE_EPSILON
        space.channels.forEachIndexed { i, channel ->
            if (missing and (1 shl i) != 0) return@forEachIndexed
            var difference = abs(component(i) - other.component(i))
            if (channel.isHue) difference = min(difference, 360.0 - difference)
            if (difference > epsilon * max(1.0, abs(channel.referenceRange.endInclusive))) return false
        }
        return isAlphaMissing || abs(alpha - other.alpha) <= epsilon
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
        // Every ColorValue comes through here: the constructor stores what it is given, unchecked.
        // It is internal rather than private only so the companion needs no synthetic accessor,
        // which would otherwise show up in the public ABI.
        internal fun create(space: ColorSpace, components: DoubleArray, alpha: Double?, missing: Int): ColorValue {
            val count = space.channels.size
            require(components.size == count) { "${space.id} takes $count components, got ${components.size}" }
            require(missing and ((1 shl count) - 1).inv() == 0) { "Missing mask $missing names no channel of ${space.id}" }
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
            if (alpha == null) return ColorValue(space, values[0], values[1], values[2], values[3], missing or ALPHA_MISSING, 0.0)
            require(alpha.isFinite() && alpha in 0.0..1.0) { "Alpha must be in 0..1, was $alpha" }
            return ColorValue(space, values[0], values[1], values[2], values[3], missing, alpha + 0.0)
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
