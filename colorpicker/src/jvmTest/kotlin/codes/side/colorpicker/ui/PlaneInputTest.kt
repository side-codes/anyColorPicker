package codes.side.colorpicker.ui

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
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
import codes.side.color.Hsl
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

    private fun state() = ColorPickerState(Hsl(200.0, 50.0, 50.0))

    @Test
    fun focusShowsOnScreen() = runComposeUiTest {
        // Keyboard focus that changes nothing visible leaves its user guessing which control
        // the arrow keys are about to move.
        setContent {
            CompositionLocalProvider(LocalInputModeManager provides KeyboardInputMode) {
                ChannelPlane(state(), Hsl.S, Hsl.L, Modifier.testTag("plane").size(200.dp))
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
        // The other half of ChannelPlaneTest.aDisabledPlaneIgnoresTheKeyboardAndOffersNoActions:
        // without this, that test would pass for a plane nothing can ever focus, which is not
        // the same as one that refuses.
        setContent { ChannelPlane(state(), Hsl.S, Hsl.L, Modifier.testTag("plane").size(200.dp)) }
        val config = onNodeWithTag("plane").fetchSemanticsNode().config
        assertTrue(SemanticsProperties.Focused in config)
    }

    @Test
    fun aFingerDoesNotLeaveTheFocusRingBehind() = runComposeUiTest {
        // Pressing the surface takes focus so the arrow keys carry on from there, which on a
        // touch device would otherwise paint a keyboard affordance after every drag.
        setContent {
            CompositionLocalProvider(LocalInputModeManager provides TouchInputMode) {
                ChannelPlane(state(), Hsl.S, Hsl.L, Modifier.testTag("plane").size(200.dp))
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
