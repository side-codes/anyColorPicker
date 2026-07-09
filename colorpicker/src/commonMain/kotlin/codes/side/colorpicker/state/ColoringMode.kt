package codes.side.colorpicker.state

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
enum class ColoringMode {
    Independent,
    Contextual,
}
