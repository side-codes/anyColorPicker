package codes.side.colorpicker.model

/**
 * This hue in `0..<360`, wrapped rather than clamped.
 *
 * Hue is an angle, so `370` is `10` and `-10` is `350`. Clamping would answer red to both,
 * which is a plausible-looking wrong colour rather than a rejected one — the failure a caller
 * stepping a hue past the end would be least likely to notice.
 */
internal fun wrapHue(degrees: Int): Float {
    val wrapped = degrees % 360
    return (if (wrapped < 0) wrapped + 360 else wrapped).toFloat()
}
