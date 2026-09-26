package codes.side.colorpicker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import codes.side.colorpicker.foundation.checkerboard
import codes.side.colorpicker.theme.ColorPickerColors
import codes.side.colorpicker.theme.ColorPickerDefaults

/**
 * Displays [color] clipped to [shape], over a transparency checkerboard so
 * translucent colors read correctly.
 *
 * @param contentDescription optional accessibility description of the shown color;
 * when `null` the swatch is decorative.
 * @param colors checkerboard colors; see [ColorPickerDefaults.colors].
 *
 * Falls back to [ColorPickerDefaults.SwatchSize] when [modifier] specifies no size, as inside a
 * scrolling column or a lazy list.
 */
@Composable
public fun ColorSwatch(
    color: Color,
    modifier: Modifier = Modifier,
    shape: Shape = ColorPickerDefaults.currentShapes().swatchShape,
    contentDescription: String? = null,
    colors: ColorPickerColors = ColorPickerDefaults.currentColors(),
) {
    Box(
        modifier = modifier
            .defaultMinSize(
                minWidth = ColorPickerDefaults.SwatchSize,
                minHeight = ColorPickerDefaults.SwatchSize,
            )
            .clip(shape)
            .checkerboard(colors.checkerboardLight, colors.checkerboardDark)
            .background(color)
            .semantics {
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
            },
    )
}
