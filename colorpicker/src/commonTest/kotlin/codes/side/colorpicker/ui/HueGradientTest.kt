package codes.side.colorpicker.ui

import androidx.compose.ui.graphics.Color
import codes.side.colorpicker.conversion.toComposeColor
import codes.side.colorpicker.model.HslColor
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The contextual hue track is drawn by linearly interpolating between a handful of stops.
 * HSL to RGB is piecewise linear in hue with breakpoints every 60 degrees, so stops that
 * miss those breakpoints make the track disagree with the color it is meant to preview.
 */
class HueGradientTest {

    /** What `Brush.horizontalGradient` draws at [fraction] for evenly spaced [stops]. */
    private fun sampleGradient(stops: List<Color>, fraction: Float): Color {
        val segments = stops.size - 1
        val t = (fraction * segments).coerceIn(0f, segments.toFloat())
        val index = t.toInt().coerceAtMost(segments - 1)
        val local = t - index
        val a = stops[index]
        val b = stops[index + 1]
        return Color(
            red = a.red + (b.red - a.red) * local,
            green = a.green + (b.green - a.green) * local,
            blue = a.blue + (b.blue - a.blue) * local,
        )
    }

    private fun worstChannelError(saturation: Float, lightness: Float): Pair<Int, Float> {
        val stops = buildHueGradient(saturation, lightness)
        var worstHue = 0
        var worst = 0f
        for (hue in 0..360) {
            val drawn = sampleGradient(stops, hue / 360f)
            // 360 normalizes to 0, which is the same color either way.
            val actual = HslColor(hue.toFloat(), saturation, lightness).toComposeColor()
            val error = maxOf(
                abs(drawn.red - actual.red),
                abs(drawn.green - actual.green),
                abs(drawn.blue - actual.blue),
            )
            if (error > worst) {
                worst = error
                worstHue = hue
            }
        }
        return worstHue to worst
    }

    @Test
    fun trackMatchesTheTrueColorAtEverySaturatedHue() {
        val (hue, error) = worstChannelError(saturation = 1f, lightness = 0.5f)
        assertTrue(
            error <= 1f / 255f,
            "worst channel error ${(error * 255).roundToInt()}/255 at hue $hue",
        )
    }

    @Test
    fun trackMatchesTheTrueColorAtReducedSaturation() {
        val (hue, error) = worstChannelError(saturation = 0.6f, lightness = 0.4f)
        assertTrue(
            error <= 1f / 255f,
            "worst channel error ${(error * 255).roundToInt()}/255 at hue $hue",
        )
    }
}
