package codes.side.colorpicker.model

import androidx.compose.runtime.Immutable
import kotlin.math.roundToInt

/**
 * An immutable color in the sRGB color space.
 *
 * All channels — [red], [green], [blue], and [alpha] — are in `0..1`. The constructor
 * throws [IllegalArgumentException] for out-of-range or NaN values; use [fromInt] for
 * a clamping alternative that takes `0..255` integer channels.
 *
 * The no-argument constructor `RgbColor()` is opaque black. Note that each model's
 * default is intentionally its space's most natural origin, so defaults differ per
 * model: `RgbColor()` = black, `HslColor()` = red, `CmykColor()` = white,
 * `LabColor()` = mid-gray.
 */
@Immutable
public class RgbColor(
    red: Float = 0f,
    green: Float = 0f,
    blue: Float = 0f,
    alpha: Float = 1f,
) : PickerColor {

    init {
        require(red in 0f..1f) { "Red must be in 0..1, was $red" }
        require(green in 0f..1f) { "Green must be in 0..1, was $green" }
        require(blue in 0f..1f) { "Blue must be in 0..1, was $blue" }
        require(alpha in 0f..1f) { "Alpha must be in 0..1, was $alpha" }
    }

    // "+ 0f" normalizes -0.0f to 0.0f so equality can't split on signed zero.
    /** Red channel in `0..1`. */
    public val red: Float = red + 0f

    /** Green channel in `0..1`. */
    public val green: Float = green + 0f

    /** Blue channel in `0..1`. */
    public val blue: Float = blue + 0f
    override val alpha: Float = alpha + 0f

    /** [red] scaled to `0..255` and rounded to the nearest integer. */
    public val intRed: Int get() = (red * 255f).roundToInt()

    /** [green] scaled to `0..255` and rounded to the nearest integer. */
    public val intGreen: Int get() = (green * 255f).roundToInt()

    /** [blue] scaled to `0..255` and rounded to the nearest integer. */
    public val intBlue: Int get() = (blue * 255f).roundToInt()

    /** [alpha] scaled to `0..255` and rounded to the nearest integer. */
    public val intAlpha: Int get() = (alpha * 255f).roundToInt()

    /** Returns a copy of this color, replacing only the channels passed explicitly. */
    public fun copy(
        red: Float = this.red,
        green: Float = this.green,
        blue: Float = this.blue,
        alpha: Float = this.alpha,
    ): RgbColor = RgbColor(red = red, green = green, blue = blue, alpha = alpha)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RgbColor) return false
        return red == other.red &&
                green == other.green &&
                blue == other.blue &&
                alpha == other.alpha
    }

    override fun hashCode(): Int {
        var result = red.hashCode()
        result = 31 * result + green.hashCode()
        result = 31 * result + blue.hashCode()
        result = 31 * result + alpha.hashCode()
        return result
    }

    override fun toString(): String =
        "RgbColor(red=$red, green=$green, blue=$blue, alpha=$alpha)"

    public companion object {
        /** Opaque black. */
        public val Black: RgbColor = RgbColor(0f, 0f, 0f)

        /** Opaque white. */
        public val White: RgbColor = RgbColor(1f, 1f, 1f)

        /** Opaque pure red. */
        public val Red: RgbColor = RgbColor(1f, 0f, 0f)

        /** Opaque pure green. */
        public val Green: RgbColor = RgbColor(0f, 1f, 0f)

        /** Opaque pure blue. */
        public val Blue: RgbColor = RgbColor(0f, 0f, 1f)

        /**
         * Creates an [RgbColor] from `0..255` integer channels. Unlike the constructor,
         * out-of-range values are clamped instead of throwing.
         */
        public fun fromInt(red: Int, green: Int, blue: Int, alpha: Int = 255): RgbColor = RgbColor(
            red = (red / 255f).coerceIn(0f, 1f),
            green = (green / 255f).coerceIn(0f, 1f),
            blue = (blue / 255f).coerceIn(0f, 1f),
            alpha = (alpha / 255f).coerceIn(0f, 1f),
        )
    }
}
