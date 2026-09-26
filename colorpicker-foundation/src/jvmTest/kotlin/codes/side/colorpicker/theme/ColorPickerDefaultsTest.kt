package codes.side.colorpicker.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

@OptIn(ExperimentalTestApi::class)
class ColorPickerDefaultsTest {

    @Test
    fun aPartialColorsCallKeepsTheSchemesOtherCell() = runComposeUiTest {
        var built: ColorPickerColors? = null
        var dim: Color? = null
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                built = ColorPickerDefaults.colors(checkerboardLight = Color.Red)
                dim = MaterialTheme.colorScheme.surfaceDim
            }
        }
        assertEquals(Color.Red, built?.checkerboardLight)
        assertEquals(dim, built?.checkerboardDark)
    }

    @Test
    fun anUnspecifiedColorTakesTheSchemes() = runComposeUiTest {
        var built: ColorPickerColors? = null
        var bright: Color? = null
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                built = ColorPickerDefaults.colors(checkerboardLight = Color.Unspecified)
                bright = MaterialTheme.colorScheme.surfaceBright
            }
        }
        assertEquals(bright, built?.checkerboardLight)
    }

    @Test
    fun aPartialShapesCallKeepsTheThemesOtherShapes() = runComposeUiTest {
        val small = RoundedCornerShape(3.dp)
        val medium = RoundedCornerShape(7.dp)
        var built: ColorPickerShapes? = null
        setContent {
            MaterialTheme(shapes = Shapes(small = small, medium = medium)) {
                built = ColorPickerDefaults.shapes(trackShape = RectangleShape, swatchShape = null)
            }
        }
        assertSame(RectangleShape, built?.trackShape)
        assertSame(small, built?.swatchShape)
        assertSame(medium, built?.planeShape)
    }

    @Test
    fun anUnspecifiedSizeTakesItsConstant() = runComposeUiTest {
        var built: ColorPickerDimensions? = null
        setContent { built = ColorPickerDefaults.dimensions(trackHeight = Dp.Unspecified, thumbWidth = 48.dp) }
        assertEquals(ColorPickerDefaults.TrackHeight, built?.trackHeight)
        assertEquals(48.dp, built?.thumbWidth)
        assertEquals(ColorPickerDefaults.PlaneMinSize, built?.planeMinSize)
    }
}
