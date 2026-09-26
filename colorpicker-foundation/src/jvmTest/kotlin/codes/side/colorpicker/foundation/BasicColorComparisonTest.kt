package codes.side.colorpicker.foundation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class BasicColorComparisonTest {

    @Test
    fun eachHalfSaysWhichColorItIs() = runComposeUiTest {
        setContent { BasicColorComparison(Color.Red, Color.Blue, onRestoreOriginal = null, Modifier.size(96.dp, 48.dp)) }
        onNodeWithContentDescription("Original color").assertExists()
        onNodeWithContentDescription("New color").assertExists()
    }

    @Test
    fun theOriginalHalfIsAButtonThatRestores() = runComposeUiTest {
        var restored = 0
        setContent { BasicColorComparison(Color.Red, Color.Blue, onRestoreOriginal = { restored++ }, Modifier.size(96.dp, 48.dp)) }
        onNodeWithContentDescription("Original color")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(SemanticsMatcher("its action says it restores") { it.config[SemanticsActions.OnClick].label == "Restore original color" })
            .performClick()
        assertEquals(1, restored)
    }

    @Test
    fun withNothingToRestoreTheOriginalHalfIsNoButton() = runComposeUiTest {
        setContent { BasicColorComparison(Color.Red, Color.Blue, onRestoreOriginal = null, Modifier.size(96.dp, 48.dp)) }
        onNodeWithContentDescription("Original color")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
    }

    @Test
    fun aNullLabelSaysNothing() = runComposeUiTest {
        setContent {
            BasicColorComparison(Color.Red, Color.Blue, onRestoreOriginal = null, Modifier.size(96.dp, 48.dp), originalLabel = null, currentLabel = null)
        }
        onNodeWithContentDescription("Original color").assertDoesNotExist()
        onNodeWithContentDescription("New color").assertDoesNotExist()
    }

    @Test
    fun theOriginalSitsAtTheStart() = runComposeUiTest {
        var direction by mutableStateOf(LayoutDirection.Ltr)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                BasicColorComparison(Color.Red, Color.Blue, onRestoreOriginal = null, Modifier.size(96.dp, 48.dp).testTag("comparison"))
            }
        }
        fun startAndEnd(): Pair<Color, Color> {
            val pixels = onNodeWithTag("comparison").captureToImage().toPixelMap()
            return pixels[pixels.width / 4, pixels.height / 2] to pixels[pixels.width * 3 / 4, pixels.height / 2]
        }
        assertEquals(Color.Red to Color.Blue, startAndEnd(), "left to right")
        direction = LayoutDirection.Rtl
        waitForIdle()
        assertEquals(Color.Blue to Color.Red, startAndEnd(), "right to left, the original is on the right")
    }

    @Test
    fun theHalvesFillAHeightGivenOnlyAsAMinimum() = runComposeUiTest {
        // In a scrolling column the height has no maximum, which fillMaxHeight alone cannot fill.
        setContent {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                BasicColorComparison(Color.Red, Color.Blue, onRestoreOriginal = {}, Modifier.width(96.dp).defaultMinSize(minHeight = 48.dp))
            }
        }
        onNodeWithContentDescription("Original color").assertHeightIsEqualTo(48.dp)
        onNodeWithContentDescription("New color").assertHeightIsEqualTo(48.dp)
    }

    @Test
    fun aTranslucentColorShowsTheCheckerboardThrough() = runComposeUiTest {
        setContent {
            BasicColorComparison(
                Color.Red,
                Color.Transparent,
                onRestoreOriginal = null,
                Modifier.size(96.dp, 48.dp).testTag("comparison"),
                checkerboardLight = Color.White,
                checkerboardDark = Color.Black,
            )
        }
        val pixels = onNodeWithTag("comparison").captureToImage().toPixelMap()
        val cells = (pixels.width / 2 until pixels.width).map { pixels[it, 2] }.toSet()
        assertTrue(Color.White in cells && Color.Black in cells, "the end half shows both cells, found $cells")
    }
}
