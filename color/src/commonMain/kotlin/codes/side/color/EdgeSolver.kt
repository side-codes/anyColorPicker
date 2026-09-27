package codes.side.color

/**
 * How [GamutMapping.ChromaReduction] finds a gamut's edge along a line of constant OkLCh lightness and
 * hue: the largest chroma, up to the color's own, that the gamut holds. Each gives the same bits for a
 * color whatever was asked before it. The two agree to 1e-12 in chroma, except near black, where a
 * gamut's channels are about as small as the tolerance its edge is found to, so two can reach zero
 * within it of each other and the solvers may stop on different ones; the colors then differ by less
 * than 1e-8 in linear light.
 */
public abstract class EdgeSolver internal constructor() {

    /**
     * Each channel's cubic solved in closed form, with cube roots or trigonometry, and each root
     * polished by Newton's method. The default.
     */
    public object ClosedForm : EdgeSolver() {
        override fun toString(): String = "ClosedForm"
    }

    /**
     * Newton's method on a channel that is outside, inside a stretch where its cubic is monotone,
     * walking in to the edge from past the most chroma the gamut reaches, or, for a color inside the
     * sliver past pure blue, from its own chroma: no cube root or trigonometry. Faster where each color
     * needs an edge of its own, as along LCH's lines of constant hue, which curve through Oklab.
     */
    public object Iterative : EdgeSolver() {
        override fun toString(): String = "Iterative"
    }
}
