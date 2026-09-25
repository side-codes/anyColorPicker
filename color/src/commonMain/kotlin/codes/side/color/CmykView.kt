package codes.side.color

import androidx.compose.runtime.Immutable
import kotlin.jvm.JvmInline

/**
 * A color in [Cmyk], its components by name, 0–1. A null component is `none`. Get one with
 * [asCmyk] or [toCmyk].
 */
@Immutable
@JvmInline
public value class CmykColor internal constructor(public val value: ColorValue) {
    public val c: Double? get() = value[Cmyk.C]
    public val m: Double? get() = value[Cmyk.M]
    public val y: Double? get() = value[Cmyk.Y]
    public val k: Double? get() = value[Cmyk.K]
    public val alpha: Double? get() = value.alphaOrNull

    /** A copy with the components given; the rest are kept, `none` included. Null writes `none`. */
    public fun with(
        c: Double? = this.c,
        m: Double? = this.m,
        y: Double? = this.y,
        k: Double? = this.k,
        alpha: Double? = this.alpha,
    ): CmykColor = CmykColor(Cmyk(c, m, y, k, alpha))

    override fun toString(): String = value.toString()
}

/** This color as a [CmykColor]. It must be in [Cmyk] already; [toCmyk] converts. */
public fun ColorValue.asCmyk(): CmykColor = CmykColor(requireIn(Cmyk))

/** This color converted to [Cmyk], as a [CmykColor]. */
public fun ColorValue.toCmyk(): CmykColor = CmykColor(to(Cmyk))
