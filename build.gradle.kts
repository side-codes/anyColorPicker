import kotlinx.validation.ExperimentalBCVApi

plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.androidKmpLibrary) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.binaryCompatibilityValidator)
}

apiValidation {
    // Sample modules are not published; only the library's API surface is tracked.
    ignoredProjects += listOf("androidApp", "shared")

    @OptIn(ExperimentalBCVApi::class)
    klib {
        // Also track the klib ABI (iOS targets). Klib dumps only require compiling
        // klibs, which works on all hosts (only linking needs macOS).
        enabled = true
    }
}
