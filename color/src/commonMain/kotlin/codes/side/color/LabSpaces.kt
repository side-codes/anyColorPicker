package codes.side.color

import codes.side.color.internal.ColorRules
import codes.side.color.internal.D50_XYZ
import kotlin.math.cbrt

/** CIELab with a D50 white, as CSS `lab()` defines it. */
@OptIn(ExperimentalColorSpaceApi::class)
public object Lab : ColorSpace(
    "lab",
    listOf(
        ColorChannel("l", 0.0..100.0, analogous = AnalogousCategory.Lightness),
        ColorChannel("a", -125.0..125.0, analogous = AnalogousCategory.OpponentA),
        ColorChannel("b", -125.0..125.0, analogous = AnalogousCategory.OpponentB),
    ),
    XyzD50,
) {
    public val L: ColorChannel get() = channels[0]
    public val A: ColorChannel get() = channels[1]
    public val B: ColorChannel get() = channels[2]

    private const val EPSILON = 216.0 / 24389.0
    private const val KAPPA = 24389.0 / 27.0

    override val whitePoint: WhitePoint get() = WhitePoint.D50

    override fun toBase(src: DoubleArray, dst: DoubleArray) {
        val l = src[0]
        val fy = (l + 16.0) / 116.0
        val fx = src[1] / 500.0 + fy
        val fz = fy - src[2] / 200.0
        val fx3 = fx * fx * fx
        val fz3 = fz * fz * fz
        dst[0] = (if (fx3 > EPSILON) fx3 else (116.0 * fx - 16.0) / KAPPA) * D50_XYZ[0]
        dst[1] = (if (l > KAPPA * EPSILON) fy * fy * fy else l / KAPPA) * D50_XYZ[1]
        dst[2] = (if (fz3 > EPSILON) fz3 else (116.0 * fz - 16.0) / KAPPA) * D50_XYZ[2]
    }

    override fun fromBase(src: DoubleArray, dst: DoubleArray) {
        val fx = f(src[0] / D50_XYZ[0])
        val fy = f(src[1] / D50_XYZ[1])
        val fz = f(src[2] / D50_XYZ[2])
        dst[0] = 116.0 * fy - 16.0
        dst[1] = 500.0 * (fx - fy)
        dst[2] = 200.0 * (fy - fz)
    }

    private fun f(t: Double): Double = if (t > EPSILON) cbrt(t) else (KAPPA * t + 16.0) / 116.0

    public operator fun invoke(l: Double?, a: Double?, b: Double?, alpha: Double? = 1.0): ColorValue =
        colorOf(arrayOf(l, a, b), alpha)
}

/** CIE LCH, the polar form of [Lab]. CSS `lch()`; its hue is powerless at C ≤ 0.0015. */
public object Lch : PolarColorSpace("lch", Lab, 150.0, ColorRules.LCH_POWERLESS_CHROMA, HueFamily.CieLab)
