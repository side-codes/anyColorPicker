package codes.side.colorpicker.foundation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * One swatch split in two: [original] in the start half and [current] in the end half, over a transparency
 * checkerboard. It has no size and no shape of its own; give it both through [modifier], as a clip and a size.
 *
 * @param onRestoreOriginal called when the original half is pressed, which makes that half a button that puts
 * the original back; `null` leaves it a swatch.
 * @param originalLabel what a screen reader calls the original half, "Original color" by default; `null` says
 * nothing.
 * @param currentLabel what a screen reader calls the current half, "New color" by default; `null` says
 * nothing.
 * @param restoreLabel what a screen reader says pressing the original half does, "Restore original color" by
 * default.
 */
@Composable
public fun BasicColorComparison(
    original: Color,
    current: Color,
    onRestoreOriginal: (() -> Unit)?,
    modifier: Modifier = Modifier,
    checkerboardLight: Color = Color.White,
    checkerboardDark: Color = Color.LightGray,
    originalLabel: String? = ColorPickerStrings.current.originalColor(),
    currentLabel: String? = ColorPickerStrings.current.newColor(),
    restoreLabel: String? = ColorPickerStrings.current.restoreOriginal(),
) {
    Box(modifier.checkerboard(checkerboardLight, checkerboardDark)) {
        // Sized to the box, which takes its size from the modifier alone: a height given only as a minimum,
        // as in a scrolling column, is one fillMaxHeight cannot fill.
        Row(Modifier.matchParentSize()) {
            val restore = if (onRestoreOriginal != null) {
                Modifier.clickable(onClickLabel = restoreLabel, role = Role.Button, onClick = onRestoreOriginal)
            } else {
                Modifier
            }
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(restore)
                    .semantics { originalLabel?.let { contentDescription = it } }
                    .background(original),
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .semantics { currentLabel?.let { contentDescription = it } }
                    .background(current),
            )
        }
    }
}
