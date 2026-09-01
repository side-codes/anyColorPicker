import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.dokka)
    `maven-publish`
    signing
}

group = "codes.side"
version = "1.0.0"

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
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
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

// Maven Central publication. The -javadoc jar packages the Dokka HTML output so the
// published artifact carries real API documentation.
val javadocJar = tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    val dokkaHtml = tasks.named("dokkaGeneratePublicationHtml")
    dependsOn(dokkaHtml)
    from(dokkaHtml)
}

publishing {
    publications.withType<MavenPublication> {
        artifact(javadocJar)

        pom {
            name.set("andColorPicker")
            description.set("Multiplatform color picker library for Android & iOS")
            url.set("https://github.com/side-codes/andColorPicker")
            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            developers {
                developer {
                    id.set("dummyco")
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
                connection.set("scm:git:github.com/side-codes/andColorPicker.git")
                developerConnection.set("scm:git:ssh://github.com/side-codes/andColorPicker.git")
                url.set("https://github.com/side-codes/andColorPicker/tree/master")
            }
        }
    }

    repositories {
        maven {
            name = "sonatype"
            // OSSRH (s01.oss.sonatype.org) was decommissioned 2025-06-30. These endpoints are the
            // Central Portal's OSSRH Staging API compatibility service; credentials must be
            // Central Portal user tokens, and deployments are released from central.sonatype.com.
            val releasesRepoUrl = "https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/"
            val snapshotsRepoUrl = "https://central.sonatype.com/repository/maven-snapshots/"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
            credentials {
                username = findProperty("ossrhUsername") as String? ?: System.getenv("OSSRH_USERNAME")
                password = findProperty("ossrhPassword") as String? ?: System.getenv("OSSRH_PASSWORD")
            }
        }
    }
}

signing {
    val signingKeyId = findProperty("signing.keyId") as String? ?: System.getenv("SIGNING_KEY_ID")
    val signingPassword = findProperty("signing.password") as String? ?: System.getenv("SIGNING_PASSWORD")
    val signingKey = findProperty("signing.key") as String? ?: System.getenv("SIGNING_KEY")
    if (signingKey != null) {
        useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
    }
    sign(publishing.publications)
}
