package codes.side.color

import codes.side.color.internal.cuspLightness
import codes.side.color.internal.maxChroma
import codes.side.color.internal.maxSaturation
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * An RGB gamut to test or map colors against: the `0..1` cube of [space]'s channels. Its transfer
 * curve is taken to fix 0 and 1, as every standard-dynamic-range curve does, so the cube is the same
 * in linear light.
 *
 * Near pure blue the chroma a gamut holds at constant OkLCh lightness and hue splits in two
 * (264.052–264.208° in sRGB, 245.067–245.284° in Rec. 2020; Display P3 has no such hues). [maxChroma]
 * and [cusp] answer for the outer stretch, where a line of constant lightness and hue leaves the
 * gamut for good, so pure blue is its own hue's cusp. The sliver inside that stretch is out of gamut,
 * which [isInGamut] reports.
 */
public class RgbGamut internal constructor(
    /** The RGB space whose `0..1` cube this gamut is. */
    public val space: RgbColorSpace,
    /** Peak luminance relative to diffuse white: `1.0`, standard dynamic range. */
    public val peakLuminance: Double = 1.0,
) {
    internal val lmsToLinear: DoubleArray get() = space.lmsToLinear

    internal val linearToLms: DoubleArray get() = space.linearToLms

    /**
     * The most chroma this gamut holds at OkLCh [lightness] and [hue], in degrees. Zero at lightness 0
     * and 1, where only black and white are left, and beyond them.
     */
    public fun maxChroma(lightness: Double, hue: Double): Double {
        require(lightness.isFinite() && hue.isFinite()) { "Lightness and hue must be finite, were $lightness and $hue" }
        if (lightness <= 0.0 || lightness >= 1.0) return 0.0
        val radians = hue * PI / 180.0
        val a = cos(radians)
        val b = sin(radians)
        val sMax = maxSaturation(lmsToLinear, a, b)
        return maxChroma(lmsToLinear, lightness, a, b, sMax, cuspLightness(lmsToLinear, a, b, sMax))
    }

    /** The most colorful color of [hue], in degrees, that this gamut holds, in [OkLch]. */
    public fun cusp(hue: Double): ColorValue {
        require(hue.isFinite()) { "Hue must be finite, was $hue" }
        val radians = hue * PI / 180.0
        val a = cos(radians)
        val b = sin(radians)
        val sMax = maxSaturation(lmsToLinear, a, b)
        val lightness = cuspLightness(lmsToLinear, a, b, sMax)
        return OkLch(lightness, lightness * sMax, hue)
    }

    /**
     * A prepared bulk mapping from [from] into this gamut's encoded RGB, with [method]. The default is
     * [GamutMapping.ChromaReduction], the method for planes and gradients: being exact, it keeps a
     * ramp's chroma smooth where [GamutMapping.Css]'s search stops anywhere within its epsilon.
     */
    public fun mapper(from: ColorSpace, method: GamutMapping = GamutMapping.ChromaReduction): GamutMapper =
        GamutMapper(from, this, method)

    override fun toString(): String = "RgbGamut(${space.id})"
}
