package codes.side.colorpicker.sample

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import codes.side.colorpicker.conversion.toComposeColor
import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.state.ColoringMode
import codes.side.colorpicker.state.rememberSaveableColorPickerState
import codes.side.colorpicker.ui.AlphaSlider
import codes.side.colorpicker.ui.BlueSlider
import codes.side.colorpicker.ui.ColorPickerDialog
import codes.side.colorpicker.ui.ColorSwatch
import codes.side.colorpicker.ui.CyanSlider
import codes.side.colorpicker.ui.GreenSlider
import codes.side.colorpicker.ui.HueSlider
import codes.side.colorpicker.ui.KeySlider
import codes.side.colorpicker.ui.LabASlider
import codes.side.colorpicker.ui.LabBSlider
import codes.side.colorpicker.ui.LightnessLabSlider
import codes.side.colorpicker.ui.LightnessSlider
import codes.side.colorpicker.ui.MagentaSlider
import codes.side.colorpicker.ui.RedSlider
import codes.side.colorpicker.ui.SaturationSlider
import codes.side.colorpicker.ui.YellowSlider
import codes.side.colorpicker.util.randomHslColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleApp() {
    val colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme = colorScheme) {
        val state = rememberSaveableColorPickerState(
            initialColor = HslColor(hue = 200f, saturation = 0.8f, lightness = 0.5f),
        )
        var showDialog by rememberSaveable { mutableStateOf(false) }
        var coloringMode by rememberSaveable { mutableStateOf(ColoringMode.Contextual) }

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
                        color = state.hslColor.toComposeColor(),
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                    )
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

                // Color values readout
                item {
                    val hsl = state.hslColor
                    val rgb = state.rgbColor
                    val cmyk = state.cmykColor
                    val lab = state.labColor
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "#${
                                state.argbInt
                                    .toUInt()
                                    .toString(16)
                                    .uppercase()
                                    .padStart(8, '0')
                            }",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { state.updateFromHsl(randomHslColor()) }) {
                                Text("Random")
                            }
                            Button(onClick = { showDialog = true }) {
                                Text("Dialog")
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    val h = hsl.intHue.pad(3)
                    val s = hsl.intSaturation.pad(3)
                    val l = hsl.intLightness.pad(3)
                    val ha = hsl.intAlpha.pad(3)
                    val r = rgb.intRed.pad(3)
                    val g = rgb.intGreen.pad(3)
                    val b = rgb.intBlue.pad(3)
                    val ra = rgb.intAlpha.pad(3)
                    val c = cmyk.intCyan.pad(3)
                    val m = cmyk.intMagenta.pad(3)
                    val y = cmyk.intYellow.pad(3)
                    val k = cmyk.intKey.pad(3)
                    val ll = lab.intL.pad(3)
                    val la = lab.intA.pad(3)
                    val lb = lab.intB.pad(3)
                    Readout("HSL  H:$h  S:$s  L:$l  A:$ha")
                    Readout("RGB  R:$r  G:$g  B:$b  A:$ra")
                    Readout("CMYK C:$c  M:$m  Y:$y  K:$k")
                    Readout("LAB  L:$ll  a:$la  b:$lb")
                }

                // HSL section
                item { SectionHeader("HSL") }
                item { HueSlider(state = state, coloringMode = coloringMode) }
                item { SaturationSlider(state = state, coloringMode = coloringMode) }
                item { LightnessSlider(state = state, coloringMode = coloringMode) }

                item { HorizontalDivider() }

                // RGB section
                item { SectionHeader("RGB") }
                item { RedSlider(state = state, coloringMode = coloringMode) }
                item { GreenSlider(state = state, coloringMode = coloringMode) }
                item { BlueSlider(state = state, coloringMode = coloringMode) }

                item { HorizontalDivider() }

                // CMYK section
                item { SectionHeader("CMYK") }
                item { CyanSlider(state = state, coloringMode = coloringMode) }
                item { MagentaSlider(state = state, coloringMode = coloringMode) }
                item { YellowSlider(state = state, coloringMode = coloringMode) }
                item { KeySlider(state = state, coloringMode = coloringMode) }

                item { HorizontalDivider() }

                // LAB section
                item { SectionHeader("LAB") }
                item { LightnessLabSlider(state = state, coloringMode = coloringMode) }
                item { LabASlider(state = state, coloringMode = coloringMode) }
                item { LabBSlider(state = state, coloringMode = coloringMode) }

                item { HorizontalDivider() }

                // Alpha section
                item { SectionHeader("Alpha") }
                item { AlphaSlider(state = state) }
            }
        }

        if (showDialog) {
            ColorPickerDialog(
                onColorSelected = { color ->
                    state.updateFromHsl(color)
                    showDialog = false
                },
                onDismiss = { showDialog = false },
                initialColor = state.hslColor,
            )
        }
    }
}

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

private fun Int.pad(width: Int): String = toString().padStart(width)
