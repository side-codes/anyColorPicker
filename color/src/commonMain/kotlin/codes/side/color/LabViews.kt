package codes.side.color

import androidx.compose.runtime.Immutable
import kotlin.jvm.JvmInline

/**
 * A color in [Lab], its components by name: L 0–100, a and b around ±125. A null component is
 * `none`. Get one with [asLab] or [toLab].
 */
@Immutable
@JvmInline
public value class LabColor internal constructor(public val value: ColorValue) {
    public val l: Double? get() = value[Lab.L]
    public val a: Double? get() = value[Lab.A]
    public val b: Double? get() = value[Lab.B]
    public val alpha: Double? get() = value.alphaOrNull

    /** A copy with the components given; the rest are kept, `none` included. Null writes `none`. */
    public fun with(
        l: Double? = this.l,
        a: Double? = this.a,
        b: Double? = this.b,
        alpha: Double? = this.alpha,
    ): LabColor = LabColor(Lab(l, a, b, alpha))

    override fun toString(): String = value.toString()
}

/** This color as a [LabColor]. It must be in [Lab] already; [toLab] converts. */
public fun ColorValue.asLab(): LabColor = LabColor(requireIn(Lab))

/** This color converted to [Lab], as a [LabColor]. */
public fun ColorValue.toLab(): LabColor = LabColor(to(Lab))

/**
 * A color in [Lch], its components by name: L 0–100, chroma from 0 (150 is CSS's 100%), hue in
 * degrees. A null component is `none`. Get one with [asLch] or [toLch].
 */
@Immutable
@JvmInline
public value class LchColor internal constructor(public val value: ColorValue) {
    public val l: Double? get() = value[Lch.L]
    public val c: Double? get() = value[Lch.C]
    public val h: Double? get() = value[Lch.H]
    public val alpha: Double? get() = value.alphaOrNull

    /** A copy with the components given; the rest are kept, `none` included. Null writes `none`. */
    public fun with(
        l: Double? = this.l,
        c: Double? = this.c,
        h: Double? = this.h,
        alpha: Double? = this.alpha,
    ): LchColor = LchColor(Lch(l, c, h, alpha))

    override fun toString(): String = value.toString()
}

/** This color as an [LchColor]. It must be in [Lch] already; [toLch] converts. */
public fun ColorValue.asLch(): LchColor = LchColor(requireIn(Lch))

/** This color converted to [Lch], as an [LchColor]. */
public fun ColorValue.toLch(): LchColor = LchColor(to(Lch))
