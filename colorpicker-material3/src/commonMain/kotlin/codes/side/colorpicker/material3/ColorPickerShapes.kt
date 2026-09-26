package codes.side.colorpicker.material3

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape

/**
 * Shapes used by color picker components. Obtain instances via
 * [ColorPickerDefaults.shapes] so defaults come from a single source.
 *
 * @property trackShape outer shape of a slider's gradient track.
 * @property swatchShape shape of a [ColorSwatch].
 * @property planeShape shape of a [ColorPlane]'s
 * surface. The position indicator is drawn outside it, so it stays whole at the edges.
 */
@Immutable
public class ColorPickerShapes(
    public val trackShape: Shape,
    public val swatchShape: Shape,
    public val planeShape: Shape,
) {
    // Not a data class: a field added later would break componentN, and a hand-written copy can keep
    // its old overload beside a new one.

    /** A copy with the given shapes; a shape left `null` keeps this one's. */
    public fun copy(
        trackShape: Shape? = this.trackShape,
        swatchShape: Shape? = this.swatchShape,
        planeShape: Shape? = this.planeShape,
    ): ColorPickerShapes = ColorPickerShapes(
        trackShape = trackShape ?: this.trackShape,
        swatchShape = swatchShape ?: this.swatchShape,
        planeShape = planeShape ?: this.planeShape,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ColorPickerShapes) return false
        return trackShape == other.trackShape &&
                swatchShape == other.swatchShape &&
                planeShape == other.planeShape
    }

    override fun hashCode(): Int {
        var result = trackShape.hashCode()
        result = 31 * result + swatchShape.hashCode()
        result = 31 * result + planeShape.hashCode()
        return result
    }

    override fun toString(): String =
        "ColorPickerShapes(trackShape=$trackShape, swatchShape=$swatchShape, planeShape=$planeShape)"
}
