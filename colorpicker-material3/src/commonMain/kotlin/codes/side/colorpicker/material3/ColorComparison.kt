package codes.side.colorpicker.material3

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import codes.side.colorpicker.foundation.BasicColorComparison
import codes.side.colorpicker.foundation.ColorPickerStrings

/**
 * One swatch split in two, [original] in the start half and [current] in the end half, in the theme's swatch
 * shape over its transparency checkerboard: a [BasicColorComparison] drawn as a [ColorSwatch] is.
 *
 * Falls back to [ColorPickerDefaults.SwatchSize] tall and twice that wide when [modifier] gives it no size.
 *
 * @param onRestoreOriginal called when the original half is pressed, which makes that half a button that puts
 * the original back; `null` leaves it a swatch.
 * @param enabled when false the original half, when it is a button, is a disabled one. The colors are drawn as
 * they are either way, since a dimmed swatch would show colors other than the ones compared.
 * @param colors checkerboard colors; see [ColorPickerDefaults.colors].
 * @param originalLabel what a screen reader calls the original half; `null` says nothing.
 * @param currentLabel what a screen reader calls the current half; `null` says nothing.
 * @param restoreLabel what a screen reader says pressing the original half does.
 */
@Composable
public fun ColorComparison(
    original: Color,
    current: Color,
    onRestoreOriginal: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ColorPickerDefaults.currentShapes().swatchShape,
    colors: ColorPickerColors = ColorPickerDefaults.currentColors(),
    originalLabel: String? = ColorPickerStrings.current.originalColor(),
    currentLabel: String? = ColorPickerStrings.current.newColor(),
    restoreLabel: String? = ColorPickerStrings.current.restoreOriginal(),
) {
    BasicColorComparison(
        original = original,
        current = current,
        onRestoreOriginal = onRestoreOriginal,
        modifier = modifier
            .defaultMinSize(minWidth = ColorPickerDefaults.SwatchSize * 2, minHeight = ColorPickerDefaults.SwatchSize)
            .clip(shape),
        enabled = enabled,
        checkerboardLight = colors.checkerboardLight,
        checkerboardDark = colors.checkerboardDark,
        originalLabel = originalLabel,
        currentLabel = currentLabel,
        restoreLabel = restoreLabel,
    )
}
