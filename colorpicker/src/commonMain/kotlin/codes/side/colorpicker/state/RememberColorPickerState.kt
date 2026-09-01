package codes.side.colorpicker.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.model.PickerColor

/**
 * Creates and remembers a [ColorPickerState] for the lifetime of the composition.
 *
 * [initialColor] is read only once, when the state is first created; passing a
 * different value on later recompositions does NOT reset the state (matching the
 * `rememberScrollState` convention). Its runtime type selects the initial origin
 * space. The state does not survive configuration changes or process death — use
 * [rememberSaveableColorPickerState] for that.
 */
@Composable
public fun rememberColorPickerState(
    initialColor: PickerColor = HslColor(),
): ColorPickerState {
    return remember { ColorPickerState(initialColor) }
}
