pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

rootProject.name = "FiveOfAKind"

include(":android")
include(":desktop")
include(":jsApp")
include(":common")