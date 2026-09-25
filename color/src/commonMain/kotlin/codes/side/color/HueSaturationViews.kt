package codes.side.color

import androidx.compose.runtime.Immutable
import kotlin.jvm.JvmInline

/**
 * A color in [Hsl], its components by name: hue in degrees, saturation and lightness 0–100. A null
 * component is `none`. Get one with [asHsl] or [toHsl].
 */
@Immutable
@JvmInline
public value class HslColor internal constructor(public val value: ColorValue) {
    public val h: Double? get() = value[Hsl.H]
    public val s: Double? get() = value[Hsl.S]
    public val l: Double? get() = value[Hsl.L]
    public val alpha: Double? get() = value.alphaOrNull

    /** A copy with the components given; the rest are kept, `none` included. Null writes `none`. */
    public fun with(
        h: Double? = this.h,
        s: Double? = this.s,
        l: Double? = this.l,
        alpha: Double? = this.alpha,
    ): HslColor = HslColor(Hsl(h, s, l, alpha))

    override fun toString(): String = value.toString()
}

/** This color as an [HslColor]. It must be in [Hsl] already; [toHsl] converts. */
public fun ColorValue.asHsl(): HslColor = HslColor(requireIn(Hsl))

/** This color converted to [Hsl], as an [HslColor]. */
public fun ColorValue.toHsl(): HslColor = HslColor(to(Hsl))

/**
 * A color in [Hwb], its components by name: hue in degrees, whiteness and blackness 0–100. A null
 * component is `none`. Get one with [asHwb] or [toHwb].
 */
@Immutable
@JvmInline
public value class HwbColor internal constructor(public val value: ColorValue) {
    public val h: Double? get() = value[Hwb.H]
    public val w: Double? get() = value[Hwb.W]
    public val b: Double? get() = value[Hwb.B]
    public val alpha: Double? get() = value.alphaOrNull

    /** A copy with the components given; the rest are kept, `none` included. Null writes `none`. */
    public fun with(
        h: Double? = this.h,
        w: Double? = this.w,
        b: Double? = this.b,
        alpha: Double? = this.alpha,
    ): HwbColor = HwbColor(Hwb(h, w, b, alpha))

    override fun toString(): String = value.toString()
}

/** This color as an [HwbColor]. It must be in [Hwb] already; [toHwb] converts. */
public fun ColorValue.asHwb(): HwbColor = HwbColor(requireIn(Hwb))

/** This color converted to [Hwb], as an [HwbColor]. */
public fun ColorValue.toHwb(): HwbColor = HwbColor(to(Hwb))

/**
 * A color in [Hsv], its components by name: hue in degrees, saturation and value 0–100. A null
 * component is `none`. Get one with [asHsv] or [toHsv].
 */
@Immutable
@JvmInline
public value class HsvColor internal constructor(public val value: ColorValue) {
    public val h: Double? get() = value[Hsv.H]
    public val s: Double? get() = value[Hsv.S]
    public val v: Double? get() = value[Hsv.V]
    public val alpha: Double? get() = value.alphaOrNull

    /** A copy with the components given; the rest are kept, `none` included. Null writes `none`. */
    public fun with(
        h: Double? = this.h,
        s: Double? = this.s,
        v: Double? = this.v,
        alpha: Double? = this.alpha,
    ): HsvColor = HsvColor(Hsv(h, s, v, alpha))

    override fun toString(): String = value.toString()
}

/** This color as an [HsvColor]. It must be in [Hsv] already; [toHsv] converts. */
public fun ColorValue.asHsv(): HsvColor = HsvColor(requireIn(Hsv))

/** This color converted to [Hsv], as an [HsvColor]. */
public fun ColorValue.toHsv(): HsvColor = HsvColor(to(Hsv))
