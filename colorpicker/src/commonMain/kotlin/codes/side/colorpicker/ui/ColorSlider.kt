package codes.side.colorpicker.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ColorSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    gradientColors: ImmutableList<Color>,
    thumbColor: Color,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null,
    valueLabel: (@Composable () -> Unit)? = null,
    onValueChangeFinished: (() -> Unit)? = null,
    trackHeight: Dp = 16.dp,
    showCheckerboard: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(modifier = modifier.fillMaxWidth()) {
        if (label != null || valueLabel != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box { label?.invoke() }
                Box { valueLabel?.invoke() }
            }
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            modifier = Modifier.fillMaxWidth(),
            interactionSource = interactionSource,
            track = {
                GradientTrack(
                    colors = gradientColors,
                    thumbFraction = value,
                    interactionSource = interactionSource,
                    showCheckerboard = showCheckerboard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(trackHeight),
                )
            },
            colors = SliderDefaults.colors(
                thumbColor = thumbColor,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
            ),
        )
    }
}
