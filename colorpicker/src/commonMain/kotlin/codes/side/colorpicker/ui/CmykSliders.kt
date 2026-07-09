package codes.side.colorpicker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import codes.side.colorpicker.conversion.toComposeColor
import codes.side.colorpicker.model.CmykColor
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.ColoringMode
import kotlinx.collections.immutable.persistentListOf

private val PureCyan = CmykColor(cyan = 1f, magenta = 0f, yellow = 0f, key = 0f).toComposeColor()
private val PureMagenta = CmykColor(cyan = 0f, magenta = 1f, yellow = 0f, key = 0f).toComposeColor()
private val PureYellow = CmykColor(cyan = 0f, magenta = 0f, yellow = 1f, key = 0f).toComposeColor()

@Composable
fun CyanSlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    coloringMode: ColoringMode = ColoringMode.Contextual,
    label: (@Composable () -> Unit)? = { SliderLabel("Cyan") },
    valueLabel: (@Composable () -> Unit)? = { SliderValueLabel("${state.cmykColor.intCyan}%") },
) {
    val cmyk = state.cmykColor
    val gradientColors = remember(cmyk.magenta, cmyk.yellow, cmyk.key, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> persistentListOf(Color.White, PureCyan)
            ColoringMode.Contextual -> persistentListOf(
                CmykColor(cyan = 0f, magenta = cmyk.magenta, yellow = cmyk.yellow, key = cmyk.key).toComposeColor(),
                CmykColor(cyan = 1f, magenta = cmyk.magenta, yellow = cmyk.yellow, key = cmyk.key).toComposeColor(),
            )
        }
    }
    val thumbColor = remember(cmyk, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> CmykColor(cyan = cmyk.cyan, magenta = 0f, yellow = 0f, key = 0f).toComposeColor()
            ColoringMode.Contextual -> cmyk.toComposeColor()
        }
    }

    ColorSlider(
        value = cmyk.cyan,
        onValueChange = { state.updateFromCmyk(cmyk.copy(cyan = it.coerceIn(0f, 1f))) },
        gradientColors = gradientColors,
        thumbColor = thumbColor,
        label = label,
        valueLabel = valueLabel,
        modifier = modifier,
        onValueChangeFinished = { state.isInteracting = false },
    )
}

@Composable
fun MagentaSlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    coloringMode: ColoringMode = ColoringMode.Contextual,
    label: (@Composable () -> Unit)? = { SliderLabel("Magenta") },
    valueLabel: (@Composable () -> Unit)? = { SliderValueLabel("${state.cmykColor.intMagenta}%") },
) {
    val cmyk = state.cmykColor
    val gradientColors = remember(cmyk.cyan, cmyk.yellow, cmyk.key, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> persistentListOf(Color.White, PureMagenta)
            ColoringMode.Contextual -> persistentListOf(
                CmykColor(cyan = cmyk.cyan, magenta = 0f, yellow = cmyk.yellow, key = cmyk.key).toComposeColor(),
                CmykColor(cyan = cmyk.cyan, magenta = 1f, yellow = cmyk.yellow, key = cmyk.key).toComposeColor(),
            )
        }
    }
    val thumbColor = remember(cmyk, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> CmykColor(cyan = 0f, magenta = cmyk.magenta, yellow = 0f, key = 0f).toComposeColor()
            ColoringMode.Contextual -> cmyk.toComposeColor()
        }
    }

    ColorSlider(
        value = cmyk.magenta,
        onValueChange = { state.updateFromCmyk(cmyk.copy(magenta = it.coerceIn(0f, 1f))) },
        gradientColors = gradientColors,
        thumbColor = thumbColor,
        label = label,
        valueLabel = valueLabel,
        modifier = modifier,
        onValueChangeFinished = { state.isInteracting = false },
    )
}

@Composable
fun YellowSlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    coloringMode: ColoringMode = ColoringMode.Contextual,
    label: (@Composable () -> Unit)? = { SliderLabel("Yellow") },
    valueLabel: (@Composable () -> Unit)? = { SliderValueLabel("${state.cmykColor.intYellow}%") },
) {
    val cmyk = state.cmykColor
    val gradientColors = remember(cmyk.cyan, cmyk.magenta, cmyk.key, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> persistentListOf(Color.White, PureYellow)
            ColoringMode.Contextual -> persistentListOf(
                CmykColor(cyan = cmyk.cyan, magenta = cmyk.magenta, yellow = 0f, key = cmyk.key).toComposeColor(),
                CmykColor(cyan = cmyk.cyan, magenta = cmyk.magenta, yellow = 1f, key = cmyk.key).toComposeColor(),
            )
        }
    }
    val thumbColor = remember(cmyk, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> CmykColor(cyan = 0f, magenta = 0f, yellow = cmyk.yellow, key = 0f).toComposeColor()
            ColoringMode.Contextual -> cmyk.toComposeColor()
        }
    }

    ColorSlider(
        value = cmyk.yellow,
        onValueChange = { state.updateFromCmyk(cmyk.copy(yellow = it.coerceIn(0f, 1f))) },
        gradientColors = gradientColors,
        thumbColor = thumbColor,
        label = label,
        valueLabel = valueLabel,
        modifier = modifier,
        onValueChangeFinished = { state.isInteracting = false },
    )
}

@Composable
fun KeySlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    coloringMode: ColoringMode = ColoringMode.Contextual,
    label: (@Composable () -> Unit)? = { SliderLabel("Key") },
    valueLabel: (@Composable () -> Unit)? = { SliderValueLabel("${state.cmykColor.intKey}%") },
) {
    val cmyk = state.cmykColor
    val gradientColors = remember(cmyk.cyan, cmyk.magenta, cmyk.yellow, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> persistentListOf(Color.White, Color.Black)
            ColoringMode.Contextual -> persistentListOf(
                CmykColor(cyan = cmyk.cyan, magenta = cmyk.magenta, yellow = cmyk.yellow, key = 0f).toComposeColor(),
                CmykColor(cyan = cmyk.cyan, magenta = cmyk.magenta, yellow = cmyk.yellow, key = 1f).toComposeColor(),
            )
        }
    }
    val thumbColor = remember(cmyk, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> CmykColor(cyan = 0f, magenta = 0f, yellow = 0f, key = cmyk.key).toComposeColor()
            ColoringMode.Contextual -> cmyk.toComposeColor()
        }
    }

    ColorSlider(
        value = cmyk.key,
        onValueChange = { state.updateFromCmyk(cmyk.copy(key = it.coerceIn(0f, 1f))) },
        gradientColors = gradientColors,
        thumbColor = thumbColor,
        label = label,
        valueLabel = valueLabel,
        modifier = modifier,
        onValueChangeFinished = { state.isInteracting = false },
    )
}
