package codes.side.colorpicker.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class GradientTrackTest {

    // Red for three quarters of the track, then red to blue. Evenly spaced, the same three colors
    // would reach halfway to blue by 70%.
    private val stops = TrackStops(
        colors = listOf(Color.Red, Color.Red, Color.Blue),
        positions = floatArrayOf(0f, 0.75f, 1f),
    )

    // The track drawn whole: a thumb of no width at the start leaves no gap.
    private fun redAt(direction: LayoutDirection, fraction: Float): Color {
        var pixel = Color.Unspecified
        runComposeUiTest {
            setContent {
                CompositionLocalProvider(LocalLayoutDirection provides direction) {
                    GradientTrack(
                        brush = stops.brush(direction),
                        thumbFraction = 0f,
                        interactionSource = remember { MutableInteractionSource() },
                        checkerboardLight = Color.White,
                        checkerboardDark = Color.LightGray,
                        trackShape = RectangleShape,
                        thumbWidth = 0.dp,
                        thumbTrackGap = 0.dp,
                        modifier = Modifier.size(width = 400.dp, height = 60.dp).testTag("track"),
                    )
                }
            }
            val pixels = onNodeWithTag("track").captureToImage().toPixelMap()
            pixel = pixels[(fraction * (pixels.width - 1)).toInt(), pixels.height / 2]
        }
        return pixel
    }

    @Test
    fun theTrackDrawsItsStopsWhereTheyArePlaced() {
        val at = redAt(LayoutDirection.Ltr, 0.7f)
        assertTrue(at.red > 0.9f && at.blue < 0.1f, "70% along should still be red, was $at")
    }

    @Test
    fun positionedStopsAreMirroredInRightToLeft() {
        val mirrored = redAt(LayoutDirection.Rtl, 0.3f)
        assertTrue(mirrored.red > 0.9f && mirrored.blue < 0.1f, "30% from the left is 70% along in RTL, was $mirrored")
        val end = redAt(LayoutDirection.Rtl, 0.03f)
        assertTrue(end.blue > 0.7f, "the track's end is at the left in RTL, was $end")
    }
}
