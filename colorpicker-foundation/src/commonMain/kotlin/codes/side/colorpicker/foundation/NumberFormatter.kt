package codes.side.colorpicker.foundation

/**
 * Numbers in one locale's own digits, separators, minus and percent placement: `40%`, `40 %` in French, `%40` in
 * Turkish, `٤٠٪` in Egyptian Arabic. It formats what it is given; rounding is the caller's, so every platform shows the
 * same number. Nothing is grouped, since no channel value runs to thousands.
 */
internal interface NumberFormatter {
    /** [value] as a whole number. */
    fun integer(value: Long): String

    /** [percent] whole percent, with the locale's sign and spacing. */
    fun percent(percent: Long): String

    /** [value] with exactly [places] decimals. */
    fun decimal(value: Double, places: Int): String
}

/** A formatter for [localeTag], a BCP 47 tag such as `"fr-FR"`, over the platform's own. */
internal expect fun NumberFormatter(localeTag: String): NumberFormatter
