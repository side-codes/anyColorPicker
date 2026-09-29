// A build of its own that uses the libraries as an app does: from published artifacts, never from the
// project's sources. CI links it for iOS against what publishToMavenLocal has just published; with
// -PpickerRepo=snapshots it reads the Central snapshot repository instead.
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        if (providers.gradleProperty("pickerRepo").orNull == "snapshots") {
            maven("https://central.sonatype.com/repository/maven-snapshots/") {
                content { includeGroup("codes.side") }
            }
        } else {
            mavenLocal {
                content { includeGroup("codes.side") }
            }
        }
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "consumer"
