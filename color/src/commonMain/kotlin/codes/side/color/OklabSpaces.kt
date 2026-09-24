package codes.side.color

import codes.side.color.internal.LMS_TO_OKLAB
import codes.side.color.internal.LMS_TO_XYZ_D65
import codes.side.color.internal.MatrixStep
import codes.side.color.internal.OKLAB_TO_LMS
import codes.side.color.internal.Step
import codes.side.color.internal.XYZ_D65_TO_LMS
import codes.side.color.internal.runSteps
import kotlin.math.cbrt

/** Oklab with CSS's 64-bit matrices. CSS `oklab()`. */
@OptIn(ExperimentalColorSpaceApi::class)
public object Oklab : ColorSpace(
    "oklab",
    listOf(
        ColorChannel("l", 0.0..1.0, analogous = AnalogousCategory.Lightness),
        ColorChannel("a", -0.4..0.4, analogous = AnalogousCategory.OpponentA),
        ColorChannel("b", -0.4..0.4, analogous = AnalogousCategory.OpponentB),
    ),
    XyzD65,
) {
    public val L: ColorChannel get() = channels[0]
    public val A: ColorChannel get() = channels[1]
    public val B: ColorChannel get() = channels[2]

    private val cube = Step { v ->
        v[0] = v[0] * v[0] * v[0]
        v[1] = v[1] * v[1] * v[1]
        v[2] = v[2] * v[2] * v[2]
    }
    private val cubeRoot = Step { v ->
        v[0] = cbrt(v[0])
        v[1] = cbrt(v[1])
        v[2] = cbrt(v[2])
    }
    private val toXyz = listOf(MatrixStep(OKLAB_TO_LMS), cube, MatrixStep(LMS_TO_XYZ_D65))
    private val fromXyz = listOf(MatrixStep(XYZ_D65_TO_LMS), cubeRoot, MatrixStep(LMS_TO_OKLAB))

    override fun toBase(src: DoubleArray, dst: DoubleArray): Unit = runSteps(toXyz, src, dst, 3)

    override fun fromBase(src: DoubleArray, dst: DoubleArray): Unit = runSteps(fromXyz, src, dst, 3)

    internal override fun stepsToBase(): List<Step> = toXyz

    internal override fun stepsFromBase(): List<Step> = fromXyz

    public operator fun invoke(l: Double?, a: Double?, b: Double?, alpha: Double? = 1.0): ColorValue =
        colorOf(arrayOf(l, a, b), alpha)
}

/** OkLCh, the polar form of [Oklab]. CSS `oklch()`; its hue is powerless at C ≤ 0.000004. */
public object OkLch : PolarColorSpace("oklch", Oklab, 0.4, 0.000004, HueFamily.Oklab)
