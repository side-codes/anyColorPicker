package codes.side.colorpicker.material3

import androidx.compose.runtime.Composable
import codes.side.color.ColorSpace
import codes.side.colorpicker.foundation.ColorPickerStrings

/** Whether a picker over [space] draws a plane: [space] has one hue and two other channels. */
internal fun expectsPlane(space: ColorSpace): Boolean = space.channels.size == 3 && space.channels.count { it.isHue } == 1

/** What a screen reader calls the sliders of a picker over [space]: each channel's spoken name, then alpha's. */
@Composable
internal fun spokenSliderNames(space: ColorSpace): List<String> =
    space.channels.map { ColorPickerStrings.current.channelSpokenName(it) } + ColorPickerStrings.current.alphaName()
