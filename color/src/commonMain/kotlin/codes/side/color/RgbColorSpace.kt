package codes.side.color

import codes.side.color.internal.MatrixStep
import codes.side.color.internal.Step
import codes.side.color.internal.invert
import codes.side.color.internal.multiply
import codes.side.color.internal.runSteps
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign

/** The xy chromaticities of an RGB space's red, green and blue. */
public class RgbPrimaries(
    public val redX: Double,
    public val redY: Double,
    public val greenX: Double,
    public val greenY: Double,
    public val blueX: Double,
    public val blueY: Double,
) {
    init {
        for (value in doubleArrayOf(redX, redY, greenX, greenY, blueX, blueY)) {
            require(value.isFinite()) { "A primary's chromaticity must be finite: $this" }
        }
        for (y in doubleArrayOf(redY, greenY, blueY)) {
            require(y != 0.0) { "A primary's y must not be 0: $this" }
        }
    }

    override fun equals(other: Any?): Boolean = other is RgbPrimaries &&
        redX == other.redX && redY == other.redY &&
        greenX == other.greenX && greenY == other.greenY &&
        blueX == other.blueX && blueY == other.blueY

    override fun hashCode(): Int = listOf(redX, redY, greenX, greenY, blueX, blueY).hashCode()

    override fun toString(): String = "RgbPrimaries(R $redX,$redY G $greenX,$greenY B $blueX,$blueY)"

    public companion object {
        public val Srgb: RgbPrimaries = RgbPrimaries(0.640, 0.330, 0.300, 0.600, 0.150, 0.060)
        public val DisplayP3: RgbPrimaries = RgbPrimaries(0.680, 0.320, 0.265, 0.690, 0.150, 0.060)
    }
}

/**
 * An RGB space's transfer curve. [decode] takes an encoded value to linear light and [encode] goes
 * back. Both mirror for negative input, `f(-x) = -f(x)`, so extended-range values survive.
 */
public abstract class TransferFunction protected constructor() {
    public abstract fun decode(encoded: Double): Double

    public abstract fun encode(linear: Double): Double

    public companion object {
        /** No curve: the values are linear already. */
        public val Linear: TransferFunction = object : TransferFunction() {
            override fun decode(encoded: Double): Double = encoded
            override fun encode(linear: Double): Double = linear
            override fun toString(): String = "Linear"
        }

        /** The sRGB curve with the IEC/CSS constants, also used by Display P3. */
        public val Srgb: TransferFunction = object : TransferFunction() {
            override fun decode(encoded: Double): Double {
                val magnitude = abs(encoded)
                return if (magnitude <= 0.04045) {
                    encoded / 12.92
                } else {
                    sign(encoded) * ((magnitude + 0.055) / 1.055).pow(2.4)
                }
            }

            override fun encode(linear: Double): Double {
                val magnitude = abs(linear)
                return if (magnitude > 0.0031308) {
                    sign(linear) * (1.055 * magnitude.pow(1.0 / 2.4) - 0.055)
                } else {
                    12.92 * linear
                }
            }

            override fun toString(): String = "Srgb"
        }

        /** A pure power curve, `sign(x)·|x|^exponent`. */
        public fun gamma(exponent: Double): TransferFunction {
            require(exponent > 0.0 && exponent.isFinite()) { "Gamma must be positive, was $exponent" }
            return object : TransferFunction() {
                override fun decode(encoded: Double): Double = sign(encoded) * abs(encoded).pow(exponent)
                override fun encode(linear: Double): Double = sign(linear) * abs(linear).pow(1.0 / exponent)
                override fun toString(): String = "Gamma($exponent)"
            }
        }
    }
}

/**
 * An RGB space: primaries, a white and a transfer curve. Components are `0..1` inside the gamut
 * and unbounded outside it.
 *
 * Built with [ColorSpace.rgb]; the library's are [SrgbLinear], [Srgb] and [DisplayP3].
 */
