package codes.side.colorpicker.ui

import androidx.compose.ui.graphics.Color
import codes.side.colorpicker.conversion.toComposeColor
import codes.side.colorpicker.model.LabColor
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The LAB tracks are drawn by interpolating between evenly spaced stops while the
 * Contextual thumb above them is painted with the true color, so the two disagree by
 * whatever the strip cannot follow.
 *
 * These are budgets, not correctness bounds. A LAB track spends over half its travel
 * outside sRGB, where the mapped color runs along the gamut surface and turns a corner at
 * every edge of the RGB cube it crosses; a linear strip cuts those corners at any stop
 * count. The figures below are what [LAB_CHANNEL_STOPS] measures, rounded up — they exist
 * to catch a regression, not to certify the track is accurate.
 */
class LabGradientTest {

    /** What [buildLabAGradient] and friends hold to with a fixed context. */
    private val independentBudget = 12f

    /** The contextual tracks, over the contexts a picker actually reaches. */
    private val contextualBudget = 26f

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

    /** The worst channel error of [stops] against [color], in steps of 255. */
    private fun worstChannelError(stops: List<Color>, color: (Float) -> LabColor): Float {
        var worst = 0f
        for (i in 0..400) {
            val fraction = i / 400f
            val drawn = sampleGradient(stops, fraction)
            val actual = color(fraction).toComposeColor()
            val error = maxOf(
                abs(drawn.red - actual.red),
                abs(drawn.green - actual.green),
                abs(drawn.blue - actual.blue),
            )
            if (error > worst) worst = error
        }
        return worst * 255f
    }

    private fun assertWithin(budget: Float, track: String, context: String, error: Float) {
        assertTrue(
            error <= budget,
            "$track track at $context is off by ${error.roundToInt()}/255, over the ${budget.roundToInt()} budget",
        )
    }

    @Test
    fun independentLightnessTrackMatchesTheTrueColor() {
        val error = worstChannelError(buildLabLightnessGradient(a = 0f, b = 0f)) { fraction ->
            LabColor(l = fraction * 100f, a = 0f, b = 0f)
        }
        assertWithin(independentBudget, "L*", "a=0 b=0", error)
    }

    @Test
    fun independentATrackMatchesTheTrueColor() {
        val error = worstChannelError(buildLabAGradient(l = 50f, b = 0f)) { fraction ->
            LabColor(l = 50f, a = labAxisFromFraction(fraction), b = 0f)
        }
        assertWithin(independentBudget, "a*", "L=50 b=0", error)
    }

    @Test
    fun independentBTrackMatchesTheTrueColor() {
        val error = worstChannelError(buildLabBGradient(l = 50f, a = 0f)) { fraction ->
            LabColor(l = 50f, a = 0f, b = labAxisFromFraction(fraction))
        }
        assertWithin(independentBudget, "b*", "L=50 a=0", error)
    }

    /**
     * L* 0 and 100 are left out: the only in-gamut color on either row is black or white,
     * and the mapping steps onto it across a just-noticeable difference, which no gradient
     * can follow.
     */
    @Test
    fun contextualTracksMatchTheTrueColor() {
        for (l in 20..80 step 10) {
            for (v in -60..60 step 20) {
                assertWithin(
                    contextualBudget,
                    "a*",
                    "L=$l b=$v",
                    worstChannelError(buildLabAGradient(l = l.toFloat(), b = v.toFloat())) { fraction ->
                        LabColor(l = l.toFloat(), a = labAxisFromFraction(fraction), b = v.toFloat())
                    },
                )
                assertWithin(
                    contextualBudget,
                    "b*",
                    "L=$l a=$v",
                    worstChannelError(buildLabBGradient(l = l.toFloat(), a = v.toFloat())) { fraction ->
                        LabColor(l = l.toFloat(), a = v.toFloat(), b = labAxisFromFraction(fraction))
                    },
                )
                assertWithin(
                    contextualBudget,
                    "L*",
                    "a=$v b=$v",
                    worstChannelError(buildLabLightnessGradient(a = v.toFloat(), b = v.toFloat())) { fraction ->
                        LabColor(l = fraction * 100f, a = v.toFloat(), b = v.toFloat())
                    },
                )
            }
        }
    }
}
