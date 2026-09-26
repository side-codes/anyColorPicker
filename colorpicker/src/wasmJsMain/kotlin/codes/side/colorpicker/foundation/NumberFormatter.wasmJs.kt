package codes.side.colorpicker.foundation

internal actual fun NumberFormatter(localeTag: String): NumberFormatter = IntlNumberFormatter(localeTag)

private class IntlNumberFormatter(private val tag: String) : NumberFormatter {
    override fun integer(value: Long): String = intlFormat(tag, value.toDouble(), "decimal", 0)

    override fun percent(percent: Long): String = intlFormat(tag, percent / 100.0, "percent", 0)

    override fun decimal(value: Double, places: Int): String = intlFormat(tag, value, "decimal", places)
}

// Kotlin/Wasm allows js() only as the whole body of a package-level function. Its JavaScript interop is still marked
// experimental, and it is the only way to reach Intl, which holds the browser's number formats.
@OptIn(ExperimentalWasmJsInterop::class)
private fun intlFormat(tag: String, value: Double, style: String, places: Int): String =
    js("new Intl.NumberFormat(tag, { style: style, minimumFractionDigits: places, maximumFractionDigits: places, useGrouping: false }).format(value)")
