package codes.side.colorpicker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import codes.side.color.ColorSpace
import codes.side.color.ColorSpaces
import codes.side.color.Hsl
import codes.side.color.Okhsl
import codes.side.color.Srgb
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.theme.ColorPickerColors
import codes.side.colorpicker.theme.ColorPickerDefaults
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ColorPickerTest {

    private fun teal() = ColorPickerState(Hsl(200.0, 80.0, 50.0))

    private fun ComposeUiTest.sliderLabels(): List<String> =
        onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).fetchSemanticsNodes()
            .map { it.config[SemanticsProperties.ContentDescription].single() }

    // A plane is the only component offering accessibility actions of its own.
    private fun ComposeUiTest.planeCount(): Int =
        onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions)).fetchSemanticsNodes().size

    @Test
    fun everyLibrarySpaceShowsItsPlaneItsChannelsAndAlpha() = runComposeUiTest {
        val state = ColorPickerState(Srgb(0.4, 0.6, 0.8))
        var space by mutableStateOf<ColorSpace>(Okhsl)
        setContent { ColorPicker(state, space = space) }
        for (each in ColorSpaces.all) {
            space = each
            waitForIdle()
            assertEquals(each.channels.map { channelSpokenLabel(it) } + ALPHA_LABEL, sliderLabels(), "${each.id}'s sliders")
            assertEquals(if (hasPlane(each)) 1 else 0, planeCount(), "${each.id}'s plane")
        }
    }

    @Test
    fun withoutAlphaOrPlaneOnlyTheChannelsRemain() = runComposeUiTest {
        setContent { ColorPicker(teal(), space = Okhsl, showPlane = false, showAlpha = false) }
        assertEquals(listOf("Hue", "Saturation", "Lightness"), sliderLabels())
        assertEquals(0, planeCount())
    }

    @Test
    fun aReplacedSlotIsDrawnInsteadOfTheDefault() = runComposeUiTest {
        setContent {
            ColorPicker(
                teal(),
                space = Hsl,
                channelSlider = { state, channel -> if (channel === Hsl.H) Text("Farbton") else ChannelSlider(state, channel) },
            )
        }
        onNodeWithText("Farbton").assertExists()
        onNodeWithText("Hue").assertDoesNotExist()
        onNodeWithText("Saturation").assertExists()
    }

    @Test
    fun aReplacedSlotInheritsThePickersColorsWithoutForwardingThem() = runComposeUiTest {
        val custom = ColorPickerColors(
            checkerboardLight = Color.Red,
            checkerboardDark = Color.Green,
            disabledAlpha = 0.5f,
            disabledSaturation = 1f,
        )
        var seen: ColorPickerColors? = null
        setContent {
            // Nothing is passed to this slot, so what it reads came through the theme the picker provides.
            ColorPicker(teal(), space = Hsl, colors = custom, alphaSlider = { seen = ColorPickerDefaults.currentColors() })
        }
        assertEquals(custom, seen)
    }

    @Test
    fun aDisabledPickerRefusesADragOnASlotThatNeverSawEnabled() = runComposeUiTest {
        val state = teal()
        setContent {
            ColorPicker(
                state,
                space = Hsl,
                showPlane = false,
                enabled = false,
                // The trap this guards: replacing a slider to relabel it, and not forwarding enabled.
                channelSlider = { s, channel ->
                    Box(Modifier.testTag(channel.id).width(200.dp)) { ChannelSlider(s, channel, label = null, valueLabel = null) }
                },
            )
        }
        onNodeWithTag("h").performTouchInput { swipeRight() }
        assertEquals(Hsl(200.0, 80.0, 50.0), state.value)
    }

    @Test
    fun aDisabledPickerReportsItselfDisabled() = runComposeUiTest {
        setContent { ColorPicker(teal(), Modifier.testTag("picker"), enabled = false) }
        onNodeWithTag("picker").assertIsNotEnabled()
    }

    @Test
    fun theDefaultSlotsReportTheEndOfAnEdit() = runComposeUiTest {
        var finished = 0
        setContent { ColorPicker(ColorPickerState(Okhsl(30.0, 0.5, 0.5)), onValueChangeFinished = { finished++ }) }
        sliderNamed("Hue").performSemanticsAction(SemanticsActions.SetProgress) { it(0.5f) }
        sliderNamed("Alpha").performSemanticsAction(SemanticsActions.SetProgress) { it(0.5f) }
        assertEquals(2, finished)
        val actions = onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions)).fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]
        runOnUiThread { actions[0].action!!.invoke() }
        assertEquals(3, finished, "and the plane's")
    }
}
