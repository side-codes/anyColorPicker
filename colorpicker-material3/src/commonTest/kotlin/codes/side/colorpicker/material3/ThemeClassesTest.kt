package codes.side.colorpicker.material3

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

class ThemeClassesTest {

    private val colors = ColorPickerColors(Color.Red, Color.Blue, 0.5f, 0.25f)
    private val shapes = ColorPickerShapes(RectangleShape, CircleShape, RectangleShape)
    private val dimensions = ColorPickerDimensions(16.dp, 4.dp, 6.dp, 200.dp, 24.dp)

    @Test
    fun aColorsCopyChangesWhatItIsGivenAndKeepsAnUnspecifiedColor() {
        assertEquals(ColorPickerColors(Color.Red, Color.Green, 0.5f, 0.25f), colors.copy(checkerboardDark = Color.Green))
        assertEquals(ColorPickerColors(Color.Red, Color.Blue, 1f, 0.25f), colors.copy(disabledAlpha = 1f))
        assertEquals(colors, colors.copy(checkerboardLight = Color.Unspecified))
    }

    @Test
    fun aShapesCopyKeepsANullShape() {
        assertEquals(shapes, shapes.copy(swatchShape = null))
        assertSame(CircleShape, shapes.copy(planeShape = CircleShape).planeShape)
    }

    @Test
    fun aDimensionsCopyKeepsAnUnspecifiedSizeAndTakesZero() {
        assertEquals(dimensions, dimensions.copy(trackHeight = Dp.Unspecified))
        assertEquals(0.dp, dimensions.copy(thumbWidth = 0.dp).thumbWidth)
    }

    @Test
    fun equalValuesAreEqualAndHashAlike() {
        val sameColors = ColorPickerColors(Color.Red, Color.Blue, 0.5f, 0.25f)
        assertEquals(colors, sameColors)
        assertEquals(colors.hashCode(), sameColors.hashCode())
        assertNotEquals(colors, colors.copy(disabledSaturation = 1f))
        val sameShapes = ColorPickerShapes(RectangleShape, CircleShape, RectangleShape)
        assertEquals(shapes, sameShapes)
        assertEquals(shapes.hashCode(), sameShapes.hashCode())
        assertNotEquals(shapes, shapes.copy(trackShape = CircleShape))
        val sameDimensions = ColorPickerDimensions(16.dp, 4.dp, 6.dp, 200.dp, 24.dp)
        assertEquals(dimensions, sameDimensions)
        assertEquals(dimensions.hashCode(), sameDimensions.hashCode())
        assertNotEquals(dimensions, dimensions.copy(planeMinSize = 100.dp))
    }
}
