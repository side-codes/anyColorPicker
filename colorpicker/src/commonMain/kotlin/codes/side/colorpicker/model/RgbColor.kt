package codes.side.colorpicker.model

import androidx.compose.runtime.Immutable
import kotlin.math.roundToInt

@Immutable
data class RgbColor(
    val red: Float = 0f,
    val green: Float = 0f,
    val blue: Float = 0f,
    override val alpha: Float = 1f,
) : PickerColor {

    init {
        require(red in 0f..1f) { "Red must be in 0..1, was $red" }
        require(green in 0f..1f) { "Green must be in 0..1, was $green" }
        require(blue in 0f..1f) { "Blue must be in 0..1, was $blue" }
        require(alpha in 0f..1f) { "Alpha must be in 0..1, was $alpha" }
    }

    val intRed: Int get() = (red * 255f).roundToInt()
    val intGreen: Int get() = (green * 255f).roundToInt()
    val intBlue: Int get() = (blue * 255f).roundToInt()
    val intAlpha: Int get() = (alpha * 255f).roundToInt()

    companion object {
        val Black = RgbColor(0f, 0f, 0f)
        val White = RgbColor(1f, 1f, 1f)
        val Red = RgbColor(1f, 0f, 0f)
        val Green = RgbColor(0f, 1f, 0f)
        val Blue = RgbColor(0f, 0f, 1f)

        fun fromInt(red: Int, green: Int, blue: Int, alpha: Int = 255) = RgbColor(
            red = (red / 255f).coerceIn(0f, 1f),
            green = (green / 255f).coerceIn(0f, 1f),
            blue = (blue / 255f).coerceIn(0f, 1f),
            alpha = (alpha / 255f).coerceIn(0f, 1f),
        )
    }
}
