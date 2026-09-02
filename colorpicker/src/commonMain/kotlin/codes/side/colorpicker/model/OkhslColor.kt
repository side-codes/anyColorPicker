package codes.side.colorpicker.model

import androidx.compose.runtime.Immutable
import kotlin.math.roundToInt

/**
 * An immutable color in the Okhsl color space — Björn Ottosson's perceptual
 * replacement for [HslColor], built on [OklabColor].
 *
 * [hue] is in degrees; the constructor accepts `0..360` but `360` is normalized to `0`,
 * so the stored value is always in `0..360` (exclusive). [saturation], [lightness] and
 * [alpha] are in `0..1`. The constructor throws [IllegalArgumentException] for
 * out-of-range or NaN values; use [fromInt] for a clamping alternative.
 *
 * Two properties distinguish it from [HslColor]. [lightness] tracks perceived
 * lightness, so a blue and a yellow at the same value look equally light, which is not
 * true of HSL. And [saturation] is normalized against the sRGB gamut: `1` means as
 * colorful as this hue and lightness can be on the display, so every coordinate in the
 * cube maps to a color that actually exists. Nothing here needs gamut mapping, and no
 * part of a slider's travel is unreachable.
 *
 * The normalization follows Ottosson's reference implementation, which approximates the
 * gamut below the cusp with a straight line to black. About 4% of sRGB — the most
 * saturated blues and violets — sits marginally outside that line, and converting such a
 * color in and back out again loses up to 4 steps of 255. Reproducing the reference
 * exactly is the deliberate trade: these coordinates mean the same thing here as they do
 * anywhere else that implements the space.
 *
 * The no-argument constructor `OkhslColor()` is opaque red (hue 0, full saturation, mid
 * lightness), matching `HslColor()`. Note that each model's default is intentionally its
 * space's most natural origin, so defaults differ per model: `RgbColor()` = black,
 * `HslColor()` = red, `CmykColor()` = white, `LabColor()` = mid-gray.
 */
@Immutable
public class OkhslColor(
    hue: Float = 0f,
    saturation: Float = 1f,
    lightness: Float = 0.5f,
    alpha: Float = 1f,
) : PickerColor {

    init {
        require(hue in 0f..360f) { "Hue must be in 0..360, was $hue" }
        require(saturation in 0f..1f) { "Saturation must be in 0..1, was $saturation" }
        require(lightness in 0f..1f) { "Lightness must be in 0..1, was $lightness" }
        require(alpha in 0f..1f) { "Alpha must be in 0..1, was $alpha" }
    }

    // 360f is accepted for compatibility but stored as the equivalent 0f;
    // "+ 0f" normalizes -0.0f to 0.0f so equality can't split on signed zero.
    /** Hue in degrees, in `0..360` (exclusive) after normalization. */
    public val hue: Float = if (hue == 360f) 0f else hue + 0f

    /** Saturation in `0..1`, where `1` is the most colorful this hue and lightness can be in sRGB. */
    public val saturation: Float = saturation + 0f

    /** Perceived lightness in `0..1`. */
    public val lightness: Float = lightness + 0f
    override val alpha: Float = alpha + 0f

    /** [hue] in degrees, rounded to the nearest integer. */
    public val intHue: Int get() = hue.roundToInt()

    /** [saturation] scaled to `0..100` percent and rounded to the nearest integer. */
    public val intSaturation: Int get() = (saturation * 100f).roundToInt()

    /** [lightness] scaled to `0..100` percent and rounded to the nearest integer. */
    public val intLightness: Int get() = (lightness * 100f).roundToInt()

    /** [alpha] scaled to `0..255` and rounded to the nearest integer. */
    public val intAlpha: Int get() = (alpha * 255f).roundToInt()

    /** Returns a copy of this color, replacing only the channels passed explicitly. */
    public fun copy(
        hue: Float = this.hue,
        saturation: Float = this.saturation,
        lightness: Float = this.lightness,
        alpha: Float = this.alpha,
    ): OkhslColor = OkhslColor(hue = hue, saturation = saturation, lightness = lightness, alpha = alpha)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OkhslColor) return false
        return hue == other.hue &&
                saturation == other.saturation &&
                lightness == other.lightness &&
                alpha == other.alpha
    }

    override fun hashCode(): Int {
        var result = hue.hashCode()
        result = 31 * result + saturation.hashCode()
        result = 31 * result + lightness.hashCode()
        result = 31 * result + alpha.hashCode()
        return result
    }

    override fun toString(): String =
        "OkhslColor(hue=$hue, saturation=$saturation, lightness=$lightness, alpha=$alpha)"

    public companion object {
        /** Opaque black. */
        public val Black: OkhslColor = OkhslColor(hue = 0f, saturation = 0f, lightness = 0f)

        /** Opaque white. */
        public val White: OkhslColor = OkhslColor(hue = 0f, saturation = 0f, lightness = 1f)

        /**
         * Creates an [OkhslColor] from integer channels: [hue] in degrees, [saturation]
         * and [lightness] in `0..100` percent, [alpha] in `0..255`. Unlike the
         * constructor, out-of-range values are clamped instead of throwing.
         */
        public fun fromInt(
            hue: Int,
            saturation: Int,
            lightness: Int,
            alpha: Int = 255,
        ): OkhslColor = OkhslColor(
            hue = hue.toFloat().coerceIn(0f, 360f),
            saturation = (saturation / 100f).coerceIn(0f, 1f),
            lightness = (lightness / 100f).coerceIn(0f, 1f),
            alpha = (alpha / 255f).coerceIn(0f, 1f),
        )
    }
}
