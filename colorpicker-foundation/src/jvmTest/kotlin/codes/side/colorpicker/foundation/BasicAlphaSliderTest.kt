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
import codes.side.color.ColorValue
import codes.side.color.Okhsl
import codes.side.color.Srgb
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.assertNear
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class BasicAlphaSliderTest {

    // The alpha slider over [state], tagged "slider", 220 dp wide, drawn with foundation alone.
    private fun ComposeUiTest.showAlpha(
        state: ColorPickerState,
        direction: LayoutDirection = LayoutDirection.Ltr,
        track: @Composable AlphaSliderScope.() -> Unit = { Box(Modifier.fillMaxWidth().height(8.dp)) },
        thumb: @Composable AlphaSliderScope.() -> Unit = { Box(Modifier.size(20.dp)) },
    ) {
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                BasicAlphaSlider(state, Modifier.width(220.dp).testTag("slider"), track = track, thumb = thumb)
            }
        }
    }

    // A track that draws the gradient it is handed, tagged "track".
    private val drawnTrack: @Composable AlphaSliderScope.() -> Unit = {
        Canvas(Modifier.fillMaxWidth().height(8.dp).testTag("track")) { drawRect(gradient) }
    }

    private fun ComposeUiTest.trackPixels(): PixelMap =
        onNodeWithTag("track", useUnmergedTree = true).captureToImage().toPixelMap()

    @Test
    fun theSlotsSeeTheAlphaAndAnOpaqueThumbColor() = runComposeUiTest {
        var seen: Pair<Double, Color>? = null
        showAlpha(
            ColorPickerState(Srgb(1.0, 0.0, 0.0, 0.25)),
            thumb = {
                seen = alpha to thumbColor
                Box(Modifier.size(20.dp))
            },
        )
        assertEquals(0.25 to Color.Red, seen)
    }

    @Test
    fun aMissingAlphaReadsZero() = runComposeUiTest {
        var seen: Pair<Double, Float>? = null
        showAlpha(
            ColorPickerState(Srgb.color(doubleArrayOf(1.0, 0.0, 0.0), alpha = null)),
            thumb = {
                seen = alpha to fraction
                Box(Modifier.size(20.dp))
            },
        )
        assertEquals(0.0 to 0f, seen)
    }

    @Test
    fun theGradientFadesTheColorInFromTheTracksStart() = runComposeUiTest {
        showAlpha(ColorPickerState(Srgb(1.0, 0.0, 0.0, 0.5)), track = drawnTrack, thumb = {})
        val pixels = trackPixels()
        assertTrue(pixels[1, pixels.height / 2].alpha < 0.1f, "transparent at the left: ${pixels[1, pixels.height / 2]}")
        val end = pixels[pixels.width - 2, pixels.height / 2]
        assertTrue(end.alpha > 0.9f && end.red > 0.9f, "opaque red at the right: $end")
    }

    @Test
    fun rightToLeftTheColorFadesInFromTheRight() = runComposeUiTest {
        showAlpha(ColorPickerState(Srgb(1.0, 0.0, 0.0, 0.5)), direction = LayoutDirection.Rtl, track = drawnTrack, thumb = {})
        val pixels = trackPixels()
        assertTrue(pixels[pixels.width - 2, pixels.height / 2].alpha < 0.1f, "transparent at the right")
        assertTrue(pixels[1, pixels.height / 2].alpha > 0.9f, "opaque at the left")
    }

    @Test
    fun anEditKeepsTheColorsSpace() = runComposeUiTest {
        val state = ColorPickerState(Okhsl(30.0, 0.8, 0.6))
        showAlpha(state)
        onNodeWithTag("slider").performSemanticsAction(SemanticsActions.SetProgress) { it(0.25f) }
        assertEquals(Okhsl(30.0, 0.8, 0.6, 0.25), state.value)
    }

    @Test
    fun anEditGoesThroughTheEditPath() = runComposeUiTest {
        val red = Srgb(1.0, 0.0, 0.0)
        val state = ColorPickerState(red)
        var reported: ColorValue? = null
        state.onEdit = { reported = it }
        showAlpha(state)
        onNodeWithTag("slider").performSemanticsAction(SemanticsActions.SetProgress) { it(0.25f) }
        assertEquals(Srgb(1.0, 0.0, 0.0, 0.25), reported)
        assertSame(red, state.value, "an edit goes to the sink, not the value")
    }

    @Test
    fun keyPressesBuildOnTheLastEmission() = runComposeUiTest {
        val state = ColorPickerState(Srgb(1.0, 0.0, 0.0, 0.5))
        val emitted = mutableListOf<ColorValue>()
        state.onEdit = {
            state.emit(it)
            emitted += it
        }
        showAlpha(state)
        onNodeWithTag("slider").requestFocus()
        onNodeWithTag("slider").performKeyInput {
            pressKey(Key.DirectionRight)
            pressKey(Key.DirectionRight)
        }
        assertEquals(2, emitted.size)
        assertNear(0.51, emitted[0].alpha, 1e-12)
        assertNear(0.52, emitted[1].alpha, 1e-12)
    }
}
