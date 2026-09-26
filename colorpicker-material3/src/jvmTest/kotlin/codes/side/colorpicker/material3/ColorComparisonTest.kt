package codes.side.colorpicker.material3

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ColorComparisonTest {

    @Test
    fun givenNoSizeItIsASwatchTallAndTwiceAsWide() = runComposeUiTest {
        setContent { Column { ColorComparison(Color.Red, Color.Blue, onRestoreOriginal = null, Modifier.testTag("comparison")) } }
        onNodeWithTag("comparison").assertHeightIsEqualTo(ColorPickerDefaults.SwatchSize)
        onNodeWithTag("comparison").assertWidthIsEqualTo(ColorPickerDefaults.SwatchSize * 2)
    }

    @Test
    fun itDrawsTheThemesCheckerboard() = runComposeUiTest {
        setContent {
            ColorPickerTheme(colors = ColorPickerDefaults.colors(checkerboardLight = Color.White, checkerboardDark = Color.Black)) {
                ColorComparison(Color.Red, Color.Transparent, onRestoreOriginal = null, Modifier.testTag("comparison"))
            }
        }
        val pixels = onNodeWithTag("comparison").captureToImage().toPixelMap()
        val cells = (pixels.width / 2 until pixels.width - 8).map { pixels[it, pixels.height / 2] }.toSet()
        assertTrue(Color.White in cells && Color.Black in cells, "the end half shows the theme's cells, found $cells")
    }
}
