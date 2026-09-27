package codes.side.colorpicker.material3

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import codes.side.color.Hsl
import codes.side.colorpicker.state.ColorPickerState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SliderFocusTest {

    private fun state() = ColorPickerState(Hsl(200.0, 50.0, 50.0))

    @Test
    fun aSliderShowsKeyboardFocus() = runComposeUiTest {
        // The arrow keys move whichever slider has focus, so a column of sliders that shows none leaves a keyboard
        // user guessing which one they are about to change.
        setContent {
            CompositionLocalProvider(LocalInputModeManager provides KeyboardInputMode) {
                ChannelSlider(state(), Hsl.S, Modifier.testTag("slider").width(300.dp))
            }
        }
        waitForIdle()
        val before = onNodeWithTag("slider").captureToImage().toPixelMap()

        sliderIn("slider").requestFocus()
        waitForIdle()
        val after = onNodeWithTag("slider").captureToImage().toPixelMap()

        val changed = changedPixels(before, after)
        assertTrue(changed > 100, "only $changed pixels changed when the slider took focus")
    }

    @Test
    fun aFingerLeavesNoFocusRingOnASlider() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(LocalInputModeManager provides TouchInputMode) {
                ChannelSlider(state(), Hsl.S, Modifier.testTag("slider").width(300.dp))
            }
        }
        waitForIdle()
        val before = onNodeWithTag("slider").captureToImage().toPixelMap()

        sliderIn("slider").requestFocus()
        waitForIdle()
        val after = onNodeWithTag("slider").captureToImage().toPixelMap()

        assertEquals(0, changedPixels(before, after), "nothing is drawn for a touch user")
    }
}
