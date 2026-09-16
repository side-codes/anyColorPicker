package codes.side.colorpicker.ui

import androidx.compose.runtime.Immutable

/**
 * What a screen reader calls the four directions a [ColorPlane] can be moved in.
 *
 * A plane has two degrees of freedom and the adjustable semantics a slider uses carry one, so
 * the way to move it without a pointer is a set of named actions. The names are read aloud, so
 * an app in another language passes its own; the defaults here name the axis rather than the
 * channel, because [ColorPlane] does not know what it is showing.
 */
@Immutable
public data class PlaneActionLabels(
    public val increaseX: String,
    public val decreaseX: String,
    public val increaseY: String,
    public val decreaseY: String,
) {
    public companion object {
        /** Axis names, for a plane whose channels are not known. */
        public val Default: PlaneActionLabels = PlaneActionLabels(
            increaseX = "Increase horizontally",
            decreaseX = "Decrease horizontally",
            increaseY = "Increase vertically",
            decreaseY = "Decrease vertically",
        )
    }
}
