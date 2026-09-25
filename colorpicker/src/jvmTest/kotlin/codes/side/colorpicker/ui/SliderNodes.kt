package codes.side.colorpicker.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag

/**
 * The Material slider inside the component tagged [tag]. The tag lands on the column that holds the
 * labels and the slider, and the slider's own node is the one with a progress range.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.sliderIn(tag: String): SemanticsNodeInteraction =
    onNode(hasAnyAncestor(hasTestTag(tag)) and SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
