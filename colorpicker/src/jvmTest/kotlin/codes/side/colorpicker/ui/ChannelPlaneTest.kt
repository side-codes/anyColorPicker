package codes.side.colorpicker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import codes.side.color.ColorChannel
import codes.side.color.ColorSpace
import codes.side.color.ColorValue
import codes.side.color.DisplayP3
import codes.side.color.Hsl
import codes.side.color.Hsv
import codes.side.color.OkLch
import codes.side.color.Okhsl
import codes.side.color.Srgb
import codes.side.color.compose.toComposeColor
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.assertNear
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ChannelPlaneTest {

    private fun state() = ColorPickerState(Hsl(200.0, 50.0, 50.0))

    // Counts the key presses that reach past the plane to its parent.
    private fun Modifier.countKeyDowns(onKeyDown: () -> Unit): Modifier = onKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown) onKeyDown()
        false
    }

    private fun ComposeUiTest.show(state: ColorPickerState, x: ColorChannel = Hsl.S, y: ColorChannel = Hsl.L, enabled: Boolean = true) {
        setContent { ChannelPlane(state, x, y, Modifier.testTag("plane").size(200.dp), enabled = enabled) }
    }

    private fun ComposeUiTest.pixels(): PixelMap = onNodeWithTag("plane").captureToImage().toPixelMap()

    // The pixel at a fraction across and up the plane.
    private fun PixelMap.at(fx: Float, fy: Float): Color = this[(fx * (width - 1)).toInt(), ((1f - fy) * (height - 1)).toInt()]

    private fun assertColor(expected: Color, actual: Color, tolerance: Float, what: String) {
        val off = maxOf(abs(expected.red - actual.red), abs(expected.green - actual.green), abs(expected.blue - actual.blue))
        assertTrue(off <= tolerance, "$what: expected $expected, was $actual")
    }

    @Test
    fun arrowKeysMoveEachChannelByItsStep() = runComposeUiTest {
        val state = state()
        show(state)
        onNodeWithTag("plane").requestFocus()
        onNodeWithTag("plane").performKeyInput { pressKey(Key.DirectionRight) }
        assertNear(51.0, state[Hsl.S], message = "right")
        onNodeWithTag("plane").performKeyInput { pressKey(Key.DirectionUp) }
        assertNear(51.0, state[Hsl.L], message = "up adds, since y grows upward")
        onNodeWithTag("plane").performKeyInput { pressKey(Key.DirectionLeft) }
        onNodeWithTag("plane").performKeyInput { pressKey(Key.DirectionDown) }
        assertNear(50.0, state[Hsl.S], message = "left")
        assertNear(50.0, state[Hsl.L], message = "down")
    }

    @Test
    fun shiftArrowMovesByThePageStep() = runComposeUiTest {
        val state = state()
        show(state)
        onNodeWithTag("plane").requestFocus()
        onNodeWithTag("plane").performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.DirectionRight) } }
        assertNear(60.0, state[Hsl.S])
    }

    @Test
    fun theEdgesHold() = runComposeUiTest {
        val state = ColorPickerState(Hsl(200.0, 100.0, 50.0))
        show(state)
        onNodeWithTag("plane").requestFocus()
        repeat(3) { onNodeWithTag("plane").performKeyInput { pressKey(Key.DirectionRight) } }
        assertNear(100.0, state[Hsl.S], message = "saturation cannot go past full")
    }

    @Test
    fun anArrowThePlaneCannotUseIsPassedOn() = runComposeUiTest {
        var passedOn = 0
        val state = ColorPickerState(Hsl(200.0, 100.0, 50.0))
        setContent {
            Box(Modifier.countKeyDowns { passedOn++ }) {
                ChannelPlane(state, Hsl.S, Hsl.L, Modifier.testTag("plane").size(200.dp))
            }
        }
        onNodeWithTag("plane").requestFocus()
        onNodeWithTag("plane").performKeyInput { pressKey(Key.DirectionRight) }
        assertEquals(1, passedOn, "at the edge it goes past")
    }

    @Test
    fun anArrowThePlaneCanUseIsKept() = runComposeUiTest {
        var passedOn = 0
        val state = state()
        setContent {
            Box(Modifier.countKeyDowns { passedOn++ }) {
                ChannelPlane(state, Hsl.S, Hsl.L, Modifier.testTag("plane").size(200.dp))
            }
        }
        onNodeWithTag("plane").requestFocus()
        onNodeWithTag("plane").performKeyInput { pressKey(Key.DirectionRight) }
        assertNear(51.0, state[Hsl.S])
        assertEquals(0, passedOn, "mid-field it is the plane's to use")
    }

    @Test
    fun aDisabledPlaneIgnoresTheKeyboardAndOffersNoActions() = runComposeUiTest {
        val state = state()
        show(state, enabled = false)
        val config = onNodeWithTag("plane").fetchSemanticsNode().config
        assertTrue(SemanticsProperties.Focused !in config, "nothing to focus")
        assertTrue(SemanticsActions.CustomActions !in config, "nothing to offer when it is off")
        onNodeWithTag("plane").performKeyInput { pressKey(Key.DirectionRight) }
        assertNear(50.0, state[Hsl.S])
    }

    @Test
    fun aScreenReaderIsGivenOneActionPerDirection() = runComposeUiTest {
        val state = state()
        show(state)
        val actions = onNodeWithTag("plane").fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertEquals(
            listOf("Increase saturation", "Decrease saturation", "Increase lightness", "Decrease lightness"),
            actions.map { it.label },
        )
        runOnUiThread { assertTrue(actions[0].action!!.invoke()) }
        waitForIdle()
        assertNear(60.0, state[Hsl.S], message = "an action takes the page step")
        runOnUiThread { actions[3].action!!.invoke() }
        waitForIdle()
        assertNear(40.0, state[Hsl.L])
    }

    @Test
    fun theDefaultTextNamesTheChannels() = runComposeUiTest {
        show(ColorPickerState(Hsl(200.0, 40.0, 60.0)))
        onNodeWithTag("plane").assertContentDescriptionEquals("Saturation and lightness")
        assertEquals("40% saturation, 60% lightness", onNodeWithTag("plane").fetchSemanticsNode().config[SemanticsProperties.StateDescription])
    }

    @Test
    fun stepsComeFromTheChannels() = runComposeUiTest {
        val state = ColorPickerState(OkLch(0.5, 0.1, 30.0))
        show(state, OkLch.C, OkLch.L)
        onNodeWithTag("plane").requestFocus()
        onNodeWithTag("plane").performKeyInput { pressKey(Key.DirectionRight) }
        assertNear(0.101, state[OkLch.C], 1e-12)
        onNodeWithTag("plane").performKeyInput { pressKey(Key.DirectionUp) }
        assertNear(0.51, state[OkLch.L], 1e-12)
        onNodeWithTag("plane").performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.DirectionLeft) } }
        assertNear(0.091, state[OkLch.C], 1e-12)
    }

    @Test
    fun aPressWritesBothChannelsInOneEdit() = runComposeUiTest {
        val red = Srgb(1.0, 0.0, 0.0)
        val state = ColorPickerState(red)
        val reported = mutableListOf<ColorValue>()
        state.onEdit = { reported += it }
        show(state)
        onNodeWithTag("plane").performTouchInput {
            down(center)
            up()
        }
        assertEquals(1, reported.size, "one edit for both channels")
        assertEquals(Hsl, reported[0].space)
        assertNear(50.0, reported[0][Hsl.S], 1.0)
        assertNear(50.0, reported[0][Hsl.L], 1.0)
    }

    @Test
    fun twoChannelsMustBeDifferentChannelsOfOneSpace() {
        requirePlaneChannels(OkLch.C, OkLch.L)
        assertFailsWith<IllegalArgumentException> { requirePlaneChannels(Hsl.S, OkLch.L) }
        assertFailsWith<IllegalArgumentException> { requirePlaneChannels(Hsl.S, Hsl.S) }
    }

    @Test
    fun theHslPlaneIsExactInside() = runComposeUiTest {
        // Lightness 100 parks the indicator on the top edge, clear of the sampled points.
        show(ColorPickerState(Hsl(0.0, 100.0, 100.0)))
        val px = pixels()
        assertColor(Hsl(0.0, 50.0, 75.0).toComposeColor(), px.at(0.5f, 0.75f), 3f / 255f, "s 50, l 75")
        assertColor(Hsl(0.0, 50.0, 25.0).toComposeColor(), px.at(0.5f, 0.25f), 3f / 255f, "s 50, l 25")
    }

    @Test
    fun theHsvPlaneIsExactInside() = runComposeUiTest {
        // Value 100 at saturation 0 parks the indicator on the top left corner.
        show(ColorPickerState(Hsv(0.0, 0.0, 100.0)), Hsv.S, Hsv.V)
        val px = pixels()
        assertColor(Hsv(0.0, 50.0, 50.0).toComposeColor(), px.at(0.5f, 0.5f), 3f / 255f, "s 50, v 50")
        assertColor(Hsv(0.0, 75.0, 25.0).toComposeColor(), px.at(0.75f, 0.25f), 3f / 255f, "s 75, v 25")
    }

    @Test
    fun aGreysPlaneIsDrawnAtTheRememberedHue() = runComposeUiTest {
        val state = ColorPickerState(Hsl(120.0, 80.0, 100.0))
        state.value = Srgb(1.0, 1.0, 1.0)
        show(state)
        assertColor(Color(0xFF00FF00), pixels().at(0.99f, 0.5f), 3f / 255f, "full saturation at mid lightness")
    }

    // The planes these tests rasterize are black along the bottom, and draw nothing until their raster
    // arrives, whatever the capture shows behind them.
    private fun ComposeUiTest.rasterDrawn(): Boolean = pixels().at(0.5f, 0.01f).let { it.alpha > 0.99f && it.red < 0.06f }

    @Test
    fun aRasterPlaneArrives() = runComposeUiTest {
        // Full saturation and lightness park the indicator in the top right corner, clear of the samples.
        show(ColorPickerState(Okhsl(29.2, 1.0, 1.0)), Okhsl.S, Okhsl.L)
        waitUntil(timeoutMillis = 5_000) { rasterDrawn() }
        val px = pixels()
        assertColor(Color.White, px.at(0.5f, 0.99f), 0.06f, "lightness 1 is white")
        assertColor(Color.Black, px.at(0.5f, 0.01f), 0.06f, "lightness 0 is black")
        val left = px.at(0.01f, 0.5f)
        assertTrue(abs(left.red - left.green) < 0.02f && abs(left.green - left.blue) < 0.02f, "saturation 0 is grey, was $left")
    }

    @Test
    fun aPreviewDrawsItsRasterInTheFirstFrame() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                ChannelPlane(ColorPickerState(Okhsl(29.2, 1.0, 1.0)), Okhsl.S, Okhsl.L, Modifier.testTag("plane").size(200.dp))
            }
        }
        assertTrue(rasterDrawn(), "a preview draws its raster without waiting")
        assertColor(Color.Black, pixels().at(0.5f, 0.01f), 0.06f, "lightness 0 is black")
    }

    @Test
    fun theLastHueWins() = runComposeUiTest {
        val state = ColorPickerState(Okhsl(30.0, 0.0, 1.0))
        show(state, Okhsl.S, Okhsl.L)
        waitForIdle()
        state.value = Okhsl(250.0, 0.0, 1.0)
        waitForIdle()
        val expected = Okhsl(250.0, 1.0, 0.5).toComposeColor()
        waitUntil(timeoutMillis = 5_000) { abs(pixels().at(0.99f, 0.5f).blue - expected.blue) < 0.03f }
        // Long enough for a raster of hue 30 that was not abandoned to land over it.
        Thread.sleep(300)
        waitForIdle()
        assertColor(expected, pixels().at(0.99f, 0.5f), 6f / 255f, "the plane shows the last hue")
    }

    @Test
    fun thePlaneRedrawsWhileTheHueKeepsMoving() = runComposeUiTest {
        // A hue drag changes the held channel more often than a 256 × 256 raster takes to build. A
        // plane that dropped every raster a newer hue made stale would show the first hue throughout.
        val state = ColorPickerState(Okhsl(30.0, 0.0, 1.0))
        show(state, Okhsl.S, Okhsl.L)
        waitUntil(timeoutMillis = 5_000) { rasterDrawn() }
        val before = pixels().at(0.99f, 0.5f)
        for (step in 1..60) {
            state.value = Okhsl(30.0 + step * 3.0, 0.0, 1.0)
            waitForIdle()
            Thread.sleep(5)
        }
        val during = pixels().at(0.99f, 0.5f)
        assertTrue(abs(during.green - before.green) > 0.1f, "the plane still shows hue 30 at the end of the drag, $during")
    }

    @Test
    fun anAppHslPlaneIsRasterizedAndEditsInItsSpace() = runComposeUiTest {
        val p3Hsl = ColorSpace.hsl("--plane-hsl-p3", DisplayP3)
        val state = ColorPickerState(Srgb(1.0, 0.0, 0.0))
        show(state, p3Hsl.S, p3Hsl.L)
        waitUntil(timeoutMillis = 5_000) { rasterDrawn() }
        onNodeWithTag("plane").performTouchInput {
            down(center)
            up()
        }
        assertEquals(p3Hsl, state.value.space)
    }

    @Test
    fun thePlaneIsNotMirroredInRightToLeft() {
        fun centre(direction: LayoutDirection): Float {
            var centre = 0f
            runComposeUiTest {
                setContent {
                    CompositionLocalProvider(LocalLayoutDirection provides direction) {
                        ChannelPlane(ColorPickerState(Hsl(240.0, 90.0, 50.0)), Hsl.S, Hsl.L, Modifier.size(200.dp).testTag("plane"))
                    }
                }
                val px = pixels()
                val row = px.height / 2
                val ring = (0 until px.width).filter {
                    val c = px[it, row]
                    c.red > 0.95f && c.green > 0.95f && c.blue > 0.95f
                }
                assertTrue(ring.isNotEmpty(), "no indicator ring on the middle row")
                centre = (ring.first() + ring.last()) / 2f
            }
            return centre
        }
        val ltr = centre(LayoutDirection.Ltr)
        assertTrue(ltr > 150f, "saturation 90 belongs near the right edge, was $ltr")
        assertEquals(ltr, centre(LayoutDirection.Rtl), 1f, "the indicator moved between layout directions")
    }
}
