package codes.side.colorpicker.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp

/**
 * Dimensions used by color picker components. Obtain instances via
 * [ColorPickerDefaults.dimensions] so defaults come from a single source.
 *
 * These travel with the theme rather than as parameters because they apply to every slider a
 * picker draws. A caller wanting a taller track sets it once here instead of on each of the
 * twenty-one channel sliders.
 *
 * @property trackHeight height of a slider's gradient track.
 * @property thumbWidth width the track reserves for the thumb. A custom thumb wider than this
 * must say so, or the track's gap closes underneath it.
 * @property thumbTrackGap clearance left between the thumb and each end of the track.
 * @property planeMinSize size a [codes.side.colorpicker.ui.ColorPlane] falls back to.
 * @property planeThumbSize diameter of a [codes.side.colorpicker.ui.ColorPlane]'s indicator.
 */
@Immutable
public data class ColorPickerDimensions(
    public val trackHeight: Dp,
    public val thumbWidth: Dp,
    public val thumbTrackGap: Dp,
    public val planeMinSize: Dp,
    public val planeThumbSize: Dp,
)
