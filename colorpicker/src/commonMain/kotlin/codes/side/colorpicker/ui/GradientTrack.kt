package codes.side.colorpicker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import codes.side.colorpicker.theme.ColorPickerDefaults
import kotlinx.collections.immutable.ImmutableList

// M3 token defaults (from SliderTokens). The handle narrows by this much while pressed;
// the same shrink is applied to a custom thumb so the track keeps its M3 feel.
private val PressedWidthReduction = 2.dp
private val TrackInsideCornerSize = 2.dp

/**
 * Gradient track with an M3-style gap around the thumb.
 *
 * The gap shrinks when the thumb is pressed/dragged (matching M3's behavior where
 * the thumb narrows on interaction, causing the track to come closer). The track's
 * outer corners come from [trackShape]; in right-to-left layouts the gradient and
 * the thumb gap are mirrored to match the mirrored M3 [androidx.compose.material3.Slider].
 *
 * @param showCheckerboard if true, draws a transparency checkerboard underneath
 * the gradient (only within the track segments). Used by [AlphaSlider].
 */
@Composable
internal fun GradientTrack(
    colors: ImmutableList<Color>,
    thumbFraction: Float,
    interactionSource: MutableInteractionSource,
    checkerboardLight: Color,
    checkerboardDark: Color,
    trackShape: Shape,
    modifier: Modifier = Modifier,
    showCheckerboard: Boolean = false,
    thumbWidth: Dp = ColorPickerDefaults.ThumbWidth,
    thumbTrackGap: Dp = ColorPickerDefaults.ThumbTrackGap,
) {
    val interactions = remember { mutableStateListOf<Interaction>() }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> interactions.add(interaction)
                is PressInteraction.Release -> interactions.remove(interaction.press)
                is PressInteraction.Cancel -> interactions.remove(interaction.press)
                is DragInteraction.Start -> interactions.add(interaction)
                is DragInteraction.Stop -> interactions.remove(interaction.start)
                is DragInteraction.Cancel -> interactions.remove(interaction.start)
            }
        }
    }

    val isActive = interactions.isNotEmpty()
    val currentThumbWidth = if (isActive) {
        (thumbWidth - PressedWidthReduction).coerceAtLeast(0.dp)
    } else {
        thumbWidth
    }

    val layoutDirection = LocalLayoutDirection.current
    val brush = remember(colors, layoutDirection) {
        Brush.horizontalGradient(
            if (layoutDirection == LayoutDirection.Rtl) colors.reversed() else colors,
        )
    }
    val checkerboardBrush = if (showCheckerboard) {
        rememberCheckerboardBrush(
            cellSize = CheckerboardCellSize,
            light = checkerboardLight,
            dark = checkerboardDark,
        )
    } else {
        null
    }
    val segmentsPath = remember { Path() }

    Canvas(modifier = modifier.clip(trackShape)) {
        val insideCornerSize = TrackInsideCornerSize.toPx()
        val gap = currentThumbWidth.toPx() / 2f + thumbTrackGap.toPx()
        val fractionCenter = thumbFraction.coerceIn(0f, 1f) * size.width
        // M3 Slider mirrors the thumb position in RTL, so the gap must mirror
        // to stay underneath the thumb.
        val thumbCenter = if (this.layoutDirection == LayoutDirection.Rtl) {
            size.width - fractionCenter
        } else {
            fractionCenter
        }

        val leftEnd = thumbCenter - gap
        val rightStart = thumbCenter + gap

        // Outer corners are clipped by trackShape; only the corners facing the
        // thumb gap are rounded here.
        segmentsPath.reset()

        if (leftEnd > 0f) {
            segmentsPath.addRoundRect(
                RoundRect(
                    rect = Rect(
                        offset = Offset.Zero,
                        size = Size(leftEnd, size.height),
                    ),
                    topLeft = CornerRadius.Zero,
                    bottomLeft = CornerRadius.Zero,
                    topRight = CornerRadius(insideCornerSize),
                    bottomRight = CornerRadius(insideCornerSize),
                )
            )
        }

        if (rightStart < size.width) {
            segmentsPath.addRoundRect(
                RoundRect(
                    rect = Rect(
                        offset = Offset(rightStart, 0f),
                        size = Size(size.width - rightStart, size.height),
                    ),
                    topLeft = CornerRadius(insideCornerSize),
                    bottomLeft = CornerRadius(insideCornerSize),
                    topRight = CornerRadius.Zero,
                    bottomRight = CornerRadius.Zero,
                )
            )
        }

        clipPath(segmentsPath) {
            if (checkerboardBrush != null) {
                drawRect(brush = checkerboardBrush)
            }
            drawRect(brush = brush)
        }
    }
}
