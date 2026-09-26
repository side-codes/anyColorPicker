package codes.side.colorpicker.ui

import androidx.compose.runtime.Immutable

/**
 * What a screen reader calls the four directions a [ColorPlane] can be moved in.
 *
 * A plane has two degrees of freedom and the adjustable semantics a slider uses carry one, so
 * the way to move it without a pointer is a set of named actions. The defaults come from
 * [codes.side.colorpicker.foundation.ColorPickerStrings]: a [ChannelPlane]'s name its channels, and
 * a [ColorPlane]'s its axes, since it does not know what it is showing.
 */
@Immutable
public class PlaneActionLabels(
    public val increaseX: String,
    public val decreaseX: String,
    public val increaseY: String,
    public val decreaseY: String,
) {
    // Not a data class, for the same reason the color models are not: componentN and copy are
    // part of the ABI, so a fifth direction would break code already compiled against four.

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlaneActionLabels) return false
        return increaseX == other.increaseX &&
                decreaseX == other.decreaseX &&
                increaseY == other.increaseY &&
                decreaseY == other.decreaseY
    }

    override fun hashCode(): Int {
        var result = increaseX.hashCode()
        result = 31 * result + decreaseX.hashCode()
        result = 31 * result + increaseY.hashCode()
        result = 31 * result + decreaseY.hashCode()
        return result
    }

    override fun toString(): String =
        "PlaneActionLabels(increaseX=$increaseX, decreaseX=$decreaseX, increaseY=$increaseY, decreaseY=$decreaseY)"
}
