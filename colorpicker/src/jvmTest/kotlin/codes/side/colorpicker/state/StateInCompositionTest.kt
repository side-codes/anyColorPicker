package codes.side.colorpicker.state

import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import codes.side.color.Hsl
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A state is created in composition, by `remember` or a picker's own saved state. Whatever it reads while
 * being built, the composable building it reads too, and recomposes when that changes.
 */
@OptIn(ExperimentalTestApi::class)
class StateInCompositionTest {

    @Test
    fun anEditDoesNotRecomposeTheComposableThatRememberedTheState() = runComposeUiTest {
        var compositions = 0
        lateinit var state: ColorPickerState
        setContent {
            compositions++
            state = remember { ColorPickerState(Hsl(30.0, 50.0, 50.0)) }
        }
        waitForIdle()
        runOnUiThread { state[Hsl.H] = 180.0 }
        waitForIdle()
        assertEquals(1, compositions, "a new hue must not recompose the screen that holds the state")
    }

    @Test
    fun anEditDoesNotRecomposeTheComposableThatRestoredTheState() = runComposeUiTest {
        // Restored in a remember, where rememberSaveable restores it.
        val saver = ColorPickerState.Saver()
        val saved = with(saver) { SaverScope { true }.save(ColorPickerState(Hsl(30.0, 50.0, 50.0))) }
        var compositions = 0
        lateinit var state: ColorPickerState
        setContent {
            compositions++
            state = remember { saver.restore(checkNotNull(saved)) ?: error("the saved state did not restore") }
        }
        waitForIdle()
        runOnUiThread { state[Hsl.H] = 180.0 }
        waitForIdle()
        assertEquals(1, compositions, "a new hue must not recompose the screen that restored the state")
    }
}
