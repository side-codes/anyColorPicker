package codes.side.color

import codes.side.color.internal.MAX_COMPONENTS
import codes.side.color.internal.Step
import codes.side.color.internal.cssName
import codes.side.color.internal.isDashedName

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
 * Two spaces are equal when their ids are: the id is a space's identity, so a space defined by
 * parameters, such as viewing conditions, puts them in its id. The library's spaces are singletons;
 * an app builds its own with the factories on [Companion].
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

    /** The RGB gamut "in gamut" is measured against, or null for a space with none (Lab, Oklab, XYZ). */
    public open val gamut: RgbGamut? get() = null

    /** How faithfully this space's conversions describe color. */
    public open val exactness: Exactness get() = Exactness.Exact

    /**
     * The components that are powerless for these values, as a bit mask over [channels]: a hue
     * whose colorfulness is at or below the space's threshold. A conversion into this space makes
     * them missing; a value constructed with them keeps them, and a conversion out of this space
     * treats them as missing and the color as the grey it is taken for. That conversion zeroes these
     * components and every channel tagged [AnalogousCategory.Colorfulness], so a space tags its
     * colorfulness channel for its grey to be the one it is taken for.
     */
    public open fun powerless(components: DoubleArray): Int = 0

    // Before a conversion out of this space: the [powerless] components and every colorfulness
    // component of [components] set to 0, leaving the grey the color is taken for. [missing] marks
    // which components are `none`, for a space whose rule depends on it.
    internal open fun makeAchromatic(components: DoubleArray, powerless: Int, missing: Int) {
        channels.forEachIndexed { i, channel ->
            if (powerless and (1 shl i) != 0 || channel.analogous == AnalogousCategory.Colorfulness) components[i] = 0.0
        }
    }

    // Whether [powerless] converts to [base] first, as Okhsl's and Okhsv's does. ColorValue.to then
    // converts once, asking [powerlessOfBase] of the result, and goes on from the base.
    internal open val powerlessFromBase: Boolean get() = false

    // [powerless], answered from the color's components in [base].
    internal open fun powerlessOfBase(base: DoubleArray): Int = 0

    /**
     * A color in this space. [missing] marks components that are `none`, one bit per channel, and a
     * null [alpha] marks alpha as `none`.
     *
     * @throws IllegalArgumentException if a component is not finite or lies outside its channel's
     * [ColorChannel.limit], alpha is outside `0..1`, or [missing] names no channel of this space.
     */
    public fun color(components: DoubleArray, alpha: Double? = 1.0, missing: Int = 0): ColorValue =
        ColorValue.create(this, components, alpha, missing)

    private val converters = ConverterCache(this)

    /** A prepared converter from this space to [target]. Converters are cached per target. */
    public fun converterTo(target: ColorSpace): ColorConverter = converters.converterTo(target)

    internal open fun stepsToBase(): List<Step> = listOf(Step { v -> toBase(v, v) })

    internal open fun stepsFromBase(): List<Step> = listOf(Step { v -> fromBase(v, v) })

    internal fun colorOf(components: Array<Double?>, alpha: Double?): ColorValue {
        var missing = 0
        val values = DoubleArray(components.size) { i ->
            val value = components[i]
            if (value == null) missing = missing or (1 shl i)
            value ?: 0.0
        }
        return ColorValue.create(this, values, alpha, missing)
    }

    final override fun equals(other: Any?): Boolean = other is ColorSpace && other.id == id

    final override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = id

    /**
     * The ways an app defines a space. Each [id] is a CSS custom name, `--` and then letters,
     * digits, `-` and `_`, so an app's space never passes for one of the library's (spaces are equal
     * when their ids are) and `toCssString` writes it as text `parseCss` reads. `--hsv`, `--okhsl`,
     * `--okhsv` and `--cmyk` are taken: they are how the library writes its own spaces.
     */
    public companion object {
        /**
         * An RGB space from its primaries and white. Its matrices are derived from the
         * chromaticities, and a white other than D65 is adapted with Bradford.
         */
        public fun rgb(
            id: String,
            primaries: RgbPrimaries,
            whitePoint: WhitePoint,
            transfer: TransferFunction,
        ): RgbColorSpace = RgbColorSpace.derive(appId(id), primaries, whitePoint, transfer)

        /** HSL over [over], with CSS's `hsl()` formulas. */
        public fun hsl(id: String, over: RgbColorSpace): HslColorSpace = HslColorSpace(appId(id), over)

        /** HSV over [over]. */
        public fun hsv(id: String, over: RgbColorSpace): HsvColorSpace = HsvColorSpace(appId(id), over)

        /** HWB over [over], with CSS's `hwb()` formulas. */
        public fun hwb(id: String, over: RgbColorSpace): HwbColorSpace = HwbColorSpace(appId(id), over)

        /**
         * The cylindrical form of [of], a space whose channels are lightness and two opponent
         * axes, as LCH is of Lab. Its hue is powerless at chroma ≤ [powerlessChroma], and 100%
         * chroma is [chromaReference].
         *
         * @throws IllegalArgumentException if [chromaReference] is not finite and positive, or
         * [powerlessChroma] not finite and at least 0.
         */
        public fun polar(
            id: String,
            of: ColorSpace,
            chromaReference: Double,
            powerlessChroma: Double,
            hueFamily: HueFamily,
        ): PolarColorSpace {
            require(chromaReference > 0.0 && chromaReference.isFinite()) { "The chroma reference must be finite and positive, was $chromaReference" }
            require(powerlessChroma >= 0.0 && powerlessChroma.isFinite()) { "The powerless chroma must be finite and not negative, was $powerlessChroma" }
            return PolarColorSpace(appId(id), of, chromaReference, powerlessChroma, hueFamily)
        }

        private fun appId(id: String): String {
            require(isDashedName(id)) { "An app's color space id is -- and then letters, digits, - and _, and $id is not" }
            require(ColorSpaces.all.none { cssName(it) == id }) { "$id is how the library writes its own space" }
            return id
        }
    }
}

internal const val XYZ_D65_ID: String = "xyz-d65"
