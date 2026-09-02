package codes.side.colorpicker.model

import androidx.compose.runtime.Immutable
import kotlin.math.roundToInt

/**
 * The reference range of the [OklabColor.a] and [OklabColor.b] axes, matching the
 * `100%` value CSS Color 4 assigns them in `oklab()`. It comfortably contains the
 * sRGB gamut, which reaches about `0.28` on a and `0.20` on b.
 */
internal const val OKLAB_AB_RANGE: Float = 0.4f

/**
 * An immutable color in the Oklab perceptual color space.
 *
 * [l] (lightness) is in `0..1` — Oklab's own scale, not CIELAB's `0..100`. [a]
 * (green-red axis) and [b] (blue-yellow axis) are in `-0.4..0.4`, the reference range
 * CSS Color 4 uses for `oklab()`. [alpha] is in `0..1`. The constructor throws
 * [IllegalArgumentException] for out-of-range or NaN values; use [fromInt] for a
 * clamping alternative.
 *
 * Oklab is perceptually uniform in a way [LabColor] is not: equal numeric steps
 * correspond to more nearly equal perceived differences, and changing [l] does not
 * drag the perceived hue along with it. That makes it the space to interpolate,
 * compare and gamut-map in. It is not a space to put sliders on — [a] and [b] are
 * unbounded by the display gamut, so most of their range is unreachable. Use
 * [OkhslColor] or [OkhsvColor] for that.
 *
 * The no-argument constructor `OklabColor()` is opaque mid-gray (L = 0.5 on the
 * neutral axis). Note that each model's default is intentionally its space's most
 * natural origin, so defaults differ per model: `RgbColor()` = black,
 * `HslColor()` = red, `CmykColor()` = white, `LabColor()` = mid-gray.
 */
@Immutable
public class OklabColor(
    l: Float = 0.5f,
    a: Float = 0f,
    b: Float = 0f,
    alpha: Float = 1f,
) : PickerColor {

    init {
        require(l in 0f..1f) { "L must be in 0..1, was $l" }
        require(a in -OKLAB_AB_RANGE..OKLAB_AB_RANGE) { "A must be in -0.4..0.4, was $a" }
        require(b in -OKLAB_AB_RANGE..OKLAB_AB_RANGE) { "B must be in -0.4..0.4, was $b" }
        require(alpha in 0f..1f) { "Alpha must be in 0..1, was $alpha" }
    }

    // "+ 0f" normalizes -0.0f to 0.0f so equality can't split on signed zero.
    /** Lightness in `0..1`. */
    public val l: Float = l + 0f

    /** Green-red axis in `-0.4..0.4`; negative is green, positive is red. */
    public val a: Float = a + 0f

    /** Blue-yellow axis in `-0.4..0.4`; negative is blue, positive is yellow. */
    public val b: Float = b + 0f
    override val alpha: Float = alpha + 0f

    /** [l] scaled to `0..100` percent and rounded to the nearest integer. */
    public val intL: Int get() = (l * 100f).roundToInt()

    /** [a] as a percentage of its reference range, in `-100..100`, per CSS `oklab()`. */
    public val intA: Int get() = (a / OKLAB_AB_RANGE * 100f).roundToInt()

    /** [b] as a percentage of its reference range, in `-100..100`, per CSS `oklab()`. */
    public val intB: Int get() = (b / OKLAB_AB_RANGE * 100f).roundToInt()

    /** [alpha] scaled to `0..255` and rounded to the nearest integer. */
    public val intAlpha: Int get() = (alpha * 255f).roundToInt()

    /** Returns a copy of this color, replacing only the channels passed explicitly. */
    public fun copy(
        l: Float = this.l,
        a: Float = this.a,
        b: Float = this.b,
        alpha: Float = this.alpha,
    ): OklabColor = OklabColor(l = l, a = a, b = b, alpha = alpha)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OklabColor) return false
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
        "OklabColor(l=$l, a=$a, b=$b, alpha=$alpha)"

    public companion object {
        /** Opaque black. */
        public val Black: OklabColor = OklabColor(l = 0f, a = 0f, b = 0f)

        /** Opaque white. */
        public val White: OklabColor = OklabColor(l = 1f, a = 0f, b = 0f)

        /**
         * Creates an [OklabColor] from integer channels: [l] in `0..100` percent, [a]
         * and [b] in `-100..100` percent of the `-0.4..0.4` reference range, [alpha] in
         * `0..255`. Unlike the constructor, out-of-range values are clamped instead of
         * throwing.
         */
        public fun fromInt(l: Int, a: Int, b: Int, alpha: Int = 255): OklabColor = OklabColor(
            l = (l / 100f).coerceIn(0f, 1f),
            a = (a / 100f * OKLAB_AB_RANGE).coerceIn(-OKLAB_AB_RANGE, OKLAB_AB_RANGE),
            b = (b / 100f * OKLAB_AB_RANGE).coerceIn(-OKLAB_AB_RANGE, OKLAB_AB_RANGE),
            alpha = (alpha / 255f).coerceIn(0f, 1f),
        )
    }
}
