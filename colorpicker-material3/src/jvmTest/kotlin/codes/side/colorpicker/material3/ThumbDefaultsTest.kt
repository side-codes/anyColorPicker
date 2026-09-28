package codes.side.colorpicker.material3

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ThumbDefaultsTest {

    // The leftmost pixel, halfway down.
    private fun PixelMap.edge(): Color = this[0, height / 2]

    @Test
    fun aPressedSliderThumbNarrowsWithinItsOwnWidth() = runComposeUiTest {
        val source = MutableInteractionSource()
        setContent { ColorPickerDefaults.SliderThumb(source, Color.Red, Modifier.testTag("thumb")) }
        val resting = onNodeWithTag("thumb").getUnclippedBoundsInRoot()
        assertTrue(onNodeWithTag("thumb").captureToImage().toPixelMap().edge().alpha > 0.9f, "a resting thumb fills its width")
        runOnUiThread { source.tryEmit(PressInteraction.Press(Offset.Zero)) }
        waitForIdle()
        assertEquals(resting, onNodeWithTag("thumb").getUnclippedBoundsInRoot(), "its layout keeps its width")
        assertTrue(onNodeWithTag("thumb").captureToImage().toPixelMap().edge().alpha < 0.1f, "a pressed thumb leaves its edges clear")
    }

    @Test
    fun aSliderThumbIsTheThemesThumbWidthAcross() = runComposeUiTest {
        setContent {
            ColorPickerTheme(dimensions = ColorPickerDefaults.dimensions(thumbWidth = 10.dp)) {
                ColorPickerDefaults.SliderThumb(remember { MutableInteractionSource() }, Color.Red, Modifier.testTag("thumb"))
            }
        }
        val bounds = onNodeWithTag("thumb").getUnclippedBoundsInRoot()
        assertEquals(10f, (bounds.right - bounds.left).value, 0.5f)
    }

    @Test
    fun aSlidersThumbTakesTheSlidersOwnThumbWidth() = runComposeUiTest {
        setContent {
            ColorSlider(
                value = 0.5f,
                onValueChange = {},
                trackColors = listOf(Color.Red, Color.Blue),
                thumbColor = Color.Red,
                dimensions = ColorPickerDefaults.dimensions(thumbWidth = 12.dp),
                thumb = { ColorPickerDefaults.SliderThumb(interactionSource, thumbColor, Modifier.testTag("thumb")) },
            )
        }
        val bounds = onNodeWithTag("thumb", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertEquals(12f, (bounds.right - bounds.left).value, 0.5f, "the gap the track leaves is the width the thumb is drawn at")
    }

    @Test
    fun aPlaneThumbIsItsDiameterAcross() = runComposeUiTest {
        setContent { ColorPickerDefaults.PlaneThumb(remember { MutableInteractionSource() }, Modifier.testTag("thumb"), diameter = 30.dp) }
        val bounds = onNodeWithTag("thumb").getUnclippedBoundsInRoot()
        assertEquals(30f, (bounds.right - bounds.left).value, 0.5f)
    }
}
