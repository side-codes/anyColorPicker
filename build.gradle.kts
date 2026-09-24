import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import kotlinx.validation.ExperimentalBCVApi

plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.kotlinxResources) apply false
    alias(libs.plugins.androidKmpLibrary) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.screenshot) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.mavenPublish) apply false
    alias(libs.plugins.binaryCompatibilityValidator)
}

apiValidation {
    // Sample modules are not published; only the library's API surface is tracked.
    ignoredProjects += listOf("androidApp", "desktopApp", "screenshot-tests", "shared", "webApp")

    @OptIn(ExperimentalBCVApi::class)
    klib {
        // Also track the klib ABI (iOS targets). Klib dumps only require compiling
        // klibs, which works on all hosts (only linking needs macOS).
        enabled = true
    }
}

// ---- Maven Central publication ----
//
// Every module that applies the publish plugin is published the same way; each sets only its POM's
// name and description.
//
// Publishing goes through the Central Portal (central.sonatype.com). Uploading to the old
// OSSRH Staging API only stages a deployment: something still has to close and release it,
// which nothing here used to do. publishAndReleaseToMavenCentral does both in one task.
//
// Credentials are Central Portal *user tokens* — an OSSRH token returns 401. The plugin
// reads them from mavenCentralUsername / mavenCentralPassword, which CI supplies as
// ORG_GRADLE_PROJECT_* environment variables.
subprojects {
    plugins.withId("com.vanniktech.maven.publish") {
        extensions.configure<MavenPublishBaseExtension> {
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
