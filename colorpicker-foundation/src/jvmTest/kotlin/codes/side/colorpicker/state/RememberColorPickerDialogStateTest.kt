package codes.side.colorpicker.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import codes.side.color.ColorSpace
import codes.side.color.ColorValue
import codes.side.color.DisplayP3
import codes.side.color.Hsv
import codes.side.color.Okhsl
import codes.side.color.Oklch
import codes.side.color.Srgb
import codes.side.colorpicker.foundation.Recreation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

@OptIn(ExperimentalTestApi::class)
class RememberColorPickerDialogStateTest {

    private val spaces = listOf(Okhsl, Oklch, Hsv, Srgb)
    private val teal = Hsv(190.0, 70.0, 60.0)

    @Test
    fun theEditAndTheSpaceSurviveRecreation() = runComposeUiTest {
        val recreation = Recreation()
        lateinit var state: ColorPickerDialogState
        setContent { recreation.Content { state = rememberColorPickerDialogState(teal, spaces) } }
        runOnIdle {
            state.space = Oklch
            state.pickerState[Oklch.C] = 0.1
        }
        val edited = state.pickerState.value
        val before = state
        recreation.recreate(this)
        runOnIdle {
            assertNotSame(before, state, "the state is a restored one, not the one remembered")
            assertSame(Oklch, state.space)
            assertEquals(edited, state.pickerState.value)
            assertEquals(teal, state.original)
        }
    }

    @Test
    fun anAppSpaceItOffersSurvivesRecreation() = runComposeUiTest {
        val p3Hsl = ColorSpace.hsl("--hsl-p3", DisplayP3)
        val recreation = Recreation()
        lateinit var state: ColorPickerDialogState
        setContent { recreation.Content { state = rememberColorPickerDialogState(teal, listOf(Okhsl, p3Hsl)) } }
        runOnIdle { state.space = p3Hsl }
        recreation.recreate(this)
        runOnIdle { assertSame(p3Hsl, state.space) }
    }

    @Test
    fun spacesListedAnewEachTimeDoNotStartOver() = runComposeUiTest {
        var recompose by mutableStateOf(0)
        lateinit var state: ColorPickerDialogState
        setContent {
            recompose
            state = rememberColorPickerDialogState(teal, listOf(Okhsl, Oklch, Hsv, Srgb))
        }
        runOnIdle { state.pickerState[Hsv.S] = 10.0 }
        val before = state
        recompose++
        runOnIdle {
            assertSame(before, state)
            assertEquals(10.0, state.pickerState[Hsv.S])
        }
    }

    @Test
    fun aNewInitialValueStartsOver() = runComposeUiTest {
        var initial by mutableStateOf<ColorValue>(teal)
        lateinit var state: ColorPickerDialogState
        setContent { state = rememberColorPickerDialogState(initial, spaces) }
        runOnIdle { state.pickerState[Hsv.S] = 10.0 }
        val green = Okhsl(140.0, 0.8, 0.5)
        initial = green
        runOnIdle {
            assertSame(green, state.original)
            assertEquals(green, state.pickerState.value)
            assertSame(Okhsl, state.space)
        }
    }
}
