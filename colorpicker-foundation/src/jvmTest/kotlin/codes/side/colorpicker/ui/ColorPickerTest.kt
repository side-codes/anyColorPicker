package codes.side.colorpicker.ui

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import codes.side.color.ColorChannel
import codes.side.color.ColorSpace
import codes.side.color.ColorSpaces
import codes.side.color.Hsl
import codes.side.color.Okhsl
import codes.side.color.Srgb
import codes.side.colorpicker.foundation.BasicColorPicker
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.theme.ColorPickerColors
import codes.side.colorpicker.theme.ColorPickerDefaults
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
        var expected: List<String>? = null
        setContent {
            expected = spokenSliderNames(space)
            ColorPicker(state, space = space)
        }
        for (each in ColorSpaces.all) {
            space = each
            waitForIdle()
            assertEquals(expected, sliderLabels(), "${each.id}'s sliders")
            assertEquals(if (expectsPlane(each)) 1 else 0, planeCount(), "${each.id}'s plane")
        }
    }

    @Test
    fun withoutAlphaOrPlaneOnlyTheChannelsRemain() = runComposeUiTest {
        setContent { ColorPicker(teal(), space = Okhsl, plane = null, alphaSlider = null) }
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
                plane = null,
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
    fun aDisabledPickerDisablesReplacedSlotsThatNeverSawEnabled() = runComposeUiTest {
        // Refusing the pointer is half of it: a slot that did not forward enabled would still take the
        // arrow keys and a screen reader's steps.
        setContent {
            ColorPicker(
                teal(),
                space = Hsl,
                enabled = false,
                plane = { s, x, y -> ChannelPlane(s, x, y, Modifier.testTag("plane").size(100.dp)) },
                channelSlider = { s, channel -> ChannelSlider(s, channel, Modifier.testTag(channel.id)) },
                alphaSlider = { s -> AlphaSlider(s, Modifier.testTag("alpha")) },
            )
        }
        for (tag in listOf("h", "alpha")) {
            val config = sliderIn(tag).fetchSemanticsNode().config
            assertTrue(SemanticsProperties.Disabled in config, "$tag should report itself disabled")
            assertTrue(SemanticsProperties.Focused !in config, "$tag should take no focus")
        }
        val plane = onNodeWithTag("plane").fetchSemanticsNode().config
        assertTrue(SemanticsProperties.Focused !in plane, "the plane should take no focus")
        assertTrue(SemanticsActions.CustomActions !in plane, "the plane should offer no actions")
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
        runOnUiThread { actions[0].action() }
        assertEquals(3, finished, "and the plane's")
    }

    @Test
    fun thePlaneSlotIsHandedItsAxes() = runComposeUiTest {
        var axes: Pair<ColorChannel, ColorChannel>? = null
        setContent { ColorPicker(teal(), space = Hsl, plane = { _, x, y -> axes = x to y }) }
        assertEquals(Hsl.S to Hsl.L, axes)
    }

    @Test
    fun aHorizontalPickerPutsThePlaneBesideTheSliders() = runComposeUiTest {
        setContent { ColorPicker(teal(), Modifier.width(600.dp), space = Hsl, orientation = Orientation.Horizontal) }
        val plane = onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions)).getUnclippedBoundsInRoot()
        val hue = sliderNamed("Hue").getUnclippedBoundsInRoot()
        assertTrue(plane.right <= hue.left, "the plane ends before the sliders start: $plane, $hue")
    }

    @Test
    fun aHorizontalPickerWithNoPlaneGivesTheSlidersTheWholeWidth() = runComposeUiTest {
        setContent {
            ColorPicker(ColorPickerState(Srgb(0.2, 0.4, 0.6)), Modifier.width(400.dp), space = Srgb, orientation = Orientation.Horizontal)
        }
        val red = sliderNamed("Red").getUnclippedBoundsInRoot()
        assertEquals(400f, (red.right - red.left).value, 0.5f)
    }

    @Test
    fun theDefaultSlotFactoriesBuildWhatThePickerDraws() = runComposeUiTest {
        setContent {
            ColorPicker(
                teal(),
                space = Hsl,
                plane = ColorPickerDefaults.plane(enabled = true, onValueChangeFinished = {}),
                channelSlider = ColorPickerDefaults.channelSlider(enabled = true, onValueChangeFinished = {}, thumb = {}),
                alphaSlider = ColorPickerDefaults.alphaSlider(enabled = true, onValueChangeFinished = {}, thumb = {}),
            )
        }
        assertEquals(listOf("Hue", "Saturation", "Lightness", "Alpha"), sliderLabels())
        assertEquals(1, planeCount())
    }

    @Test
    fun aSliderInADisabledBasicPickerLooksDisabledWithoutBeingTold() = runComposeUiTest {
        // All drain and no dimming, so a disabled hue track is grey from end to end.
        val drained = ColorPickerColors(Color.White, Color.LightGray, disabledAlpha = 1f, disabledSaturation = 0f)
        setContent {
            BasicColorPicker(
                teal(),
                Hsl,
                plane = null,
                channelSlider = { s, channel ->
                    if (channel === Hsl.H) ChannelSlider(s, channel, Modifier.width(300.dp).testTag("hue"), colors = drained)
                },
                alphaSlider = null,
                enabled = false,
            )
        }
        val pixels = onNodeWithTag("hue").captureToImage().toPixelMap()
        for (x in 0 until pixels.width) {
            for (y in 0 until pixels.height) {
                val c = pixels[x, y]
                assertTrue(maxOf(abs(c.red - c.green), abs(c.green - c.blue), abs(c.red - c.blue)) < 0.02f, "grey at $x, $y, was $c")
            }
        }
    }

    @Test
    fun aReplacedSlotOfAControlledPickerInheritsItsColors() = runComposeUiTest {
        val custom = ColorPickerColors(
            checkerboardLight = Color.Red,
            checkerboardDark = Color.Green,
            disabledAlpha = 0.5f,
            disabledSaturation = 1f,
        )
        var seen: ColorPickerColors? = null
        setContent {
            ColorPicker(Hsl(200.0, 80.0, 50.0), {}, space = Hsl, colors = custom, alphaSlider = { seen = ColorPickerDefaults.currentColors() })
        }
        assertEquals(custom, seen)
    }
}
