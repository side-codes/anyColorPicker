package codes.side.colorpicker.ui

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ColorSliderTest {

    private class Held {
        var value by mutableFloatStateOf(0.5f)
    }

    private fun ComposeUiTest.show(held: Held) {
        setContent {
            ColorSlider(
                value = held.value,
                onValueChange = { held.value = it },
                trackColors = listOf(Color.Black, Color.White),
                thumbColor = Color.Gray,
                modifier = Modifier.testTag("slider"),
            )
        }
    }

    private fun ComposeUiTest.press(key: Key) {
        sliderIn("slider").requestFocus()
        sliderIn("slider").performKeyInput { pressKey(key) }
    }

    @Test
    fun pageUpRaisesTheValueByATenth() = runComposeUiTest {
        val held = Held()
        show(held)
        press(Key.PageUp)
        assertEquals(0.6f, held.value, 1e-6f)
        press(Key.PageDown)
        press(Key.PageDown)
        assertEquals(0.4f, held.value, 1e-6f)
    }

    @Test
    fun upAndDownLeaveTheValueAlone() = runComposeUiTest {
        val held = Held()
        show(held)
        press(Key.DirectionUp)
        assertEquals(0.5f, held.value, message = "up")
        press(Key.DirectionDown)
        assertEquals(0.5f, held.value, message = "down")
    }

    @Test
    fun aScreenReaderStepsByAHundredth() = runComposeUiTest {
        show(Held())
        assertEquals(99, sliderIn("slider").fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].steps)
    }

    @Test
    fun theTrackIsMirroredInRightToLeft() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ColorSlider(
                    value = 0.5f,
                    onValueChange = {},
                    trackColors = listOf(Color.Red, Color.Blue),
                    thumbColor = Color.Gray,
                    modifier = Modifier.width(300.dp).testTag("slider"),
                )
            }
        }
        val pixels = onNodeWithTag("slider").captureToImage().toPixelMap()
        val start = pixels[pixels.width - 20, pixels.height / 2]
        val end = pixels[20, pixels.height / 2]
        assertTrue(start.red > 0.7f && start.blue < 0.3f, "red at the right, where the track starts: $start")
        assertTrue(end.blue > 0.7f && end.red < 0.3f, "blue at the left, where it ends: $end")
    }
}
