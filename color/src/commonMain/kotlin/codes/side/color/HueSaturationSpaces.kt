package codes.side.color

import codes.side.color.internal.ColorRules
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** HSL over an RGB space, with CSS Color 4's `hslToRgb` and `rgbToHsl`. S and L run 0–100. */
@OptIn(ExperimentalColorSpaceApi::class)
public open class HslColorSpace internal constructor(id: String, over: RgbColorSpace) : ColorSpace(
    id,
    listOf(
        hueChannel(over),
        ColorChannel("s", 0.0..100.0, gamutBound = 0.0..100.0, limit = 0.0..Double.POSITIVE_INFINITY, analogous = AnalogousCategory.Colorfulness),
        ColorChannel("l", 0.0..100.0, gamutBound = 0.0..100.0, analogous = AnalogousCategory.Lightness),
    ),
    over,
) {
    public val H: ColorChannel get() = channels[0]
    public val S: ColorChannel get() = channels[1]
    public val L: ColorChannel get() = channels[2]

    private val rgbGamut = over.gamut

    override val gamut: RgbGamut get() = rgbGamut

    override fun toBase(src: DoubleArray, dst: DoubleArray) {
        hslToRgb(src[0], src[1] / 100.0, src[2] / 100.0, dst)
    }

    override fun fromBase(src: DoubleArray, dst: DoubleArray) {
        val red = src[0]
        val green = src[1]
        val blue = src[2]
        val highest = max(red, max(green, blue))
        val lowest = min(red, min(green, blue))
        var hue = hexconeHue(red, green, blue, highest, lowest)
        var saturation = hslSaturation(highest, lowest)
        // Far out of gamut the formula goes negative; CSS rotates the hue instead (issue 9222).
        if (saturation < 0.0) {
            hue += 180.0
            saturation = -saturation
        }
        if (hue >= 360.0) hue -= 360.0
        dst[0] = hue
        dst[1] = saturation * 100.0
        dst[2] = (highest + lowest) / 2.0 * 100.0
    }

    /** The hue is powerless at S ≤ 0.001. */
    override fun powerless(components: DoubleArray): Int =
        if (components[1] <= ColorRules.HSL_POWERLESS_SATURATION) 1 else 0

    public operator fun invoke(h: Double?, s: Double?, l: Double?, alpha: Double? = 1.0): ColorValue =
        colorOf(arrayOf(h, s, l), alpha)
}

/** HWB over an RGB space, with CSS Color 4's `hwbToRgb` and `rgbToHwb`. W and B run 0–100. */
@OptIn(ExperimentalColorSpaceApi::class)
public open class HwbColorSpace internal constructor(id: String, over: RgbColorSpace) : ColorSpace(
    id,
    listOf(
        hueChannel(over),
        ColorChannel("w", 0.0..100.0, gamutBound = 0.0..100.0),
        ColorChannel("b", 0.0..100.0, gamutBound = 0.0..100.0),
    ),
    over,
) {
    public val H: ColorChannel get() = channels[0]
    public val W: ColorChannel get() = channels[1]
    public val B: ColorChannel get() = channels[2]

    private val rgbGamut = over.gamut

    override val gamut: RgbGamut get() = rgbGamut

    override fun toBase(src: DoubleArray, dst: DoubleArray) {
        val hue = src[0]
        val white = src[1] / 100.0
        val black = src[2] / 100.0
        if (white + black >= 1.0) {
            val grey = white / (white + black)
            dst[0] = grey
            dst[1] = grey
            dst[2] = grey
            return
        }
        hslToRgb(hue, 1.0, 0.5, dst)
        val scale = 1.0 - white - black
        for (i in 0..2) dst[i] = dst[i] * scale + white
    }

    override fun fromBase(src: DoubleArray, dst: DoubleArray) {
        val red = src[0]
        val green = src[1]
        val blue = src[2]
        val highest = max(red, max(green, blue))
        val lowest = min(red, min(green, blue))
        // CSS's rgbToHue, without HSL's half turn for a negative saturation: whiteness and blackness
        // carry no sign to turn back, so a turned hue would be another color.
        var hue = hexconeHue(red, green, blue, highest, lowest)
        if (hue >= 360.0) hue -= 360.0
        dst[0] = hue
        dst[1] = lowest * 100.0
        dst[2] = (1.0 - highest) * 100.0
    }

    /** The hue is powerless at W + B ≥ 99.999. */
    override fun powerless(components: DoubleArray): Int =
        if (components[1] + components[2] >= ColorRules.HWB_POWERLESS_WHITENESS_PLUS_BLACKNESS) 1 else 0

    // HWB has no colorfulness to zero. CSS Color 4 §4.4 moves what is left of the hue into whiteness
    // or blackness instead, by which of them is missing; from W + B = 100 up hwbToRgb's own grey stands.
    override fun makeAchromatic(components: DoubleArray, powerless: Int, missing: Int) {
        components[0] = 0.0
        if (components[1] + components[2] >= 100.0) return
        val whiteMissing = missing and (1 shl 1) != 0
        val blackMissing = missing and (1 shl 2) != 0
        when {
            !whiteMissing && !blackMissing -> components[2] = 100.0 - components[1]
            !whiteMissing -> components[1] = 100.0
            !blackMissing -> components[2] = 100.0
        }
    }

    public operator fun invoke(h: Double?, w: Double?, b: Double?, alpha: Double? = 1.0): ColorValue =
        colorOf(arrayOf(h, w, b), alpha)
}

