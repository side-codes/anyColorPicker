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
