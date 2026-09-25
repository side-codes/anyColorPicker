package codes.side.colorpicker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.dp
import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.state.ColorPickerState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A plane has two degrees of freedom, so neither the arrow keys nor a screen reader's one
 * adjustable value covers it on its own. Without both, a layout of a plane and a hue slider
 * leaves saturation and lightness unreachable to anyone not holding a pointer.
 */
@OptIn(ExperimentalTestApi::class)
class PlaneInputTest {

    private fun state() = ColorPickerState(HslColor(hue = 200f, saturation = 0.5f, lightness = 0.5f))

    @Test
    fun arrowKeysMoveOnePercent() = runComposeUiTest {
        val state = state()
        setContent { HslPlane(state = state, modifier = Modifier.testTag("plane").size(200.dp)) }
        onNodeWithTag("plane").requestFocus()

        onNodeWithTag("plane").performKeyInput { pressKey(Key.DirectionRight) }
        assertEquals(0.51f, state.hslColor.saturation, 1e-4f, "right")

        onNodeWithTag("plane").performKeyInput { pressKey(Key.DirectionUp) }
        assertEquals(0.51f, state.hslColor.lightness, 1e-4f, "up adds, since y grows upward")

        onNodeWithTag("plane").performKeyInput { pressKey(Key.DirectionLeft) }
        onNodeWithTag("plane").performKeyInput { pressKey(Key.DirectionDown) }
        assertEquals(0.5f, state.hslColor.saturation, 1e-4f, "left")
        assertEquals(0.5f, state.hslColor.lightness, 1e-4f, "down")
    }

    @Test
    fun shiftArrowMovesTenPercent() = runComposeUiTest {
        val state = state()
        setContent { HslPlane(state = state, modifier = Modifier.testTag("plane").size(200.dp)) }
        onNodeWithTag("plane").requestFocus()

        onNodeWithTag("plane").performKeyInput {
            withKeyDown(Key.ShiftLeft) { pressKey(Key.DirectionRight) }
        }
        assertEquals(0.6f, state.hslColor.saturation, 1e-4f)
    }

    @Test
    fun theEdgesHold() = runComposeUiTest {
        val state = ColorPickerState(HslColor(hue = 200f, saturation = 1f, lightness = 0.5f))
        setContent { HslPlane(state = state, modifier = Modifier.testTag("plane").size(200.dp)) }
        onNodeWithTag("plane").requestFocus()

        repeat(3) { onNodeWithTag("plane").performKeyInput { pressKey(Key.DirectionRight) } }
        assertEquals(1f, state.hslColor.saturation, 1e-4f, "saturation cannot go past full")
    }

    @Test
    fun anArrowThePlaneCannotUseIsPassedOn() = runComposeUiTest {
        // Whatever moves focus on a device with only a D-pad is upstream of the plane. Keeping
        // a press that changes nothing leaves that device with no way off the plane at all.
        var passedOn = 0
        val state = ColorPickerState(HslColor(hue = 200f, saturation = 1f, lightness = 0.5f))
        setContent {
            Box(Modifier.countKeyDowns { passedOn++ }) {
                HslPlane(state = state, modifier = Modifier.testTag("plane").size(200.dp))
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
                HslPlane(state = state, modifier = Modifier.testTag("plane").size(200.dp))
            }
        }
        onNodeWithTag("plane").requestFocus()
        onNodeWithTag("plane").performKeyInput { pressKey(Key.DirectionRight) }
        assertEquals(0.51f, state.hslColor.saturation, 1e-4f)
        assertEquals(0, passedOn, "mid-field it is the plane's to use")
    }

    @Test
    fun focusShowsOnScreen() = runComposeUiTest {
        // Keyboard focus that changes nothing visible leaves its user guessing which control
        // the arrow keys are about to move.
        setContent {
            CompositionLocalProvider(LocalInputModeManager provides KeyboardInputMode) {
                HslPlane(state = state(), modifier = Modifier.testTag("plane").size(200.dp))
            }
        }
        waitForIdle()
        val before = onNodeWithTag("plane").captureToImage().toPixelMap()

        onNodeWithTag("plane").requestFocus()
        waitForIdle()
        val after = onNodeWithTag("plane").captureToImage().toPixelMap()

        val changed = changedPixels(before, after)
        assertTrue(changed > 200, "only $changed pixels changed when the plane took focus")
    }

    @Test
    fun anEnabledPlaneIsAFocusTarget() = runComposeUiTest {
        // The other half of aDisabledPlaneIgnoresTheKeyboard: without this, that test would
        // pass for a plane nothing can ever focus, which is not the same as one that refuses.
        setContent { HslPlane(state = state(), modifier = Modifier.testTag("plane").size(200.dp)) }
        val config = onNodeWithTag("plane").fetchSemanticsNode().config
        assertTrue(SemanticsProperties.Focused in config)
    }

