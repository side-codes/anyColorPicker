package codes.side.colorpicker.model

import androidx.compose.runtime.Immutable
import kotlin.math.roundToInt

/**
 * An immutable color in the CMYK (cyan, magenta, yellow, key/black) color space.
 *
 * All channels — [cyan], [magenta], [yellow], [key], and [alpha] — are in `0..1`.
 * The constructor throws [IllegalArgumentException] for out-of-range or NaN values;
 * use [fromInt] for a clamping alternative.
 *
 * The no-argument constructor `CmykColor()` is opaque white (zero ink coverage).
 * Note that each model's default is intentionally its space's most natural origin,
 * so defaults differ per model: `RgbColor()` = black, `HslColor()` = red,
 * `CmykColor()` = white, `LabColor()` = mid-gray.
 */
@Immutable
public class CmykColor(
    cyan: Float = 0f,
    magenta: Float = 0f,
    yellow: Float = 0f,
    key: Float = 0f,
    alpha: Float = 1f,
) : PickerColor {

    init {
        require(cyan in 0f..1f) { "Cyan must be in 0..1, was $cyan" }
        require(magenta in 0f..1f) { "Magenta must be in 0..1, was $magenta" }
        require(yellow in 0f..1f) { "Yellow must be in 0..1, was $yellow" }
        require(key in 0f..1f) { "Key must be in 0..1, was $key" }
        require(alpha in 0f..1f) { "Alpha must be in 0..1, was $alpha" }
    }

    // "+ 0f" normalizes -0.0f to 0.0f so equality can't split on signed zero.
    /** Cyan channel in `0..1`. */
    public val cyan: Float = cyan + 0f
    /** Magenta channel in `0..1`. */
    public val magenta: Float = magenta + 0f
    /** Yellow channel in `0..1`. */
    public val yellow: Float = yellow + 0f
    /** Key (black) channel in `0..1`. */
    public val key: Float = key + 0f
    override val alpha: Float = alpha + 0f

    /** [cyan] scaled to `0..100` percent and rounded to the nearest integer. */
    public val intCyan: Int get() = (cyan * 100f).roundToInt()
    /** [magenta] scaled to `0..100` percent and rounded to the nearest integer. */
    public val intMagenta: Int get() = (magenta * 100f).roundToInt()
    /** [yellow] scaled to `0..100` percent and rounded to the nearest integer. */
    public val intYellow: Int get() = (yellow * 100f).roundToInt()
    /** [key] scaled to `0..100` percent and rounded to the nearest integer. */
    public val intKey: Int get() = (key * 100f).roundToInt()
    /** [alpha] scaled to `0..255` and rounded to the nearest integer. */
    public val intAlpha: Int get() = (alpha * 255f).roundToInt()

    /** Returns a copy of this color, replacing only the channels passed explicitly. */
    public fun copy(
        cyan: Float = this.cyan,
        magenta: Float = this.magenta,
        yellow: Float = this.yellow,
        key: Float = this.key,
        alpha: Float = this.alpha,
    ): CmykColor = CmykColor(cyan = cyan, magenta = magenta, yellow = yellow, key = key, alpha = alpha)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CmykColor) return false
        return cyan == other.cyan &&
            magenta == other.magenta &&
            yellow == other.yellow &&
            key == other.key &&
            alpha == other.alpha
    }

    override fun hashCode(): Int {
        var result = cyan.hashCode()
        result = 31 * result + magenta.hashCode()
        result = 31 * result + yellow.hashCode()
        result = 31 * result + key.hashCode()
        result = 31 * result + alpha.hashCode()
        return result
    }

    override fun toString(): String =
        "CmykColor(cyan=$cyan, magenta=$magenta, yellow=$yellow, key=$key, alpha=$alpha)"

    public companion object {
        /** Opaque black. */
        public val Black: CmykColor = CmykColor(0f, 0f, 0f, 1f)
        /** Opaque white. */
        public val White: CmykColor = CmykColor(0f, 0f, 0f, 0f)

        /**
         * Creates a [CmykColor] from integer channels: [cyan], [magenta], [yellow], and
         * [key] in `0..100` percent, [alpha] in `0..255`. Unlike the constructor,
         * out-of-range values are clamped instead of throwing.
         */
        public fun fromInt(cyan: Int, magenta: Int, yellow: Int, key: Int, alpha: Int = 255): CmykColor = CmykColor(
            cyan = (cyan / 100f).coerceIn(0f, 1f),
            magenta = (magenta / 100f).coerceIn(0f, 1f),
            yellow = (yellow / 100f).coerceIn(0f, 1f),
            key = (key / 100f).coerceIn(0f, 1f),
            alpha = (alpha / 255f).coerceIn(0f, 1f),
        )
    }
}
