import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.dokka)
    alias(libs.plugins.mavenPublish)
    // Tests only: they decode their reference data from JSON files under src/commonTest/resources.
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.kotlinxResources)
}

group = "codes.side"
version = providers.gradleProperty("VERSION_NAME").get()

kotlin {
    explicitApi()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    jvm()

    android {
        namespace = "codes.side.color"
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
            // @Immutable only: the annotation artifact, not the Compose runtime.
            api(libs.androidx.compose.runtime.annotation)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.resources)
        }
    }
}

// Published as the root build script sets up every module with the publish plugin.
mavenPublishing {
    pom {
        name.set("anyColorPicker color")
        description.set("Kotlin Multiplatform color model: CSS Color 4 spaces, conversions, gamut mapping and CSS color strings, without Compose")
    }
}