@OptIn(ExperimentalColorSpaceApi::class)
public open class RgbColorSpace internal constructor(
    id: String,
    public val primaries: RgbPrimaries,
    private val white: WhitePoint,
    public val transfer: TransferFunction,
    toXyzD65: DoubleArray,
    fromXyzD65: DoubleArray,
    linearTwin: RgbColorSpace?,
) : ColorSpace(id, rgbChannels(), linearTwin ?: XyzD65) {

    public val R: ColorChannel get() = channels[0]
    public val G: ColorChannel get() = channels[1]
    public val B: ColorChannel get() = channels[2]

    private val decode = Step { v ->
        v[0] = transfer.decode(v[0])
        v[1] = transfer.decode(v[1])
        v[2] = transfer.decode(v[2])
    }
    private val encode = Step { v ->
        v[0] = transfer.encode(v[0])
        v[1] = transfer.encode(v[1])
        v[2] = transfer.encode(v[2])
    }
    private val isLinear = transfer === TransferFunction.Linear

    // With a linear twin as its base, this space is only the curve; otherwise it is the curve
    // and the matrix to XYZ-D65.
    private val toBaseSteps: List<Step> = when {
        linearTwin != null -> listOf(decode)
        isLinear -> listOf(MatrixStep(toXyzD65))
        else -> listOf(decode, MatrixStep(toXyzD65))
    }
    private val fromBaseSteps: List<Step> = when {
        linearTwin != null -> listOf(encode)
        isLinear -> listOf(MatrixStep(fromXyzD65))
        else -> listOf(MatrixStep(fromXyzD65), encode)
    }

    override val whitePoint: WhitePoint get() = white

    private val ownGamut by lazy { RgbGamut(this) }

    override val gamut: RgbGamut get() = ownGamut

    override fun toBase(src: DoubleArray, dst: DoubleArray): Unit = runSteps(toBaseSteps, src, dst, 3)

    override fun fromBase(src: DoubleArray, dst: DoubleArray): Unit = runSteps(fromBaseSteps, src, dst, 3)

    internal override fun stepsToBase(): List<Step> = toBaseSteps

    internal override fun stepsFromBase(): List<Step> = fromBaseSteps

    public operator fun invoke(r: Double?, g: Double?, b: Double?, alpha: Double? = 1.0): ColorValue =
        colorOf(arrayOf(r, g, b), alpha)

    internal companion object {
        fun derive(id: String, primaries: RgbPrimaries, white: WhitePoint, transfer: TransferFunction): RgbColorSpace {
            val toXyz = multiply(bradford(white, WhitePoint.D65), rgbToXyz(primaries, white))
            return RgbColorSpace(id, primaries, white, transfer, toXyz, invert(toXyz), null)
        }

        // The matrix taking linear RGB to XYZ under the space's own white: the primaries' XYZ
        // columns, each scaled so that (1, 1, 1) lands on the white.
        private fun rgbToXyz(primaries: RgbPrimaries, white: WhitePoint): DoubleArray {
            fun column(x: Double, y: Double) = doubleArrayOf(x / y, 1.0, (1.0 - x - y) / y)
            val r = column(primaries.redX, primaries.redY)
            val g = column(primaries.greenX, primaries.greenY)
            val b = column(primaries.blueX, primaries.blueY)
            val p = doubleArrayOf(r[0], g[0], b[0], r[1], g[1], b[1], r[2], g[2], b[2])
            val inverse = invert(p)
            val w = doubleArrayOf(white.xyzX, 1.0, white.xyzZ)
            val s = DoubleArray(3) { row -> inverse[row * 3] * w[0] + inverse[row * 3 + 1] * w[1] + inverse[row * 3 + 2] * w[2] }
            return DoubleArray(9) { i -> p[i] * s[i % 3] }
        }

        private val BRADFORD = doubleArrayOf(
            0.8951, 0.2664, -0.1614,
            -0.7502, 1.7135, 0.0367,
            0.0389, -0.0685, 1.0296,
        )

        private fun bradford(from: WhitePoint, to: WhitePoint): DoubleArray {
            if (from == to) return doubleArrayOf(1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0)
            fun cone(w: WhitePoint): DoubleArray {
                val xyz = doubleArrayOf(w.xyzX, 1.0, w.xyzZ)
                return DoubleArray(3) { row -> BRADFORD[row * 3] * xyz[0] + BRADFORD[row * 3 + 1] * xyz[1] + BRADFORD[row * 3 + 2] * xyz[2] }
            }
            val source = cone(from)
            val destination = cone(to)
            val scale = doubleArrayOf(destination[0] / source[0], 0.0, 0.0, 0.0, destination[1] / source[1], 0.0, 0.0, 0.0, destination[2] / source[2])
            return multiply(invert(BRADFORD), multiply(scale, BRADFORD))
        }
    }
}

private fun rgbChannels(): List<ColorChannel> = listOf(
    ColorChannel("r", 0.0..1.0, gamutBound = 0.0..1.0, analogous = AnalogousCategory.Reds),
    ColorChannel("g", 0.0..1.0, gamutBound = 0.0..1.0, analogous = AnalogousCategory.Greens),
    ColorChannel("b", 0.0..1.0, gamutBound = 0.0..1.0, analogous = AnalogousCategory.Blues),
)
