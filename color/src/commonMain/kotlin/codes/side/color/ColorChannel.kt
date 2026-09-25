package codes.side.color

import kotlin.math.abs

/**
 * CSS Color 4's analogous-component categories. A component missing in the source stays missing
 * in a destination channel of the same category when a color is converted.
 */
public enum class AnalogousCategory {
    Reds,
    Greens,
    Blues,
    Lightness,
    Colorfulness,
    Hue,
    OpponentA,
    OpponentB,
}

/**
 * Hues measured as the same angle. HSL, HSV and HWB over one RGB space share the hexcone angle of
 * that space; OkLCh, Okhsl and Okhsv share Oklab's; LCH has CIELab's. A picker keeps one
 * remembered hue per family.
 */
public class HueFamily(public val id: String) {

    override fun equals(other: Any?): Boolean = other is HueFamily && other.id == id

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "HueFamily($id)"

    public companion object {
        /** The hue angle of Oklab, shared by OkLCh, Okhsl and Okhsv. */
        public val Oklab: HueFamily = HueFamily("oklab")

        /** The hue angle of CIELab, used by LCH. */
        public val CieLab: HueFamily = HueFamily("cielab")

        /** The hexcone hue of HSL, HSV and HWB built over [space]. */
        public fun rgbHexcone(space: ColorSpace): HueFamily = HueFamily("hexcone-${space.id}")
    }
}

/** What a channel measures. */
public sealed class ChannelKind {
    /** A quantity along an axis. */
    public data object Linear : ChannelKind()

    /** An angle in degrees, wrapped into `0..<360`. */
    public class Hue(public val family: HueFamily) : ChannelKind() {
        override fun equals(other: Any?): Boolean = other is Hue && other.family == family

        override fun hashCode(): Int = family.hashCode()

        override fun toString(): String = "Hue(${family.id})"
    }
}

/**
 * One component of a [ColorSpace], carrying what a slider, a parser and a conversion need to know
 * about it.
 *
 * Three ranges, because they answer different questions. [referenceRange] is what 100% means in
 * CSS and the span a slider covers by default. [gamutBound] is where the space's own gamut ends,
 * when it has one. [limit] holds only values that are invalid rather than merely out of gamut;
 * constructing a color outside it throws.
 *
 * [step] and [pageStep] are how far one key press moves a slider on this channel, and one Page Up.
 * By default [step] is the largest power of ten at or below a hundredth of [referenceRange]'s span,
 * and [pageStep] is ten of them.
 *
 * @throws IllegalArgumentException if [referenceRange] has no finite, positive span, if [step] is not
 * positive and finite, or if [pageStep] is not finite or is smaller than [step].
 */
public class ColorChannel(
    public val id: String,
    public val referenceRange: ClosedFloatingPointRange<Double>,
    public val kind: ChannelKind = ChannelKind.Linear,
    public val gamutBound: ClosedFloatingPointRange<Double>? = null,
    public val limit: ClosedFloatingPointRange<Double>? = null,
    public val analogous: AnalogousCategory? = null,
    public val precision: Int = 5,
    public val step: Double = defaultStep(referenceRange),
    public val pageStep: Double = step * 10.0,
) {
    init {
        val span = referenceRange.endInclusive - referenceRange.start
        require(span > 0.0 && span.isFinite()) { "Channel $id's reference range needs a finite, positive span, was $referenceRange" }
        require(step > 0.0 && step.isFinite()) { "Channel $id's step must be positive and finite, was $step" }
        require(pageStep >= step && pageStep.isFinite()) { "Channel $id's page step must be at least its step, was $pageStep" }
    }

    private var owner: ColorSpace? = null
    private var position: Int = -1

    /** The space this channel belongs to. */
    public val space: ColorSpace
        get() = checkNotNull(owner) { "Channel $id is not attached to a color space" }

    /** This channel's position in [space]'s components. */
    public val index: Int
        get() {
            space
            return position
        }

    /** True for a hue angle. */
    public val isHue: Boolean get() = kind is ChannelKind.Hue

    internal fun attach(space: ColorSpace, index: Int) {
        check(owner == null) { "Channel $id already belongs to ${owner?.id}" }
        owner = space
        position = index
    }

    override fun toString(): String = "${owner?.id ?: "?"}.$id"
}

// The largest power of ten at or below a hundredth of [range]'s span. Each power is built from exact
// integers, so a step of 0.01 is the same double as the literal 0.01.
private fun defaultStep(range: ClosedFloatingPointRange<Double>): Double {
    val target = (range.endInclusive - range.start) / 100.0
    require(target > 0.0 && target.isFinite()) { "A reference range needs a finite, positive span, was $range" }
    var exponent = 0
    while (powerOfTen(exponent) > target) exponent--
    while (powerOfTen(exponent + 1) <= target) exponent++
    return powerOfTen(exponent)
}

private fun powerOfTen(exponent: Int): Double {
    var power = 1.0
    repeat(abs(exponent)) { power *= 10.0 }
    return if (exponent < 0) 1.0 / power else power
}
