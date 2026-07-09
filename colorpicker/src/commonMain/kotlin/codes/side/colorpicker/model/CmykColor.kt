package codes.side.colorpicker.model

import androidx.compose.runtime.Immutable
import kotlin.math.roundToInt

@Immutable
data class CmykColor(
    val cyan: Float = 0f,
    val magenta: Float = 0f,
    val yellow: Float = 0f,
    val key: Float = 0f,
    override val alpha: Float = 1f,
) : PickerColor {

    init {
        require(cyan in 0f..1f) { "Cyan must be in 0..1, was $cyan" }
        require(magenta in 0f..1f) { "Magenta must be in 0..1, was $magenta" }
        require(yellow in 0f..1f) { "Yellow must be in 0..1, was $yellow" }
        require(key in 0f..1f) { "Key must be in 0..1, was $key" }
        require(alpha in 0f..1f) { "Alpha must be in 0..1, was $alpha" }
    }

    val intCyan: Int get() = (cyan * 100f).roundToInt()
    val intMagenta: Int get() = (magenta * 100f).roundToInt()
    val intYellow: Int get() = (yellow * 100f).roundToInt()
    val intKey: Int get() = (key * 100f).roundToInt()
    val intAlpha: Int get() = (alpha * 255f).roundToInt()

    companion object {
        val Black = CmykColor(0f, 0f, 0f, 1f)
        val White = CmykColor(0f, 0f, 0f, 0f)

        fun fromInt(cyan: Int, magenta: Int, yellow: Int, key: Int, alpha: Int = 255) = CmykColor(
            cyan = (cyan / 100f).coerceIn(0f, 1f),
            magenta = (magenta / 100f).coerceIn(0f, 1f),
            yellow = (yellow / 100f).coerceIn(0f, 1f),
            key = (key / 100f).coerceIn(0f, 1f),
            alpha = (alpha / 255f).coerceIn(0f, 1f),
        )
    }
}
