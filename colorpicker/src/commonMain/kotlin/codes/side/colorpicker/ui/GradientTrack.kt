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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList

// M3 token defaults (from SliderTokens)
private val HandleWidth = 4.dp
private val PressedHandleWidth = 2.dp
private val ThumbTrackGapSize = 6.dp
private val TrackInsideCornerSize = 2.dp

private val CheckerLight = Color(0xFFFFFFFF)
private val CheckerDark = Color(0xFFCCCCCC)
private val CheckerCellSize = 6.dp

/**
 * Gradient track with an M3-style gap around the thumb.
 *
 * The gap shrinks when the thumb is pressed/dragged (matching M3's behavior where
 * the thumb narrows on interaction, causing the track to come closer).
 *
 * @param showCheckerboard if true, draws a transparency checkerboard underneath
 * the gradient (only within the track segments). Used by [AlphaSlider].
 */
@Composable
internal fun GradientTrack(
    colors: ImmutableList<Color>,
    thumbFraction: Float,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
    showCheckerboard: Boolean = false,
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
    val currentThumbWidth = if (isActive) PressedHandleWidth else HandleWidth

    val brush = Brush.horizontalGradient(colors)

    Canvas(modifier = modifier) {
        val cornerSize = size.height / 2f
        val insideCornerSize = TrackInsideCornerSize.toPx()
        val gap = currentThumbWidth.toPx() / 2f + ThumbTrackGapSize.toPx()
        val thumbCenter = thumbFraction.coerceIn(0f, 1f) * size.width

        val leftEnd = thumbCenter - gap
        val rightStart = thumbCenter + gap

        val clipPath = Path()

        if (leftEnd > 0f) {
            clipPath.addRoundRect(
                RoundRect(
                    rect = Rect(
                        offset = Offset.Zero,
                        size = Size(leftEnd, size.height),
                    ),
                    topLeft = CornerRadius(cornerSize),
                    bottomLeft = CornerRadius(cornerSize),
                    topRight = CornerRadius(insideCornerSize),
                    bottomRight = CornerRadius(insideCornerSize),
                )
            )
        }

        if (rightStart < size.width) {
            clipPath.addRoundRect(
                RoundRect(
                    rect = Rect(
                        offset = Offset(rightStart, 0f),
                        size = Size(size.width - rightStart, size.height),
                    ),
                    topLeft = CornerRadius(insideCornerSize),
                    bottomLeft = CornerRadius(insideCornerSize),
                    topRight = CornerRadius(cornerSize),
                    bottomRight = CornerRadius(cornerSize),
                )
            )
        }

        clipPath(clipPath) {
            if (showCheckerboard) {
                val cell = CheckerCellSize.toPx()
                val cols = (size.width / cell).toInt() + 1
                val rows = (size.height / cell).toInt() + 1
                for (row in 0 until rows) {
                    for (col in 0 until cols) {
                        val color = if ((row + col) % 2 == 0) CheckerLight else CheckerDark
                        drawRect(
                            color = color,
                            topLeft = Offset(col * cell, row * cell),
                            size = Size(cell, cell),
                        )
                    }
                }
            }
            drawRect(brush = brush, topLeft = Offset.Zero, size = size)
        }
    }
}
