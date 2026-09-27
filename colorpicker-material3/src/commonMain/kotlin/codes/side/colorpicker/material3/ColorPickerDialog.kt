package codes.side.colorpicker.material3

import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import codes.side.color.ColorSpace
import codes.side.color.ColorValue
import codes.side.color.compose.toColorValue
import codes.side.colorpicker.foundation.AlphaSliderPart
import codes.side.colorpicker.foundation.BasicColorPickerDialogContent
import codes.side.colorpicker.foundation.ChannelSliderPart
import codes.side.colorpicker.foundation.ColorPickerDialogState
import codes.side.colorpicker.foundation.ColorPickerStrings
import codes.side.colorpicker.foundation.ColorSliderScope
import codes.side.colorpicker.foundation.PlanePart
import codes.side.colorpicker.foundation.rememberColorPickerDialogState

// The space between the dialog's header, switcher and picker.
private val DialogSpacing = 16.dp

/**
 * What a [ColorPickerDialog] hands its title, header, switcher and button slots: the dialog's state, and the
 * two ways out of it. A slot can read the state and act on it, as an "Apply" button enabled only while
 * [ColorPickerDialogState.isModified] would.
 *
 * Only the library implements it, so it can gain members without breaking a slot.
 */
@Stable
public sealed interface ColorPickerDialogScope {

    /**
     * The dialog's state: the color it opened with, the color being edited, and the spaces. Not `state`, which a
     * caller's own picker state, in scope where the dialog is written, would hide from the slot.
     */
    public val dialogState: ColorPickerDialogState

    /** Hands [ColorPickerDialogState.result] to the dialog's caller, as the default confirm button does. */
    public fun confirm()

    /** Tells the dialog's caller the dialog was dismissed, as the default dismiss button does. */
    public fun dismiss()
}

/**
 * A Material [AlertDialog] for picking a color: a [ColorComparison] of the original color and the edited one,
 * with the edited one in hex; a switcher between [spaces]; and a [ColorPicker] for the space shown; over OK
 * and Cancel.
 *
 * The dialog keeps its own [ColorPickerDialogState], saved with [initialValue] as the key: an edit in progress
 * and the space shown survive configuration changes and process death, and a different [initialValue] starts
 * the dialog over. Showing another space redraws the color in it without converting it.
 *
 * Every part is a slot. The title, header, switcher and buttons are handed a [ColorPickerDialogScope], which
 * reads the state and can confirm or dismiss; the picker's slots are handed the state's
 * [ColorPickerDialogState.pickerState]. Every slot inherits the dialog's colors, shapes and dimensions through
 * [ColorPickerTheme]. The body is laid out as [BasicColorPickerDialogContent] lays it out: with the plane
 * beside the sliders when the dialog is too short to stack them and wide enough not to.
 *
 * A screen reader announces the dialog by [ColorPickerStrings.dialogTitle], "Select color", whatever the [title]
 * slot shows. Strings provided with `ProvideColorPickerStrings` change it, and so does a `paneTitle` set in
 * [modifier].
 *
 * @param onValueSelected called by [ColorPickerDialogScope.confirm] with [ColorPickerDialogState.result]:
 * exactly [initialValue] when nothing was edited or the original was restored; otherwise the color as last
 * edited, in the space of the channel last moved, or in its own space when only alpha moved. The caller
 * closes the dialog.
 * @param onDismissRequest called when the user cancels, presses outside the dialog, or presses Back or Escape.
 * @param spaces the spaces the switcher offers, in its order; [ColorPickerDialogDefaults.Spaces] by default.
 * @param initialSpace the space shown first: [initialValue]'s own when [spaces] holds it, so a color the
 * dialog returned opens again in the space it was picked in, else the first of [spaces].
 * @param enabled when false the picker, the restore and the switcher are disabled. The buttons stay live, so
 * the dialog can still be closed.
 * @param properties the dialog window's properties.
 * @param colors checkerboard and disabled colors; see [ColorPickerDefaults.colors].
 * @param shapes track, swatch and plane shapes; see [ColorPickerDefaults.shapes].
 * @param dimensions track and plane sizes; see [ColorPickerDefaults.dimensions].
 * @param title slot for the title, [ColorPickerDialogDefaults.Title] by default; `null` leaves it out.
 * @param header slot above the switcher, [ColorPickerDialogDefaults.Header] by default; `null` leaves it out.
 * @param spaceSwitcher slot for the switcher, [ColorPickerDialogDefaults.SpaceSwitcher] by default; `null`
 * leaves it out.
 * @param confirmButton slot for the confirm button, [ColorPickerDialogDefaults.ConfirmButton] by default.
 * @param dismissButton slot for the dismiss button, [ColorPickerDialogDefaults.DismissButton] by default;
 * `null` leaves it out.
 * @param thumb draws the thumb of every slider in the picker that is not given one of its own, from its
 * [ColorSliderScope]; [ColorPickerDefaults.SliderThumb] by default.
 * @param plane slot for the plane, handed its [PlanePart]: the picker's state and the plane's axes;
 * [ColorPickerDefaults.Plane] by default, and `null` leaves it out.
 * @param channelSlider slot for each channel's slider, handed its [ChannelSliderPart]. The default colors each
 * channel as its space's sliders are colored by default.
 * @param alphaSlider slot for the alpha slider, handed its [AlphaSliderPart]; `null` leaves it out.
 * @throws IllegalArgumentException if [spaces] is empty, lists a space twice or does not hold [initialSpace].
 */
