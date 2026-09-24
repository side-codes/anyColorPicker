package codes.side.color

/** The library's color spaces. */
public object ColorSpaces {
    /** All fifteen, the parser's default set of known spaces. */
    public val all: List<ColorSpace> by lazy {
        listOf(XyzD65, XyzD50, SrgbLinear, Srgb, DisplayP3, Lab, Lch, Oklab, OkLch, Hsl, Hwb, Hsv, Okhsl, Okhsv, Cmyk)
    }
}
