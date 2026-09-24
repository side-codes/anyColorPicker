package codes.side.color.internal

/**
 * Every threshold the library takes from CSS Color 4, in one place, as the Editor's Draft of [DRAFT]
 * has them. The draft is still moving; when it changes one of these, the value and the date move
 * together, and the WPT boundary tests say which.
 */
internal object ColorRules {
    const val DRAFT: String = "2026-09-13"

    /** LCH's hue is powerless at or below this chroma. */
    const val LCH_POWERLESS_CHROMA: Double = 0.0015

    /** OkLCh's hue is powerless at or below this Oklab chroma, and Okhsl's and Okhsv's with it. */
    const val OKLCH_POWERLESS_CHROMA: Double = 0.000004

    /** HSL's hue is powerless at or below this saturation, in percent. */
    const val HSL_POWERLESS_SATURATION: Double = 0.001

    /** HWB's hue is powerless at or above this whiteness plus blackness, in percent. */
    const val HWB_POWERLESS_WHITENESS_PLUS_BLACKNESS: Double = 99.999

    /** HSV's hue is powerless at or below this |(S/100)·(V/100)|: for S·V ≥ 0, HWB's rule restated, as W + B = 100·(1 − S·V). */
    const val HSV_POWERLESS_SATURATION_TIMES_VALUE: Double = 1e-5

    /** How far apart equivalent colors' Oklab L, a, b and alpha may be (CSS Color 4 §12); in one space, a component may differ by this much of its channel's reference range. */
    const val EQUIVALENCE_EPSILON: Double = 1e-5

    /** The binary-search gamut mapping's just-noticeable difference, in ΔEOK. */
    const val GAMUT_MAPPING_JND: Double = 0.02

    /** The binary-search gamut mapping's chroma resolution. */
    const val GAMUT_MAPPING_EPSILON: Double = 1e-4

    /** How far outside `0..1` an in-gamut channel may be: color.js's and ColorAide's default, which CSS leaves open. */
    const val IN_GAMUT_TOLERANCE: Double = 7.5e-5
}
