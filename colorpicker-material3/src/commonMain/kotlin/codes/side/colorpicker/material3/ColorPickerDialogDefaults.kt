package codes.side.colorpicker.material3

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import codes.side.color.ColorSpace
import codes.side.color.HexAlpha
import codes.side.color.Hsv
import codes.side.color.Okhsl
import codes.side.color.Oklch
import codes.side.color.Srgb
import codes.side.color.compose.toComposeColor
import codes.side.color.toHexString
import codes.side.colorpicker.foundation.ColorPickerDialogState
import codes.side.colorpicker.foundation.ColorPickerStrings

// Material's guidance caps a row of segmented buttons at five.
private const val MAX_SEGMENTS = 5

// The space between the header's comparison and its hex.
private val HeaderSpacing = 16.dp

// A segment's label sits centred in the room a segmented button keeps for its check mark even when it has none,
// 13 dp either side, so Material's side padding on top of that would leave four labels too little width in a
// phone's dialog.
private val SegmentPadding = PaddingValues(
    top = SegmentedButtonDefaults.ContentPadding.calculateTopPadding(),
    bottom = SegmentedButtonDefaults.ContentPadding.calculateBottomPadding(),
)

/** Defaults for [ColorPickerDialog]: the spaces it offers, and the parts it draws unless given others. */
public object ColorPickerDialogDefaults {

    /** The spaces a [ColorPickerDialog] offers unless given others: Okhsl, OkLCh, HSV and sRGB. */
    public val Spaces: List<ColorSpace> = listOf(Okhsl, Oklch, Hsv, Srgb)

    /** The dialog's title: [ColorPickerStrings.dialogTitle], "Select color". */
    @Composable
    public fun Title(modifier: Modifier = Modifier) {
        Text(ColorPickerStrings.current.dialogTitle(), modifier)
    }

    /**
     * The dialog's header: a [ColorComparison] of [state]'s original and edited colors, and the edited color in
     * hex, `#RRGGBB`, with alpha's two digits after it when the color is not opaque. The original half is a button
     * that puts the original back, enabled while the color is modified.
     *
     * @param enabled when false the original half is a disabled button.
     */
    @Composable
    public fun Header(state: ColorPickerDialogState, modifier: Modifier = Modifier, enabled: Boolean = true) {
        val value = state.pickerState.value
        val original = remember(state.original) { state.original.toComposeColor() }
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(HeaderSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ColorComparison(
                original = original,
                current = state.pickerState.color,
                onRestoreOriginal = state::revert,
                modifier = Modifier.weight(1f),
                enabled = enabled && state.isModified,
            )
            // Monospaced, so the hex keeps its width while a slider moves.
            Text(value.toHexString(if (value.alpha == 1.0) HexAlpha.None else HexAlpha.Last), fontFamily = FontFamily.Monospace)
        }
    }

    /**
     * The dialog's switcher between [state]'s spaces, each named by [ColorPickerStrings.spaceName]: nothing for
     * one space, a row of segmented buttons for up to five, the most Material's guidance puts in a row, and a
     * button opening a menu of them beyond five.
     *
     * The row takes the width its widest label needs in every segment, or any more width given as a minimum, as
     * the dialog gives it when its body is stacked.
     *
     * @param enabled when false the switcher is dimmed and refuses input.
     */
    @Composable
    public fun SpaceSwitcher(state: ColorPickerDialogState, modifier: Modifier = Modifier, enabled: Boolean = true) {
        val spaces = state.spaces
        when {
            spaces.size < 2 -> {}
            spaces.size <= MAX_SEGMENTS -> SingleChoiceSegmentedButtonRow(modifier) {
                spaces.forEachIndexed { index, space ->
                    SegmentedButton(
                        selected = space == state.space,
                        onClick = { state.space = space },
                        shape = SegmentedButtonDefaults.itemShape(index, spaces.size),
                        enabled = enabled,
                        contentPadding = SegmentPadding,
                        // No check mark: the selected segment's fill says which it is, and the labels need the width.
                        icon = {},
                        label = {
                            Text(ColorPickerStrings.current.spaceName(space), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                    )
                }
            }
            else -> SpaceMenu(state, modifier, enabled)
        }
    }

    /** The dialog's confirm button: a [TextButton] reading [ColorPickerStrings.confirm], "OK". */
    @Composable
    public fun ConfirmButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
        TextButton(onClick = onClick, modifier = modifier) { Text(ColorPickerStrings.current.confirm()) }
    }

    /** The dialog's dismiss button: a [TextButton] reading [ColorPickerStrings.dismiss], "Cancel". */
    @Composable
    public fun DismissButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
        TextButton(onClick = onClick, modifier = modifier) { Text(ColorPickerStrings.current.dismiss()) }
    }
}

// A button naming the space shown, opening a menu of every space.
@Composable
private fun SpaceMenu(state: ColorPickerDialogState, modifier: Modifier, enabled: Boolean) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        // A screen reader hears a list to choose from, not a button named after the space shown.
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.semantics { role = Role.DropdownList }, enabled = enabled) {
            Text(ColorPickerStrings.current.spaceName(state.space), maxLines = 1)
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            DropdownArrow(Modifier.size(ButtonDefaults.IconSize))
        }
        // One choice among the spaces, as the row of segmented buttons is.
        DropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }, modifier = Modifier.selectableGroup()) {
            for (space in state.spaces) {
                DropdownMenuItem(
                    text = { Text(ColorPickerStrings.current.spaceName(space)) },
                    onClick = {
                        state.space = space
                        expanded = false
                    },
                    modifier = Modifier.semantics {
                        role = Role.RadioButton
                        selected = space == state.space
                    },
                )
            }
        }
    }
}

// Material's arrow_drop_down, drawn rather than taken from an icon artifact the library does not depend on.
@Composable
private fun DropdownArrow(modifier: Modifier) {
    val color = LocalContentColor.current
    Canvas(modifier) {
        val unit = size.width / 24f
        val arrow = Path().apply {
            moveTo(7f * unit, 10f * unit)
            lineTo(12f * unit, 15f * unit)
            lineTo(17f * unit, 10f * unit)
            close()
        }
        drawPath(arrow, color)
    }
}
