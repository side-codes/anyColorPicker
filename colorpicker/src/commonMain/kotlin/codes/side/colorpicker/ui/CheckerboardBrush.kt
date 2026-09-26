package codes.side.colorpicker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import codes.side.colorpicker.foundation.checkerboardBrush

internal val CheckerboardCellSize = 6.dp

/** Returns a [Brush] that tiles a transparency checkerboard pattern; see [checkerboardBrush]. */
@Composable
internal fun rememberCheckerboardBrush(
    cellSize: Dp,
    light: Color,
    dark: Color,
): Brush {
    val cellSizePx = with(LocalDensity.current) { cellSize.roundToPx() }.coerceAtLeast(1)
    return remember(cellSizePx, light, dark) { checkerboardBrush(cellSizePx, light, dark) }
}
