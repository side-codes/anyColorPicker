plugins {
    alias(libs.plugins.library)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    android {
        namespace = "codes.side.colorpicker.material3"
    }

    // A static framework CI links for the simulator, so a symbol the library or anything it pulls in cannot resolve
    // fails there rather than in an app.
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ColorPicker"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // api: the pickers take a ColorPickerState and hand their slots the foundation's scopes.
            api(project(":colorpicker-foundation"))
            // implementation: no material3 type appears in a public signature.
            implementation(libs.compose.material3)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmTest.dependencies {
            // Compose UI tests are JVM-only, for the reasons colorpicker-foundation's build gives.
            implementation(libs.compose.ui.test)
            implementation(compose.desktop.currentOs)
        }
    }
}

// The UI tests assert English words and numbers in en-US's format, so a machine set to another locale must not fail
// them.
tasks.withType<Test>().configureEach {
    systemProperty("user.language", "en")
    systemProperty("user.country", "US")
}

// Published as build-logic's library plugin publishes every library module.
mavenPublishing {
    pom {
        name.set("anyColorPicker Material 3")
        description.set("Material 3 color pickers, sliders, planes, swatch and dialog for Compose Multiplatform, built on anyColorPicker's foundation")
    }
}
