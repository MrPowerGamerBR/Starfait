plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "Starfait"

include(":starfait-wad")
include(":starfait-runner-core")
include(":starfait-runner-sdl3")