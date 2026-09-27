import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    alias(libs.plugins.library)
    // Tests only: they decode their reference data from JSON files under src/commonTest/resources.
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.kotlinxResources)
}

kotlin {
    android {
        namespace = "codes.side.color"
    }

    // JVM and Android keep a thread's gamut memo in one java.lang.ThreadLocal, in jvmAndAndroidMain. A group in the
    // default template rather than a dependsOn edge: an explicit dependsOn switches the template off, and iosMain
    // goes with it.
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("jvmAndAndroid") {
                withJvm()
                withCompilations { it.platformType == KotlinPlatformType.androidJvm }
            }
        }
    }

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

// Published as build-logic's library plugin publishes every library module.
mavenPublishing {
    pom {
        name.set("anyColorPicker color")
        description.set("Kotlin Multiplatform color model: CSS Color 4 spaces, conversions, gamut mapping and CSS color strings, without Compose")
    }
}
