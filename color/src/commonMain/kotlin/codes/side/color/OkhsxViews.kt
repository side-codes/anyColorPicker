package codes.side.color

import androidx.compose.runtime.Immutable
import kotlin.jvm.JvmInline

/**
 * A color in [Okhsl], its components by name: hue in degrees, saturation and lightness 0–1. A null
 * component is `none`. Get one with [asOkhsl] or [toOkhsl]; converting brings a color from outside
 * sRGB to sRGB's edge, as Okhsl describes sRGB alone.
 */
@Immutable
@JvmInline
public value class OkhslColor internal constructor(public val value: ColorValue) {
    public val h: Double? get() = value[Okhsl.H]
    public val s: Double? get() = value[Okhsl.S]
    public val l: Double? get() = value[Okhsl.L]
    public val alpha: Double? get() = value.alphaOrNull

    /** A copy with the components given; the rest are kept, `none` included. Null writes `none`. */
    public fun with(
        h: Double? = this.h,
        s: Double? = this.s,
        l: Double? = this.l,
        alpha: Double? = this.alpha,
    ): OkhslColor = OkhslColor(Okhsl(h, s, l, alpha))

    override fun toString(): String = value.toString()
}

/** This color as an [OkhslColor]. It must be in [Okhsl] already; [toOkhsl] converts. */
public fun ColorValue.asOkhsl(): OkhslColor = OkhslColor(requireIn(Okhsl))

/** This color converted to [Okhsl], as an [OkhslColor]. */
public fun ColorValue.toOkhsl(): OkhslColor = OkhslColor(to(Okhsl))

/**
 * A color in [Okhsv], its components by name: hue in degrees, saturation and value 0–1. A null
 * component is `none`. Get one with [asOkhsv] or [toOkhsv]; converting brings a color from outside
 * sRGB to sRGB's edge, as Okhsv describes sRGB alone.
 */
@Immutable
@JvmInline
public value class OkhsvColor internal constructor(public val value: ColorValue) {
    public val h: Double? get() = value[Okhsv.H]
    public val s: Double? get() = value[Okhsv.S]
    public val v: Double? get() = value[Okhsv.V]
    public val alpha: Double? get() = value.alphaOrNull

    /** A copy with the components given; the rest are kept, `none` included. Null writes `none`. */
    public fun with(
        h: Double? = this.h,
        s: Double? = this.s,
        v: Double? = this.v,
        alpha: Double? = this.alpha,
    ): OkhsvColor = OkhsvColor(Okhsv(h, s, v, alpha))

    override fun toString(): String = value.toString()
}

/** This color as an [OkhsvColor]. It must be in [Okhsv] already; [toOkhsv] converts. */
public fun ColorValue.asOkhsv(): OkhsvColor = OkhsvColor(requireIn(Okhsv))

/** This color converted to [Okhsv], as an [OkhsvColor]. */
public fun ColorValue.toOkhsv(): OkhsvColor = OkhsvColor(to(Okhsv))
