package codes.side.colorpicker.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape

/**
 * Shapes used by color picker components. Obtain instances via
 * [ColorPickerDefaults.shapes] so defaults come from a single source.
 *
 * @property trackShape outer shape of a slider's gradient track.
 * @property swatchShape shape of a [codes.side.colorpicker.ui.ColorSwatch].
 */
@Immutable
public data class ColorPickerShapes(
    public val trackShape: Shape,
    public val swatchShape: Shape,
)
