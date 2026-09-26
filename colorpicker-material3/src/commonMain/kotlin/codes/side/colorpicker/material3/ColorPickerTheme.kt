package codes.side.colorpicker.material3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf

/**
 * Colors every picker component below this point uses unless given some of its own.
 *
 * `null` means nothing has been provided and the component falls back to
 * [ColorPickerDefaults.colors], which reads `MaterialTheme` at the point of use. Every
 * component reads this in a parameter default rather than in its body, so an explicit
 * argument always wins over what an ancestor provided.
 */
internal val LocalColorPickerColors: ProvidableCompositionLocal<ColorPickerColors?> =
    compositionLocalOf { null }

/** Shapes every picker component below this point uses; see [LocalColorPickerColors]. */
internal val LocalColorPickerShapes: ProvidableCompositionLocal<ColorPickerShapes?> =
    compositionLocalOf { null }

/** Dimensions every picker component below this point uses; see [LocalColorPickerColors]. */
internal val LocalColorPickerDimensions: ProvidableCompositionLocal<ColorPickerDimensions?> =
    compositionLocalOf { null }

/**
 * Provides [colors], [shapes] and [dimensions] to everything in [content].
 *
 * Wrap a screen in this to style every picker on it at once. A ready-made picker does the same
 * for its own sliders, which is what lets a replaced slider slot inherit them without the call
 * site forwarding anything.
 */
@Composable
public fun ColorPickerTheme(
    colors: ColorPickerColors = ColorPickerDefaults.currentColors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.currentShapes(),
    dimensions: ColorPickerDimensions = ColorPickerDefaults.currentDimensions(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalColorPickerColors provides colors,
        LocalColorPickerShapes provides shapes,
        LocalColorPickerDimensions provides dimensions,
        content = content,
    )
}
