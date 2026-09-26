package codes.side.colorpicker.foundation

/**
 * The text a reader sees. Bidi marks around digits and signs differ by platform and version (Egyptian Arabic's
 * percent carries U+061C on some and not others), so tests compare what remains.
 */
internal fun String.visible(): String = filterNot { it == '‎' || it == '‏' || it == '؜' }
