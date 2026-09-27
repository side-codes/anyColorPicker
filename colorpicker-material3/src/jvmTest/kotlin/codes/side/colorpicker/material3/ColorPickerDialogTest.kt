package codes.side.colorpicker.material3

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import codes.side.color.ColorSpace
import codes.side.color.ColorValue
import codes.side.color.DisplayP3
import codes.side.color.Hsl
import codes.side.color.Hsv
import codes.side.color.Lab
import codes.side.color.Lch
import codes.side.color.OkLch
import codes.side.color.Okhsl
import codes.side.color.Oklab
import codes.side.color.Srgb
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import androidx.compose.ui.graphics.colorspace.ColorSpaces as ComposeSpaces

@OptIn(ExperimentalTestApi::class)
class ColorPickerDialogTest {

    private val teal = Okhsl(200.0, 0.8, 0.5)

    private val radioButton = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)

    @Test
    fun onlyTheLibraryImplementsTheScope() {
        // Sealed, so a member added later breaks no app: nothing outside the library can implement it.
        assertTrue(ColorPickerDialogScope::class.java.isSealed)
    }

    @Test
    fun aReplacedSliderReadsAndWritesTheDialogsState() = runComposeUiTest {
        // The dialog builds its own state, so its slots are the only place a caller can reach it.
        var selected: ColorValue? = null
        setContent {
            ColorPickerDialog(
                initialValue = teal,
                onValueSelected = { selected = it },
                onDismissRequest = {},
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
        onNodeWithText("OK").performClick()
        assertEquals(120.0, selected?.get(Okhsl.H), "confirm returns what the slot wrote")
    }

    @Test
    fun theOtherSlidersKeepWorkingBesideAReplacedOne() = runComposeUiTest {
        setContent {
            ColorPickerDialog(
                initialValue = teal,
                onValueSelected = {},
                onDismissRequest = {},
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
        setContent { ColorPickerDialog(initialValue = teal, onValueSelected = {}, onDismissRequest = {}) }
        onNodeWithText("Hue").assertExists()
        onNodeWithText("Saturation").assertExists()
        onNodeWithText("Lightness").assertExists()
        onNodeWithText("Alpha").assertExists()
    }

    @Test
    fun dismissingReportsNothing() = runComposeUiTest {
        var selected: ColorValue? = null
        var dismissed = false
        setContent { ColorPickerDialog(initialValue = teal, onValueSelected = { selected = it }, onDismissRequest = { dismissed = true }) }
        onNodeWithText("Cancel").performClick()
        assertEquals(true, dismissed)
        assertNull(selected)
    }

    @Test
    fun anUneditedConfirmReturnsTheInitialValue() = runComposeUiTest {
        // Converted into the Okhsl shown on the way out, a Display P3 red would come back clipped to sRGB.
        val p3Red = DisplayP3(1.0, 0.0, 0.0)
        var selected: ColorValue? = null
        setContent { ColorPickerDialog(initialValue = p3Red, onValueSelected = { selected = it }, onDismissRequest = {}) }
        onNodeWithText("OK").performClick()
        assertSame(p3Red, selected)
    }

    @Test
    fun aConfirmAfterRestoringReturnsTheInitialValue() = runComposeUiTest {
        val p3Red = DisplayP3(1.0, 0.0, 0.0)
        var selected: ColorValue? = null
        setContent { ColorPickerDialog(initialValue = p3Red, onValueSelected = { selected = it }, onDismissRequest = {}) }
        sliderNamed("Hue").performSemanticsAction(SemanticsActions.SetProgress) { it(0.5f) }
        onNodeWithContentDescription("Original color").performClick()
        onNodeWithText("OK").performClick()
        assertSame(p3Red, selected)
    }

    @Test
    fun anEditReturnsTheValueInTheSpaceShown() = runComposeUiTest {
        var selected: ColorValue? = null
        setContent { ColorPickerDialog(initialValue = DisplayP3(0.2, 0.4, 0.6), onValueSelected = { selected = it }, onDismissRequest = {}) }
        sliderNamed("Lightness").performSemanticsAction(SemanticsActions.SetProgress) { it(0.5f) }
        onNodeWithText("OK").performClick()
        assertSame(Okhsl, selected?.space)
    }

    @Test
    fun anAlphaEditKeepsTheValuesOwnSpace() = runComposeUiTest {
        var selected: ColorValue? = null
        setContent { ColorPickerDialog(initialValue = DisplayP3(0.2, 0.4, 0.6), onValueSelected = { selected = it }, onDismissRequest = {}) }
        sliderNamed("Alpha").performSemanticsAction(SemanticsActions.SetProgress) { it(0.5f) }
        onNodeWithText("OK").performClick()
        assertSame(DisplayP3, selected?.space)
        assertNotEquals(1.0, selected?.alpha)
    }

    @Test
    fun switchingSpaceWithoutAnEditReturnsTheInitialValue() = runComposeUiTest {
        val p3 = DisplayP3(0.2, 0.4, 0.6)
        var selected: ColorValue? = null
        setContent { ColorPickerDialog(initialValue = p3, onValueSelected = { selected = it }, onDismissRequest = {}) }
        onNodeWithText("HSV").performClick()
        onNodeWithText("Value").assertExists()
        onNodeWithText("OK").performClick()
        assertSame(p3, selected)
    }

    @Test
    fun anEditAfterSwitchingIsReturnedInTheNewSpace() = runComposeUiTest {
        var selected: ColorValue? = null
        setContent { ColorPickerDialog(initialValue = teal, onValueSelected = { selected = it }, onDismissRequest = {}) }
        onNodeWithText("HSV").performClick()
        sliderNamed("Value").performSemanticsAction(SemanticsActions.SetProgress) { it(0.3f) }
        onNodeWithText("OK").performClick()
        assertSame(Hsv, selected?.space)
    }

    @Test
    fun itOpensInTheValuesOwnSpace() = runComposeUiTest {
        setContent { ColorPickerDialog(initialValue = Hsv(190.0, 70.0, 60.0), onValueSelected = {}, onDismissRequest = {}) }
        onNodeWithText("HSV").assertIsSelected()
        onNodeWithText("Okhsl").assertIsNotSelected()
        onNodeWithText("Value").assertExists()
    }

    @Test
    fun aNewInitialValueResetsTheDialog() = runComposeUiTest {
        var initial by mutableStateOf<ColorValue>(teal)
        var selected: ColorValue? = null
        setContent { ColorPickerDialog(initialValue = initial, onValueSelected = { selected = it }, onDismissRequest = {}) }
        sliderNamed("Hue").performSemanticsAction(SemanticsActions.SetProgress) { it(0.5f) }
        initial = Okhsl(40.0, 0.5, 0.6)
        waitForIdle()
        onNodeWithText("OK").performClick()
        assertEquals(Okhsl(40.0, 0.5, 0.6), selected)
    }

    @Test
    fun anUneditedConfirmReturnsTheInitialComposeColorExactly() = runComposeUiTest {
        // Mapped into sRGB on the way out, a Display P3 red would come back clipped to #FF0B0C.
        val p3Red = Color(1f, 0f, 0f, 1f, ComposeSpaces.DisplayP3)
        var selected: Color? = null
        setContent { ColorPickerDialog(initialColor = p3Red, onColorSelected = { selected = it }, onDismissRequest = {}) }
        onNodeWithText("OK").performClick()
        assertEquals(p3Red, selected)
    }

    @Test
    fun theColorFormReturnsAComposeColor() = runComposeUiTest {
        var selected: Color? = null
        setContent { ColorPickerDialog(initialColor = Color(0xFF3366CC), onColorSelected = { selected = it }, onDismissRequest = {}) }
        sliderNamed("Lightness").performSemanticsAction(SemanticsActions.SetProgress) { it(0.3f) }
        onNodeWithText("OK").performClick()
        assertNotEquals(Color(0xFF3366CC), selected)
        assertEquals(ComposeSpaces.Srgb, selected?.colorSpace)
    }

    @Test
    fun theColorFormOpensInTheFirstSpace() = runComposeUiTest {
        // A Compose Color is sRGB whatever the user picked it in, so its space says nothing about where they were.
        setContent { ColorPickerDialog(initialColor = Color(0xFF3366CC), onColorSelected = {}, onDismissRequest = {}) }
        onNodeWithText("Okhsl").assertIsSelected()
    }

    @Test
    fun aDialogCanLeaveOutAlphaAndThePlane() = runComposeUiTest {
        setContent { ColorPickerDialog(initialValue = teal, onValueSelected = {}, onDismissRequest = {}, plane = null, alphaSlider = null) }
        onNodeWithText("Hue").assertExists()
        onNodeWithText("Alpha").assertDoesNotExist()
        onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions)).assertCountEquals(0)
    }

    @Test
    fun oneSpaceShowsNoSwitcher() = runComposeUiTest {
        setContent { ColorPickerDialog(initialValue = teal, onValueSelected = {}, onDismissRequest = {}, spaces = listOf(Okhsl)) }
        onAllNodes(radioButton).assertCountEquals(0)
        onNodeWithText("Okhsl").assertDoesNotExist()
    }

    @Test
    fun threeSpacesAreARowOfRadioButtons() = runComposeUiTest {
        setContent { ColorPickerDialog(initialValue = teal, onValueSelected = {}, onDismissRequest = {}, spaces = listOf(Okhsl, Hsl, Srgb)) }
        onAllNodes(radioButton).assertCountEquals(3)
        onNodeWithText("Okhsl").assertIsSelected()
        onNodeWithText("HSL").performClick()
        onNodeWithText("HSL").assertIsSelected()
        onNodeWithText("Okhsl").assertIsNotSelected()
    }

    @Test
    fun sevenSpacesAreAMenu() = runComposeUiTest {
        val spaces = listOf<ColorSpace>(Okhsl, OkLch, Oklab, Hsv, Hsl, Lab, Lch)
        setContent { ColorPickerDialog(initialValue = teal, onValueSelected = {}, onDismissRequest = {}, spaces = spaces) }
        onAllNodes(radioButton).assertCountEquals(0)
        onNodeWithText("Okhsl").performClick()
        onNodeWithText("LCH").performClick()
        onNodeWithText("LCH").assertExists()
        onNodeWithText("Chroma").assertExists()
        onNodeWithText("Okhsl").assertDoesNotExist()
    }

    @Test
    fun theMenuIsAListOfRadioButtons() = runComposeUiTest {
        val spaces = listOf<ColorSpace>(Okhsl, OkLch, Oklab, Hsv, Hsl, Lab, Lch)
        setContent { ColorPickerDialog(initialValue = teal, onValueSelected = {}, onDismissRequest = {}, spaces = spaces) }
        onNodeWithText("Okhsl")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.DropdownList))
            .performClick()
        onAllNodes(radioButton).assertCountEquals(7)
        onNode(radioButton and hasText("Okhsl")).assertIsSelected()
        onNode(radioButton and hasText("LCH")).assertIsNotSelected()
        onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup)).assertCountEquals(1)
    }

    @Test
    fun aDisabledMenuDoesNotOpen() = runComposeUiTest {
        val spaces = listOf<ColorSpace>(Okhsl, OkLch, Oklab, Hsv, Hsl, Lab, Lch)
        setContent { ColorPickerDialog(initialValue = teal, onValueSelected = {}, onDismissRequest = {}, spaces = spaces, enabled = false) }
        onNodeWithText("Okhsl").assertIsNotEnabled().performClick()
        onAllNodes(radioButton).assertCountEquals(0)
    }

    @Test
    fun theDialogIsAnnouncedByItsTitle() = runComposeUiTest {
        setContent { ColorPickerDialog(initialValue = teal, onValueSelected = {}, onDismissRequest = {}) }
        onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Select color")).assertExists()
        onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Dialog")).assertCountEquals(0)
    }

    @Test
    fun theHeaderShowsTheColorInHex() = runComposeUiTest {
        setContent { ColorPickerDialog(initialValue = Srgb(0x33 / 255.0, 0x66 / 255.0, 0x99 / 255.0), onValueSelected = {}, onDismissRequest = {}) }
        onNodeWithText("#336699").assertExists()
        sliderNamed("Alpha").performSemanticsAction(SemanticsActions.SetProgress) { it(0.5f) }
        onNode(SemanticsMatcher("a hex with alpha") { node -> node.config.getOrNull(SemanticsProperties.Text).orEmpty().any { Regex("#336699[0-9A-F]{2}").matches(it.text) } })
            .assertExists()
    }

    @Test
    fun theRestoreIsEnabledOnlyOnceTheColorIsEdited() = runComposeUiTest {
        setContent { ColorPickerDialog(initialValue = teal, onValueSelected = {}, onDismissRequest = {}) }
        onNodeWithContentDescription("Original color")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsNotEnabled()
        sliderNamed("Hue").performSemanticsAction(SemanticsActions.SetProgress) { it(0.5f) }
        onNodeWithContentDescription("Original color").assertIsEnabled()
    }

    @Test
    fun aHeaderToldItIsDisabledRestoresNothing() = runComposeUiTest {
        var selected: ColorValue? = null
        setContent {
            ColorPickerDialog(
                initialValue = teal,
                onValueSelected = { selected = it },
                onDismissRequest = {},
                header = { ColorPickerDialogDefaults.Header(state, enabled = false) },
            )
        }
        sliderNamed("Hue").performSemanticsAction(SemanticsActions.SetProgress) { it(0.5f) }
        onNodeWithContentDescription("Original color").assertIsNotEnabled().performClick()
        onNodeWithText("OK").performClick()
        assertNotEquals(teal, selected)
    }

    @Test
    fun aDisabledDialogStillCloses() = runComposeUiTest {
        var selected: ColorValue? = null
        setContent { ColorPickerDialog(initialValue = teal, onValueSelected = { selected = it }, onDismissRequest = {}, enabled = false) }
        sliderNamed("Hue").assertIsNotEnabled()
        onNodeWithText("HSV").assertIsNotEnabled()
        onNodeWithText("OK").assertIsEnabled().performClick()
        assertSame(teal, selected)
    }

    @Test
    fun aSlotConfirmsThroughTheScope() = runComposeUiTest {
        var selected: ColorValue? = null
        setContent {
            ColorPickerDialog(
                initialValue = teal,
                onValueSelected = { selected = it },
                onDismissRequest = {},
                confirmButton = { TextButton(onClick = { confirm() }, enabled = state.isModified) { Text("Apply") } },
            )
        }
        onNodeWithText("Apply").assertIsNotEnabled()
        sliderNamed("Hue").performSemanticsAction(SemanticsActions.SetProgress) { it(0.5f) }
        onNodeWithText("Apply").assertIsEnabled().performClick()
        assertNotEquals(teal, selected)
    }

    @Test
    fun aReplacedSlotInheritsTheDialogsTheme() = runComposeUiTest {
        setContent {
            ColorPickerDialog(
                initialValue = teal,
                onValueSelected = {},
                onDismissRequest = {},
                colors = ColorPickerDefaults.colors(checkerboardLight = Color.Red),
                header = { Text(if (ColorPickerDefaults.currentColors().checkerboardLight == Color.Red) "themed" else "unthemed") },
            )
        }
        onNodeWithText("themed").assertExists()
    }

    @Test
    fun aSlotLeftOutIsNotDrawn() = runComposeUiTest {
        setContent {
            ColorPickerDialog(initialValue = teal, onValueSelected = {}, onDismissRequest = {}, title = null, header = null, spaceSwitcher = null, dismissButton = null)
        }
        onNodeWithText("Select color").assertDoesNotExist()
        onNodeWithContentDescription("Original color").assertDoesNotExist()
        onAllNodes(radioButton).assertCountEquals(0)
        onNodeWithText("Cancel").assertDoesNotExist()
        onNodeWithText("OK").assertExists()
    }
}
