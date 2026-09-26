package codes.side.colorpicker.foundation

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// The narrowest width the picker goes beside its sliders at: under it, the sliders' half is too narrow for a
// label and a value above each track.
private val SideBySideMinWidth = 480.dp

private enum class DialogArrangement { Stacked, SideBySide }

// The tallest height the content has been laid out at. Read and written in layout alone, so growing it
// recomposes nothing.
private class HeightFloor {
    var height = 0
}

/**
 * The body of a color dialog, with no window, title or buttons: [header], [spaceSwitcher] and the [picker],
 * arranged by the space it is given rather than by the window's size, since a dialog can sit in any window.
 *
 * - Stacked, header over switcher over the picker, when that fits the height available.
 * - Otherwise, when the width available is at least 480 dp, header and switcher side by side above the
 *   picker, and [picker] is handed [Orientation.Horizontal], so the plane sits beside the sliders.
 * - Otherwise stacked, scrolling.
 *
 * Arranged side by side, it scrolls when even that does not fit.
 *
 * It never shrinks while it is composed for the same [state]: a space with no plane is laid out at the
 * height of the tallest space shown so far, so the dialog around it does not jump when the space changes.
 *
 * @param picker draws the picker in the orientation it is handed.
 * @param header drawn first; `null` leaves it out.
 * @param spaceSwitcher drawn after [header]; `null` leaves it out.
 * @param spacing the space between the parts.
 */
@Composable
public fun BasicColorPickerDialogContent(
    state: ColorPickerDialogState,
    picker: @Composable (Orientation) -> Unit,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    spaceSwitcher: (@Composable () -> Unit)? = null,
    spacing: Dp = 0.dp,
) {
    val floor = remember(state) { HeightFloor() }
    // Whether the content is laid out side by side, which the stacked form, composed then only to be measured,
    // reads to keep out of the semantics tree: a composed node that is never placed is still in it, and a
    // screen reader would find every slider twice.
    val sideBySide = remember { mutableStateOf(false) }
    val stackedScroll = rememberScrollState()
    val sideBySideScroll = rememberScrollState()
    SubcomposeLayout(modifier) { constraints ->
        // Composed whichever way it is laid out, since its height is what decides.
        val stacked = subcompose(DialogArrangement.Stacked) {
            Column(
                modifier = Modifier
                    .then(if (sideBySide.value) Modifier.clearAndSetSemantics {} else Modifier)
                    .verticalScroll(stackedScroll),
                verticalArrangement = Arrangement.spacedBy(spacing),
            ) {
                header?.invoke()
                spaceSwitcher?.invoke()
                picker(Orientation.Vertical)
            }
        }.single()
        val fits = !constraints.hasBoundedHeight || stacked.maxIntrinsicHeight(constraints.maxWidth) <= constraints.maxHeight
        val beside = !fits && constraints.maxWidth >= SideBySideMinWidth.roundToPx()
        // Unobserved, or this layout would measure again for its own write.
        if (Snapshot.withoutReadObservation { sideBySide.value } != beside) sideBySide.value = beside
        val content = if (!beside) {
            stacked
        } else {
            subcompose(DialogArrangement.SideBySide) {
                Column(Modifier.verticalScroll(sideBySideScroll), verticalArrangement = Arrangement.spacedBy(spacing)) {
                    if (header != null || spaceSwitcher != null) {
                        // The switcher at its own width, which a row of labels needs whole, and the header,
                        // which stretches, in the rest.
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing), verticalAlignment = Alignment.CenterVertically) {
                            header?.let { Box(Modifier.weight(1f)) { it() } }
                            spaceSwitcher?.let { Box(Modifier.width(IntrinsicSize.Max)) { it() } }
                        }
                    }
                    picker(Orientation.Horizontal)
                }
            }.single()
        }
        val placeable = content.measure(constraints.copy(minHeight = 0))
        val height = maxOf(placeable.height, floor.height).coerceIn(constraints.minHeight, constraints.maxHeight)
        floor.height = height
        layout(placeable.width, height) { placeable.place(0, 0) }
    }
}
