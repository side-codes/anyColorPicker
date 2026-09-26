package codes.side.colorpicker.foundation

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import codes.side.colorpicker.ui.LocalPickerEnabled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class BasicColorSliderTest {

    // Focus is asked for before every press: a key the slider leaves alone may move focus on.
    private fun ComposeUiTest.press(key: Key, times: Int = 1) {
        repeat(times) {
            onNodeWithTag("slider").requestFocus()
            onNodeWithTag("slider").performKeyInput { pressKey(key) }
        }
    }

    private fun ComposeUiTest.progress(): ProgressBarRangeInfo =
        onNodeWithTag("slider").fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo]

    // Where the centre of the slot node tagged [tag] sits, in dp from the slider's left edge. The slider
    // merges its slots into its own node, so they are found in the unmerged tree.
    private fun ComposeUiTest.centreOf(tag: String): Float {
        val slider = onNodeWithTag("slider").getUnclippedBoundsInRoot()
        val node = onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        return ((node.left + node.right) / 2 - slider.left).value
    }

    @Test
    fun theTrackIsTheWidthLessTheThumbHalfAThumbIn() = runComposeUiTest {
        showSlider(HeldSlider(), track = { Box(Modifier.fillMaxWidth().height(8.dp).testTag("track")) })
        val slider = onNodeWithTag("slider").getUnclippedBoundsInRoot()
        val track = onNodeWithTag("track", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertEquals(200f, (track.right - track.left).value, 0.5f)
        assertEquals(10f, (track.left - slider.left).value, 0.5f)
        assertEquals(20f, (slider.bottom - slider.top).value, 0.5f, "as tall as the taller slot, the thumb")
    }

    @Test
    fun theThumbsCentreSitsAtTheValueAlongTheTrack() = runComposeUiTest {
        showSlider(HeldSlider(0.25f), thumb = { Box(Modifier.size(20.dp).testTag("thumb")) })
        assertEquals(60f, centreOf("thumb"), 0.5f)
    }

    @Test
    fun rightToLeftRunsTheTrackFromTheRight() = runComposeUiTest {
        showSlider(HeldSlider(0.25f), direction = LayoutDirection.Rtl, thumb = { Box(Modifier.size(20.dp).testTag("thumb")) })
        assertEquals(160f, centreOf("thumb"), 0.5f)
    }

    @Test
    fun aValuePastAnEndIsDrawnAtThatEnd() = runComposeUiTest {
        val held = HeldSlider(1.5f)
        var seen = -1f
        showSlider(
            held,
            thumb = {
                seen = fraction
                Box(Modifier.size(20.dp))
            },
        )
        assertEquals(1f, seen)
        assertEquals(1f, progress().current)
        held.value = Float.NaN
        waitForIdle()
        assertEquals(0f, seen, "NaN is drawn at the start")
    }

    @Test
    fun theSlotsLearnWhenTheSliderIsDisabled() = runComposeUiTest {
        val held = HeldSlider()
        var seen: Boolean? = null
        showSlider(
            held,
            thumb = {
                seen = enabled
                Box(Modifier.size(20.dp))
            },
        )
        assertEquals(true, seen)
        held.enabled = false
        waitForIdle()
        assertEquals(false, seen)
    }

    @Test
    fun aSliderInADisabledPickerIsDisabled() = runComposeUiTest {
        var seen: Boolean? = null
        setContent {
            CompositionLocalProvider(LocalPickerEnabled provides false) {
                BasicColorSlider(0.5f, {}, Modifier.testTag("slider"), track = {}, thumb = { seen = enabled })
            }
        }
        assertEquals(false, seen)
        assertTrue(SemanticsProperties.Disabled in onNodeWithTag("slider").fetchSemanticsNode().config)
    }

    @Test
    fun theThumbColorReachesTheSlotsOpaque() = runComposeUiTest {
        var seen: Color? = null
        showSlider(
            HeldSlider(),
            thumbColor = Color.Red.copy(alpha = 0f),
            thumb = {
                seen = thumbColor
                Box(Modifier.size(20.dp))
            },
        )
        assertEquals(Color.Red, seen)
    }

    @Test
    fun noThumbColorReachesTheSlotsUnspecified() = runComposeUiTest {
        var seen: Color? = null
        showSlider(
            HeldSlider(),
            thumb = {
                seen = thumbColor
                Box(Modifier.size(20.dp))
            },
        )
        assertEquals(Color.Unspecified, seen)
    }

    @Test
    fun theSlotsAreHandedTheCallersSource() = runComposeUiTest {
        val source = MutableInteractionSource()
        var handed: InteractionSource? = null
        showSlider(
            HeldSlider(),
            interactionSource = source,
            thumb = {
                handed = interactionSource
                Box(Modifier.size(20.dp))
            },
        )
        assertSame(source, handed)
    }

    @Test
    fun leftAndRightMoveOneStep() = runComposeUiTest {
        val held = HeldSlider()
        showSlider(held)
        press(Key.DirectionRight)
        assertEquals(0.51f, held.value, 1e-6f)
        press(Key.DirectionLeft, times = 2)
        assertEquals(0.49f, held.value, 1e-6f)
        assertEquals(3, held.finished, "each changing key reports its end")
    }

    @Test
    fun rightToLeftSwapsLeftAndRight() = runComposeUiTest {
        val held = HeldSlider()
        showSlider(held, direction = LayoutDirection.Rtl)
        press(Key.DirectionRight)
        assertEquals(0.49f, held.value, 1e-6f)
        press(Key.DirectionLeft)
        assertEquals(0.5f, held.value, 1e-6f)
    }

    @Test
    fun pageUpAddsAPageAndPageDownTakesOne() = runComposeUiTest {
        val held = HeldSlider()
        showSlider(held)
        press(Key.PageUp)
        assertEquals(0.6f, held.value, 1e-6f)
        press(Key.PageDown, times = 2)
        assertEquals(0.4f, held.value, 1e-6f)
    }

    @Test
    fun homeAndEndJumpToTheEnds() = runComposeUiTest {
        val held = HeldSlider()
        showSlider(held)
        press(Key.MoveEnd)
        assertEquals(1f, held.value)
        press(Key.MoveHome)
        assertEquals(0f, held.value)
        assertEquals(2, held.finished)
    }

    @Test
    fun aLargerStepMovesFurther() = runComposeUiTest {
        val held = HeldSlider()
        showSlider(held, step = 0.25f)
        press(Key.DirectionRight)
        assertEquals(0.75f, held.value, 1e-6f)
    }

    @Test
    fun aKeyThatChangesNothingReportsNothing() = runComposeUiTest {
        val held = HeldSlider(1f)
        showSlider(held)
        press(Key.DirectionRight)
        press(Key.PageUp)
        press(Key.MoveEnd)
        assertEquals(emptyList(), held.reported)
        assertEquals(0, held.finished)
    }

    @Test
    fun twoPressesBeforeARecompositionMoveTwoSteps() = runComposeUiTest {
        val held = HeldSlider()
        showSlider(held)
        onNodeWithTag("slider").requestFocus()
        // One batch: nothing recomposes between the two presses.
        onNodeWithTag("slider").performKeyInput {
            pressKey(Key.DirectionRight)
            pressKey(Key.DirectionRight)
        }
        assertEquals(2, held.reported.size)
        assertEquals(0.51f, held.reported[0], 1e-6f)
        assertEquals(0.52f, held.reported[1], 1e-6f)
    }

    @Test
    fun upAndDownAreLeftForFocusToMoveOn() = runComposeUiTest {
        val held = HeldSlider()
        val parentSaw = mutableListOf<Key>()
        setContent {
            Box(
                Modifier.onKeyEvent {
                    if (it.type == KeyEventType.KeyDown) parentSaw += it.key
                    false
                },
            ) {
                BasicColorSlider(
                    value = held.value,
                    onValueChange = { held.reported += it },
                    modifier = Modifier.width(220.dp).testTag("slider"),
                    track = {},
                    thumb = { Box(Modifier.size(20.dp)) },
                )
            }
        }
        press(Key.DirectionUp)
        press(Key.DirectionDown)
        press(Key.DirectionRight)
        assertEquals(listOf(Key.DirectionUp, Key.DirectionDown), parentSaw, "the slider keeps Right and passes Up and Down on")
        assertEquals(1, held.reported.size)
    }

    @Test
    fun aScreenReaderMovesItAndItReportsTheEnd() = runComposeUiTest {
        val held = HeldSlider()
        showSlider(held)
        onNodeWithTag("slider").performSemanticsAction(SemanticsActions.SetProgress) { assertTrue(it(0.3f)) }
        assertEquals(0.3f, held.value)
        assertEquals(1, held.finished)
    }

    @Test
    fun aScreenReaderStepToWhereItIsIsRefused() = runComposeUiTest {
        val held = HeldSlider()
        showSlider(held)
        onNodeWithTag("slider").performSemanticsAction(SemanticsActions.SetProgress) { assertFalse(it(0.5f)) }
        assertEquals(emptyList(), held.reported)
        assertEquals(0, held.finished)
    }

    @Test
    fun aScreenReaderStepPastAnEndStopsThere() = runComposeUiTest {
        val held = HeldSlider()
        showSlider(held)
        onNodeWithTag("slider").performSemanticsAction(SemanticsActions.SetProgress) { it(1.7f) }
        assertEquals(1f, held.value)
    }

    @Test
    fun aScreenReaderStepsByTheStep() = runComposeUiTest {
        showSlider(HeldSlider())
        assertEquals(99, progress().steps)
        showSlider(HeldSlider(), step = 0.1f)
        assertEquals(9, progress().steps)
    }

    @Test
    fun itIsNamedAndReadAsAPercentage() = runComposeUiTest {
        showSlider(HeldSlider(0.37f), semanticLabel = "Warmth")
        val config = onNodeWithTag("slider").fetchSemanticsNode().config
        assertEquals("Warmth", config[SemanticsProperties.ContentDescription].single())
        assertEquals("37%", config[SemanticsProperties.StateDescription])
    }

    @Test
    fun aDisabledSliderTakesNoFocusKeysOrScreenReaderSteps() = runComposeUiTest {
        val held = HeldSlider()
        showSlider(held)
        // Focused while enabled, so a key would reach it if disabling left it listening.
        onNodeWithTag("slider").requestFocus()
        held.enabled = false
        waitForIdle()
        val config = onNodeWithTag("slider").fetchSemanticsNode().config
        assertTrue(SemanticsProperties.Disabled in config)
        assertTrue(SemanticsActions.RequestFocus !in config, "nothing to focus")
        assertTrue(SemanticsActions.SetProgress !in config, "nothing for a screen reader to move")
        onNodeWithTag("slider").performKeyInput { pressKey(Key.DirectionRight) }
        assertEquals(emptyList(), held.reported)
    }

    @Test
    fun aStepMustBeAboveZero() {
        assertFailsWith<IllegalArgumentException> { requireSliderSteps(0f, 0.1f) }
        assertFailsWith<IllegalArgumentException> { requireSliderSteps(0.01f, -0.1f) }
        assertFailsWith<IllegalArgumentException> { requireSliderSteps(Float.NaN, 0.1f) }
        requireSliderSteps(0.01f, 0.1f)
    }
}
