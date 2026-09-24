package codes.side.color.compose

import androidx.compose.ui.graphics.colorspace.TransferParameters
import codes.side.color.ExperimentalColorSpaceApi
import codes.side.color.TransferFunction
import kotlin.math.abs
import kotlin.math.pow

/**
 * ICC's parametric curve as Compose's [TransferParameters] hold it, `(a·x + b)^g + e` from `d` up and
 * `c·x + f` below, mirrored for negative values. Built from the parameters rather than Compose's own
 * `eotf` and `oetf`, which clamp to the space's range.
 */
@OptIn(ExperimentalColorSpaceApi::class)
internal class ParametricTransfer(parameters: TransferParameters) : TransferFunction() {
    private val g = parameters.gamma
    private val a = parameters.a
    private val b = parameters.b
    private val c = parameters.c
    private val d = parameters.d
    private val e = parameters.e
    private val f = parameters.f

    // Where the linear segment ends, in linear light; Compose's inverse switches there too.
    private val linearEnd = c * d + f

    override fun decode(encoded: Double): Double {
        val x = abs(encoded)
        val y = if (x >= d) (a * x + b).pow(g) + e else c * x + f
        return if (encoded < 0.0) -y else y
    }

    override fun encode(linear: Double): Double {
        val y = abs(linear)
        val x = if (y >= linearEnd) ((y - e).pow(1.0 / g) - b) / a else (y - f) / c
        return if (linear < 0.0) -x else x
    }

    override fun toString(): String = "Parametric(g = $g, a = $a, b = $b, c = $c, d = $d, e = $e, f = $f)"
}
