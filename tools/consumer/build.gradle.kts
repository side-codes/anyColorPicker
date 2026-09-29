plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val pickerVersion = providers.gradleProperty("pickerVersion").get()

kotlin {
    jvm()
    iosSimulatorArm64 {
        // Dynamic, so linking resolves every native symbol the libraries need, where a static framework
        // leaves that to the app it goes into.
        binaries.framework {
            baseName = "Consumer"
            isStatic = false
        }
    }
    sourceSets {
        // Material 3 alone: the other three modules and every type in its signatures come through it.
        commonMain.dependencies {
            implementation("codes.side:colorpicker-material3:$pickerVersion")
        }
    }
}
