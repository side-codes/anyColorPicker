package codes.side.colorpicker.ui

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import codes.side.color.Hsl
import codes.side.colorpicker.state.ColorPickerState
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class InteractionSourceTest {

    private val teal = Hsl(200.0, 80.0, 50.0)

    // Shows [content], presses the node tagged "part" and drags it past the touch slop, and returns
    // what [source] received.
    private fun ComposeUiTest.dragAndCollect(
        source: MutableInteractionSource,
        content: @Composable () -> Unit,
    ): List<Interaction> {
        val seen = mutableListOf<Interaction>()
        setContent {
            LaunchedEffect(source) { source.interactions.collect { seen += it } }
            content()
        }
        onNodeWithTag("part").performTouchInput {
            down(center)
            moveBy(Offset(viewConfiguration.touchSlop * 2, 0f))
        }
        waitForIdle()
        return seen
    }

    private fun List<Interaction>.dragged(): Boolean = any { it is DragInteraction.Start }

    @Test
    fun aChannelSliderReportsToTheCallersSource() = runComposeUiTest {
        val source = MutableInteractionSource()
        val seen = dragAndCollect(source) {
            ChannelSlider(
                ColorPickerState(teal),
                Hsl.H,
                Modifier.width(300.dp).testTag("part"),
                label = null,
                valueLabel = null,
                interactionSource = source,
            )
        }
        assertTrue(seen.dragged(), "$seen")
    }

    @Test
    fun anAlphaSliderReportsToTheCallersSource() = runComposeUiTest {
        val source = MutableInteractionSource()
        val seen = dragAndCollect(source) {
            AlphaSlider(
                ColorPickerState(teal),
                Modifier.width(300.dp).testTag("part"),
                label = null,
                valueLabel = null,
                interactionSource = source,
            )
        }
        assertTrue(seen.dragged(), "$seen")
    }

    @Test
    fun aColorSliderReportsToTheCallersSource() = runComposeUiTest {
        val source = MutableInteractionSource()
        val seen = dragAndCollect(source) {
            ColorSlider(
                value = 0.5f,
                onValueChange = {},
                gradientColors = persistentListOf(Color.Black, Color.White),
                thumbColor = Color.Gray,
                modifier = Modifier.width(300.dp).testTag("part"),
                interactionSource = source,
            )
        }
        assertTrue(seen.dragged(), "$seen")
    }

    @Test
    fun aChannelPlaneReportsToTheCallersSource() = runComposeUiTest {
        val source = MutableInteractionSource()
        val seen = dragAndCollect(source) {
            ChannelPlane(ColorPickerState(teal), Hsl.S, Hsl.L, Modifier.size(200.dp).testTag("part"), interactionSource = source)
        }
        assertTrue(seen.dragged(), "$seen")
    }

    @Test
    fun aColorPlaneReportsToTheCallersSource() = runComposeUiTest {
        val source = MutableInteractionSource()
        val seen = dragAndCollect(source) {
            ColorPlane(
                xValue = 0.5f,
                yValue = 0.5f,
                onValueChange = { _, _ -> },
                surface = {},
                modifier = Modifier.size(200.dp).testTag("part"),
                interactionSource = source,
            )
        }
        assertTrue(seen.dragged(), "$seen")
    }

    @Test
    fun aThumbSlotIsHandedTheCallersSource() = runComposeUiTest {
        val source = MutableInteractionSource()
        var slider: InteractionSource? = null
        var plane: InteractionSource? = null
        setContent {
            Column {
                ChannelSlider(
                    ColorPickerState(teal),
                    Hsl.H,
                    interactionSource = source,
                    thumb = {
                        slider = it
                        Box(Modifier.size(4.dp))
                    },
                )
                ChannelPlane(ColorPickerState(teal), Hsl.S, Hsl.L, Modifier.size(200.dp), interactionSource = source, thumb = { plane = it })
            }
        }
        assertSame(source, slider)
        assertSame(source, plane)
    }

    @Test
    fun aPlaneReportsToANewSource() = runComposeUiTest {
        val first = MutableInteractionSource()
        val second = MutableInteractionSource()
        var current by mutableStateOf(first)
        val toFirst = mutableListOf<Interaction>()
        val toSecond = mutableListOf<Interaction>()
        setContent {
            LaunchedEffect(Unit) { first.interactions.collect { toFirst += it } }
            LaunchedEffect(Unit) { second.interactions.collect { toSecond += it } }
            ChannelPlane(ColorPickerState(teal), Hsl.S, Hsl.L, Modifier.size(200.dp).testTag("plane"), interactionSource = current)
        }
        // A first press starts the gesture handler while the first source is current.
        onNodeWithTag("plane").performTouchInput {
            down(center)
            up()
        }
        waitForIdle()
        current = second
        waitForIdle()
        onNodeWithTag("plane").performTouchInput {
            down(center)
            up()
        }
        waitForIdle()
        assertTrue(toSecond.dragged(), "the second press reached the new source: $toSecond")
        assertEquals(1, toFirst.count { it is DragInteraction.Start }, "the old source saw only the first press")
    }
}
