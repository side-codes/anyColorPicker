package codes.side.colorpicker.foundation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class CheckerboardTest {

    // The centre of the cell [column] across and [row] down, on a board four cells wide.
    private fun PixelMap.cell(column: Int, row: Int): Color {
        val size = width / 4
        return this[column * size + size / 2, row * size + size / 2]
    }

    private fun Color.isRed() = red > 0.9f && green < 0.1f && blue < 0.1f

    private fun Color.isBlue() = red < 0.1f && green < 0.1f && blue > 0.9f

    @Test
    fun cellsAlternateFromTheTopLeft() = runComposeUiTest {
        setContent { Box(Modifier.size(24.dp).testTag("board").checkerboard(Color.Red, Color.Blue, cellSize = 6.dp)) }
        val pixels = onNodeWithTag("board").captureToImage().toPixelMap()
        assertTrue(pixels.cell(0, 0).isRed(), "top left: ${pixels.cell(0, 0)}")
        assertTrue(pixels.cell(1, 0).isBlue(), "next along: ${pixels.cell(1, 0)}")
        assertTrue(pixels.cell(0, 1).isBlue(), "next down: ${pixels.cell(0, 1)}")
        assertTrue(pixels.cell(1, 1).isRed(), "diagonal: ${pixels.cell(1, 1)}")
        assertTrue(pixels.cell(3, 3).isRed(), "far corner: ${pixels.cell(3, 3)}")
    }

    @Test
    fun itDrawsBehindTheContent() = runComposeUiTest {
        setContent {
            Box(Modifier.size(24.dp).testTag("board").checkerboard(Color.Red, Color.Blue, cellSize = 6.dp)) {
                Box(Modifier.size(12.dp).background(Color.Green))
            }
        }
        val pixels = onNodeWithTag("board").captureToImage().toPixelMap()
        val covered = pixels.cell(0, 0)
        assertTrue(covered.green > 0.9f && covered.red < 0.1f, "the content covers the board: $covered")
        assertTrue(pixels.cell(3, 3).isRed(), "the board shows where there is no content: ${pixels.cell(3, 3)}")
    }
}
