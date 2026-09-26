package codes.side.conventions

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * What every published module shares: its targets, Android SDK levels, JVM target, explicit API mode, coordinates'
 * group and version, and its Maven Central publication. A module adds its Android namespace, its dependencies and
 * its POM's name and description.
 *
 * The package is not `codes.side.build`: `.gitignore` ignores every directory named `build`, source included, so
 * git would silently leave the plugin out.
 */
class LibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.kotlin.multiplatform.library")
        pluginManager.apply("org.jetbrains.dokka")
        pluginManager.apply("com.vanniktech.maven.publish")

        group = "codes.side"
        version = providers.gradleProperty("VERSION_NAME").get()

        extensions.configure<KotlinMultiplatformExtension> {
            explicitApi()

            // Compose Multiplatform for web. The DSL is still marked experimental in the Kotlin
            // Gradle plugin, so the opt-in is required and the shape may change between Kotlin
            // versions; the libraries themselves need no wasm-specific source.
            @OptIn(ExperimentalWasmDsl::class)
            wasmJs {
                browser()
            }

            jvm()

            (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget>("android") {
                compileSdk = 37
                minSdk = 24

                withHostTest {}

                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                }
            }

            // iosX64 (Intel simulator) removed: Compose Multiplatform stopped publishing
            // iosx64 artifacts as of 1.11.0.
            iosArm64()
            iosSimulatorArm64()
        }

        // Required even for a library: without an executable binary webpack does not
        // bundle the Skiko runtime, and the browser test target cannot load Compose.
        // See https://youtrack.jetbrains.com/issue/CMP-4906
        pluginManager.withPlugin("org.jetbrains.compose") {
            extensions.configure<KotlinMultiplatformExtension> {
                @OptIn(ExperimentalWasmDsl::class)
                wasmJs {
                    binaries.executable()
                }
            }
        }

        // ---- Maven Central publication ----
        //
        // Every library module is published the same way; each sets only its POM's name and
        // description.
        //
        // Publishing goes through the Central Portal (central.sonatype.com). Uploading to the old
        // OSSRH Staging API only stages a deployment: something still has to close and release it,
        // which nothing here used to do. publishAndReleaseToMavenCentral does both in one task.
        //
        // Credentials are Central Portal *user tokens* — an OSSRH token returns 401. The plugin
        // reads them from mavenCentralUsername / mavenCentralPassword, which CI supplies as
        // ORG_GRADLE_PROJECT_* environment variables.
        extensions.configure<MavenPublishBaseExtension> {
            // Bundles each module's six publications (kotlinMultiplatform, android, jvm, wasmJs and
            // the two iOS targets) into one deployment, with the Dokka HTML as the -javadoc jar.
            configure(KotlinMultiplatform(javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml")))

            publishToMavenCentral(automaticRelease = true)

            // Signing keys only exist on CI; without this guard every sign* task fails with
            // "no configured signatory" and blocks publishToMavenLocal for contributors.
            if (providers.gradleProperty("signingInMemoryKey").isPresent) {
                signAllPublications()
            }

            pom {
                inceptionYear.set("2020")
                url.set("https://github.com/side-codes/anyColorPicker")
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
                    connection.set("scm:git:git://github.com/side-codes/anyColorPicker.git")
                    developerConnection.set("scm:git:ssh://git@github.com/side-codes/anyColorPicker.git")
                    url.set("https://github.com/side-codes/anyColorPicker")
                }
            }
        }
    }
}
