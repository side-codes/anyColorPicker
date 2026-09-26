package codes.side.colorpicker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import codes.side.colorpicker.theme.ColorPickerDefaults

// Material's slider handle height (SliderTokens.HandleHeight).
private val SliderThumbHeight = 44.dp

/**
 * The sliders' default thumb, drawn as Material draws its handle: a bar in [color] with round ends, half
 * as wide while pressed or dragged. The narrowing is drawn inside a fixed layout width, so the track
 * beside the thumb does not move as it narrows.
 */
@Composable
internal fun SliderThumb(interactionSource: InteractionSource, color: Color, modifier: Modifier = Modifier) {
    val pressed by interactionSource.collectIsPressedAsState()
    val dragged by interactionSource.collectIsDraggedAsState()
    Canvas(modifier.size(ColorPickerDefaults.ThumbWidth, SliderThumbHeight)) {
        val width = if (pressed || dragged) size.width / 2f else size.width
        drawRoundRect(
            color = color,
            topLeft = Offset((size.width - width) / 2f, 0f),
            size = Size(width, size.height),
            cornerRadius = CornerRadius(width / 2f),
        )
    }
}
