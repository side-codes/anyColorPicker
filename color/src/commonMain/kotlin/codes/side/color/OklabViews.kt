package codes.side.color

import androidx.compose.runtime.Immutable
import kotlin.jvm.JvmInline

/**
 * A color in [Oklab], its components by name: L 0–1, a and b around ±0.4. A null component is
 * `none`. Get one with [asOklab] or [toOklab].
 */
@Immutable
@JvmInline
public value class OklabColor internal constructor(public val value: ColorValue) {
    public val l: Double? get() = value[Oklab.L]
    public val a: Double? get() = value[Oklab.A]
    public val b: Double? get() = value[Oklab.B]
    public val alpha: Double? get() = value.alphaOrNull

    /** A copy with the components given; the rest are kept, `none` included. Null writes `none`. */
    public fun with(
        l: Double? = this.l,
        a: Double? = this.a,
        b: Double? = this.b,
        alpha: Double? = this.alpha,
    ): OklabColor = OklabColor(Oklab(l, a, b, alpha))

    override fun toString(): String = value.toString()
}

/** This color as an [OklabColor]. It must be in [Oklab] already; [toOklab] converts. */
public fun ColorValue.asOklab(): OklabColor = OklabColor(requireIn(Oklab))

/** This color converted to [Oklab], as an [OklabColor]. */
public fun ColorValue.toOklab(): OklabColor = OklabColor(to(Oklab))

/**
 * A color in [Oklch], its components by name: L 0–1, chroma from 0 (0.4 is CSS's 100%), hue in
 * degrees. A null component is `none`. Get one with [asOklch] or [toOklch].
 */
@Immutable
@JvmInline
public value class OklchColor internal constructor(public val value: ColorValue) {
    public val l: Double? get() = value[Oklch.L]
    public val c: Double? get() = value[Oklch.C]
    public val h: Double? get() = value[Oklch.H]
    public val alpha: Double? get() = value.alphaOrNull

    /** A copy with the components given; the rest are kept, `none` included. Null writes `none`. */
    public fun with(
        l: Double? = this.l,
        c: Double? = this.c,
        h: Double? = this.h,
        alpha: Double? = this.alpha,
    ): OklchColor = OklchColor(Oklch(l, c, h, alpha))

    override fun toString(): String = value.toString()
}

/** This color as an [OklchColor]. It must be in [Oklch] already; [toOklch] converts. */
public fun ColorValue.asOklch(): OklchColor = OklchColor(requireIn(Oklch))

/** This color converted to [Oklch], as an [OklchColor]. */
public fun ColorValue.toOklch(): OklchColor = OklchColor(to(Oklch))
