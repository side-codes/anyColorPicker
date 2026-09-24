import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The bridge has no composables. The Compose plugins are here for wasm alone: ui-graphics imports
// Skiko there, and only the Compose Gradle plugin puts skiko.mjs where the browser tests' webpack
// finds it. That plugin requires the Compose compiler, which requires the runtime on the classpath.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidKmpLibrary)
}

group = "codes.side"
version = providers.gradleProperty("VERSION_NAME").get()

kotlin {
    explicitApi()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        // Without an executable binary webpack does not bundle the Skiko runtime, and the browser
        // tests cannot load. https://youtrack.jetbrains.com/issue/CMP-4906
        binaries.executable()
    }

    jvm()

    android {
        namespace = "codes.side.color.compose"
        compileSdk = 37
        minSdk = 24

        withHostTest {}

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    iosArm64()
    iosSimulatorArm64()

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
