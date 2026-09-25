package codes.side.color

import androidx.compose.runtime.Immutable
import kotlin.jvm.JvmInline

/**
 * A color in [Srgb], its components by name: 0–1 inside sRGB, unbounded outside it. A null
 * component is `none`. Get one with [asSrgb] or [toSrgb].
 */
@Immutable
@JvmInline
public value class SrgbColor internal constructor(public val value: ColorValue) {
    public val r: Double? get() = value[Srgb.R]
    public val g: Double? get() = value[Srgb.G]
    public val b: Double? get() = value[Srgb.B]
    public val alpha: Double? get() = value.alphaOrNull

    /** A copy with the components given; the rest are kept, `none` included. Null writes `none`. */
    public fun with(
        r: Double? = this.r,
        g: Double? = this.g,
        b: Double? = this.b,
        alpha: Double? = this.alpha,
    ): SrgbColor = SrgbColor(Srgb(r, g, b, alpha))

    override fun toString(): String = value.toString()
}

/** This color as an [SrgbColor]. It must be in [Srgb] already; [toSrgb] converts. */
public fun ColorValue.asSrgb(): SrgbColor = SrgbColor(requireIn(Srgb))

/** This color converted to [Srgb], as an [SrgbColor]. */
public fun ColorValue.toSrgb(): SrgbColor = SrgbColor(to(Srgb))

/**
 * A color in [SrgbLinear], its components by name: linear light, 0–1 inside sRGB, unbounded outside
 * it. A null component is `none`. Get one with [asSrgbLinear] or [toSrgbLinear].
 */
@Immutable
@JvmInline
public value class SrgbLinearColor internal constructor(public val value: ColorValue) {
    public val r: Double? get() = value[SrgbLinear.R]
    public val g: Double? get() = value[SrgbLinear.G]
    public val b: Double? get() = value[SrgbLinear.B]
    public val alpha: Double? get() = value.alphaOrNull

    /** A copy with the components given; the rest are kept, `none` included. Null writes `none`. */
    public fun with(
        r: Double? = this.r,
        g: Double? = this.g,
        b: Double? = this.b,
        alpha: Double? = this.alpha,
    ): SrgbLinearColor = SrgbLinearColor(SrgbLinear(r, g, b, alpha))

    override fun toString(): String = value.toString()
}

/** This color as an [SrgbLinearColor]. It must be in [SrgbLinear] already; [toSrgbLinear] converts. */
public fun ColorValue.asSrgbLinear(): SrgbLinearColor = SrgbLinearColor(requireIn(SrgbLinear))

/** This color converted to [SrgbLinear], as an [SrgbLinearColor]. */
public fun ColorValue.toSrgbLinear(): SrgbLinearColor = SrgbLinearColor(to(SrgbLinear))

/**
 * A color in [DisplayP3], its components by name: 0–1 inside Display P3, unbounded outside it. A
 * null component is `none`. Get one with [asDisplayP3] or [toDisplayP3].
 */
@Immutable
@JvmInline
public value class DisplayP3Color internal constructor(public val value: ColorValue) {
    public val r: Double? get() = value[DisplayP3.R]
    public val g: Double? get() = value[DisplayP3.G]
    public val b: Double? get() = value[DisplayP3.B]
    public val alpha: Double? get() = value.alphaOrNull

    /** A copy with the components given; the rest are kept, `none` included. Null writes `none`. */
    public fun with(
        r: Double? = this.r,
        g: Double? = this.g,
        b: Double? = this.b,
        alpha: Double? = this.alpha,
    ): DisplayP3Color = DisplayP3Color(DisplayP3(r, g, b, alpha))

    override fun toString(): String = value.toString()
}

/** This color as a [DisplayP3Color]. It must be in [DisplayP3] already; [toDisplayP3] converts. */
public fun ColorValue.asDisplayP3(): DisplayP3Color = DisplayP3Color(requireIn(DisplayP3))

/** This color converted to [DisplayP3], as a [DisplayP3Color]. */
public fun ColorValue.toDisplayP3(): DisplayP3Color = DisplayP3Color(to(DisplayP3))
