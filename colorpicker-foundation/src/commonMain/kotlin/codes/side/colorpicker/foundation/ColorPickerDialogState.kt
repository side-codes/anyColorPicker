package codes.side.colorpicker.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import codes.side.color.ColorSpace
import codes.side.color.ColorSpaces
import codes.side.color.ColorValue
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.colorPickerStateSaver
import codes.side.colorpicker.state.spacesById

/**
 * What a color dialog edits: the color it opened with, the spaces it offers, the one it shows, and the
 * [ColorPickerState] its picker edits.
 *
 * Showing another space does not convert the color. The picker for the new space draws the same color, and
 * an edit made there leaves the color in that space, so a Display P3 color is not clipped to sRGB because
 * an sRGB space was looked at.
 */
@Stable
public class ColorPickerDialogState internal constructor(
    initialValue: ColorValue,
    spaces: List<ColorSpace>,
    initialSpace: ColorSpace,
    /** The state the dialog's picker edits, starting at [original]. */
    public val pickerState: ColorPickerState,
) {

    /**
     * A state opening on [initialValue], shown in [initialSpace].
     *
     * @param spaces the spaces the dialog offers, in the order a switcher lists them.
     * @param initialSpace [initialValue]'s own space when [spaces] holds it, else the first of [spaces].
     * @throws IllegalArgumentException if [spaces] lists a space twice or does not hold [initialSpace].
     * @throws NoSuchElementException if [spaces] is empty and [initialSpace] is left to its default.
     */
    public constructor(
        initialValue: ColorValue,
        spaces: List<ColorSpace>,
        initialSpace: ColorSpace = if (initialValue.space in spaces) initialValue.space else spaces.first(),
    ) : this(initialValue, spaces, initialSpace, ColorPickerState(initialValue))

    init {
        require(spaces.distinct().size == spaces.size) { "A space is listed twice in $spaces" }
        require(initialSpace in spaces) { "$initialSpace is not one of $spaces" }
    }

    /** The color the dialog opened with. */
    public val original: ColorValue = initialValue

    /** The spaces the dialog offers, in the order a switcher lists them. */
    public val spaces: List<ColorSpace> = spaces.toList()

    private var shown by mutableStateOf(initialSpace)

    /**
     * The space the picker shows. Setting it redraws the color in that space without converting it.
     *
     * @throws IllegalArgumentException when set to a space [spaces] does not hold.
     */
    public var space: ColorSpace
        get() = shown
        set(value) {
            require(value in spaces) { "$value is not one of $spaces" }
            shown = value
        }

    /** Whether the color in [pickerState] is no longer [original]. */
    public val isModified: Boolean get() = pickerState.value != original

    /**
     * The color confirming the dialog returns: [original] itself while nothing is modified, so a color the
     * user did not touch comes back exactly as it went in; otherwise the color as last edited, in the space
     * of the last channel moved, or in the color's own space when only alpha moved.
     */
    public val result: ColorValue get() = if (isModified) pickerState.value else original

    /** Puts [original] back into [pickerState]. */
    public fun revert() {
        pickerState.value = original
    }

    public companion object {
        /**
         * Saves a dialog state: the original color, the spaces offered and the one shown, and the picker's
         * state as [ColorPickerState.Saver] saves it. A saved space is found by id among [knownSpaces] and
         * then the library's own; a state naming a space found in neither restores as null, so the dialog
         * starts over.
         *
         * @throws IllegalArgumentException if two different spaces in [knownSpaces] share an id.
         */
        public fun Saver(knownSpaces: Collection<ColorSpace> = ColorSpaces.all): Saver<ColorPickerDialogState, Any> =
            colorPickerDialogStateSaver(knownSpaces)
    }
}

/**
 * A [ColorPickerDialogState] remembered across recomposition and saved across configuration changes and
 * process death, where the platform restores saved state: the edit in progress and the space shown survive
 * them. A different [initialValue] or [spaces] starts the state over.
 *
 * The saver knows every space in [spaces] and [initialValue]'s own.
 *
 * @throws IllegalArgumentException if [spaces] lists a space twice or does not hold [initialSpace].
 * @throws NoSuchElementException if [spaces] is empty and [initialSpace] is left to its default.
 */
@Composable
public fun rememberColorPickerDialogState(
    initialValue: ColorValue,
    spaces: List<ColorSpace>,
    initialSpace: ColorSpace = if (initialValue.space in spaces) initialValue.space else spaces.first(),
): ColorPickerDialogState {
    val saver = remember(spaces, initialValue.space) { ColorPickerDialogState.Saver(spaces + initialValue.space) }
    return rememberSaveable(initialValue, spaces, saver = saver) {
        ColorPickerDialogState(initialValue, spaces, initialSpace)
    }
}

// The first element of every saved list. A later format changes it, and a list of another format
// restores as null rather than being misread.
private const val SAVED_FORMAT = 1

// A saved dialog is [SAVED_FORMAT, the ids of the spaces offered, the id of the space shown, the
// original as ColorPickerState.Saver saves a state holding it, the picker's state].
private fun colorPickerDialogStateSaver(knownSpaces: Collection<ColorSpace>): Saver<ColorPickerDialogState, Any> {
    val spaces = spacesById(knownSpaces)
    val stateSaver = colorPickerStateSaver(knownSpaces)
    return Saver(
        save = { dialog ->
            arrayListOf(
                SAVED_FORMAT,
                ArrayList(dialog.spaces.map { it.id }),
                dialog.space.id,
                with(stateSaver) { save(ColorPickerState(dialog.original)) },
                with(stateSaver) { save(dialog.pickerState) },
            )
        },
        restore = { saved -> restored(saved, spaces, stateSaver) },
    )
}

private fun restored(
    saved: Any,
    spaces: Map<String, ColorSpace>,
    stateSaver: Saver<ColorPickerState, Any>,
): ColorPickerDialogState? {
    val list = saved as? List<*> ?: return null
    if (list.size != 5 || list[0] != SAVED_FORMAT) return null
    val ids = list[1] as? List<*> ?: return null
    val offered = ids.map { id -> (id as? String)?.let(spaces::get) ?: return null }
    val shown = (list[2] as? String)?.let(spaces::get) ?: return null
    val original = list[3]?.let(stateSaver::restore)?.value ?: return null
    val pickerState = list[4]?.let(stateSaver::restore) ?: return null
    return try {
        ColorPickerDialogState(original, offered, shown, pickerState)
    } catch (_: IllegalArgumentException) {
        null
    }
}
