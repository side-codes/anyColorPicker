package codes.side.colorpicker.foundation

import java.util.Locale

/**
 * Runs [block] with the JVM's default locale set to [tag], and restores it after. Compose reads the locale when a
 * composition starts, so [block] must start its composition inside.
 */
internal fun <T> withDefaultLocale(tag: String, block: () -> T): T {
    val previous = Locale.getDefault()
    Locale.setDefault(Locale.forLanguageTag(tag))
    try {
        return block()
    } finally {
        Locale.setDefault(previous)
    }
}
