package codes.side.colorpicker.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import codes.side.color.ColorChannel
import codes.side.color.Hsl
import codes.side.colorpicker.foundation.ColorPickerStrings
import codes.side.colorpicker.foundation.PlaneActionLabels
import codes.side.colorpicker.foundation.ProvideColorPickerStrings
import codes.side.colorpicker.foundation.visible
import codes.side.colorpicker.foundation.withDefaultLocale
import codes.side.colorpicker.state.ColorPickerState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class StringsInComponentsTest {

    private object Custom : ColorPickerStrings {
        @Composable
        override fun channelSpokenName(channel: ColorChannel): String = "‹${channel.id}›"

        @Composable
        override fun alphaName(): String = "Opacity"

        @Composable
        override fun planeDescription(x: ColorChannel, y: ColorChannel): String = "Field"

        @Composable
        override fun dialogTitle(): String = "Choose"
    }

    @Test
    fun providedStringsReachEverySliderAndThePlane() = runComposeUiTest {
        setContent { ProvideColorPickerStrings(Custom) { ColorPicker(ColorPickerState(Hsl(200.0, 40.0, 50.0)), space = Hsl) } }
        val sliders = onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).fetchSemanticsNodes()
            .map { it.config[SemanticsProperties.ContentDescription].single() }
        assertEquals(listOf("‹h›", "‹s›", "‹l›", "Opacity"), sliders)
        // The alpha slider's label above its track is the same word.
        onAllNodesWithText("Opacity").assertCountEquals(1)
        val plane = onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions)).fetchSemanticsNode()
        assertEquals("Field", plane.config[SemanticsProperties.ContentDescription].single())
    }

    // Says which member each text came from, and whether it was asked for the screen reader.
    private object Wording : ColorPickerStrings {
        @Composable
        override fun channelName(channel: ColorChannel): String = "name ${channel.id}"

        @Composable
        override fun channelValue(channel: ColorChannel, value: Double, forAccessibility: Boolean): String =
            if (forAccessibility) "spoken ${channel.id}" else "shown ${channel.id}"

        @Composable
        override fun planeValue(x: ColorChannel, xValue: Double, y: ColorChannel, yValue: Double): String = "at ${x.id} ${y.id}"

        @Composable
        override fun planeActions(x: ColorChannel, y: ColorChannel): PlaneActionLabels =
            PlaneActionLabels(increaseX = "more ${x.id}", decreaseX = "less ${x.id}", increaseY = "more ${y.id}", decreaseY = "less ${y.id}")
    }

    @Test
    fun providedWordsReachTheLabelsTheValuesAndThePlanesActions() = runComposeUiTest {
        val state = ColorPickerState(Hsl(200.0, 40.0, 60.0))
        setContent {
            ProvideColorPickerStrings(Wording) {
                Column {
                    ChannelSlider(state, Hsl.S, Modifier.testTag("slider"))
                    ChannelPlane(state, Hsl.S, Hsl.L, Modifier.testTag("plane"))
                }
            }
        }
        onNodeWithText("name s").assertExists()
        onNodeWithText("shown s").assertExists()
        assertEquals("spoken s", sliderIn("slider").fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        val plane = onNodeWithTag("plane").fetchSemanticsNode().config
        assertEquals("at s l", plane[SemanticsProperties.StateDescription])
        assertEquals(listOf("more s", "less s", "more l", "less l"), plane[SemanticsActions.CustomActions].map { it.label })
    }

    @Test
    fun anArgumentBeatsTheProvidedStrings() = runComposeUiTest {
        setContent {
            ProvideColorPickerStrings(Custom) {
                ChannelSlider(ColorPickerState(Hsl(200.0, 40.0, 50.0)), Hsl.H, Modifier.testTag("slider"), semanticLabel = "Argument")
            }
        }
        assertEquals("Argument", sliderIn("slider").fetchSemanticsNode().config[SemanticsProperties.ContentDescription].single())
    }

    @Test
    fun theDialogTakesItsWordsFromTheStrings() = runComposeUiTest {
        setContent {
            ProvideColorPickerStrings(Custom) {
                ColorPickerDialog(initialValue = Hsl(200.0, 40.0, 50.0), onValueSelected = {}, onDismiss = {})
            }
        }
        onNodeWithText("Choose").assertExists()
        onNodeWithText("OK").assertExists()
        onNodeWithText("Cancel").assertExists()
    }

    @Test
    fun theDialogReadsSelectColorAndOkByDefault() = runComposeUiTest {
        setContent { ColorPickerDialog(initialValue = Hsl(200.0, 40.0, 50.0), onValueSelected = {}, onDismiss = {}) }
        onNodeWithText("Select color").assertExists()
        onNodeWithText("OK").assertExists()
    }

    @Test
    fun theGenericSliderAnnouncesItsPosition() = runComposeUiTest {
        setContent {
            ColorSlider(
                value = 0.37f,
                onValueChange = {},
                trackColors = listOf(Color.Black, Color.White),
                thumbColor = Color.Gray,
                modifier = Modifier.testTag("slider"),
            )
        }
        assertEquals("37%", sliderIn("slider").fetchSemanticsNode().config[SemanticsProperties.StateDescription])
    }

    @Test
    fun aDeviceInEgyptianArabicReadsItsOwnDigits() = withDefaultLocale("ar-EG") {
        runComposeUiTest {
            val state = ColorPickerState(Hsl(200.0, 40.0, 60.0))
            setContent {
                Column {
                    ChannelSlider(state, Hsl.S, Modifier.testTag("slider"))
                    ChannelPlane(state, Hsl.S, Hsl.L, Modifier.testTag("plane"))
                }
            }
            val spoken = sliderIn("slider").fetchSemanticsNode().config[SemanticsProperties.StateDescription]
            assertEquals("٤٠٪", spoken.visible())
            // The value label above the track, not only what a screen reader hears.
            val labels = onAllNodes(hasAnyAncestor(hasTestTag("slider")) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Text))
                .fetchSemanticsNodes().map { node -> node.config[SemanticsProperties.Text].joinToString("") { it.text }.visible() }
            assertTrue("٤٠٪" in labels, "the slider's labels read $labels")
            val plane = onNodeWithTag("plane").fetchSemanticsNode().config[SemanticsProperties.StateDescription]
            assertEquals("٤٠٪ saturation, ٦٠٪ lightness", plane.visible())
        }
    }
}
