package codes.side.colorpicker.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
