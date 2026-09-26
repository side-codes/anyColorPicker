package codes.side.colorpicker.material3

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import codes.side.color.Okhsl
import codes.side.colorpicker.state.ColorPickerState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ComponentDimensionsTest {

    private val seed = Okhsl(30.0, 0.5, 0.5)

    private fun ComposeUiTest.heightOf(tag: String): Float =
        onNodeWithTag(tag).getUnclippedBoundsInRoot().let { (it.bottom - it.top).value }

    @Test
    fun aSliderTakesItsTrackHeightFromItsDimensions() = runComposeUiTest {
        setContent {
            Column {
                Box(Modifier.testTag("default")) { ChannelSlider(ColorPickerState(seed), Okhsl.H) }
                Box(Modifier.testTag("raised")) {
                    ChannelSlider(ColorPickerState(seed), Okhsl.H, dimensions = ColorPickerDefaults.dimensions(trackHeight = 80.dp))
                }
            }
        }
        assertTrue(heightOf("raised") > heightOf("default"), "an 80dp track should make the slider taller: ${heightOf("raised")} vs ${heightOf("default")}")
    }

    @Test
    fun aPlaneTakesItsFallbackSizeFromItsDimensions() = runComposeUiTest {
        setContent {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                ChannelPlane(
                    ColorPickerState(seed),
                    Okhsl.S,
                    Okhsl.L,
                    Modifier.testTag("plane"),
                    dimensions = ColorPickerDefaults.dimensions(planeMinSize = 120.dp),
                )
            }
        }
        assertEquals(120f, heightOf("plane"), 0.5f)
    }

    @Test
    fun aCopyOfTheThemesDimensionsKeepsItsOtherSizes() = runComposeUiTest {
        setContent {
            ColorPickerTheme(dimensions = ColorPickerDefaults.dimensions(trackHeight = 80.dp)) {
                Column {
                    Box(Modifier.testTag("themed")) { ChannelSlider(ColorPickerState(seed), Okhsl.H) }
                    Box(Modifier.testTag("copied")) {
                        ChannelSlider(
                            ColorPickerState(seed),
                            Okhsl.H,
                            dimensions = ColorPickerDefaults.currentDimensions().copy(thumbWidth = 48.dp),
                        )
                    }
                }
            }
        }
        assertEquals(heightOf("themed"), heightOf("copied"), 0.5f, "a wider thumb gap keeps the theme's 80dp track")
    }
}
