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
import codes.side.color.ColorValue
import codes.side.color.Okhsl
import codes.side.color.Srgb
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.assertNear
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame

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
    fun anEditGoesThroughTheEditPath() = runComposeUiTest {
        val red = Srgb(1.0, 0.0, 0.0)
        val state = ColorPickerState(red)
        var reported: ColorValue? = null
        state.onEdit = { reported = it }
        setContent { AlphaSlider(state, Modifier.testTag("slider")) }
        sliderIn("slider").performSemanticsAction(SemanticsActions.SetProgress) { it(0.25f) }
        assertEquals(Srgb(1.0, 0.0, 0.0, 0.25), reported)
        assertSame(red, state.value, "an edit goes to the sink, not the value")
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
    fun keyPressesBuildOnTheLastEmission() = runComposeUiTest {
        val state = ColorPickerState(Srgb(1.0, 0.0, 0.0, 0.5))
        val emitted = mutableListOf<ColorValue>()
        state.onEdit = {
            state.emit(it)
            emitted += it
        }
        setContent { AlphaSlider(state, Modifier.testTag("slider")) }
        sliderIn("slider").requestFocus()
        sliderIn("slider").performKeyInput {
            pressKey(Key.DirectionRight)
            pressKey(Key.DirectionRight)
        }
        assertEquals(2, emitted.size)
        assertNear(0.51, emitted[0].alpha, 1e-12)
        assertNear(0.52, emitted[1].alpha, 1e-12)
    }
}
