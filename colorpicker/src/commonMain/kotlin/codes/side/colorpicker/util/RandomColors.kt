package codes.side.colorpicker.util

import codes.side.colorpicker.model.HslColor
import kotlin.random.Random

fun randomHslColor(pure: Boolean = false): HslColor {
    return HslColor(
        hue = Random.nextFloat() * 360f,
        saturation = if (pure) 1f else Random.nextFloat(),
        lightness = if (pure) 0.5f else Random.nextFloat(),
    )
}
