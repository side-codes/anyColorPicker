plugins {
    `kotlin-dsl`
}

// compileOnly: the root build puts each plugin on the classpath once, with apply false, and the
// convention plugin applies them by id. Bundling them here as well would load them twice.
dependencies {
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.mavenPublish.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("library") {
            id = "codes.side.library"
            implementationClass = "codes.side.conventions.LibraryConventionPlugin"
        }
    }
}
