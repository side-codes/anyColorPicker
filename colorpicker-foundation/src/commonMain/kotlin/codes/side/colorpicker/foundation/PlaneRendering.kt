package codes.side.colorpicker.foundation

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * How a [BasicChannelPlane] builds a field it samples on a grid, provided through
 * [LocalPlaneRendering]. HSL's saturation × lightness and HSV's saturation × value are two gradients
 * either way.
 */
public abstract class PlaneRendering internal constructor() {

    /**
     * The default. Each row goes straight to pixels through
     * [GamutMapper.convertToArgb][codes.side.color.GamutMapper.convertToArgb], and a color outside sRGB
     * has its chroma reduced by [EdgeSolver.Iterative][codes.side.color.EdgeSolver.Iterative]. Its pixels
     * are [Canonical]'s to within one level of each channel. Its rows are shared among up to four threads
     * where the platform has them; the browser has one.
     */
    public object Fast : PlaneRendering() {
        override fun toString(): String = "Fast"
    }

    /**
     * Each color mapped to Doubles by [EdgeSolver.ClosedForm][codes.side.color.EdgeSolver.ClosedForm]
     * and rounded, one row after another: the reference [Fast] is tested against.
     */
    public object Canonical : PlaneRendering() {
        override fun toString(): String = "Canonical"
    }
}

/** How the [BasicChannelPlane]s below build their fields: [PlaneRendering.Fast] unless provided. */
public val LocalPlaneRendering: ProvidableCompositionLocal<PlaneRendering> = staticCompositionLocalOf { PlaneRendering.Fast }
