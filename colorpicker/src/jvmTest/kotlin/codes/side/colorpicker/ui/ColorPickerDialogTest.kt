package codes.side.colorpicker.ui

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import codes.side.colorpicker.model.HslColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The dialog builds its own state, so its slots are the only place a caller can reach it. A
 * slot that cannot read or write that state can host a label and nothing else, which is not
 * enough to replace a slider.
 */
@OptIn(ExperimentalTestApi::class)
class ColorPickerDialogTest {

    private val teal = HslColor(hue = 200f, saturation = 0.8f, lightness = 0.5f)

    @Test
    fun aReplacedSliderReadsAndWritesTheDialogsState() = runComposeUiTest {
        var selected: HslColor? = null
        setContent {
            ColorPickerDialog(
                onColorSelected = { selected = it },
                onDismiss = {},
                initialColor = teal,
                hueSlider = { state ->
                    Text("Farbton ${state.hslColor.intHue}")
                    TextButton(onClick = { state.updateHue(120f) }) { Text("Grün") }
                },
            )
        }
        onNodeWithText("Farbton 200").assertExists()

        onNodeWithText("Grün").performClick()
        onNodeWithText("Farbton 120").assertExists()

        onNodeWithText("Select").performClick()
        assertEquals(120f, selected?.hue, "confirm returns what the slot wrote")
    }

    @Test
    fun theOtherSlidersKeepWorkingBesideAReplacedOne() = runComposeUiTest {
        setContent {
            ColorPickerDialog(
                onColorSelected = {},
                onDismiss = {},
                initialColor = teal,
                hueSlider = { Text("Farbton") },
            )
        }
        onNodeWithText("Farbton").assertExists()
        onNodeWithText("Hue").assertDoesNotExist()
        onNodeWithText("Saturation").assertExists()
        onNodeWithText("Lightness").assertExists()
    }

    @Test
    fun theDefaultSlidersAreDrawnWhenNoSlotIsPassed() = runComposeUiTest {
        setContent {
            ColorPickerDialog(onColorSelected = {}, onDismiss = {}, initialColor = teal)
        }
        onNodeWithText("Hue").assertExists()
        onNodeWithText("Saturation").assertExists()
        onNodeWithText("Lightness").assertExists()
        onNodeWithText("Alpha").assertExists()
    }

    @Test
    fun dismissingReportsNothing() = runComposeUiTest {
        var selected: HslColor? = null
        var dismissed = false
        setContent {
            ColorPickerDialog(
                onColorSelected = { selected = it },
                onDismiss = { dismissed = true },
                initialColor = teal,
            )
        }
        onNodeWithText("Cancel").performClick()
        assertEquals(true, dismissed)
        assertNull(selected)
    }
}