/**
 * HSV over an RGB space. S and V run 0–100, in step with HSL and HWB. CSS has no `hsv()`, so the
 * powerless rule, |(S/100)·(V/100)| ≤ 1e-5, is the library's: for S·V ≥ 0 it is HWB's
 * W + B ≥ 99.999 restated, since W + B = 100·(1 − S·V). A negative S·V is a color, not a grey.
 *
 * A color whose channels are all negative has a negative S. HSL turns such a hue half a turn
 * instead, which works only because HSL's hexcone is symmetric under that turn; HSV's is not, and a
 * negative S is what round-trips.
 */
@OptIn(ExperimentalColorSpaceApi::class)
public open class HsvColorSpace internal constructor(id: String, over: RgbColorSpace) : ColorSpace(
    id,
    listOf(
        hueChannel(over),
        ColorChannel("s", 0.0..100.0, gamutBound = 0.0..100.0, analogous = AnalogousCategory.Colorfulness),
        ColorChannel("v", 0.0..100.0, gamutBound = 0.0..100.0),
    ),
    over,
) {
    public val H: ColorChannel get() = channels[0]
    public val S: ColorChannel get() = channels[1]
    public val V: ColorChannel get() = channels[2]

    private val rgbGamut = over.gamut

    override val gamut: RgbGamut get() = rgbGamut

    override fun toBase(src: DoubleArray, dst: DoubleArray) {
        val hue = src[0]
        val saturation = src[1] / 100.0
        val value = src[2] / 100.0
        fun f(n: Double): Double {
            val k = (n + hue / 60.0).mod(6.0)
            return value - value * saturation * max(0.0, min(k, min(4.0 - k, 1.0)))
        }
        dst[0] = f(5.0)
        dst[1] = f(3.0)
        dst[2] = f(1.0)
    }

    override fun fromBase(src: DoubleArray, dst: DoubleArray) {
        val red = src[0]
        val green = src[1]
        val blue = src[2]
        val highest = max(red, max(green, blue))
        val lowest = min(red, min(green, blue))
        dst[0] = hexconeHue(red, green, blue, highest, lowest)
        dst[1] = if (highest == 0.0) 0.0 else (highest - lowest) / highest * 100.0
        dst[2] = highest * 100.0
    }

    override fun powerless(components: DoubleArray): Int =
        if (abs(components[1] / 100.0 * (components[2] / 100.0)) <= ColorRules.HSV_POWERLESS_SATURATION_TIMES_VALUE) 1 else 0

    public operator fun invoke(h: Double?, s: Double?, v: Double?, alpha: Double? = 1.0): ColorValue =
        colorOf(arrayOf(h, s, v), alpha)
}

/** HSL over [Srgb]. CSS `hsl()`. */
public object Hsl : HslColorSpace("hsl", Srgb)

/** HWB over [Srgb]. CSS `hwb()`. */
public object Hwb : HwbColorSpace("hwb", Srgb)

/** HSV over [Srgb]. */
public object Hsv : HsvColorSpace("hsv", Srgb)

private fun hueChannel(over: RgbColorSpace): ColorChannel = ColorChannel(
    id = "h",
    referenceRange = 0.0..360.0,
    kind = ChannelKind.Hue(HueFamily.rgbHexcone(over)),
    analogous = AnalogousCategory.Hue,
)

// CSS Color 4 hslToRgb, with saturation and lightness as fractions.
private fun hslToRgb(hue: Double, saturation: Double, lightness: Double, dst: DoubleArray) {
    fun f(n: Double): Double {
        val k = (n + hue / 30.0).mod(12.0)
        val a = saturation * min(lightness, 1.0 - lightness)
        return lightness - a * max(-1.0, min(k - 3.0, min(9.0 - k, 1.0)))
    }
    dst[0] = f(0.0)
    dst[1] = f(8.0)
    dst[2] = f(4.0)
}

// CSS rgbToHsl's saturation as a fraction: 0 for a grey and at lightness 0 or 1, and negative far out
// of gamut, where HSL turns the hue half a turn instead.
private fun hslSaturation(highest: Double, lowest: Double): Double {
    val lightness = (highest + lowest) / 2.0
    if (highest == lowest || lightness == 0.0 || lightness == 1.0) return 0.0
    return (highest - lightness) / min(lightness, 1.0 - lightness)
}

// The hexcone hue CSS's rgbToHsl computes; 0 where there is none, which the powerless rule catches.
private fun hexconeHue(red: Double, green: Double, blue: Double, highest: Double, lowest: Double): Double {
    val delta = highest - lowest
    if (delta == 0.0) return 0.0
    val sixths = when (highest) {
        red -> (green - blue) / delta + if (green < blue) 6.0 else 0.0
        green -> (blue - red) / delta + 2.0
        else -> (red - green) / delta + 4.0
    }
    return sixths * 60.0
}
