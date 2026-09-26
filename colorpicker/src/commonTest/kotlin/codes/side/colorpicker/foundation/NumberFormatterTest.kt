package codes.side.colorpicker.foundation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NumberFormatterTest {

    @Test
    fun englishIsPlain() {
        val en = NumberFormatter("en-US")
        assertEquals("128", en.integer(128))
        assertEquals("-12", en.integer(-12))
        assertEquals("1000", en.integer(1000), "no grouping")
        assertEquals("50%", en.percent(50))
        assertEquals("-0.125", en.decimal(-0.125, 3))
        assertEquals("0.500", en.decimal(0.5, 3))
    }

    @Test
    fun frenchSpacesItsPercentAndWritesACommaDecimal() {
        val fr = NumberFormatter("fr-FR")
        val percent = fr.percent(50)
        // A no-break space on some platforms, a narrow one on others.
        assertTrue(Regex("50[  ]%").matches(percent), percent)
        assertEquals("0,500", fr.decimal(0.5, 3))
    }

    @Test
    fun turkishPutsThePercentFirst() {
        assertEquals("%50", NumberFormatter("tr-TR").percent(50))
    }

    @Test
    fun egyptianArabicWritesItsOwnDigits() {
        val ar = NumberFormatter("ar-EG")
        assertEquals("٤٠", ar.integer(40).visible())
        assertEquals("٤٠٪", ar.percent(40).visible())
    }

    @Test
    fun swedishWritesItsOwnMinus() {
        assertEquals("−0,125", NumberFormatter("sv-SE").decimal(-0.125, 3).visible())
    }

    @Test
    fun zeroKeepsNoSignInAnyLocale() {
        for (tag in listOf("en-US", "fr-FR", "sv-SE", "ar-EG")) {
            val zero = NumberFormatter(tag).decimal(roundHalfAwayFromZero(-0.0004, 3), 3).visible()
            assertFalse(zero.any { it == '-' || it == '−' }, "$tag wrote $zero")
        }
    }
}
