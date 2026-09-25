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
 * A color in [OkLch], its components by name: L 0–1, chroma from 0 (0.4 is CSS's 100%), hue in
 * degrees. A null component is `none`. Get one with [asOkLch] or [toOkLch].
 */
@Immutable
@JvmInline
public value class OkLchColor internal constructor(public val value: ColorValue) {
    public val l: Double? get() = value[OkLch.L]
    public val c: Double? get() = value[OkLch.C]
    public val h: Double? get() = value[OkLch.H]
    public val alpha: Double? get() = value.alphaOrNull

    /** A copy with the components given; the rest are kept, `none` included. Null writes `none`. */
    public fun with(
        l: Double? = this.l,
        c: Double? = this.c,
        h: Double? = this.h,
        alpha: Double? = this.alpha,
    ): OkLchColor = OkLchColor(OkLch(l, c, h, alpha))

    override fun toString(): String = value.toString()
}

/** This color as an [OkLchColor]. It must be in [OkLch] already; [toOkLch] converts. */
public fun ColorValue.asOkLch(): OkLchColor = OkLchColor(requireIn(OkLch))

/** This color converted to [OkLch], as an [OkLchColor]. */
public fun ColorValue.toOkLch(): OkLchColor = OkLchColor(to(OkLch))
