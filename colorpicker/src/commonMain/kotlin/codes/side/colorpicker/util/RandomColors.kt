package codes.side.colorpicker.util

import codes.side.colorpicker.model.HslColor
import kotlin.random.Random

/**
 * Returns a random opaque [HslColor] with a uniformly random hue.
 *
 * @param pure if `true`, saturation is fixed at `1` and lightness at `0.5` (a fully
 * saturated "pure" hue); if `false` (the default), both are also uniformly random.
 */
public fun randomHslColor(pure: Boolean = false): HslColor {
    return HslColor(
        hue = Random.nextFloat() * 360f,
        saturation = if (pure) 1f else Random.nextFloat(),
        lightness = if (pure) 0.5f else Random.nextFloat(),
    )
}
