import kotlinx.validation.ExperimentalBCVApi

// Every plugin goes on the classpath here, once, so build-logic's convention plugin, which only compiles against
// them, can apply them by id without loading a second copy.
plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.kotlinxResources) apply false
    alias(libs.plugins.androidKmpLibrary) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.screenshot) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.mavenPublish) apply false
    alias(libs.plugins.binaryCompatibilityValidator)
}

apiValidation {
    // Sample modules are not published; only the library's API surface is tracked.
    ignoredProjects += listOf("androidApp", "desktopApp", "screenshot-tests", "shared", "webApp")

    @OptIn(ExperimentalBCVApi::class)
    klib {
        // Also track the klib ABI (iOS targets). Klib dumps only require compiling
        // klibs, which works on all hosts (only linking needs macOS).
        enabled = true
    }
}
