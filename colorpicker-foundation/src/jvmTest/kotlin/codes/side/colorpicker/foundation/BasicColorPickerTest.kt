package codes.side.colorpicker.foundation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import codes.side.color.ColorChannel
import codes.side.color.ColorValue
import codes.side.color.Hsl
import codes.side.color.Okhsl
import codes.side.color.Srgb
import codes.side.color.compose.toColorValue
import codes.side.color.compose.toComposeColor
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.assertNear
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class BasicColorPickerTest {

    private val teal = Hsl(200.0, 80.0, 50.0)

    // Each part as a box tagged with what it is: the plane 40 dp tall, each channel and alpha 20 dp.
    private val boxPlane: @Composable (ColorPickerState, ColorChannel, ColorChannel) -> Unit = { _, _, _ ->
        Box(Modifier.fillMaxWidth().height(40.dp).testTag("plane"))
    }
    private val boxSlider: @Composable (ColorPickerState, ColorChannel) -> Unit = { _, channel ->
        Box(Modifier.fillMaxWidth().height(20.dp).testTag(channel.id))
    }
    private val boxAlpha: @Composable (ColorPickerState) -> Unit = {
        Box(Modifier.fillMaxWidth().height(20.dp).testTag("alpha"))
    }

    // Each channel as a BasicChannelSlider tagged with the channel's id.
    private val channelSliders: @Composable (ColorPickerState, ColorChannel) -> Unit = { state, channel ->
        BasicChannelSlider(
            state,
            channel,
            Modifier.fillMaxWidth().testTag(channel.id),
            track = { Box(Modifier.fillMaxWidth().height(8.dp)) },
            thumb = { Box(Modifier.size(20.dp)) },
        )
    }

    private fun ComposeUiTest.bounds(tag: String): DpRect = onNodeWithTag(tag).getUnclippedBoundsInRoot()

    @Test
    fun thePlaneGetsItsAxesAndThePartsStackInOrder() = runComposeUiTest {
        var axes: Pair<ColorChannel, ColorChannel>? = null
        setContent {
            BasicColorPicker(
                ColorPickerState(teal),
                Hsl,
                plane = { _, x, y ->
                    axes = x to y
                    Box(Modifier.fillMaxWidth().height(40.dp).testTag("plane"))
                },
                channelSlider = boxSlider,
                alphaSlider = boxAlpha,
                modifier = Modifier.width(300.dp),
            )
        }
        assertEquals(Hsl.S to Hsl.L, axes)
        val tops = listOf("plane", "h", "s", "l", "alpha").map { bounds(it).top.value }
        assertEquals(tops.sorted(), tops, "the plane, the channels in order, then alpha")
    }

    @Test
    fun aNullPlaneAndANullAlphaSliderAreLeftOut() = runComposeUiTest {
        setContent { BasicColorPicker(ColorPickerState(teal), Hsl, plane = null, channelSlider = boxSlider, alphaSlider = null) }
        onNodeWithTag("plane").assertDoesNotExist()
        onNodeWithTag("alpha").assertDoesNotExist()
        onNodeWithTag("h").assertExists()
    }

    @Test
    fun aSpaceWithNoPlaneNeverDrawsOne() = runComposeUiTest {
        setContent { BasicColorPicker(ColorPickerState(teal), Srgb, plane = boxPlane, channelSlider = boxSlider, alphaSlider = boxAlpha) }
        onNodeWithTag("plane").assertDoesNotExist()
        onNodeWithTag("r").assertExists()
    }

    @Test
    fun spacingSeparatesTheParts() = runComposeUiTest {
        setContent {
            BasicColorPicker(ColorPickerState(teal), Hsl, plane = boxPlane, channelSlider = boxSlider, alphaSlider = boxAlpha, spacing = 10.dp)
        }
        assertEquals(10f, (bounds("h").top - bounds("plane").bottom).value, 0.5f)
    }

    @Test
    fun horizontalPutsThePlaneInTheStartHalf() = runComposeUiTest {
        setContent {
            BasicColorPicker(
                ColorPickerState(teal),
                Hsl,
                plane = boxPlane,
                channelSlider = boxSlider,
                alphaSlider = boxAlpha,
                modifier = Modifier.width(400.dp),
                orientation = Orientation.Horizontal,
            )
        }
        assertTrue(bounds("plane").right.value <= 200.5f, "the plane in the left half: ${bounds("plane")}")
        assertTrue(bounds("h").left.value >= 199.5f, "the sliders in the right half: ${bounds("h")}")
    }

    @Test
    fun rightToLeftPutsThePlaneOnTheRight() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BasicColorPicker(
                    ColorPickerState(teal),
                    Hsl,
                    plane = boxPlane,
                    channelSlider = boxSlider,
                    alphaSlider = boxAlpha,
                    modifier = Modifier.width(400.dp),
                    orientation = Orientation.Horizontal,
                )
            }
        }
        assertTrue(bounds("plane").left.value >= 199.5f, "the plane in the start half, the right: ${bounds("plane")}")
        assertTrue(bounds("h").right.value <= 200.5f, "the sliders on the left: ${bounds("h")}")
    }

    @Test
    fun horizontalWithNoPlaneGivesTheSlidersTheWholeWidth() = runComposeUiTest {
        setContent {
            BasicColorPicker(
                ColorPickerState(teal),
                Srgb,
                plane = boxPlane,
                channelSlider = boxSlider,
                alphaSlider = boxAlpha,
                modifier = Modifier.width(400.dp),
                orientation = Orientation.Horizontal,
            )
        }
        assertEquals(400f, (bounds("r").right - bounds("r").left).value, 0.5f)
    }

    @Test
    fun aDisabledPickerDisablesALibrarySliderAndRefusesTouchesToTheRest() = runComposeUiTest {
        var clicks = 0
        setContent {
            BasicColorPicker(
                ColorPickerState(teal),
                Hsl,
                plane = null,
                channelSlider = { state, channel ->
                    if (channel === Hsl.H) {
                        channelSliders(state, channel)
                    } else {
                        Box(Modifier.fillMaxWidth().height(20.dp).testTag(channel.id).clickable { clicks++ })
                    }
                },
                alphaSlider = null,
                modifier = Modifier.width(300.dp),
                enabled = false,
            )
        }
        assertTrue(SemanticsProperties.Disabled in onNodeWithTag("h").fetchSemanticsNode().config, "a library slider in a slot is disabled with the picker")
        onNodeWithTag("s").performClick()
        assertEquals(0, clicks, "a slot the picker did not make is refused the touch")
    }

    @Test
    fun theValueFormReportsEachEditAndDrawsOnlyTheCallersValue() = runComposeUiTest {
        val reported = mutableListOf<ColorValue>()
        setContent {
            BasicColorPicker(
                value = Srgb(1.0, 0.0, 0.0),
                onValueChange = { reported += it },
                space = Hsl,
                plane = null,
                channelSlider = channelSliders,
                alphaSlider = null,
                modifier = Modifier.width(300.dp),
            )
        }
        onNodeWithTag("h").performSemanticsAction(SemanticsActions.SetProgress) { it(0.5f) }
        assertEquals(1, reported.size)
        assertEquals(Hsl, reported.single().space)
        assertNear(180.0, reported.single()[Hsl.H])
        waitForIdle()
        val progress = onNodeWithTag("h").fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo]
        assertEquals(0f, progress.current, "the caller kept red, so the hue stays at 0")
    }

    @Test
    fun theColorFormReportsAComposeColor() = runComposeUiTest {
        var reported: Color? = null
        setContent {
            BasicColorPicker(
                color = Color.Red,
                onColorChange = { reported = it },
                space = Hsl,
                plane = null,
                channelSlider = channelSliders,
                alphaSlider = null,
                modifier = Modifier.width(300.dp),
            )
        }
        onNodeWithTag("l").performSemanticsAction(SemanticsActions.SetProgress) { it(0.25f) }
        val expected = Hsl(0.0, 100.0, 25.0).toComposeColor()
        val actual = reported!!
        assertTrue(
            abs(actual.red - expected.red) < 1f / 255f && abs(actual.green - expected.green) < 1f / 255f && abs(actual.blue - expected.blue) < 1f / 255f,
            "expected $expected, was $actual",
        )
    }

    @Test
    fun aPartReadsWhetherThePickerIsEnabled() = runComposeUiTest {
        var enabled by mutableStateOf(true)
        var seen: Boolean? = null
        setContent {
            BasicColorPicker(
                ColorPickerState(teal),
                Hsl,
                plane = null,
                channelSlider = { _, _ -> seen = LocalColorPickerEnabled.current },
                alphaSlider = null,
                enabled = enabled,
            )
        }
        assertEquals(true, seen)
        enabled = false
        waitForIdle()
        assertEquals(false, seen, "a disabled picker says so to a part never handed enabled")
    }

    @Test
    fun aColorCallerKeepsTheExactEmittedValue() = runComposeUiTest {
        var color by mutableStateOf(Okhsl(30.0, 0.5, 0.5).toComposeColor())
        lateinit var state: ColorPickerState
        setContent {
            state = rememberControlledPickerState(
                color,
                Okhsl,
                onChange = { color = it },
                toValue = { it.toColorValue() },
                fromValue = { it.toComposeColor() },
            )
        }
        runOnUiThread { state.edit(Okhsl.S, 0.537) }
        waitForIdle()
        assertEquals(Okhsl, state.value.space, "not rebuilt from the 8-bit color")
        assertEquals(0.537, state[Okhsl.S])
    }
}
