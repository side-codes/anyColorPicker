package codes.side.color

/** A reference white as a CIE 1931 xy chromaticity. Its XYZ is taken with Y = 1. */
public class WhitePoint(public val x: Double, public val y: Double) {

    init {
        require(x > 0.0 && y > 0.0 && x + y < 1.0) { "Not a chromaticity: ($x, $y)" }
    }

    /** X of this white with Y = 1. */
    public val xyzX: Double get() = x / y

    /** Z of this white with Y = 1. */
    public val xyzZ: Double get() = (1.0 - x - y) / y

    override fun equals(other: Any?): Boolean = other is WhitePoint && other.x == x && other.y == y

    override fun hashCode(): Int = 31 * x.hashCode() + y.hashCode()

    override fun toString(): String = "WhitePoint($x, $y)"

    public companion object {
        /** CSS's D65, (0.3127, 0.3290). */
        public val D65: WhitePoint = WhitePoint(0.3127, 0.3290)

        /** CSS's D50, (0.3457, 0.3585). */
        public val D50: WhitePoint = WhitePoint(0.3457, 0.3585)
    }
}
