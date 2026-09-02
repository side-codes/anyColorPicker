// Exists only to host Compose Preview Screenshot Tests.
//
// The official com.android.compose.screenshot plugin hangs its per-variant tasks off the
// `screenshotTest` component, which com.android.library creates and the KMP Android
// library plugin does not — applying it directly to :colorpicker registers the aggregate
// task but never updateDebugScreenshotTest or validateDebugScreenshotTest. A thin
// com.android.library module that depends on the published library gives the plugin the
// shape it expects. Nothing here is published.
plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.screenshot)
}

android {
    namespace = "codes.side.colorpicker.screenshot"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    experimentalProperties["android.experimental.enableScreenshotTest"] = true
}

dependencies {
    screenshotTestImplementation(project(":colorpicker"))
    screenshotTestImplementation(libs.compose.material3)
    screenshotTestImplementation(libs.androidx.compose.ui.tooling)
    screenshotTestImplementation(libs.androidx.compose.ui.tooling.preview)
    screenshotTestImplementation(libs.screenshot.validation.api)
}

// The README embeds these renders. The plugin names every reference
// <Function>_<preview name>_<hash>_0.png, and that hash changes whenever a preview's
// parameters change — pointing the README straight at them would break each image link
// on the next tweak. Copy them out under stable names keyed on the function name.
val readmeGoldens = mapOf(
    "HslIndependentPreview" to "hsl-independent",
    "HslContextualPreview" to "hsl-contextual",
    "RgbIndependentPreview" to "rgb-independent",
    "RgbContextualPreview" to "rgb-contextual",
    "CmykIndependentPreview" to "cmyk-independent",
    "CmykContextualPreview" to "cmyk-contextual",
    "LabIndependentPreview" to "lab-independent",
    "LabContextualPreview" to "lab-contextual",
    "OkhslIndependentPreview" to "okhsl-independent",
    "OkhslContextualPreview" to "okhsl-contextual",
    "OkhsvIndependentPreview" to "okhsv-independent",
    "OkhsvContextualPreview" to "okhsv-contextual",
    "CustomThumbPreview" to "custom-thumb",
    "SwatchPreview" to "color-swatch",
)

val docsImageDir = rootProject.layout.projectDirectory.dir("docs/images")

val copyGoldensToDocs = tasks.register<Copy>("copyGoldensToDocs") {
    description = "Copies screenshot-test references into docs/images under README-stable names."
    from(layout.projectDirectory.dir("src/screenshotTestDebug/reference"))
    include("**/*.png")
    includeEmptyDirs = false
    rename { fileName ->
        val function = fileName.substringBefore('_')
        val docName = readmeGoldens[function]
            ?: error("Preview function '$function' has no docs/images name in readmeGoldens.")
        "$docName.png"
    }
    eachFile { path = name }
    into(docsImageDir)

    // A Copy that matches nothing still succeeds, which would silently rot the README.
    val expected = readmeGoldens.values.toList()
    val outDir = docsImageDir.asFile
    doLast {
        val missing = expected.filterNot { outDir.resolve("$it.png").isFile }
        check(missing.isEmpty()) { "No golden was copied for: $missing" }
    }
}

// Recording new references without refreshing the README images would leave the two
// silently disagreeing, so the copy always follows the update.
// The screenshot plugin registers its per-variant tasks from a variant callback, after
// this script is evaluated, so tasks.named() cannot see them yet. Match lazily instead.
tasks.matching { it.name == "updateDebugScreenshotTest" }.configureEach {
    finalizedBy(copyGoldensToDocs)
}
