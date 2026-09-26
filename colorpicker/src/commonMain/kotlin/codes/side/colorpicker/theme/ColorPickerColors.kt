package codes.side.colorpicker.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse

/**
 * Colors used by color picker components. Obtain instances via
 * [ColorPickerDefaults.colors] so defaults come from a single source.
 *
 * @property checkerboardLight color of the light cells of the transparency checkerboard.
 * @property checkerboardDark color of the dark cells of the transparency checkerboard.
 * @property disabledAlpha opacity a disabled control draws at, `1f` to leave it alone. One value
 * rather than a parallel set of disabled colors: a picker's track is a gradient of the colors
 * being chosen, so there is no fixed color to swap it for.
 * @property disabledSaturation how much colour a disabled control keeps, `0f` for grey and `1f`
 * to leave it alone. Dimming shows paler versions of real colours and composites against a
 * background the library does not own; draining the colour does neither. Set both, either, or
 * neither.
 */
@Immutable
public class ColorPickerColors(
    public val checkerboardLight: Color,
    public val checkerboardDark: Color,
    public val disabledAlpha: Float,
    public val disabledSaturation: Float,
) {
    // Not a data class: a field added later would break componentN, and a hand-written copy can keep
    // its old overload beside a new one.

    /** A copy with the given values; a color left [Color.Unspecified] keeps this one's. */
    public fun copy(
        checkerboardLight: Color = this.checkerboardLight,
        checkerboardDark: Color = this.checkerboardDark,
        disabledAlpha: Float = this.disabledAlpha,
        disabledSaturation: Float = this.disabledSaturation,
    ): ColorPickerColors = ColorPickerColors(
        checkerboardLight = checkerboardLight.takeOrElse { this.checkerboardLight },
        checkerboardDark = checkerboardDark.takeOrElse { this.checkerboardDark },
        disabledAlpha = disabledAlpha,
        disabledSaturation = disabledSaturation,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ColorPickerColors) return false
        // compareTo, as a data class compares its floats: -0 and 0 differ, as their hash codes do.
        return checkerboardLight == other.checkerboardLight &&
                checkerboardDark == other.checkerboardDark &&
                disabledAlpha.compareTo(other.disabledAlpha) == 0 &&
                disabledSaturation.compareTo(other.disabledSaturation) == 0
    }

    override fun hashCode(): Int {
        var result = checkerboardLight.hashCode()
        result = 31 * result + checkerboardDark.hashCode()
        result = 31 * result + disabledAlpha.hashCode()
        result = 31 * result + disabledSaturation.hashCode()
        return result
    }

    override fun toString(): String =
        "ColorPickerColors(checkerboardLight=$checkerboardLight, checkerboardDark=$checkerboardDark, disabledAlpha=$disabledAlpha, disabledSaturation=$disabledSaturation)"
}
