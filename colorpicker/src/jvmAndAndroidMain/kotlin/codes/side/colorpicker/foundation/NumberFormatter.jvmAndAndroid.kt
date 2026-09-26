package codes.side.colorpicker.foundation

import java.text.NumberFormat
import java.util.Locale

internal actual fun NumberFormatter(localeTag: String): NumberFormatter = JavaTextNumberFormatter(Locale.forLanguageTag(localeTag))

// java.text formats are not thread-safe; each formatter is remembered by one composition.
private class JavaTextNumberFormatter(private val locale: Locale) : NumberFormatter {
    private val integers = NumberFormat.getIntegerInstance(locale).apply { isGroupingUsed = false }
    private val percents = NumberFormat.getPercentInstance(locale).apply {
        isGroupingUsed = false
        maximumFractionDigits = 0
    }
    private val decimals = HashMap<Int, NumberFormat>()

    override fun integer(value: Long): String = integers.format(value)

    override fun percent(percent: Long): String = percents.format(percent / 100.0)

    override fun decimal(value: Double, places: Int): String =
        decimals.getOrPut(places) {
            NumberFormat.getNumberInstance(locale).apply {
                isGroupingUsed = false
                minimumFractionDigits = places
                maximumFractionDigits = places
            }
        }.format(value)
}
