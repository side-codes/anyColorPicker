package codes.side.colorpicker.foundation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import codes.side.color.ColorSpace
import codes.side.color.Hsl
import codes.side.color.RgbPrimaries
import codes.side.color.TransferFunction
import codes.side.color.WhitePoint
import codes.side.colorpicker.state.ColorPickerState
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ControlledPickerRestorationTest {

    @Test
    fun aGreyInTheCallersCustomSpaceKeepsItsRememberedHueAfterRecreation() = runComposeUiTest {
        val custom = ColorSpace.rgb("--caller-rgb", RgbPrimaries.Srgb, WhitePoint.D65, TransferFunction.Srgb)
        var external by mutableStateOf(Hsl(200.0, 80.0, 50.0).to(custom))
        lateinit var state: ColorPickerState
        val recreation = Recreation()
        setContent {
            recreation.Content {
                state = rememberControlledPickerState(external, Hsl, { external = it }, { it }, { it })
            }
        }
        runOnIdle { external = custom(0.5, 0.5, 0.5) }
        waitForIdle()
        assertEquals(200.0, state.displayValue(Hsl.H), 1e-9)
        recreation.recreate(this)
        assertEquals(external, state.value)
        assertEquals(200.0, state.displayValue(Hsl.H), 1e-9)
    }
}
