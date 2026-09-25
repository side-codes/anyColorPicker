package codes.side.colorpicker.ui

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import codes.side.color.ColorChannel
import codes.side.color.ColorSpace
import codes.side.color.ColorSpaces
import codes.side.color.ColorValue
import codes.side.color.Okhsl
import codes.side.color.compose.toColorValue
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.ColoringMode
import codes.side.colorpicker.theme.ColorPickerColors
import codes.side.colorpicker.theme.ColorPickerDefaults
import codes.side.colorpicker.theme.ColorPickerShapes

/**
 * An [AlertDialog] hosting a [ColorPicker] for [space], with a swatch of the color and confirm and
 * dismiss buttons.
 *
 * The dialog owns its picker state, saved with [initialValue] as the key: an edit in progress survives
 * configuration changes and process death, and a different [initialValue] starts the dialog over from
 * it.
 *
 * @param onValueSelected called with the color when the confirm button is pressed: in [space] once a
 * channel has moved, and exactly [initialValue] if nothing did. The caller dismisses the dialog.
 * @param onDismiss called when the user cancels or dismisses the dialog.
 * @param space the picker's space; Okhsl by default.
 * @param title dialog title; [confirmText] and [dismissText] label the buttons. Pass localized strings
 * to replace the English defaults.
 * @param enabled when false the picker is dimmed and refuses input. The buttons stay live, so the dialog
 * can still be dismissed.
 * @param colors checkerboard and disabled colors, used by the swatch and the picker; see
 * [ColorPickerDefaults.colors].
 * @param plane slot for the plane; null keeps [ColorPicker]'s.
 * @param channelSlider slot for each channel's slider; null keeps [ColorPicker]'s. Every slot is handed
 * the state the dialog owns, which a replacement needs to read and write.
 * @param alphaSlider slot for the alpha slider; null keeps [ColorPicker]'s.
 *
 * The remaining parameters are [ColorPicker]'s.
 */
@Composable
public fun ColorPickerDialog(
    initialValue: ColorValue,
    onValueSelected: (ColorValue) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    space: ColorSpace = Okhsl,
    title: String = "Pick a Color",
    confirmText: String = "Select",
    dismissText: String = "Cancel",
    showPlane: Boolean = hasPlane(space),
    showAlpha: Boolean = true,
    enabled: Boolean = true,
    coloringMode: ColoringMode = defaultColoringMode(space),
    colors: ColorPickerColors = ColorPickerDefaults.currentColors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.currentShapes(),
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
    plane: (@Composable (ColorPickerState) -> Unit)? = null,
    channelSlider: (@Composable (ColorPickerState, ColorChannel) -> Unit)? = null,
    alphaSlider: (@Composable (ColorPickerState) -> Unit)? = null,
) {
    val state = rememberDialogState(initialValue, initialValue, space)
    DialogContent(
        state = state,
        onConfirm = { onValueSelected(state.value) },
        onDismiss = onDismiss,
        modifier = modifier,
        space = space,
        title = title,
        confirmText = confirmText,
        dismissText = dismissText,
        showPlane = showPlane,
        showAlpha = showAlpha,
        enabled = enabled,
        coloringMode = coloringMode,
        colors = colors,
        shapes = shapes,
        thumb = thumb,
        plane = plane,
        channelSlider = channelSlider,
        alphaSlider = alphaSlider,
    )
}

/**
 * [ColorPickerDialog] over a Compose [Color]. [onColorSelected] receives exactly [initialColor] if
 * nothing was edited, and otherwise the color as `toComposeColor()`: brought into sRGB by CSS gamut
 * mapping, eight bits a channel. The rest is the [ColorValue] form's.
 *
 * @throws IllegalArgumentException for [Color.Unspecified] and Compose's HDR spaces, which
 * [toColorValue] refuses.
 */
@Composable
public fun ColorPickerDialog(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    space: ColorSpace = Okhsl,
    title: String = "Pick a Color",
    confirmText: String = "Select",
    dismissText: String = "Cancel",
    showPlane: Boolean = hasPlane(space),
    showAlpha: Boolean = true,
    enabled: Boolean = true,
    coloringMode: ColoringMode = defaultColoringMode(space),
    colors: ColorPickerColors = ColorPickerDefaults.currentColors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.currentShapes(),
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
    plane: (@Composable (ColorPickerState) -> Unit)? = null,
    channelSlider: (@Composable (ColorPickerState, ColorChannel) -> Unit)? = null,
    alphaSlider: (@Composable (ColorPickerState) -> Unit)? = null,
) {
    val initialValue = remember(initialColor) { initialColor.toColorValue() }
    val state = rememberDialogState(initialColor, initialValue, space)
    DialogContent(
        state = state,
        // Mapped into sRGB, an untouched Display P3 color would come back clipped.
        onConfirm = { onColorSelected(if (state.value == initialValue) initialColor else state.color) },
        onDismiss = onDismiss,
        modifier = modifier,
        space = space,
        title = title,
        confirmText = confirmText,
        dismissText = dismissText,
        showPlane = showPlane,
        showAlpha = showAlpha,
        enabled = enabled,
        coloringMode = coloringMode,
        colors = colors,
        shapes = shapes,
        thumb = thumb,
        plane = plane,
        channelSlider = channelSlider,
        alphaSlider = alphaSlider,
    )
}

// The dialog's own state, saved under [key]: a new key starts over from [initialValue], while a
// configuration change restores the edit in progress.
@Composable
private fun rememberDialogState(key: Any, initialValue: ColorValue, space: ColorSpace): ColorPickerState {
    val saver = remember(space, initialValue.space) { ColorPickerState.Saver(ColorSpaces.all + space + initialValue.space) }
    return rememberSaveable(key, saver = saver) { ColorPickerState(initialValue) }
}

@Composable
private fun DialogContent(
    state: ColorPickerState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier,
    space: ColorSpace,
    title: String,
    confirmText: String,
    dismissText: String,
    showPlane: Boolean,
    showAlpha: Boolean,
    enabled: Boolean,
    coloringMode: ColoringMode,
    colors: ColorPickerColors,
    shapes: ColorPickerShapes,
    thumb: (@Composable (InteractionSource) -> Unit)?,
    plane: (@Composable (ColorPickerState) -> Unit)?,
    channelSlider: (@Composable (ColorPickerState, ColorChannel) -> Unit)?,
    alphaSlider: (@Composable (ColorPickerState) -> Unit)?,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                ColorSwatch(
                    color = state.color,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = colors,
                )
                Spacer(Modifier.height(16.dp))
                // The slots are nullable rather than defaulted: null means whatever the picker draws,
                // without the dialog repeating the picker's defaults.
                ColorPicker(
                    state = state,
                    space = space,
                    showPlane = showPlane,
                    showAlpha = showAlpha,
                    enabled = enabled,
                    coloringMode = coloringMode,
                    colors = colors,
                    shapes = shapes,
                    thumb = thumb,
                    plane = plane ?: defaultPlane(space, enabled) {},
                    channelSlider = channelSlider ?: defaultChannelSlider(enabled, coloringMode, {}, thumb),
                    alphaSlider = alphaSlider ?: defaultAlphaSlider(enabled, {}, thumb),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissText) }
        },
    )
}
