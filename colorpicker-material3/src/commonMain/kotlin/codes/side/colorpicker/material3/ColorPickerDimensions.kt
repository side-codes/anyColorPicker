package codes.side.colorpicker.material3

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.takeOrElse

/**
 * Dimensions used by color picker components. Obtain instances via
 * [ColorPickerDefaults.dimensions] so defaults come from a single source.
 *
 * Every slider and plane takes them as a parameter that defaults to the theme's, so a caller
 * wanting a taller track sets it once in a [ColorPickerTheme] rather than on each slider a picker
 * draws.
 *
 * @property trackHeight height of a slider's gradient track.
 * @property thumbWidth width the track reserves for the thumb. A custom thumb wider than this
 * must say so, or the track's gap closes underneath it.
 * @property thumbTrackGap clearance left between the thumb and each end of the track.
 * @property planeMinSize size a [ColorPlane] falls back to.
 * @property planeThumbSize diameter of a [ColorPlane]'s indicator.
 */
@Immutable
public class ColorPickerDimensions(
    public val trackHeight: Dp,
    public val thumbWidth: Dp,
    public val thumbTrackGap: Dp,
    public val planeMinSize: Dp,
    public val planeThumbSize: Dp,
) {
    // Not a data class: a field added later would break componentN, and a hand-written copy can keep
    // its old overload beside a new one.

    /** A copy with the given sizes; a size left [Dp.Unspecified] keeps this one's. */
    public fun copy(
        trackHeight: Dp = this.trackHeight,
        thumbWidth: Dp = this.thumbWidth,
        thumbTrackGap: Dp = this.thumbTrackGap,
        planeMinSize: Dp = this.planeMinSize,
        planeThumbSize: Dp = this.planeThumbSize,
    ): ColorPickerDimensions = ColorPickerDimensions(
        trackHeight = trackHeight.takeOrElse { this.trackHeight },
        thumbWidth = thumbWidth.takeOrElse { this.thumbWidth },
        thumbTrackGap = thumbTrackGap.takeOrElse { this.thumbTrackGap },
        planeMinSize = planeMinSize.takeOrElse { this.planeMinSize },
        planeThumbSize = planeThumbSize.takeOrElse { this.planeThumbSize },
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ColorPickerDimensions) return false
        return trackHeight == other.trackHeight &&
                thumbWidth == other.thumbWidth &&
                thumbTrackGap == other.thumbTrackGap &&
                planeMinSize == other.planeMinSize &&
                planeThumbSize == other.planeThumbSize
    }

    override fun hashCode(): Int {
        var result = trackHeight.hashCode()
        result = 31 * result + thumbWidth.hashCode()
        result = 31 * result + thumbTrackGap.hashCode()
        result = 31 * result + planeMinSize.hashCode()
        result = 31 * result + planeThumbSize.hashCode()
        return result
    }

    override fun toString(): String =
        "ColorPickerDimensions(trackHeight=$trackHeight, thumbWidth=$thumbWidth, thumbTrackGap=$thumbTrackGap, planeMinSize=$planeMinSize, planeThumbSize=$planeThumbSize)"
}
