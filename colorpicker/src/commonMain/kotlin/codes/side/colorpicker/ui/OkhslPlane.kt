package codes.side.colorpicker.ui

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import codes.side.colorpicker.conversion.okhslRowFiller
import codes.side.colorpicker.conversion.toComposeColor
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.theme.ColorPickerDefaults
import codes.side.colorpicker.theme.ColorPickerShapes

/**
 * Two-dimensional saturation and lightness picker for the hue currently held by [state], in
 * Okhsl.
 *
 * The perceptual counterpart to [HslPlane], and the arrangement Okhsl was designed for.
 * Lightness is perceived lightness, so a row of this surface looks equally light all the way
 * across, and saturation is measured against the display gamut, so the right edge is the most
 * colourful the hue can reach at that lightness rather than a region that runs off what the
 * screen can show. Dragging writes both channels at once; hue and alpha are left alone, so an
 * [OkhslHueSlider] and an [AlphaSlider] compose with this to make a full picker.
 *
 * The field is sampled rather than interpolated from a pair of gradients, because Okhsl is
 * linear in neither axis. What survives sampling is a seam along the lightness where the
 * gamut turns its corner, worst about 35 of 255 over roughly an eighth of a percent of the
 * surface; see the row count this plane is rasterized at. The hue slider makes a related
 * trade for the same reason: its independent track is drawn at a fixed saturation, off the
 * gamut boundary.
 *
 * @param semanticLabel accessibility description of the surface; pass a localized string to
 * replace the English default, or `null` to omit.
 * @param semanticValueText accessibility announcement of the current pair of values.
 * @param thumb optional replacement for the position indicator; see [ColorPlane].
 */
@Composable
public fun OkhslPlane(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    semanticLabel: String? = "Saturation and lightness",
    semanticValueText: String? =
        "${state.okhslColor.intSaturation}% saturation, ${state.okhslColor.intLightness}% lightness",
    shapes: ColorPickerShapes = ColorPickerDefaults.shapes(),
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
) {
    val okhsl = state.okhslColor
    val interaction = remember(state) { SliderInteractionGuard(state) }
    val field = remember(okhsl.hue) { okhslRowFiller(okhsl.hue) }
    val bitmap = rememberPlaneBitmap(
        key = okhsl.hue,
        width = OK_PLANE_COLUMNS,
        height = OKHSL_PLANE_ROWS,
        fillRow = field,
    )

    ColorPlane(
        xValue = okhsl.saturation,
        yValue = okhsl.lightness,
        onValueChange = { x, y ->
            interaction.begin()
            state.updateFromOkhsl(state.okhslColor.copy(saturation = x, lightness = y))
        },
        surface = { drawPlaneBitmap(bitmap) },
        modifier = modifier,
        onValueChangeFinished = { interaction.end() },
        semanticLabel = semanticLabel,
        semanticValueText = semanticValueText,
        shapes = shapes,
        thumb = thumb,
    )
}