@Composable
public fun ColorPickerDialog(
    initialValue: ColorValue,
    onValueSelected: (ColorValue) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    spaces: List<ColorSpace> = ColorPickerDialogDefaults.Spaces,
    initialSpace: ColorSpace = if (initialValue.space in spaces) initialValue.space else requireNotNull(spaces.firstOrNull()) { "No spaces to offer" },
    enabled: Boolean = true,
    properties: DialogProperties = DialogProperties(),
    colors: ColorPickerColors = ColorPickerDefaults.currentColors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.currentShapes(),
    dimensions: ColorPickerDimensions = ColorPickerDefaults.currentDimensions(),
    title: (@Composable ColorPickerDialogScope.() -> Unit)? = { ColorPickerDialogDefaults.Title() },
    header: (@Composable ColorPickerDialogScope.() -> Unit)? = { ColorPickerDialogDefaults.Header(dialogState, enabled = enabled) },
    spaceSwitcher: (@Composable ColorPickerDialogScope.() -> Unit)? = { ColorPickerDialogDefaults.SpaceSwitcher(dialogState, enabled = enabled) },
    confirmButton: @Composable ColorPickerDialogScope.() -> Unit = { ColorPickerDialogDefaults.ConfirmButton(onClick = { confirm() }) },
    dismissButton: (@Composable ColorPickerDialogScope.() -> Unit)? = { ColorPickerDialogDefaults.DismissButton(onClick = { dismiss() }) },
    thumb: @Composable ColorSliderScope.() -> Unit = { ColorPickerDefaults.SliderThumb(interactionSource, thumbColor) },
    plane: (@Composable (PlanePart) -> Unit)? = { ColorPickerDefaults.Plane(it) },
    channelSlider: @Composable (ChannelSliderPart) -> Unit = { ChannelSlider(it.state, it.channel) },
    alphaSlider: (@Composable (AlphaSliderPart) -> Unit)? = { AlphaSlider(it.state) },
) {
    val state = rememberColorPickerDialogState(initialValue, spaces, initialSpace)
    DialogBody(
        state = state,
        onConfirm = { onValueSelected(state.result) },
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        enabled = enabled,
        properties = properties,
        colors = colors,
        shapes = shapes,
        dimensions = dimensions,
        title = title,
        header = header,
        spaceSwitcher = spaceSwitcher,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        thumb = thumb,
        plane = plane,
        channelSlider = channelSlider,
        alphaSlider = alphaSlider,
    )
}

/**
 * [ColorPickerDialog] over a Compose [Color]. [onColorSelected] receives exactly [initialColor] when nothing
 * was edited or the original was restored, and otherwise the color as `toComposeColor()`: brought into sRGB by
 * CSS gamut mapping, eight bits a channel.
 *
 * [initialSpace] is the first of [spaces] by default: a Compose color is sRGB whatever space the user last
 * picked it in, so its space says nothing about where they were. Pass the space yourself to open where they
 * left off.
 *
 * The rest is the [ColorValue] form's.
 *
 * @throws IllegalArgumentException for [Color.Unspecified] and Compose's HDR spaces, which [toColorValue]
 * refuses, and if [spaces] is empty, lists a space twice or does not hold [initialSpace].
 */
