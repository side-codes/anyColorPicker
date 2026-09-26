package codes.side.colorpicker.foundation

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** A value a test holds for a [BasicColorSlider], and what the slider reported. */
internal class HeldSlider(value: Float = 0.5f) {
    var value by mutableFloatStateOf(value)
    var enabled by mutableStateOf(true)
    val reported = mutableListOf<Float>()
    var finished = 0
}

/**
 * [held]'s slider, tagged "slider", drawn with foundation alone: 220 dp wide with a 20 dp thumb over an
 * 8 dp track, so the track is 200 dp wide and starts 10 dp in. Each report is written back, as a caller
 * that owns the value does.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.showSlider(
    held: HeldSlider,
    direction: LayoutDirection = LayoutDirection.Ltr,
    thumbColor: Color = Color.Unspecified,
    step: Float = 0.01f,
    semanticLabel: String? = null,
    interactionSource: MutableInteractionSource? = null,
    track: @Composable ColorSliderScope.() -> Unit = { Box(Modifier.fillMaxWidth().height(8.dp)) },
    thumb: @Composable ColorSliderScope.() -> Unit = { Box(Modifier.size(20.dp)) },
) {
    setContent {
        CompositionLocalProvider(LocalLayoutDirection provides direction) {
            BasicColorSlider(
                value = held.value,
                onValueChange = {
                    held.reported += it
                    held.value = it
                },
                modifier = Modifier.width(220.dp).testTag("slider"),
                enabled = held.enabled,
                thumbColor = thumbColor,
                onValueChangeFinished = { held.finished++ },
                step = step,
                semanticLabel = semanticLabel,
                interactionSource = interactionSource,
                track = track,
                thumb = thumb,
            )
        }
    }
}

/** How far from [showSlider]'s left edge, in pixels, [fraction] of the way along its track lies, left to right. */
internal fun Density.alongTrack(fraction: Float): Float = (10.dp + 200.dp * fraction).toPx()