    @Test
    fun aDisabledPlaneIgnoresTheKeyboard() = runComposeUiTest {
        val state = state()
        setContent {
            HslPlane(state = state, enabled = false, modifier = Modifier.testTag("plane").size(200.dp))
        }
        val config = onNodeWithTag("plane").fetchSemanticsNode().config
        assertTrue(SemanticsProperties.Focused !in config, "nothing to focus")
        onNodeWithTag("plane").performKeyInput { pressKey(Key.DirectionRight) }
        assertEquals(0.5f, state.hslColor.saturation, 1e-4f)
    }

    @Test
    fun aFingerDoesNotLeaveTheFocusRingBehind() = runComposeUiTest {
        // Pressing the surface takes focus so the arrow keys carry on from there, which on a
        // touch device would otherwise paint a keyboard affordance after every drag.
        setContent {
            CompositionLocalProvider(LocalInputModeManager provides TouchInputMode) {
                HslPlane(state = state(), modifier = Modifier.testTag("plane").size(200.dp))
            }
        }
        waitForIdle()
        val before = onNodeWithTag("plane").captureToImage().toPixelMap()

        onNodeWithTag("plane").requestFocus()
        waitForIdle()
        val after = onNodeWithTag("plane").captureToImage().toPixelMap()

        assertEquals(0, changedPixels(before, after), "nothing is drawn for a touch user")
    }

    @Test
    fun aScreenReaderIsGivenOneActionPerDirection() = runComposeUiTest {
        val state = state()
        setContent { HslPlane(state = state, modifier = Modifier.testTag("plane").size(200.dp)) }

        val actions = onNodeWithTag("plane").fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]
        assertEquals(
            listOf(
                "Increase saturation",
                "Decrease saturation",
                "Increase lightness",
                "Decrease lightness",
            ),
            actions.map { it.label },
            "the plane names its own channels, not its axes",
        )

        // The coarse step, because there is no modifier key to hold in an action menu.
        runOnUiThread { assertTrue(actions[0].action!!.invoke()) }
        waitForIdle()
        assertEquals(0.6f, state.hslColor.saturation, 1e-4f)

        runOnUiThread { actions[3].action!!.invoke() }
        waitForIdle()
        assertEquals(0.4f, state.hslColor.lightness, 1e-4f)
    }

    @Test
    fun aDisabledPlaneOffersNoActions() = runComposeUiTest {
        val state = state()
        setContent {
            HslPlane(state = state, enabled = false, modifier = Modifier.testTag("plane").size(200.dp))
        }
        val config = onNodeWithTag("plane").fetchSemanticsNode().config
        assertTrue(SemanticsActions.CustomActions !in config, "nothing to offer when it is off")
    }

    @Test
    fun aPlaneCanStepByItsOwnAmount() = runComposeUiTest {
        var x = 0.5f
        var y = 0.5f
        setContent {
            ColorPlaneImpl(
                xValue = x,
                yValue = y,
                onValueChange = { newX, newY ->
                    x = newX
                    y = newY
                },
                onStep = { dx, dy, coarse ->
                    val amount = if (coarse) 0.25f else 0.05f
                    x += dx * amount
                    y += dy * amount
                    true
                },
                surface = {},
                modifier = Modifier.testTag("plane").size(200.dp),
            )
        }
        onNodeWithTag("plane").requestFocus()
        onNodeWithTag("plane").performKeyInput { pressKey(Key.DirectionRight) }
        assertEquals(0.55f, x, 1e-6f)
        onNodeWithTag("plane").performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.DirectionUp) } }
        assertEquals(0.75f, y, 1e-6f)
    }
}

private fun Modifier.countKeyDowns(onKeyDown: () -> Unit): Modifier = onKeyEvent { event ->
    if (event.type == KeyEventType.KeyDown) onKeyDown()
    false
}

private fun changedPixels(before: PixelMap, after: PixelMap): Int {
    var changed = 0
    for (x in 0 until before.width) {
        for (y in 0 until before.height) {
            if (before[x, y] != after[x, y]) changed++
        }
    }
    return changed
}

private fun inputMode(mode: InputMode) = object : InputModeManager {
    override val inputMode: InputMode = mode
    override fun requestInputMode(inputMode: InputMode): Boolean = false
}

private val TouchInputMode = inputMode(InputMode.Touch)
private val KeyboardInputMode = inputMode(InputMode.Keyboard)
