plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "Starfait"

include(":starfait-annotations")
include(":starfait-annotation-processor")
include(":starfait-wad")
include(":starfait-runner-core")
include(":starfait-runner-sdl3")