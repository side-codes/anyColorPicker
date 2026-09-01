import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.dokka)
    alias(libs.plugins.mavenPublish)
}

group = "codes.side"
version = providers.gradleProperty("VERSION_NAME").get()

kotlin {
    explicitApi()

    jvm()

    android {
        namespace = "codes.side.colorpicker"
        compileSdk = 37
        minSdk = 24

        withHostTest {}

        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    // iosX64 (Intel simulator) removed: Compose Multiplatform stopped publishing
    // iosx64 artifacts as of 1.11.0.
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
            // api: types from these modules appear in the library's public API
            // (@Composable/@Immutable/@Stable from runtime; Modifier, Color, Shape,
            // Dp from ui; ImmutableList in ColorSlider's signature). Consumers
            // compile against them, so they must be on the consumer's compile
            // classpath. foundation is api because every public picker composable
            // is designed to be composed with foundation layouts and its widgets
            // are foundation-based slot hosts.
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.ui)
            api(libs.kotlinx.collections.immutable)
            // implementation: no material3 or coroutines types leak into public
            // signatures (material3 is an internal rendering detail).
            implementation(libs.compose.material3)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.compose.ui.test)
        }
        jvmTest.dependencies {
            // runComposeUiTest needs a real renderer on the JVM target.
            implementation(compose.desktop.currentOs)
        }
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
        }
    }
}

// ---- Maven Central publication ----
//
// Publishing goes through the Central Portal (central.sonatype.com). Uploading to the old
// OSSRH Staging API only stages a deployment: something still has to close and release it,
// which nothing here used to do. publishAndReleaseToMavenCentral does both in one task.
//
// Credentials are Central Portal *user tokens* — an OSSRH token returns 401. The plugin
// reads them from mavenCentralUsername / mavenCentralPassword, which CI supplies as
// ORG_GRADLE_PROJECT_* environment variables.

mavenPublishing {
    // Bundles all five publications (kotlinMultiplatform, android, jvm and the two iOS
    // targets) into one deployment, with the Dokka HTML as the -javadoc jar.
    configure(KotlinMultiplatform(javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml")))

    publishToMavenCentral(automaticRelease = true)

    // Signing keys only exist on CI; without this guard every sign* task fails with
    // "no configured signatory" and blocks publishToMavenLocal for contributors.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }

    pom {
        name.set("andColorPicker")
        description.set("Multiplatform color picker library for Android & iOS")
        inceptionYear.set("2020")
        url.set("https://github.com/side-codes/andColorPicker")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("smelfungus")
                name.set("Illia Achour")
                email.set("ilyaachour@gmail.com")
            }
            developer {
                id.set("N7k")
                name.set("Maksim Novik")
                email.set("nvk.mse@gmail.com")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/side-codes/andColorPicker.git")
            developerConnection.set("scm:git:ssh://git@github.com/side-codes/andColorPicker.git")
            url.set("https://github.com/side-codes/andColorPicker")
        }
    }
}
