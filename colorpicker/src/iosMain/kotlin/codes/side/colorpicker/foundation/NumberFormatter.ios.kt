package codes.side.colorpicker.foundation

import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.NSNumberFormatterPercentStyle
import platform.Foundation.NSNumberFormatterStyle

// NSLocale takes identifiers such as fr_FR; canonicalising accepts the BCP 47 fr-FR too.
internal actual fun NumberFormatter(localeTag: String): NumberFormatter =
    FoundationNumberFormatter(NSLocale(localeIdentifier = NSLocale.canonicalLocaleIdentifierFromString(localeTag)))

private class FoundationNumberFormatter(private val locale: NSLocale) : NumberFormatter {
    private val integers = formatter(NSNumberFormatterDecimalStyle, 0)
    private val percents = formatter(NSNumberFormatterPercentStyle, 0)
    private val decimals = HashMap<Int, NSNumberFormatter>()

    override fun integer(value: Long): String = integers.format(NSNumber(longLong = value))

    override fun percent(percent: Long): String = percents.format(NSNumber(double = percent / 100.0))

    override fun decimal(value: Double, places: Int): String =
        decimals.getOrPut(places) { formatter(NSNumberFormatterDecimalStyle, places) }.format(NSNumber(double = value))

    private fun formatter(style: NSNumberFormatterStyle, places: Int): NSNumberFormatter = NSNumberFormatter().apply {
        locale = this@FoundationNumberFormatter.locale
        numberStyle = style
        usesGroupingSeparator = false
        minimumFractionDigits = places.toULong()
        maximumFractionDigits = places.toULong()
    }

    private fun NSNumberFormatter.format(number: NSNumber): String =
        checkNotNull(stringFromNumber(number)) { "NSNumberFormatter could not format $number" }
}
