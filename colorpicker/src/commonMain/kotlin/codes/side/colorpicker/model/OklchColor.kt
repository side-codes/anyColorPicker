package codes.side.colorpicker.model

import androidx.compose.runtime.Immutable
import kotlin.math.roundToInt

/**
 * An immutable color in the OkLCh color space — the cylindrical form of [OklabColor],
 * and the space CSS exposes as `oklch()`.
 *
 * [l] (lightness) is in `0..1`. [chroma] is in `0..0.4`, the reference range CSS Color 4
 * assigns `100%` in `oklch()`. [hue] is in degrees; the constructor accepts `0..360` but
 * `360` is normalized to `0`, so the stored value is always in `0..360` (exclusive).
 * [alpha] is in `0..1`. The constructor throws [IllegalArgumentException] for
 * out-of-range or NaN values; use [fromInt] for a clamping alternative.
 *
 * Chroma is not bounded by the display gamut: how much of the `0..0.4` range is
 * reachable depends on [l] and [hue], and the rest converts to the nearest displayable
 * color. That makes OkLCh a poor space to put a raw chroma slider on — use
 * [OkhslColor] or [OkhsvColor], whose saturation is normalized against the gamut, and
 * keep OkLCh for interchange with CSS and for hue-preserving manipulation.
 *
 * The no-argument constructor `OklchColor()` is opaque mid-gray (L = 0.5, no chroma).
 * Note that each model's default is intentionally its space's most natural origin, so
 * defaults differ per model: `RgbColor()` = black, `HslColor()` = red,
 * `CmykColor()` = white, `LabColor()` = mid-gray.
 */
@Immutable
public class OklchColor(
    l: Float = 0.5f,
    chroma: Float = 0f,
    hue: Float = 0f,
    alpha: Float = 1f,
) : PickerColor {

    init {
        require(l in 0f..1f) { "L must be in 0..1, was $l" }
        require(chroma in 0f..OKLAB_AB_RANGE) { "Chroma must be in 0..0.4, was $chroma" }
        require(hue in 0f..360f) { "Hue must be in 0..360, was $hue" }
        require(alpha in 0f..1f) { "Alpha must be in 0..1, was $alpha" }
    }

    // "+ 0f" normalizes -0.0f to 0.0f so equality can't split on signed zero.
    /** Lightness in `0..1`. */
    public val l: Float = l + 0f

    /** Chroma in `0..0.4`; `0` is neutral gray. */
    public val chroma: Float = chroma + 0f

    // 360f is accepted for compatibility but stored as the equivalent 0f.
    /** Hue in degrees, in `0..360` (exclusive) after normalization. */
    public val hue: Float = if (hue == 360f) 0f else hue + 0f
    override val alpha: Float = alpha + 0f

    /** [l] scaled to `0..100` percent and rounded to the nearest integer. */
    public val intL: Int get() = (l * 100f).roundToInt()

    /** [chroma] as a percentage of its reference range, in `0..100`, per CSS `oklch()`. */
    public val intChroma: Int get() = (chroma / OKLAB_AB_RANGE * 100f).roundToInt()

    /** [hue] in degrees, rounded to the nearest integer. */
    public val intHue: Int get() = hue.roundToInt()

    /** [alpha] scaled to `0..255` and rounded to the nearest integer. */
    public val intAlpha: Int get() = (alpha * 255f).roundToInt()

    /** Returns a copy of this color, replacing only the channels passed explicitly. */
    public fun copy(
        l: Float = this.l,
        chroma: Float = this.chroma,
        hue: Float = this.hue,
        alpha: Float = this.alpha,
    ): OklchColor = OklchColor(l = l, chroma = chroma, hue = hue, alpha = alpha)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OklchColor) return false
        return l == other.l &&
                chroma == other.chroma &&
                hue == other.hue &&
                alpha == other.alpha
    }

    override fun hashCode(): Int {
        var result = l.hashCode()
        result = 31 * result + chroma.hashCode()
        result = 31 * result + hue.hashCode()
        result = 31 * result + alpha.hashCode()
        return result
    }

    override fun toString(): String =
        "OklchColor(l=$l, chroma=$chroma, hue=$hue, alpha=$alpha)"

    public companion object {
        /** Opaque black. */
        public val Black: OklchColor = OklchColor(l = 0f, chroma = 0f, hue = 0f)

        /** Opaque white. */
        public val White: OklchColor = OklchColor(l = 1f, chroma = 0f, hue = 0f)

        /**
         * Creates an [OklchColor] from integer channels: [l] in `0..100` percent,
         * [chroma] in `0..100` percent of the `0..0.4` reference range, [hue] in degrees,
         * [alpha] in `0..255`. Unlike the constructor, out-of-range values are clamped
         * instead of throwing.
         */
        public fun fromInt(l: Int, chroma: Int, hue: Int, alpha: Int = 255): OklchColor = OklchColor(
            l = (l / 100f).coerceIn(0f, 1f),
            chroma = (chroma / 100f * OKLAB_AB_RANGE).coerceIn(0f, OKLAB_AB_RANGE),
            hue = hue.toFloat().coerceIn(0f, 360f),
            alpha = (alpha / 255f).coerceIn(0f, 1f),
        )
    }
}
