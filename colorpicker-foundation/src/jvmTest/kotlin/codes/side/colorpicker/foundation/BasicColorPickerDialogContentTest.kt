package codes.side.colorpicker.foundation

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import codes.side.color.Hsv
import codes.side.color.Okhsl
import codes.side.color.Srgb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class BasicColorPickerDialogContentTest {

    private val state = ColorPickerDialogState(Hsv(190.0, 70.0, 60.0), listOf(Okhsl, Hsv, Srgb))

    // A stand-in picker, 400 dp tall stacked and 200 dp beside its sliders.
    private val picker: @Composable (Orientation) -> Unit = { orientation ->
        val height = if (orientation == Orientation.Vertical) 400.dp else 200.dp
        Box(Modifier.testTag("picker $orientation").fillMaxWidth().height(height))
    }

    @Composable
    private fun Content(width: Dp, height: Dp, picker: @Composable (Orientation) -> Unit = this.picker) {
        Box(Modifier.requiredSize(width, height)) {
            BasicColorPickerDialogContent(
                state = state,
                picker = picker,
                modifier = Modifier.testTag("content"),
                header = { BasicText("header", Modifier.testTag("header").height(40.dp)) },
                spaceSwitcher = { BasicText("switcher", Modifier.testTag("switcher").height(40.dp)) },
                spacing = 10.dp,
            )
        }
    }

    @Test
    fun itStacksWhenThatFits() = runComposeUiTest {
        setContent { Content(width = 320.dp, height = 600.dp) }
        onNodeWithTag("picker ${Orientation.Vertical}").assertExists()
        onNodeWithTag("picker ${Orientation.Horizontal}").assertDoesNotExist()
        onNodeWithTag("content").assertHeightIsEqualTo(500.dp)
        assertEquals(50.dp, onNodeWithTag("switcher").getBoundsInRoot().top)
    }

    @Test
    fun stackedTheSwitcherSpansTheWidth() = runComposeUiTest {
        setContent { Content(width = 320.dp, height = 600.dp) }
        onNodeWithTag("switcher").assertWidthIsEqualTo(320.dp)
    }

    // Slots of a size only a measurement tells, as BoxWithConstraints, a lazy list or a tab row are: none of them
    // answers an intrinsic measurement, and asking throws.
    @Composable
    private fun UnmeasurableContent(width: Dp, height: Dp) {
        Box(Modifier.requiredSize(width, height)) {
            BasicColorPickerDialogContent(
                state = state,
                picker = { orientation ->
                    BoxWithConstraints(Modifier.testTag("picker $orientation").fillMaxWidth()) {
                        Box(Modifier.height(if (orientation == Orientation.Vertical) 400.dp else 200.dp))
                    }
                },
                modifier = Modifier.testTag("content"),
                header = { BoxWithConstraints(Modifier.testTag("header")) { Box(Modifier.fillMaxWidth().height(40.dp)) } },
                spaceSwitcher = { BoxWithConstraints(Modifier.testTag("switcher")) { Box(Modifier.size(120.dp, 40.dp)) } },
                spacing = 10.dp,
            )
        }
    }

    @Test
    fun slotsThatAnswerNoIntrinsicMeasurementAreStacked() = runComposeUiTest {
        setContent { UnmeasurableContent(width = 320.dp, height = 600.dp) }
        onNodeWithTag("picker ${Orientation.Vertical}").assertExists()
        onNodeWithTag("content").assertHeightIsEqualTo(500.dp)
    }

    @Test
    fun slotsThatAnswerNoIntrinsicMeasurementGoBesideTheSliders() = runComposeUiTest {
        setContent { UnmeasurableContent(width = 600.dp, height = 300.dp) }
        onNodeWithTag("picker ${Orientation.Horizontal}").assertExists()
        onNodeWithTag("switcher").assertWidthIsEqualTo(120.dp)
        onNodeWithTag("header").assertWidthIsEqualTo(470.dp)
    }

    @Test
    fun inAColumnThatScrollsItStacksAtItsOwnHeight() = runComposeUiTest {
        // A scrolling column measures its content with no height limit, which the content fits by definition.
        setContent {
            Box(Modifier.requiredSize(320.dp, 300.dp)) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    BasicColorPickerDialogContent(
                        state = state,
                        picker = picker,
                        modifier = Modifier.testTag("content"),
                        header = { BasicText("header", Modifier.height(40.dp)) },
                        spacing = 10.dp,
                    )
                }
            }
        }
        onNodeWithTag("picker ${Orientation.Vertical}").assertExists()
        onNodeWithTag("content").assertHeightIsEqualTo(450.dp)
    }

    @Test
    fun aShortWideSpacePutsThePlaneBesideTheSliders() = runComposeUiTest {
        setContent { Content(width = 600.dp, height = 300.dp) }
        onNodeWithTag("picker ${Orientation.Horizontal}").assertExists()
        assertEquals(onNodeWithTag("header").getBoundsInRoot().top, onNodeWithTag("switcher").getBoundsInRoot().top, "header and switcher share a row")
        onNodeWithTag("content").assertHeightIsEqualTo(250.dp)
    }

    @Test
    fun besideTheSlidersTheSwitcherKeepsItsWidthAndTheHeaderTakesTheRest() = runComposeUiTest {
        setContent {
            Box(Modifier.requiredSize(600.dp, 300.dp)) {
                BasicColorPickerDialogContent(
                    state = state,
                    picker = picker,
                    header = { BasicText("header", Modifier.testTag("header").fillMaxWidth().height(40.dp)) },
                    spaceSwitcher = { BasicText("switcher", Modifier.testTag("switcher").width(120.dp).height(40.dp)) },
                    spacing = 10.dp,
                )
            }
        }
        onNodeWithTag("switcher").assertWidthIsEqualTo(120.dp)
        onNodeWithTag("header").assertWidthIsEqualTo(470.dp)
    }

    @Test
    fun theStackedFormMeasuredToDecideIsNotShown() = runComposeUiTest {
        setContent { Content(width = 600.dp, height = 300.dp) }
        onAllNodesWithTag("picker ${Orientation.Vertical}").assertCountEquals(0)
        onAllNodesWithTag("header").assertCountEquals(1)
    }

    @Test
    fun theStackedFormMeasuredToDecideNeverTakesFocus() = runComposeUiTest {
        var hiddenFocused = false
        setContent {
            Content(width = 600.dp, height = 300.dp) { orientation ->
                val height = if (orientation == Orientation.Vertical) 400.dp else 200.dp
                Box(
                    Modifier
                        .testTag("picker $orientation")
                        .fillMaxWidth()
                        .height(height)
                        .onFocusChanged { if (it.isFocused && orientation == Orientation.Vertical) hiddenFocused = true }
                        .focusable(),
                )
            }
        }
        onNodeWithTag("picker ${Orientation.Horizontal}").requestFocus()
        repeat(3) {
            onNodeWithTag("picker ${Orientation.Horizontal}").performKeyInput { pressKey(Key.Tab) }
        }
        assertFalse(hiddenFocused, "focus reached the stacked form, which is not shown")
    }

    @Test
    fun theStackedFormLetsGoOfFocusWhenItStopsBeingShown() = runComposeUiTest {
        var tall by mutableStateOf(false)
        var stackedFocused = false
        setContent {
            Content(width = 600.dp, height = 300.dp) { orientation ->
                val stacked = orientation == Orientation.Vertical
                Box(
                    Modifier
                        .testTag("picker $orientation")
                        .fillMaxWidth()
                        .height(if (!stacked) 200.dp else if (tall) 400.dp else 100.dp)
                        .onFocusChanged { if (stacked) stackedFocused = it.isFocused }
                        .focusable(),
                )
            }
        }
        onNodeWithTag("picker ${Orientation.Vertical}").requestFocus()
        assertTrue(stackedFocused)
        tall = true
        waitForIdle()
        onNodeWithTag("picker ${Orientation.Horizontal}").assertExists()
        assertFalse(stackedFocused, "the stacked form, no longer shown, still holds focus")
    }

    @Test
    fun focusInTheFormShownIsKept() = runComposeUiTest {
        setContent {
            Content(width = 600.dp, height = 300.dp) { orientation ->
                val height = if (orientation == Orientation.Vertical) 400.dp else 200.dp
                Box(Modifier.testTag("picker $orientation").fillMaxWidth().height(height).focusable())
            }
        }
        onNodeWithTag("picker ${Orientation.Horizontal}").requestFocus()
        waitForIdle()
        onNodeWithTag("picker ${Orientation.Horizontal}").assertIsFocused()
    }

    @Test
    fun itStacksAgainOnceThatFits() = runComposeUiTest {
        var tall by mutableStateOf(true)
        setContent {
            Content(width = 600.dp, height = 300.dp) { orientation ->
                Box(Modifier.testTag("picker $orientation").fillMaxWidth().height(if (tall) 400.dp else 100.dp))
            }
        }
        onNodeWithTag("picker ${Orientation.Horizontal}").assertExists()
        tall = false
        waitForIdle()
        onNodeWithTag("picker ${Orientation.Vertical}").assertExists()
        onAllNodesWithTag("picker ${Orientation.Horizontal}").assertCountEquals(0)
        onAllNodesWithTag("header").assertCountEquals(1)
    }

    @Test
    fun aShortNarrowSpaceScrolls() = runComposeUiTest {
        setContent { Content(width = 400.dp, height = 300.dp) }
        onNodeWithTag("picker ${Orientation.Vertical}").assertExists()
        onNodeWithTag("content").assertHeightIsEqualTo(300.dp)
        onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
            .assert(SemanticsMatcher("scrolls") { it.config[SemanticsProperties.VerticalScrollAxisRange].maxValue() > 0f })
    }

    @Test
    fun itScrollsBesideTheSlidersWhenEvenThatDoesNotFit() = runComposeUiTest {
        setContent { Content(width = 600.dp, height = 150.dp) }
        onNodeWithTag("picker ${Orientation.Horizontal}").assertExists()
        onNodeWithTag("content").assertHeightIsEqualTo(150.dp)
    }

    @Test
    fun itNeverShrinks() = runComposeUiTest {
        var tall by mutableStateOf(true)
        setContent {
            Content(width = 320.dp, height = 600.dp) { orientation ->
                Box(Modifier.testTag("picker $orientation").fillMaxWidth().height(if (tall) 400.dp else 100.dp))
            }
        }
        onNodeWithTag("content").assertHeightIsEqualTo(500.dp)
        tall = false
        waitForIdle()
        onNodeWithTag("content").assertHeightIsEqualTo(500.dp)
    }

    @Test
    fun itGrowsWithTheTallestSpace() = runComposeUiTest {
        var tall by mutableStateOf(false)
        setContent {
            Content(width = 320.dp, height = 600.dp) { orientation ->
                Box(Modifier.testTag("picker $orientation").fillMaxWidth().height(if (tall) 400.dp else 100.dp))
            }
        }
        onNodeWithTag("content").assertHeightIsEqualTo(200.dp)
        tall = true
        waitForIdle()
        onNodeWithTag("content").assertHeightIsEqualTo(500.dp)
    }

    @Test
    fun partsLeftOutTakeNoSpace() = runComposeUiTest {
        setContent {
            Box(Modifier.size(320.dp, 600.dp)) {
                BasicColorPickerDialogContent(state = state, picker = picker, modifier = Modifier.testTag("content"), spacing = 10.dp)
            }
        }
        onNodeWithTag("content").assertHeightIsEqualTo(400.dp)
    }
}