@Composable
public fun ColorPickerDialog(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    spaces: List<ColorSpace> = ColorPickerDialogDefaults.Spaces,
    initialSpace: ColorSpace = requireNotNull(spaces.firstOrNull()) { "No spaces to offer" },
    enabled: Boolean = true,
    properties: DialogProperties = DialogProperties(),
    colors: ColorPickerColors = ColorPickerDefaults.currentColors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.currentShapes(),
    dimensions: ColorPickerDimensions = ColorPickerDefaults.currentDimensions(),
    title: (@Composable ColorPickerDialogScope.() -> Unit)? = { ColorPickerDialogDefaults.Title() },
    header: (@Composable ColorPickerDialogScope.() -> Unit)? = { ColorPickerDialogDefaults.Header(dialogState, enabled = enabled) },
    spaceSwitcher: (@Composable ColorPickerDialogScope.() -> Unit)? = { ColorPickerDialogDefaults.SpaceSwitcher(dialogState, enabled = enabled) },
    confirmButton: @Composable ColorPickerDialogScope.() -> Unit = { ColorPickerDialogDefaults.ConfirmButton(onClick = { confirm() }) },
    dismissButton: (@Composable ColorPickerDialogScope.() -> Unit)? = { ColorPickerDialogDefaults.DismissButton(onClick = { dismiss() }) },
    thumb: @Composable ColorSliderScope.() -> Unit = { ColorPickerDefaults.SliderThumb(interactionSource, thumbColor) },
    plane: (@Composable (PlanePart) -> Unit)? = { ColorPickerDefaults.Plane(it) },
    channelSlider: @Composable (ChannelSliderPart) -> Unit = { ChannelSlider(it.state, it.channel) },
    alphaSlider: (@Composable (AlphaSliderPart) -> Unit)? = { AlphaSlider(it.state) },
) {
    val initialValue = remember(initialColor) { initialColor.toColorValue() }
    val state = rememberColorPickerDialogState(initialValue, spaces, initialSpace)
    DialogBody(
        state = state,
        // Mapped into sRGB, an untouched Display P3 color would come back clipped.
        onConfirm = { onColorSelected(if (state.isModified) state.pickerState.color else initialColor) },
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        enabled = enabled,
        properties = properties,
        colors = colors,
        shapes = shapes,
        dimensions = dimensions,
        title = title,
        header = header,
        spaceSwitcher = spaceSwitcher,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        thumb = thumb,
        plane = plane,
        channelSlider = channelSlider,
        alphaSlider = alphaSlider,
    )
}

private class DialogScope(
    override val dialogState: ColorPickerDialogState,
    private val onConfirm: () -> Unit,
    private val onDismiss: () -> Unit,
) : ColorPickerDialogScope {
    override fun confirm() = onConfirm()

    override fun dismiss() = onDismiss()
}

@Composable
private fun DialogBody(
    state: ColorPickerDialogState,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    properties: DialogProperties,
    colors: ColorPickerColors,
    shapes: ColorPickerShapes,
    dimensions: ColorPickerDimensions,
    title: (@Composable ColorPickerDialogScope.() -> Unit)?,
    header: (@Composable ColorPickerDialogScope.() -> Unit)?,
    spaceSwitcher: (@Composable ColorPickerDialogScope.() -> Unit)?,
    confirmButton: @Composable ColorPickerDialogScope.() -> Unit,
    dismissButton: (@Composable ColorPickerDialogScope.() -> Unit)?,
    thumb: @Composable ColorSliderScope.() -> Unit,
    plane: (@Composable (PlanePart) -> Unit)?,
    channelSlider: @Composable (ChannelSliderPart) -> Unit,
    alphaSlider: (@Composable (AlphaSliderPart) -> Unit)?,
) {
    val currentOnConfirm by rememberUpdatedState(onConfirm)
    val currentOnDismiss by rememberUpdatedState(onDismissRequest)
    val scope = remember(state) { DialogScope(state, { currentOnConfirm() }, { currentOnDismiss() }) }
    // Material's dialog window announces itself as "Dialog"; the title's words say which one.
    val paneTitle = ColorPickerStrings.current.dialogTitle()
    // Provided rather than passed down, so a replaced slot inherits the dialog's theme, as a picker's does.
    ColorPickerTheme(colors = colors, shapes = shapes, dimensions = dimensions) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = { scope.confirmButton() },
            // Inside the caller's modifier, so a paneTitle set there wins.
            modifier = modifier.semantics { this.paneTitle = paneTitle },
            dismissButton = dismissButton?.let { slot -> { scope.slot() } },
            title = title?.let { slot -> { scope.slot() } },
            text = {
                BasicColorPickerDialogContent(
                    state = state,
                    picker = { orientation ->
                        ColorPicker(
                            state = state.pickerState,
                            space = state.space,
                            enabled = enabled,
                            orientation = orientation,
                            thumb = thumb,
                            plane = plane,
                            channelSlider = channelSlider,
                            alphaSlider = alphaSlider,
                        )
                    },
                    header = header?.let { slot -> { scope.slot() } },
                    spaceSwitcher = spaceSwitcher?.let { slot -> { scope.slot() } },
                    spacing = DialogSpacing,
                )
            },
            properties = properties,
        )
    }
}
