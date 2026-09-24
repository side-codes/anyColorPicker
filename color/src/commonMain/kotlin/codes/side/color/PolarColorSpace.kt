package codes.side.color

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * The cylindrical form of a lightness-and-opponent-axes space: lightness, chroma and hue, as LCH is
 * of Lab and OkLCh of Oklab. Built with [ColorSpace.polar].
 */
@OptIn(ExperimentalColorSpaceApi::class)
public open class PolarColorSpace internal constructor(
    id: String,
    of: ColorSpace,
    chromaReference: Double,
    private val powerlessChroma: Double,
    hueFamily: HueFamily,
) : ColorSpace(id, polarChannels(of, chromaReference, hueFamily), of) {

    public val L: ColorChannel get() = channels[0]
    public val C: ColorChannel get() = channels[1]
    public val H: ColorChannel get() = channels[2]

    override fun toBase(src: DoubleArray, dst: DoubleArray) {
        val l = src[0]
        val chroma = src[1]
        val radians = src[2] * PI / 180.0
        dst[0] = l
        dst[1] = chroma * cos(radians)
        dst[2] = chroma * sin(radians)
    }

    override fun fromBase(src: DoubleArray, dst: DoubleArray) {
        val l = src[0]
        val a = src[1]
        val b = src[2]
        var hue = atan2(b, a) * 180.0 / PI
        if (hue < 0.0) hue += 360.0
        dst[0] = l
        dst[1] = hypot(a, b)
        dst[2] = hue
    }

    /** The hue is powerless at chroma ≤ the space's threshold. */
    override fun powerless(components: DoubleArray): Int = if (components[1] <= powerlessChroma) 1 shl 2 else 0

    public operator fun invoke(l: Double?, c: Double?, h: Double?, alpha: Double? = 1.0): ColorValue =
        colorOf(arrayOf(l, c, h), alpha)
}

private fun polarChannels(of: ColorSpace, chromaReference: Double, hueFamily: HueFamily): List<ColorChannel> {
    require(of.channels.size == 3) { "${of.id} is not a lightness-and-opponent-axes space" }
    val lightness = of.channels[0]
    return listOf(
        ColorChannel(
            id = lightness.id,
            referenceRange = lightness.referenceRange,
            gamutBound = lightness.gamutBound,
            limit = lightness.limit,
            analogous = lightness.analogous,
            precision = lightness.precision,
        ),
        ColorChannel(
            id = "c",
            referenceRange = 0.0..chromaReference,
            limit = 0.0..Double.POSITIVE_INFINITY,
            analogous = AnalogousCategory.Colorfulness,
        ),
        ColorChannel(
            id = "h",
            referenceRange = 0.0..360.0,
            kind = ChannelKind.Hue(hueFamily),
            analogous = AnalogousCategory.Hue,
        ),
    )
}
