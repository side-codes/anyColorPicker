package codes.side.colorpicker.foundation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import codes.side.color.ColorChannel
import codes.side.color.Hsl
import codes.side.color.OkLch
import codes.side.color.Srgb
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.assertNear
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class BasicChannelPlaneTest {

    // A plane over [x] and [y] of [state], tagged "plane", 200 dp square.
    private fun ComposeUiTest.showChannels(
        state: ColorPickerState,
        x: ColorChannel = Hsl.S,
        y: ColorChannel = Hsl.L,
        thumb: @Composable ChannelPlaneScope.() -> Unit = { Box(Modifier.size(20.dp)) },
    ) {
        setContent { BasicChannelPlane(state, x, y, Modifier.size(200.dp).testTag("plane"), thumb = thumb) }
    }

    @Test
    fun theThumbSeesTheChannelsAndTheOpaqueColorUnderIt() = runComposeUiTest {
        var seen: Triple<ColorChannel, ColorChannel, Color>? = null
        showChannels(
            ColorPickerState(Hsl(0.0, 100.0, 50.0, 0.3)),
            thumb = {
                seen = Triple(x, y, thumbColor)
                Box(Modifier.size(20.dp))
            },
        )
        assertEquals(Triple(Hsl.S, Hsl.L, Color(0xFFFF0000)), seen)
    }

    @Test
    fun aValuePastTheRangeIsDrawnAtTheEdge() = runComposeUiTest {
        var seen = -1f
        showChannels(
            ColorPickerState(OkLch(0.7, 0.5, 150.0)),
            OkLch.C,
            OkLch.L,
            thumb = {
                seen = xFraction
                Box(Modifier.size(20.dp))
            },
        )
        assertEquals(1f, seen)
    }

    @Test
    fun aTouchWritesBothChannelsInTheirSpace() = runComposeUiTest {
        val state = ColorPickerState(Srgb(1.0, 0.0, 0.0))
        showChannels(state)
        onNodeWithTag("plane").performTouchInput { click(center) }
        assertEquals(Hsl, state.value.space)
        assertNear(50.0, state[Hsl.S], 1.0)
        assertNear(50.0, state[Hsl.L], 1.0)
    }
}
