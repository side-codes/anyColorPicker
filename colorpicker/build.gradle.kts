import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidKmpLibrary)
    `maven-publish`
    signing
}

group = "codes.side"
version = "1.0.0"

kotlin {
    jvm()

    android {
        namespace = "codes.side.colorpicker"
        compileSdk = 37
        minSdk = 24

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
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.collections.immutable)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
        }
    }
}

// Maven Central publication
val javadocJar = tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
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
