package codes.side.colorpicker.model

import androidx.compose.runtime.Immutable
import kotlin.math.roundToInt

@Immutable
data class HslColor(
    val hue: Float = 0f,
    val saturation: Float = 1f,
    val lightness: Float = 0.5f,
    override val alpha: Float = 1f,
) : PickerColor {

    init {
        require(hue in 0f..360f) { "Hue must be in 0..360, was $hue" }
        require(saturation in 0f..1f) { "Saturation must be in 0..1, was $saturation" }
        require(lightness in 0f..1f) { "Lightness must be in 0..1, was $lightness" }
        require(alpha in 0f..1f) { "Alpha must be in 0..1, was $alpha" }
    }

    val intHue: Int get() = hue.roundToInt()
    val intSaturation: Int get() = (saturation * 100f).roundToInt()
    val intLightness: Int get() = (lightness * 100f).roundToInt()
    val intAlpha: Int get() = (alpha * 255f).roundToInt()

    companion object {
        val Black = HslColor(hue = 0f, saturation = 0f, lightness = 0f)
        val White = HslColor(hue = 0f, saturation = 0f, lightness = 1f)
        val Red = HslColor(hue = 0f, saturation = 1f, lightness = 0.5f)

        fun fromInt(hue: Int, saturation: Int, lightness: Int, alpha: Int = 255) = HslColor(
            hue = hue.toFloat().coerceIn(0f, 360f),
            saturation = (saturation / 100f).coerceIn(0f, 1f),
            lightness = (lightness / 100f).coerceIn(0f, 1f),
            alpha = (alpha / 255f).coerceIn(0f, 1f),
        )
    }
}
