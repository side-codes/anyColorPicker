package codes.side.color

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EquivalenceTest {

    // CSS Color 4 §12's worked examples, each with the answer it states.
    private val cssExamples = listOf(
        Triple("red", "rgb(255, 0, 0)", true),
        Triple("red", "color(srgb 1 0 0)", true),
        Triple("#ff000080", "rgb(255 0 0 / 50%)", false),
        Triple("hsl(0 0% 50%)", "hsl(200 0% 50%)", true),
        Triple("white", "hwb(120 100% 0%)", true),
        Triple("oklch(50% 0 40)", "oklch(50% 0 200)", true),
        Triple("lch(50% 0.001 30)", "lch(50% 0.001 200)", true),
        Triple("lch(50% 30 0)", "lch(50% 30 360)", true),
        Triple("rgb(none 128 0)", "rgb(0 128 0)", false),
        Triple("oklch(50% 0 none)", "oklab(50% 0 0)", false),
        Triple("red", "color(srgb-linear 1 0 0)", true),
        Triple("color(display-p3 1 1 1)", "white", true),
        Triple("lab(50% 0 0)", "oklab(0.5 0 0)", false),
        Triple("red", "color(display-p3 0.91748756 0.20028681 0.13856059)", true),
    )

    @Test
    fun cssWorkedExamplesGiveTheAnswersTheyState() {
        val wrong = cssExamples.filter { (a, b, equivalent) ->
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
