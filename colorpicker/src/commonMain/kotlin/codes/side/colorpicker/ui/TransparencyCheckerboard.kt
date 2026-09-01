package codes.side.colorpicker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import codes.side.colorpicker.theme.ColorPickerColors
import codes.side.colorpicker.theme.ColorPickerDefaults

/** Fills the available size with a tiled transparency checkerboard pattern. */
@Composable
internal fun TransparencyCheckerboard(
    modifier: Modifier = Modifier,
    cellSize: Dp = CheckerboardCellSize,
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
) {
    val brush = rememberCheckerboardBrush(
        cellSize = cellSize,
        light = colors.checkerboardLight,
        dark = colors.checkerboardDark,
    )
    Canvas(modifier = modifier) {
        drawRect(brush = brush)
    }
}
