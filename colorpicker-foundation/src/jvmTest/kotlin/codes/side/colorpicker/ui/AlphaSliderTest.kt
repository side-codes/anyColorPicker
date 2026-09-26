package codes.side.colorpicker.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import codes.side.color.Okhsl
import codes.side.color.Srgb
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.assertNear
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalTestApi::class)
class AlphaSliderTest {

    @Test
    fun readsTheValuesAlpha() = runComposeUiTest {
        setContent { AlphaSlider(ColorPickerState(Srgb(1.0, 0.0, 0.0, 0.5)), Modifier.testTag("slider")) }
        onNodeWithText("Alpha").assertExists()
        onNodeWithText("128").assertExists()
        assertEquals(0.5f, sliderIn("slider").fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current)
    }

    @Test
    fun aMissingAlphaReadsZero() = runComposeUiTest {
        val state = ColorPickerState(Srgb.color(doubleArrayOf(1.0, 0.0, 0.0), alpha = null))
        setContent { AlphaSlider(state, Modifier.testTag("slider")) }
        onNodeWithText("0").assertExists()
        sliderIn("slider").performSemanticsAction(SemanticsActions.SetProgress) { it(0.25f) }
        assertFalse(state.value.isAlphaMissing, "moving the slider gives alpha a value")
        assertNear(0.25, state.value.alpha)
        assertEquals(Srgb, state.value.space)
    }

    @Test
    fun anEditKeepsTheSpace() = runComposeUiTest {
        val state = ColorPickerState(Okhsl(30.0, 0.8, 0.6))
        setContent { AlphaSlider(state, Modifier.testTag("slider")) }
        sliderIn("slider").performSemanticsAction(SemanticsActions.SetProgress) { it(0.25f) }
        assertEquals(Okhsl(30.0, 0.8, 0.6, 0.25), state.value)
    }

    @Test
    fun keyStepsMoveAHundredthAndATenthAndReportTheirEnd() = runComposeUiTest {
        var finished = 0
        val state = ColorPickerState(Srgb(1.0, 0.0, 0.0, 0.5))
        setContent { AlphaSlider(state, Modifier.testTag("slider"), onValueChangeFinished = { finished++ }) }
        sliderIn("slider").requestFocus()
        sliderIn("slider").performKeyInput { pressKey(Key.DirectionRight) }
        assertNear(0.51, state.value.alpha, 1e-12)
        sliderIn("slider").performKeyInput { pressKey(Key.PageDown) }
        assertNear(0.41, state.value.alpha, 1e-12)
        assertEquals(2, finished)
    }

    @Test
    fun homeAndEndMakeItTransparentAndOpaque() = runComposeUiTest {
        val state = ColorPickerState(Srgb(1.0, 0.0, 0.0, 0.5))
        setContent { AlphaSlider(state, Modifier.testTag("slider")) }
        sliderIn("slider").requestFocus()
        sliderIn("slider").performKeyInput { pressKey(Key.MoveHome) }
        assertNear(0.0, state.value.alpha)
        sliderIn("slider").performKeyInput { pressKey(Key.MoveEnd) }
        assertNear(1.0, state.value.alpha)
    }

    @Test
    fun aScreenReaderStepsAlphaByAHundredth() = runComposeUiTest {
        setContent { AlphaSlider(ColorPickerState(Srgb(1.0, 0.0, 0.0, 0.5)), Modifier.testTag("slider")) }
        assertEquals(99, sliderIn("slider").fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].steps)
    }
}
