package codes.side.colorpicker.ui

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import codes.side.color.ColorChannel
import codes.side.color.ColorSpace
import codes.side.color.ColorValue
import codes.side.color.DisplayP3
import codes.side.color.Hsl
import codes.side.color.Lab
import codes.side.color.OkLch
import codes.side.color.Srgb
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.ColoringMode
import codes.side.colorpicker.state.assertNear
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ChannelSliderTest {

    private fun ComposeUiTest.show(
        state: ColorPickerState,
        channel: ColorChannel,
        direction: LayoutDirection = LayoutDirection.Ltr,
        range: ClosedFloatingPointRange<Double> = channel.referenceRange,
        coloringMode: ColoringMode? = null,
        onValueChangeFinished: () -> Unit = {},
    ) {
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                val modifier = Modifier.size(width = 300.dp, height = 60.dp).testTag("slider")
                if (coloringMode == null) {
                    ChannelSlider(state, channel, modifier, range = range, onValueChangeFinished = onValueChangeFinished)
                } else {
                    ChannelSlider(state, channel, modifier, range = range, coloringMode = coloringMode, onValueChangeFinished = onValueChangeFinished)
                }
            }
        }
    }

    private fun ComposeUiTest.press(key: Key, times: Int = 1) {
        sliderIn("slider").requestFocus()
        repeat(times) { sliderIn("slider").performKeyInput { pressKey(key) } }
    }

    private fun ComposeUiTest.progress() =
        sliderIn("slider").fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo]

    @Test
    fun showsTheChannelsNameAndValue() = runComposeUiTest {
        show(ColorPickerState(Hsl(200.0, 80.0, 50.0)), Hsl.H)
        onNodeWithText("Hue").assertExists()
        onNodeWithText("200°").assertExists()
        sliderIn("slider").assertContentDescriptionEquals("Hue")
        assertEquals("200°", sliderIn("slider").fetchSemanticsNode().config[SemanticsProperties.StateDescription])
    }

    @Test
    fun labIsReadOutWithItsStars() = runComposeUiTest {
        show(ColorPickerState(Lab(40.0, 10.0, 10.0)), Lab.L)
        onNodeWithText("L").assertExists()
        sliderIn("slider").assertContentDescriptionEquals("L*")
    }

    @Test
    fun aGreysHueSliderShowsTheHueLastChosen() = runComposeUiTest {
        val state = ColorPickerState(Hsl(200.0, 80.0, 50.0))
        state.value = Srgb(0.5, 0.5, 0.5)
        show(state, Hsl.H)
        onNodeWithText("200°").assertExists()
        assertEquals(200f / 360f, progress().current, 1e-6f)
    }

    @Test
    fun arrowKeysMoveByTheChannelsStep() = runComposeUiTest {
        val state = ColorPickerState(OkLch(0.6, 0.1, 30.0))
        show(state, OkLch.C)
        press(Key.DirectionRight)
        assertNear(0.101, state[OkLch.C], 1e-12)
        press(Key.DirectionDown, times = 2)
        assertNear(0.099, state[OkLch.C], 1e-12)
    }

    @Test
    fun anRgbArrowIsOneByte() = runComposeUiTest {
        val state = ColorPickerState(Srgb(128 / 255.0, 0.0, 0.0))
        show(state, Srgb.R)
        press(Key.DirectionRight)
        assertNear(129 / 255.0, state[Srgb.R], 1e-12)
        onNodeWithText("129").assertExists()
    }

    @Test
    fun pageKeysMoveByTheChannelsPageStep() = runComposeUiTest {
        val state = ColorPickerState(Hsl(200.0, 50.0, 50.0))
        show(state, Hsl.S)
        press(Key.PageUp)
        assertNear(60.0, state[Hsl.S])
        press(Key.PageDown, times = 2)
        assertNear(40.0, state[Hsl.S])
    }

    @Test
    fun keysStopAtTheEndsOfTheRange() = runComposeUiTest {
        var finished = 0
        val state = ColorPickerState(Hsl(200.0, 99.5, 50.0))
        show(state, Hsl.S, onValueChangeFinished = { finished++ })
        press(Key.DirectionRight)
        assertNear(100.0, state[Hsl.S])
        assertEquals(1, finished)
        press(Key.DirectionRight)
        assertNear(100.0, state[Hsl.S])
        assertEquals(1, finished, "a press that changes nothing reports nothing")
    }

    @Test
    fun theRightEndOfAHueStaysAtTheRight() = runComposeUiTest {
        val state = ColorPickerState(Hsl(359.5, 80.0, 50.0))
        show(state, Hsl.H)
        press(Key.DirectionRight)
        assertEquals(LAST_HUE, state[Hsl.H])
        sliderIn("slider").performSemanticsAction(SemanticsActions.SetProgress) { it(0.5f) }
        sliderIn("slider").performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }
        assertEquals(LAST_HUE, state[Hsl.H], "dragged to the end, the hue must not wrap to 0")
        assertEquals(1f, progress().current)
    }

    @Test
    fun eachKeyStepReportsItsEndOnce() = runComposeUiTest {
        var finished = 0
        show(ColorPickerState(Hsl(200.0, 50.0, 50.0)), Hsl.S, onValueChangeFinished = { finished++ })
        press(Key.DirectionRight, times = 2)
        press(Key.PageUp)
        assertEquals(3, finished)
    }

    @Test
    fun rightToLeftSwapsTheArrowKeys() = runComposeUiTest {
        val state = ColorPickerState(Hsl(200.0, 50.0, 50.0))
        show(state, Hsl.S, direction = LayoutDirection.Rtl)
        press(Key.DirectionRight)
        assertNear(49.0, state[Hsl.S])
        press(Key.DirectionUp)
        assertNear(50.0, state[Hsl.S], message = "up still raises")
    }

    @Test
    fun aScreenReaderStepsByTheChannelsStep() = runComposeUiTest {
        show(ColorPickerState(OkLch(0.6, 0.1, 30.0)), OkLch.C)
        // Compose moves a slider by a (steps + 1)th of its range per increment: 0.4 in steps of 0.001.
        assertEquals(399, progress().steps)
    }

    @Test
    fun theStepsAScreenReaderTakesFitTheRange() {
        assertEquals(359, accessibilitySteps(0.0..360.0, 1.0))
        assertEquals(254, accessibilitySteps(0.0..1.0, 1.0 / 255.0))
        assertEquals(49, accessibilitySteps(0.0..50.0, 1.0))
        assertEquals(0, accessibilitySteps(0.0..0.5, 1.0))
    }

    @Test
    fun aValueOutsideTheRangePinsTheThumbAndKeepsItsNumber() = runComposeUiTest {
        val vivid = OkLch(0.7, 0.5, 150.0)
        val state = ColorPickerState(vivid)
        show(state, OkLch.C)
        assertEquals(1f, progress().current)
        onNodeWithText("0.500").assertExists()
        assertSame(vivid, state.value, "showing the value must not write it")
        press(Key.DirectionLeft)
        assertNear(0.4, state[OkLch.C], message = "a key press moves it into range")
    }

    @Test
    fun aNarrowerRangeSpansTheWholeTrack() = runComposeUiTest {
        val state = ColorPickerState(Hsl(200.0, 25.0, 50.0))
        show(state, Hsl.S, range = 0.0..50.0)
        assertEquals(0.5f, progress().current, 1e-6f)
        sliderIn("slider").performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }
        assertNear(50.0, state[Hsl.S])
    }

    @Test
    fun editsLandInTheChannelsSpaceThroughTheEditPath() = runComposeUiTest {
        val red = Srgb(1.0, 0.0, 0.0)
        val state = ColorPickerState(red)
        var reported: ColorValue? = null
        state.onEdit = { reported = it }
        show(state, Hsl.S)
        sliderIn("slider").performSemanticsAction(SemanticsActions.SetProgress) { it(0.5f) }
        assertEquals(Hsl, reported?.space)
        assertNear(50.0, reported?.get(Hsl.S))
        assertSame(red, state.value, "an edit goes to the sink, not the value")
    }

    @Test
    fun anAppChannelIsLabelledByItsIdAndEditsInItsSpace() = runComposeUiTest {
        val p3Hsl = ColorSpace.hsl("--slider-hsl-p3", DisplayP3)
        val state = ColorPickerState(Srgb(1.0, 0.0, 0.0))
        show(state, p3Hsl.S)
        onNodeWithText("s").assertExists()
        sliderIn("slider").performSemanticsAction(SemanticsActions.SetProgress) { it(0.25f) }
        assertEquals(p3Hsl, state.value.space)
        assertNear(25.0, state[p3Hsl.S], 1e-4)
    }

    @Test
    fun aDisabledSliderSaysSoAndIgnoresKeys() = runComposeUiTest {
        val state = ColorPickerState(Hsl(200.0, 50.0, 50.0))
        setContent { ChannelSlider(state, Hsl.S, Modifier.testTag("slider"), enabled = false) }
        val config = sliderIn("slider").fetchSemanticsNode().config
        assertTrue(SemanticsProperties.Disabled in config)
        assertTrue(SemanticsProperties.Focused !in config, "nothing to focus")
        assertNear(50.0, state[Hsl.S])
    }

    @Test
    fun isInteractingWhileTheThumbIsHeld() = runComposeUiTest {
        val state = ColorPickerState(Hsl(200.0, 50.0, 50.0))
        show(state, Hsl.S)
        sliderIn("slider").performTouchInput {
            down(center)
            moveBy(Offset(20f, 0f))
        }
        // Material's slider reports a drag from a coroutine of its own, after the touch slop.
        waitForIdle()
        assertTrue(state.isInteracting)
        sliderIn("slider").performTouchInput { up() }
        waitForIdle()
        assertFalse(state.isInteracting)
    }

    @Test
    fun theTrackIsMirroredInRightToLeft() = runComposeUiTest {
        show(ColorPickerState(Hsl(180.0, 100.0, 50.0)), Hsl.H, direction = LayoutDirection.Rtl, coloringMode = ColoringMode.Contextual)
        val pixels = onNodeWithTag("slider").captureToImage().toPixelMap()
        // Mirrored: yellow, hue 60, sits a sixth of the way in from the right.
        val yellow = pixels[pixels.width - pixels.width / 6 - 1, pixels.height / 2]
        assertTrue(yellow.red > 0.7f && yellow.green > 0.7f && yellow.blue < 0.3f, "expected yellow a sixth from the right, got $yellow")
    }
}
