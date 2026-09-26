package codes.side.colorpicker.sample

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import codes.side.color.ColorSpaces
import codes.side.color.HexAlpha
import codes.side.color.Okhsl
import codes.side.color.toCssString
import codes.side.color.toHexString
import codes.side.colorpicker.material3.ChannelSlider
import codes.side.colorpicker.material3.ColorPicker
import codes.side.colorpicker.material3.ColorPickerDefaults
import codes.side.colorpicker.material3.ColorPickerDialog
import codes.side.colorpicker.material3.ColorSwatch
import codes.side.colorpicker.material3.RgbColorPicker
import codes.side.colorpicker.state.ColoringMode
import codes.side.colorpicker.state.rememberSaveableColorPickerState
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleApp() {
    val colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme = colorScheme) {
        val state = rememberSaveableColorPickerState(Okhsl(250.0, 0.8, 0.6))
        // Saved by id: saved state holds strings, not color spaces.
        var spaceId by rememberSaveable { mutableStateOf(Okhsl.id) }
        val space = ColorSpaces.all.first { it.id == spaceId }
        var showDialog by rememberSaveable { mutableStateOf(false) }
        var coloringMode by rememberSaveable { mutableStateOf(ColoringMode.Independent) }
        var enabled by rememberSaveable { mutableStateOf(true) }
        // Held as an app holding a Compose Color would hold it, with no ColorPickerState of its own.
        var color by remember { mutableStateOf(Color(0xFF3366CC)) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Color Picker") },
                    colors = TopAppBarDefaults.topAppBarColors(),
                )
            },
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Live preview swatch
                item {
                    ColorSwatch(
                        color = state.color,
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                    )
                }

                // The value as CSS writes it, in its own space, and as hex, mapped into sRGB
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Readout(state.value.toCssString(precision = 4))
                            Readout(state.value.toHexString(HexAlpha.Last))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    state.value = Okhsl(Random.nextDouble(360.0), Random.nextDouble(0.4, 1.0), Random.nextDouble(0.3, 0.8))
                                },
                            ) {
                                Text("Random")
                            }
                            Button(onClick = { showDialog = true }) {
                                Text("Dialog")
                            }
                        }
                    }
                }

                // The picker's space: every one the library ships
                item {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for (each in ColorSpaces.all) {
                            FilterChip(
                                selected = each === space,
                                onClick = { spaceId = each.id },
                                label = { Text(each.id) },
                            )
                        }
                    }
                }

                // Coloring mode switcher
                item {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        val modes = ColoringMode.entries
                        modes.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = coloringMode == mode,
                                onClick = { coloringMode = mode },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = modes.size,
                                ),
                                label = { Text(mode.name) },
                            )
                        }
                    }
                }

                // Everything below reads this, so the whole sample greys out together.
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Enabled")
                        Switch(checked = enabled, onCheckedChange = { enabled = it })
                    }
                }

                // One picker for whichever space is chosen above. Moving a channel leaves the color in
                // that space, so the CSS readout changes function as the chips change.
                item {
                    ColorPicker(
                        state = state,
                        space = space,
                        enabled = enabled,
                        coloringMode = coloringMode,
                    )
                }

                item { HorizontalDivider() }

                // Every slider takes a `thumb` slot. The composable is free to read the
                // picker state, so the thumb here restyles itself as the color changes.
                item { SectionHeader("Custom thumb") }
                item {
                    ChannelSlider(
                        state = state,
                        channel = Okhsl.H,
                        enabled = enabled,
                        coloringMode = coloringMode,
                        thumb = { SquareThumb(state.color, interactionSource) },
                        dimensions = ColorPickerDefaults.currentDimensions().copy(thumbWidth = SquareThumbSize),
                    )
                }

                item { HorizontalDivider() }

                // The same state in a picker with no Material in it: the Basic components, drawn by
                // FoundationPicker alone.
                item { SectionHeader("Built on foundation") }
                item { FoundationPicker(state, enabled) }

                item { HorizontalDivider() }

                // A picker over a value the app holds: every change arrives in the callback, and the
                // picker draws only what the app passes back.
                item { SectionHeader("Controlled by a Compose Color") }
                item {
                    ColorSwatch(
                        color = color,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    )
                }
                item {
                    RgbColorPicker(
                        color = color,
                        onColorChange = { color = it },
                        enabled = enabled,
                        coloringMode = coloringMode,
                    )
                }
            }
        }

        if (showDialog) {
            ColorPickerDialog(
                initialValue = state.value,
                onValueSelected = { value ->
                    state.value = value
                    showDialog = false
                },
                onDismiss = { showDialog = false },
                space = space,
            )
        }
    }
}

/**
 * A rounded square filled with the picked color, ringed in a neutral picked from the
 * color's own luminance and lifted off the track by a shadow, so it stays legible against
 * a track that is, by definition, the same color.
 */
@Composable
private fun SquareThumb(color: Color, interaction: InteractionSource) {
    val fill = color.copy(alpha = 1f)
    val shape = RoundedCornerShape(16.dp)
    // The ring is the fill lifted most of the way to white, so it tracks the color
    // continuously instead of flipping between two neutrals at a luminance threshold —
    // that flip is visible as a snap the moment you drag across it.
    val ring = lerp(fill, Color.White, 0.6f)

    // The slot is handed the slider's InteractionSource precisely so a thumb can do this.
    val dragged by interaction.collectIsDraggedAsState()
    val pressed by interaction.collectIsPressedAsState()
    val elevation by animateDpAsState(
        targetValue = if (dragged || pressed) 4.dp else 0.dp,
        label = "thumbElevation",
    )

    Box(
        Modifier
            .size(SquareThumbSize)
            .shadow(elevation, shape)
            .background(ring, shape)
            .padding(5.dp)
            .background(fill, RoundedCornerShape(11.dp)),
    )
}

// Material 3's minimum interactive size (LocalMinimumInteractiveComponentSize).
private val SquareThumbSize = 48.dp

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun Readout(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
    )
}
