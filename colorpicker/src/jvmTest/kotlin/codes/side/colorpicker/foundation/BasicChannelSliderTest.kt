package codes.side.colorpicker.foundation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import codes.side.color.ColorChannel
import codes.side.color.Hsl
import codes.side.color.OkLch
import codes.side.color.Srgb
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.ColoringMode
import codes.side.colorpicker.state.assertNear
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class BasicChannelSliderTest {

    // [channel]'s slider over [state], tagged "slider", 220 dp wide, drawn with foundation alone.
    private fun ComposeUiTest.showChannel(
        state: ColorPickerState,
        channel: ColorChannel,
        direction: LayoutDirection = LayoutDirection.Ltr,
        coloringMode: ColoringMode = ColoringMode.defaultFor(channel.space),
        track: @Composable ChannelSliderScope.() -> Unit = { Box(Modifier.fillMaxWidth().height(8.dp)) },
        thumb: @Composable ChannelSliderScope.() -> Unit = { Box(Modifier.size(20.dp)) },
    ) {
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                BasicChannelSlider(
                    state = state,
                    channel = channel,
                    modifier = Modifier.width(220.dp).testTag("slider"),
                    coloringMode = coloringMode,
                    track = track,
                    thumb = thumb,
                )
            }
        }
    }

    // A track that draws the gradient it is handed, tagged "track".
    private val drawnTrack: @Composable ChannelSliderScope.() -> Unit = {
        Canvas(Modifier.fillMaxWidth().height(8.dp).testTag("track")) { drawRect(gradient) }
    }

    // The slider merges its slots into its own node, so the track is found in the unmerged tree.
    private fun ComposeUiTest.trackPixels(): PixelMap =
        onNodeWithTag("track", useUnmergedTree = true).captureToImage().toPixelMap()

    @Test
    fun theSlotsSeeTheChannelAndTheValueItShows() = runComposeUiTest {
        val state = ColorPickerState(Hsl(200.0, 80.0, 50.0))
        state.value = Srgb(0.5, 0.5, 0.5)
        var seen: Pair<ColorChannel, Double>? = null
        showChannel(
            state,
            Hsl.H,
            thumb = {
                seen = channel to value
                Box(Modifier.size(20.dp))
            },
        )
        assertEquals(Hsl.H to 200.0, seen, "a grey's hue slider shows the hue last chosen")
    }

    @Test
    fun aValuePastTheTrackPinsTheThumbAndKeepsItsNumber() = runComposeUiTest {
        var seenFraction = -1f
        var seenValue = -1.0
        showChannel(
            ColorPickerState(OkLch(0.7, 0.5, 150.0)),
            OkLch.C,
            thumb = {
                seenFraction = fraction
                seenValue = value
                Box(Modifier.size(20.dp))
            },
        )
        assertEquals(1f, seenFraction)
        assertEquals(0.5, seenValue)
    }

    @Test
    fun theThumbColorIsTheOpaqueColorUnderIt() = runComposeUiTest {
        var seen: Color? = null
        showChannel(
            ColorPickerState(Hsl(0.0, 100.0, 50.0, 0.3)),
            Hsl.H,
            coloringMode = ColoringMode.Contextual,
            thumb = {
                seen = thumbColor
                Box(Modifier.size(20.dp))
            },
        )
        assertEquals(Color(0xFFFF0000), seen)
    }

    @Test
    fun theGradientRunsFromTheTracksStart() = runComposeUiTest {
        showChannel(ColorPickerState(Hsl(200.0, 80.0, 50.0)), Hsl.L, coloringMode = ColoringMode.Contextual, track = drawnTrack, thumb = {})
        val pixels = trackPixels()
        assertTrue(pixels.dark(1), "lightness 0 at the left: ${pixels.mid(1)}")
        assertTrue(pixels.light(pixels.width - 2), "lightness 100 at the right: ${pixels.mid(pixels.width - 2)}")
    }

    @Test
    fun rightToLeftTheGradientRunsFromTheRight() = runComposeUiTest {
        showChannel(
            ColorPickerState(Hsl(200.0, 80.0, 50.0)),
            Hsl.L,
            direction = LayoutDirection.Rtl,
            coloringMode = ColoringMode.Contextual,
            track = drawnTrack,
            thumb = {},
        )
        val pixels = trackPixels()
        assertTrue(pixels.light(1), "lightness 100 at the left: ${pixels.mid(1)}")
        assertTrue(pixels.dark(pixels.width - 2), "lightness 0 at the right: ${pixels.mid(pixels.width - 2)}")
    }

    @Test
    fun aScreenReaderStepWritesTheChannelInItsSpace() = runComposeUiTest {
        val state = ColorPickerState(Srgb(1.0, 0.0, 0.0))
        showChannel(state, Hsl.S)
        onNodeWithTag("slider").performSemanticsAction(SemanticsActions.SetProgress) { it(0.5f) }
        assertEquals(Hsl, state.value.space)
        assertNear(50.0, state[Hsl.S])
    }

    @Test
    fun keysStepByTheChannelsStep() = runComposeUiTest {
        val state = ColorPickerState(OkLch(0.6, 0.1, 30.0))
        showChannel(state, OkLch.C)
        onNodeWithTag("slider").requestFocus()
        onNodeWithTag("slider").performKeyInput { pressKey(Key.DirectionRight) }
        assertNear(0.101, state[OkLch.C], 1e-12)
    }
}

// The pixel halfway down at [x].
private fun PixelMap.mid(x: Int): Color = this[x, height / 2]

private fun PixelMap.dark(x: Int): Boolean = mid(x).let { it.red < 0.1f && it.green < 0.1f && it.blue < 0.1f }

private fun PixelMap.light(x: Int): Boolean = mid(x).let { it.red > 0.9f && it.green > 0.9f && it.blue > 0.9f }
