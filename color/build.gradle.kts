import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
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
        }
    }
}
