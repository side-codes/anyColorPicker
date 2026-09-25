package codes.side.color

import androidx.compose.runtime.Immutable
import kotlin.jvm.JvmInline

/**
 * A color in [XyzD65], its components by name: Y = 1 is diffuse white. A null component is `none`.
 * Get one with [asXyzD65] or [toXyzD65].
 */
@Immutable
@JvmInline
public value class XyzD65Color internal constructor(public val value: ColorValue) {
    public val x: Double? get() = value[XyzD65.X]
    public val y: Double? get() = value[XyzD65.Y]
    public val z: Double? get() = value[XyzD65.Z]
    public val alpha: Double? get() = value.alphaOrNull

    /** A copy with the components given; the rest are kept, `none` included. Null writes `none`. */
    public fun with(
        x: Double? = this.x,
        y: Double? = this.y,
        z: Double? = this.z,
        alpha: Double? = this.alpha,
    ): XyzD65Color = XyzD65Color(XyzD65(x, y, z, alpha))

    override fun toString(): String = value.toString()
}

/** This color as an [XyzD65Color]. It must be in [XyzD65] already; [toXyzD65] converts. */
public fun ColorValue.asXyzD65(): XyzD65Color = XyzD65Color(requireIn(XyzD65))

/** This color converted to [XyzD65], as an [XyzD65Color]. */
public fun ColorValue.toXyzD65(): XyzD65Color = XyzD65Color(to(XyzD65))

/**
 * A color in [XyzD50], its components by name: Y = 1 is diffuse white. A null component is `none`.
 * Get one with [asXyzD50] or [toXyzD50].
 */
@Immutable
@JvmInline
public value class XyzD50Color internal constructor(public val value: ColorValue) {
    public val x: Double? get() = value[XyzD50.X]
    public val y: Double? get() = value[XyzD50.Y]
    public val z: Double? get() = value[XyzD50.Z]
    public val alpha: Double? get() = value.alphaOrNull

    /** A copy with the components given; the rest are kept, `none` included. Null writes `none`. */
    public fun with(
        x: Double? = this.x,
        y: Double? = this.y,
        z: Double? = this.z,
        alpha: Double? = this.alpha,
    ): XyzD50Color = XyzD50Color(XyzD50(x, y, z, alpha))

    override fun toString(): String = value.toString()
}

/** This color as an [XyzD50Color]. It must be in [XyzD50] already; [toXyzD50] converts. */
public fun ColorValue.asXyzD50(): XyzD50Color = XyzD50Color(requireIn(XyzD50))

/** This color converted to [XyzD50], as an [XyzD50Color]. */
public fun ColorValue.toXyzD50(): XyzD50Color = XyzD50Color(to(XyzD50))
