package codes.side.colorpicker.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SliderThumbTest {

    // The leftmost pixel, halfway down.
    private fun PixelMap.edge(): Color = this[0, height / 2]

    @Test
    fun aPressedThumbNarrowsWithinItsOwnWidth() = runComposeUiTest {
        val source = MutableInteractionSource()
        setContent { SliderThumb(source, Color.Red, Modifier.testTag("thumb")) }
        val resting = onNodeWithTag("thumb").getUnclippedBoundsInRoot()
        assertTrue(onNodeWithTag("thumb").captureToImage().toPixelMap().edge().alpha > 0.9f, "a resting thumb fills its width")
        runOnUiThread { source.tryEmit(PressInteraction.Press(Offset.Zero)) }
        waitForIdle()
        assertEquals(resting, onNodeWithTag("thumb").getUnclippedBoundsInRoot(), "its layout keeps its width")
        assertTrue(onNodeWithTag("thumb").captureToImage().toPixelMap().edge().alpha < 0.1f, "a pressed thumb leaves its edges clear")
    }
}
