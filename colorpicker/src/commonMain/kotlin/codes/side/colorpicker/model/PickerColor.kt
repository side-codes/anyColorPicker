package codes.side.colorpicker.model

import androidx.compose.runtime.Immutable

@Immutable
sealed interface PickerColor {
    val alpha: Float
}
