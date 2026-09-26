// The bridge has no composables, but it is compiled by the Compose compiler for good: the compiler
// gives ComposeColorSpaces a $stable field that Compose consumers read at run time, so removing it
// would break them. The Compose Gradle plugin also puts skiko.mjs where the wasm browser tests'
// webpack finds it, as ui-graphics imports Skiko there; it requires the compiler, and the compiler
// requires the runtime on the classpath.
plugins {
    alias(libs.plugins.library)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    android {
        namespace = "codes.side.color.compose"
    }

    sourceSets {
        commonMain.dependencies {
            // api: ColorValue and Compose's Color are both in the bridge's signatures.
            api(project(":color"))
            api(libs.compose.ui.graphics)
            // ui-graphics brings the runtime to consumers anyway; the Compose compiler needs it here.
            implementation(libs.compose.runtime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

// The Compose plugin unpacks Skiko only for a module whose code depends on compose.ui, though
// ui-graphics imports it too. Its condition is attached after evaluation, so it is replaced then.
afterEvaluate {
    tasks.named("unpackSkikoWasmRuntime") {
        setOnlyIf { true }
    }
}

// Published as build-logic's library plugin publishes every library module.
mavenPublishing {
    pom {
        name.set("anyColorPicker color for Compose")
        description.set("Converts between anyColorPicker's ColorValue and Compose Multiplatform's Color, with explicit gamut mapping")
    }
}
