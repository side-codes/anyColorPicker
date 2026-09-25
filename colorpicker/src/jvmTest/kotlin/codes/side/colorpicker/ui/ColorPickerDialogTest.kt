package codes.side.colorpicker.ui

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import codes.side.color.ColorValue
import codes.side.color.DisplayP3
import codes.side.color.Okhsl
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * The dialog builds its own state, so its slots are the only place a caller can reach it. A slot that
 * cannot read or write that state can host a label and nothing else, which is not enough to replace a
 * slider.
 */
@OptIn(ExperimentalTestApi::class)
class ColorPickerDialogTest {

    private val teal = Okhsl(200.0, 0.8, 0.5)

    @Test
    fun aReplacedSliderReadsAndWritesTheDialogsState() = runComposeUiTest {
        var selected: ColorValue? = null
        setContent {
            ColorPickerDialog(
                initialValue = teal,
                onValueSelected = { selected = it },
                onDismiss = {},
                channelSlider = { state, channel ->
                    if (channel === Okhsl.H) {
                        Text("Farbton ${state.displayValue(channel).roundToInt()}")
                        TextButton(onClick = { state[Okhsl.H] = 120.0 }) { Text("Grün") }
                    } else {
                        ChannelSlider(state, channel)
                    }
                },
            )
        }
        onNodeWithText("Farbton 200").assertExists()
        onNodeWithText("Grün").performClick()
        onNodeWithText("Farbton 120").assertExists()
        onNodeWithText("Select").performClick()
        assertEquals(120.0, selected?.get(Okhsl.H), "confirm returns what the slot wrote")
    }

    @Test
    fun theOtherSlidersKeepWorkingBesideAReplacedOne() = runComposeUiTest {
        setContent {
            ColorPickerDialog(
                initialValue = teal,
                onValueSelected = {},
                onDismiss = {},
                channelSlider = { state, channel -> if (channel === Okhsl.H) Text("Farbton") else ChannelSlider(state, channel) },
            )
        }
        onNodeWithText("Farbton").assertExists()
        onNodeWithText("Hue").assertDoesNotExist()
        onNodeWithText("Saturation").assertExists()
        onNodeWithText("Lightness").assertExists()
    }

    @Test
    fun theDefaultSlidersAreDrawnWhenNoSlotIsPassed() = runComposeUiTest {
        setContent { ColorPickerDialog(initialValue = teal, onValueSelected = {}, onDismiss = {}) }
        onNodeWithText("Hue").assertExists()
        onNodeWithText("Saturation").assertExists()
        onNodeWithText("Lightness").assertExists()
        onNodeWithText("Alpha").assertExists()
    }

    @Test
    fun dismissingReportsNothing() = runComposeUiTest {
        var selected: ColorValue? = null
        var dismissed = false
        setContent { ColorPickerDialog(initialValue = teal, onValueSelected = { selected = it }, onDismiss = { dismissed = true }) }
        onNodeWithText("Cancel").performClick()
        assertEquals(true, dismissed)
        assertNull(selected)
    }

    @Test
    fun anUneditedConfirmReturnsTheInitialValue() = runComposeUiTest {
        // Converted into the dialog's Okhsl on the way out, a Display P3 red would come back clipped to sRGB.
        val p3Red = DisplayP3(1.0, 0.0, 0.0)
        var selected: ColorValue? = null
        setContent { ColorPickerDialog(initialValue = p3Red, onValueSelected = { selected = it }, onDismiss = {}) }
        onNodeWithText("Select").performClick()
        assertSame(p3Red, selected)
    }

    @Test
    fun anEditReturnsTheValueInTheDialogsSpace() = runComposeUiTest {
        var selected: ColorValue? = null
        setContent { ColorPickerDialog(initialValue = DisplayP3(0.2, 0.4, 0.6), onValueSelected = { selected = it }, onDismiss = {}) }
        sliderNamed("Lightness").performSemanticsAction(SemanticsActions.SetProgress) { it(0.5f) }
        onNodeWithText("Select").performClick()
        assertEquals(Okhsl, selected?.space)
    }

    @Test
    fun aNewInitialValueResetsTheDialog() = runComposeUiTest {
        var initial by mutableStateOf<ColorValue>(teal)
        var selected: ColorValue? = null
        setContent { ColorPickerDialog(initialValue = initial, onValueSelected = { selected = it }, onDismiss = {}) }
        sliderNamed("Hue").performSemanticsAction(SemanticsActions.SetProgress) { it(0.5f) }
        initial = Okhsl(40.0, 0.5, 0.6)
        waitForIdle()
        onNodeWithText("Select").performClick()
        assertEquals(Okhsl(40.0, 0.5, 0.6), selected)
    }

    @Test
    fun theColorFormReturnsAComposeColor() = runComposeUiTest {
        val blue = Color(0xFF3366CC)
        var selected: Color? = null
        setContent { ColorPickerDialog(initialColor = blue, onColorSelected = { selected = it }, onDismiss = {}) }
        onNodeWithText("Select").performClick()
        assertEquals(blue, selected)
    }
}
