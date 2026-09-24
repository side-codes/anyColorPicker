package codes.side.color

import codes.side.color.internal.MatrixStep
import codes.side.color.internal.Step
import codes.side.color.internal.XYZ_D50_TO_D65
import codes.side.color.internal.XYZ_D65_TO_D50
import codes.side.color.internal.runSteps

private fun xyzChannels(): List<ColorChannel> = listOf(
    ColorChannel("x", 0.0..1.0, analogous = AnalogousCategory.Reds),
    ColorChannel("y", 0.0..1.0, analogous = AnalogousCategory.Greens),
    ColorChannel("z", 0.0..1.0, analogous = AnalogousCategory.Blues),
)

/** CIE XYZ relative to D65, Y = 1 at diffuse white; the root every space converts through. CSS `xyz-d65`. */
@OptIn(ExperimentalColorSpaceApi::class)
public object XyzD65 : ColorSpace(XYZ_D65_ID, xyzChannels(), null) {
    public val X: ColorChannel get() = channels[0]
    public val Y: ColorChannel get() = channels[1]
    public val Z: ColorChannel get() = channels[2]

    override fun toBase(src: DoubleArray, dst: DoubleArray) {
        if (src !== dst) src.copyInto(dst, 0, 0, 3)
    }

    override fun fromBase(src: DoubleArray, dst: DoubleArray) {
        if (src !== dst) src.copyInto(dst, 0, 0, 3)
    }

    internal override fun stepsToBase(): List<Step> = emptyList()

    internal override fun stepsFromBase(): List<Step> = emptyList()

    public operator fun invoke(x: Double?, y: Double?, z: Double?, alpha: Double? = 1.0): ColorValue =
        colorOf(arrayOf(x, y, z), alpha)
}

/** CIE XYZ relative to D50, reached from D65 by CSS's Bradford matrix. CSS `xyz-d50`. */
@OptIn(ExperimentalColorSpaceApi::class)
public object XyzD50 : ColorSpace("xyz-d50", xyzChannels(), XyzD65) {
    public val X: ColorChannel get() = channels[0]
    public val Y: ColorChannel get() = channels[1]
    public val Z: ColorChannel get() = channels[2]

    private val toD65 = listOf<Step>(MatrixStep(XYZ_D50_TO_D65))
    private val fromD65 = listOf<Step>(MatrixStep(XYZ_D65_TO_D50))

    override val whitePoint: WhitePoint get() = WhitePoint.D50

    override fun toBase(src: DoubleArray, dst: DoubleArray): Unit = runSteps(toD65, src, dst, 3)

    override fun fromBase(src: DoubleArray, dst: DoubleArray): Unit = runSteps(fromD65, src, dst, 3)

    internal override fun stepsToBase(): List<Step> = toD65

    internal override fun stepsFromBase(): List<Step> = fromD65

    public operator fun invoke(x: Double?, y: Double?, z: Double?, alpha: Double? = 1.0): ColorValue =
        colorOf(arrayOf(x, y, z), alpha)
}
