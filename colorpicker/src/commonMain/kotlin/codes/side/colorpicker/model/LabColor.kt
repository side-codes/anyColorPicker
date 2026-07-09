package codes.side.colorpicker.model

import androidx.compose.runtime.Immutable
import kotlin.math.roundToInt

@Immutable
data class LabColor(
    val l: Float = 50f,
    val a: Float = 0f,
    val b: Float = 0f,
    override val alpha: Float = 1f,
) : PickerColor {

    init {
        require(l in 0f..100f) { "L must be in 0..100, was $l" }
        require(a in -128f..127f) { "A must be in -128..127, was $a" }
        require(b in -128f..127f) { "B must be in -128..127, was $b" }
        require(alpha in 0f..1f) { "Alpha must be in 0..1, was $alpha" }
    }

    val intL: Int get() = l.roundToInt()
    val intA: Int get() = a.roundToInt()
    val intB: Int get() = b.roundToInt()
    val intAlpha: Int get() = (alpha * 255f).roundToInt()

    companion object {
        val Black = LabColor(l = 0f, a = 0f, b = 0f)
        val White = LabColor(l = 100f, a = 0f, b = 0f)

        fun fromInt(l: Int, a: Int, b: Int, alpha: Int = 255) = LabColor(
            l = l.toFloat().coerceIn(0f, 100f),
            a = a.toFloat().coerceIn(-128f, 127f),
            b = b.toFloat().coerceIn(-128f, 127f),
            alpha = (alpha / 255f).coerceIn(0f, 1f),
        )
    }
}
