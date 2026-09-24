package codes.side.color

import kotlin.math.max

/**
 * Naive CMYK over [Srgb]: K = 1 − max(R, G, B), with no color profile. A reversible way to put four
 * numbers on screen, not what a press prints, which takes an ICC profile.
 */
@OptIn(ExperimentalColorSpaceApi::class)
public object Cmyk : ColorSpace(
    "cmyk",
    listOf(
        ColorChannel("c", 0.0..1.0, gamutBound = 0.0..1.0),
        ColorChannel("m", 0.0..1.0, gamutBound = 0.0..1.0),
        ColorChannel("y", 0.0..1.0, gamutBound = 0.0..1.0),
        ColorChannel("k", 0.0..1.0, gamutBound = 0.0..1.0),
    ),
    Srgb,
) {
    public val C: ColorChannel get() = channels[0]
    public val M: ColorChannel get() = channels[1]
    public val Y: ColorChannel get() = channels[2]
    public val K: ColorChannel get() = channels[3]

    override val gamut: RgbGamut get() = Srgb.gamut

    override val exactness: Exactness get() = Exactness.NonColorimetric

    override fun toBase(src: DoubleArray, dst: DoubleArray) {
        val white = 1.0 - src[3]
        val red = (1.0 - src[0]) * white
        val green = (1.0 - src[1]) * white
        val blue = (1.0 - src[2]) * white
        dst[0] = red
        dst[1] = green
        dst[2] = blue
    }

    override fun fromBase(src: DoubleArray, dst: DoubleArray) {
        val red = src[0]
        val green = src[1]
        val blue = src[2]
        val key = 1.0 - max(red, max(green, blue))
        if (key >= 1.0) {
            dst[0] = 0.0
            dst[1] = 0.0
            dst[2] = 0.0
        } else {
            dst[0] = (1.0 - red - key) / (1.0 - key)
            dst[1] = (1.0 - green - key) / (1.0 - key)
            dst[2] = (1.0 - blue - key) / (1.0 - key)
        }
        dst[3] = key
    }

    public operator fun invoke(c: Double?, m: Double?, y: Double?, k: Double?, alpha: Double? = 1.0): ColorValue =
        colorOf(arrayOf(c, m, y, k), alpha)
}
