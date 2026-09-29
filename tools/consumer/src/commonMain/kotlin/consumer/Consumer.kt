package consumer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import codes.side.color.ColorValue
import codes.side.color.Okhsv
import codes.side.color.Oklch
import codes.side.color.Srgb
import codes.side.color.compose.toComposeColor
import codes.side.color.parseCss
import codes.side.color.toGamut
import codes.side.colorpicker.foundation.BasicAlphaSlider
import codes.side.colorpicker.foundation.BasicChannelPlane
import codes.side.colorpicker.foundation.BasicChannelSlider
import codes.side.colorpicker.foundation.BasicColorPicker
import codes.side.colorpicker.material3.ColorPicker
import codes.side.colorpicker.material3.ColorPickerDialog
import codes.side.colorpicker.material3.ColorSwatch
import codes.side.colorpicker.state.rememberSaveableColorPickerState

/** Each published module in use, so compiling and linking this resolves every one of their artifacts. */
@Composable
fun Consumer() {
    val state = rememberSaveableColorPickerState(ColorValue.parseCss("oklch(70% 0.15 140)"))
    var dialogOpen by remember { mutableStateOf(false) }
    ColorPicker(state)
    ColorSwatch(color = state.color, modifier = Modifier)
    BasicColorPicker(
        state = state,
        space = Okhsv,
        plane = { part -> BasicChannelPlane(part.state, part.x, part.y, thumb = {}) },
        channelSlider = { part -> BasicChannelSlider(part.state, part.channel, track = {}, thumb = {}) },
        alphaSlider = { part -> BasicAlphaSlider(part.state, track = {}, thumb = {}) },
    )
    if (dialogOpen) {
        ColorPickerDialog(
            initialValue = state.value,
            onValueSelected = { value ->
                state.value = value
                dialogOpen = false
            },
            onDismissRequest = { dialogOpen = false },
        )
    }
}

/** The color model and the Compose bridge, outside composition. */
fun mappedColor(): Color = Oklch(0.7, 0.3, 150.0).toGamut(Srgb.gamut).toComposeColor()
