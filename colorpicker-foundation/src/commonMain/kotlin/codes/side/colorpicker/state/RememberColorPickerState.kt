package codes.side.colorpicker.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import codes.side.color.ColorValue

/**
 * Creates and remembers a [ColorPickerState] for the lifetime of the composition.
 *
 * [initialValue] is read once, when the state is created; a different one on a later
 * recomposition does not reset it, as with `rememberScrollState`. The state does not survive
 * configuration changes or process death; [rememberSaveableColorPickerState] does.
 */
@Composable
public fun rememberColorPickerState(initialValue: ColorValue): ColorPickerState = remember { ColorPickerState(initialValue) }

/** Like [rememberColorPickerState], starting from a Compose [Color]. */
@Composable
public fun rememberColorPickerState(initialColor: Color): ColorPickerState = remember { ColorPickerState(initialColor) }
