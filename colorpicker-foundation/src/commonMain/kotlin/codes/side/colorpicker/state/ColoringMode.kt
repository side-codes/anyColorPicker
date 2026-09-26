package codes.side.colorpicker.state

import codes.side.color.ColorSpace

/**
 * Controls how a slider's gradient track is rendered with respect to other channels.
 *
 * - [Independent]: the gradient shows the component's full theoretical range,
 *   independent of the other channels. E.g. the hue slider always shows the full
 *   rainbow regardless of saturation and lightness.
 *
 * - [Contextual]: the gradient shows what the resulting color would be at each
 *   position, contextual on the current values of the other channels. E.g. the
 *   hue slider shows the rainbow rendered at the current saturation and lightness.
 */
public enum class ColoringMode {
    /** Gradient shows the channel's full theoretical range, ignoring other channels. */
    Independent,

    /** Gradient shows the resulting color at each position given the other channels. */
    Contextual,
    ;

    public companion object {
        /**
         * The coloring a slider over [space] takes unless given another: [Independent] for a space with a
         * hue, whose other channels have anchors worth showing, and [Contextual] otherwise.
         */
        public fun defaultFor(space: ColorSpace): ColoringMode =
            if (space.channels.any { it.isHue }) Independent else Contextual
    }
}
