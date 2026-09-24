package codes.side.color

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EquivalenceTest {

    @Test
    fun cssWorkedExamplesGiveTheAnswersTheyState() {
        val wrong = CSS.equivalentColors.filter { (_, a, b, equivalent) ->
            val first = ColorValue.parseCss(a)
            val second = ColorValue.parseCss(b)
            first.isEquivalentTo(second) != equivalent || second.isEquivalentTo(first) != equivalent
        }
        assertEquals(emptyList(), wrong)
    }

    @Test
    fun aMissingAlphaEqualsOnlyAMissingAlpha() {
        assertFalse(Srgb(1.0, 0.0, 0.0, alpha = null).isEquivalentTo(Srgb(1.0, 0.0, 0.0, alpha = 0.0)))
        assertTrue(Srgb(1.0, 0.0, 0.0, alpha = null).isEquivalentTo(Srgb(1.0, 0.0, 0.0, alpha = null)))
    }

    @Test
    fun anAppsHslOverAnotherRgbSpaceIsItsOwnSpace() {
        val hslP3 = ColorSpace.hsl("--hsl-p3", DisplayP3)
        assertTrue(hslP3(0.0, 0.0, 50.0).isEquivalentTo(hslP3(200.0, 0.0, 50.0)))
        assertFalse(hslP3(null, 0.0, 50.0).isEquivalentTo(hslP3(null, 0.0, 50.0).to(DisplayP3)))
    }
}
