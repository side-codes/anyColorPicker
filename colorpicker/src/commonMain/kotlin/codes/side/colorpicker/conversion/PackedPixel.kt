package codes.side.colorpicker.conversion

/** Opaque white as a packed `0xAARRGGBB` pixel. */
internal const val OPAQUE_WHITE: Int = -0x1

/** Opaque black as a packed `0xAARRGGBB` pixel. */
internal const val OPAQUE_BLACK: Int = -0x1000000

/** Linear-light sRGB as one opaque packed `0xAARRGGBB` pixel, clamped into the gamut. */
internal fun packOpaque(red: Double, green: Double, blue: Double): Int =
    (0xFF shl 24) or
        (linearToSrgbByte(red) shl 16) or
        (linearToSrgbByte(green) shl 8) or
        linearToSrgbByte(blue)
