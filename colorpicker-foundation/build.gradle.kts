import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    alias(libs.plugins.library)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    android {
        namespace = "codes.side.colorpicker.foundation"
    }

    // Everything but Android draws through Skiko, so the one platform-specific thing the
    // library needs — handing a pixel array to the toolkit as an image — has a single
    // implementation in skikoMain and an Android one beside it. JVM and Android both have
    // java.text, so the number formatter has one implementation in jvmAndAndroidMain. Both are
    // groups in the default template rather than dependsOn edges: an explicit dependsOn switches
    // the template off, and iosMain, appleMain and nativeMain go with it.
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("skiko") {
                withJvm()
                withWasmJs()
                withIos()
            }
            group("jvmAndAndroid") {
                withJvm()
                withCompilations { it.platformType == KotlinPlatformType.androidJvm }
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            // api: types from these modules appear in the library's public API
            // (@Composable/@Immutable/@Stable from runtime; Modifier, Color, Shape,
            // Dp from ui). Consumers
            // compile against them, so they must be on the consumer's compile
            // classpath. foundation is api because every public picker composable
            // is designed to be composed with foundation layouts and its widgets
            // are foundation-based slot hosts.
            // ColorValue, ColorSpace and ColorChannel are in the state's signatures, and so is the
            // bridge's conversion to Compose's Color.
            api(project(":color"))
            api(project(":color-compose"))
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.ui)
            // implementation: no coroutines type appears in a public signature.
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmTest.dependencies {
            // Compose UI tests are JVM-only on purpose. In commonTest they compile into
            // every target: androidHostTest has no Android runtime for runComposeUiTest to
            // attach to, and on iOS instantiating real UI makes CMP's UIKit view layer
            // reachable from the test binary, which then needs UIKit symbols newer than the
            // runner's Xcode SDK provides. The desktop renderer gives the same coverage
            // without either problem.
            implementation(libs.compose.ui.test)
            implementation(compose.desktop.currentOs)
        }
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
        }
    }
}

// The UI tests assert English words and numbers in en-US's format, so a machine set to another locale must not fail
// them. The formatting tests name their locales and do not depend on this.
tasks.withType<Test>().configureEach {
    systemProperty("user.language", "en")
    systemProperty("user.country", "US")
}

// Published as build-logic's library plugin publishes every library module.
mavenPublishing {
    pom {
        name.set("anyColorPicker foundation")
        description.set("Color picker behaviour for Compose Multiplatform without Material: sliders, planes and pickers that take their look from slots, with the picker state and the strings")
    }
}
