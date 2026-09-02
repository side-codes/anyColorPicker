package codes.side.colorpicker.model

import androidx.compose.runtime.Immutable
import kotlin.math.roundToInt

/**
 * An immutable color in the CIELAB (L*a*b*) color space, D65 reference white.
 *
 * [l] (lightness) is in `0..100`; [a] (green-red axis) and [b] (blue-yellow axis) are
 * in `-128..127`; [alpha] is in `0..1`. The constructor throws [IllegalArgumentException]
 * for out-of-range or NaN values; use [fromInt] for a clamping alternative.
 *
 * The no-argument constructor `LabColor()` is opaque mid-gray (L = 50 on the neutral
 * axis). Note that each model's default is intentionally its space's most natural
 * origin, so defaults differ per model: `RgbColor()` = black, `HslColor()` = red,
 * `CmykColor()` = white, `LabColor()` = mid-gray.
 */
@Immutable
public class LabColor(
    l: Float = 50f,
    a: Float = 0f,
    b: Float = 0f,
    alpha: Float = 1f,
) : PickerColor {

    init {
        require(l in 0f..100f) { "L must be in 0..100, was $l" }
        require(a in -128f..127f) { "A must be in -128..127, was $a" }
        require(b in -128f..127f) { "B must be in -128..127, was $b" }
        require(alpha in 0f..1f) { "Alpha must be in 0..1, was $alpha" }
    }

    // "+ 0f" normalizes -0.0f to 0.0f so equality can't split on signed zero.
    /** Lightness (L*) in `0..100`. */
    public val l: Float = l + 0f

    /** Green-red axis (a*) in `-128..127`; negative is green, positive is red. */
    public val a: Float = a + 0f

    /** Blue-yellow axis (b*) in `-128..127`; negative is blue, positive is yellow. */
    public val b: Float = b + 0f
    override val alpha: Float = alpha + 0f

    /** [l] rounded to the nearest integer. */
    public val intL: Int get() = l.roundToInt()

    /** [a] rounded to the nearest integer. */
    public val intA: Int get() = a.roundToInt()

    /** [b] rounded to the nearest integer. */
    public val intB: Int get() = b.roundToInt()

    /** [alpha] scaled to `0..255` and rounded to the nearest integer. */
    public val intAlpha: Int get() = (alpha * 255f).roundToInt()

    /** Returns a copy of this color, replacing only the channels passed explicitly. */
    public fun copy(
        l: Float = this.l,
        a: Float = this.a,
        b: Float = this.b,
        alpha: Float = this.alpha,
    ): LabColor = LabColor(l = l, a = a, b = b, alpha = alpha)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LabColor) return false
        return l == other.l &&
                a == other.a &&
                b == other.b &&
                alpha == other.alpha
    }

    override fun hashCode(): Int {
        var result = l.hashCode()
        result = 31 * result + a.hashCode()
        result = 31 * result + b.hashCode()
        result = 31 * result + alpha.hashCode()
        return result
    }

    override fun toString(): String =
        "LabColor(l=$l, a=$a, b=$b, alpha=$alpha)"

    public companion object {
        /** Opaque black. */
        public val Black: LabColor = LabColor(l = 0f, a = 0f, b = 0f)

        /** Opaque white. */
        public val White: LabColor = LabColor(l = 100f, a = 0f, b = 0f)

        /**
         * Creates a [LabColor] from integer channels: [l] in `0..100`, [a] and [b] in
         * `-128..127`, [alpha] in `0..255`. Unlike the constructor, out-of-range values
         * are clamped instead of throwing.
         */
        public fun fromInt(l: Int, a: Int, b: Int, alpha: Int = 255): LabColor = LabColor(
            l = l.toFloat().coerceIn(0f, 100f),
            a = a.toFloat().coerceIn(-128f, 127f),
            b = b.toFloat().coerceIn(-128f, 127f),
            alpha = (alpha / 255f).coerceIn(0f, 1f),
        )
    }
}
