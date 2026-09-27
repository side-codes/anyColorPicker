package codes.side.colorpicker.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import codes.side.color.Okhsv
import codes.side.colorpicker.foundation.BasicAlphaSlider
import codes.side.colorpicker.foundation.BasicChannelPlane
import codes.side.colorpicker.foundation.BasicChannelSlider
import codes.side.colorpicker.foundation.BasicColorPicker
import codes.side.colorpicker.foundation.LocalColorPickerEnabled
import codes.side.colorpicker.foundation.checkerboard
import codes.side.colorpicker.state.ColorPickerState

/**
 * A picker with no Material in it: foundation's [BasicColorPicker] with round thumbs, pill tracks and a rounded plane
 * of its own. Dragging, the keyboard, the screen reader and the disabled state all come from the Basic components;
 * this file only draws.
 */
@Composable
fun FoundationPicker(state: ColorPickerState, enabled: Boolean, modifier: Modifier = Modifier) {
    BasicColorPicker(
        state = state,
        space = Okhsv,
        plane = { part ->
            BasicChannelPlane(
                part.state,
                part.x,
                part.y,
                // The plane draws its surface itself, outside any slot, so it is dimmed here as a whole, thumb and
                // all, from the picker's state.
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.6f)
                    .alpha(if (LocalColorPickerEnabled.current) 1f else DisabledAlpha),
                shape = RoundedCornerShape(16.dp),
                thumb = { RoundThumb(thumbColor, 28.dp, dimmed = false) },
            )
        },
        channelSlider = { part ->
            BasicChannelSlider(
                part.state,
                part.channel,
                Modifier.fillMaxWidth(),
                track = { PillTrack(gradient, this.enabled) },
                thumb = { RoundThumb(thumbColor, 24.dp, dimmed = !this.enabled) },
            )
        },
        alphaSlider = { part ->
            BasicAlphaSlider(
                part.state,
                Modifier.fillMaxWidth(),
                track = { PillTrack(gradient, this.enabled, checkerboard = true) },
                thumb = { RoundThumb(thumbColor, 24.dp, dimmed = !this.enabled) },
            )
        },
        modifier = modifier,
        enabled = enabled,
        spacing = 16.dp,
    )
}

// The opacity Material gives disabled content.
private const val DisabledAlpha = 0.38f

// A 12 dp pill of the track's gradient, over a checkerboard when the gradient is translucent.
@Composable
private fun PillTrack(gradient: Brush, enabled: Boolean, checkerboard: Boolean = false) {
    val pill = Modifier
        .fillMaxWidth()
        .height(12.dp)
        .alpha(if (enabled) 1f else DisabledAlpha)
        .clip(CircleShape)
    Box(
        (if (checkerboard) pill.checkerboard(Color.White, Color.LightGray) else pill).background(gradient),
    )
}

// The color in a white ring, lifted by a shadow so it reads against a track of the same color.
@Composable
private fun RoundThumb(color: Color, size: Dp, dimmed: Boolean) {
    Box(
        Modifier
            .size(size)
            .alpha(if (dimmed) DisabledAlpha else 1f)
            .shadow(2.dp, CircleShape)
            .background(Color.White, CircleShape)
            .padding(3.dp)
            .background(color, CircleShape),
    )
}
