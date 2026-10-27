plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "Starfait"

includeBuild("../HarmonyFramework")

include(":starfait-common")
include(":starfait-annotations")
include(":starfait-annotation-processor")
include(":starfait-wad")
include(":starfait-runner-core")
include(":starfait-renderer-gl")
include(":starfait-runner-sdl3")
include(":starfait-runner-web")