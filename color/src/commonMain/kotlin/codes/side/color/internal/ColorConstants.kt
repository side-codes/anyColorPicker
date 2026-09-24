// CSS Color 4's conversion constants. The whites, the RGB matrices and the LMS shortcuts are derived
// in exact fractions and rounded once; Bradford's and Oklab's matrices are CSS's published 64-bit
// values. ColorConstantsDerivationTest recomputes each one and holds every literal to it bit for
// bit: to add a constant, add its derivation there first and paste the value the failure reports.
package codes.side.color.internal

/** The D65 white as XYZ with Y = 1, from its chromaticity (0.3127, 0.3290). */
internal val D65_XYZ: DoubleArray = doubleArrayOf(
    0.9504559270516717, 1.0, 1.0890577507598784,
)

/** The D50 white as XYZ with Y = 1, from its chromaticity (0.3457, 0.3585). */
internal val D50_XYZ: DoubleArray = doubleArrayOf(
    0.9642956764295676, 1.0, 0.8251046025104602,
)

/** Linear sRGB to XYZ-D65, derived from the sRGB primaries. Row-major. */
internal val SRGB_LINEAR_TO_XYZ_D65: DoubleArray = doubleArrayOf(
    0.4123907992659595, 0.35758433938387796, 0.1804807884018343,
    0.21263900587151036, 0.7151686787677559, 0.07219231536073371,
    0.01933081871559185, 0.11919477979462599, 0.9505321522496606,
)

/** The exact inverse of [SRGB_LINEAR_TO_XYZ_D65]. Row-major. */
internal val XYZ_D65_TO_SRGB_LINEAR: DoubleArray = doubleArrayOf(
    3.2409699419045213, -1.5373831775700935, -0.4986107602930033,
    -0.9692436362808798, 1.8759675015077206, 0.04155505740717561,
    0.05563007969699361, -0.20397695888897657, 1.0569715142428786,
)

/** Linear Display P3 to XYZ-D65, derived from the P3 primaries. Row-major. */
internal val DISPLAY_P3_LINEAR_TO_XYZ_D65: DoubleArray = doubleArrayOf(
    0.48657094864821626, 0.26566769316909294, 0.1982172852343625,
    0.22897456406974884, 0.6917385218365062, 0.079286914093745,
    0.0, 0.045113381858902575, 1.0439443689009757,
)

/** The exact inverse of [DISPLAY_P3_LINEAR_TO_XYZ_D65]. Row-major. */
internal val XYZ_D65_TO_DISPLAY_P3_LINEAR: DoubleArray = doubleArrayOf(
    2.4934969119414245, -0.9313836179191236, -0.40271078445071684,
    -0.829488969561575, 1.7626640603183468, 0.02362468584194359,
    0.035845830243784335, -0.07617238926804171, 0.9568845240076873,
)

/** Bradford adaptation from D65 to D50, CSS's published matrix. Row-major. */
internal val XYZ_D65_TO_D50: DoubleArray = doubleArrayOf(
    1.0479297925449969, 0.022946870601609652, -0.05019226628920524,
    0.02962780877005599, 0.9904344267538799, -0.017073799063418826,
    -0.009243040646204504, 0.015055191490298152, 0.7518742814281371,
)

/** Bradford adaptation from D50 to D65, CSS's published matrix. Row-major. */
internal val XYZ_D50_TO_D65: DoubleArray = doubleArrayOf(
    0.955473421488075, -0.02309845494876471, 0.06325924320057072,
    -0.0283697093338637, 1.0099953980813041, 0.021041441191917323,
    0.012314014864481998, -0.020507649298898964, 1.330365926242124,
)

/** XYZ-D65 to Oklab's LMS, CSS's published matrix. Row-major. */
internal val XYZ_D65_TO_LMS: DoubleArray = doubleArrayOf(
    0.819022437996703, 0.3619062600528904, -0.1288737815209879,
    0.0329836539323885, 0.9292868615863434, 0.0361446663506424,
    0.0481771893596242, 0.2642395317527308, 0.6335478284694309,
)

/** Oklab's LMS to XYZ-D65, CSS's published matrix. Row-major. */
internal val LMS_TO_XYZ_D65: DoubleArray = doubleArrayOf(
    1.2268798758459243, -0.5578149944602171, 0.2813910456659647,
    -0.0405757452148008, 1.112286803280317, -0.0717110580655164,
    -0.0763729366746601, -0.4214933324022432, 1.5869240198367816,
)

/** Cube-rooted LMS to Oklab, CSS's published matrix. Row-major. */
internal val LMS_TO_OKLAB: DoubleArray = doubleArrayOf(
    0.210454268309314, 0.7936177747023054, -0.0040720430116193,
    1.9779985324311684, -2.42859224204858, 0.450593709617411,
    0.0259040424655478, 0.7827717124575296, -0.8086757549230774,
)

/** Oklab to cube-rooted LMS, CSS's published matrix. Row-major. */
internal val OKLAB_TO_LMS: DoubleArray = doubleArrayOf(
    1.0, 0.3963377773761749, 0.2158037573099136,
    1.0, -0.1055613458156586, -0.0638541728258133,
    1.0, -0.0894841775298119, -1.2914855480194092,
)

/** Oklab's LMS straight to linear sRGB: [XYZ_D65_TO_SRGB_LINEAR] × [LMS_TO_XYZ_D65], multiplied exactly. sRGB's gamut, and Okhsl and Okhsv with it, is found through it. Row-major. */
internal val LMS_TO_SRGB_LINEAR: DoubleArray = doubleArrayOf(
    4.076741636075958, -3.307711539258062, 0.2309699031821045,
    -1.268437973285032, 2.6097573492876887, -0.34131937600265727,
    -0.004196076138675557, -0.7034186179359363, 1.7076146940746117,
)

/** Linear sRGB straight to Oklab's LMS: [XYZ_D65_TO_LMS] × [SRGB_LINEAR_TO_XYZ_D65], multiplied exactly. Row-major. */
internal val SRGB_LINEAR_TO_LMS: DoubleArray = doubleArrayOf(
    0.412221469470763, 0.5363325372617348, 0.0514459932675022,
    0.21190349581782522, 0.6806995506452344, 0.10739695353694055,
    0.08830245919005643, 0.2817188391361215, 0.6299787016738221,
)

/** Oklab's LMS straight to linear Display P3: [XYZ_D65_TO_DISPLAY_P3_LINEAR] × [LMS_TO_XYZ_D65], multiplied exactly. Row-major. */
internal val LMS_TO_DISPLAY_P3_LINEAR: DoubleArray = doubleArrayOf(
    3.1277689713618737, -2.257135762591638, 0.1293667912297652,
    -1.0910090184377979, 2.413331710306922, -0.3223226918691248,
    -0.02601080193857049, -0.5080413317041669, 1.5340521336427373,
)

/** Linear Display P3 straight to Oklab's LMS: [XYZ_D65_TO_LMS] × [DISPLAY_P3_LINEAR_TO_XYZ_D65], multiplied exactly. Row-major. */
internal val DISPLAY_P3_LINEAR_TO_LMS: DoubleArray = doubleArrayOf(
    0.48137985274995443, 0.46211837101131803, 0.056501776238727555,
    0.22883194181124475, 0.6532168193835676, 0.11795123880518778,
    0.08394575232299319, 0.22416527097756642, 0.6918889766994404,
)
