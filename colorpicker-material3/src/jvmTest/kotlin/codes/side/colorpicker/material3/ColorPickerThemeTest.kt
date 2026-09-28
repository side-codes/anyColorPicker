package codes.side.colorpicker.material3

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ColorPickerThemeTest {

    @Test
    fun aThemeGivenOnlyDimensionsLeavesTheColorsToANestedMaterialTheme() = runComposeUiTest {
        val dark = darkColorScheme()
        lateinit var colors: ColorPickerColors
        lateinit var dimensions: ColorPickerDimensions
        setContent {
            MaterialTheme(lightColorScheme()) {
                ColorPickerTheme(dimensions = ColorPickerDefaults.dimensions(trackHeight = 24.dp)) {
                    MaterialTheme(dark) {
                        colors = ColorPickerDefaults.currentColors()
                        dimensions = ColorPickerDefaults.currentDimensions()
                    }
                }
            }
        }
        assertEquals(dark.surfaceBright, colors.checkerboardLight, "the dark card's checkerboard, not the light theme's")
        assertEquals(24.dp, dimensions.trackHeight)
    }

    @Test
    fun aNestedThemeKeepsWhatItDoesNotSet() = runComposeUiTest {
        lateinit var colors: ColorPickerColors
        lateinit var dimensions: ColorPickerDimensions
        setContent {
            ColorPickerTheme(colors = ColorPickerDefaults.colors(checkerboardLight = Color.White, checkerboardDark = Color.Black)) {
                ColorPickerTheme(dimensions = ColorPickerDefaults.dimensions(trackHeight = 24.dp)) {
                    colors = ColorPickerDefaults.currentColors()
                    dimensions = ColorPickerDefaults.currentDimensions()
                }
            }
        }
        assertEquals(Color.White, colors.checkerboardLight)
        assertEquals(Color.Black, colors.checkerboardDark)
        assertEquals(24.dp, dimensions.trackHeight)
    }
}
