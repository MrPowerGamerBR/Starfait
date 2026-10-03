import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.3.10"
    kotlin("plugin.serialization") version "2.3.10"
    application
}

group = "com.mrpowergamerbr.starfait"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val lwjglVersion = "3.4.2"
val jomlVersion = "1.10.9"
val lwjglNatives = "natives-linux"

dependencies {
    implementation("com.github.ajalt.clikt:clikt:5.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0-RC")

    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))

    implementation("org.lwjgl:lwjgl")
    implementation("org.lwjgl:lwjgl-opengl")
    implementation("org.lwjgl:lwjgl-sdl")
    implementation("org.lwjgl:lwjgl-stb")
    implementation("org.lwjgl:lwjgl::$lwjglNatives")
    implementation("org.lwjgl:lwjgl-opengl::$lwjglNatives")
    implementation("org.lwjgl:lwjgl-sdl::$lwjglNatives")
    implementation("org.lwjgl:lwjgl-stb::$lwjglNatives")
    implementation("org.joml:joml:$jomlVersion")

    implementation(kotlin("reflect"))
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)

    compilerOptions.freeCompilerArgs.add("-Xannotation-default-target=param-property")
}

val testGames = layout.projectDirectory.dir("test-games")

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }

        register<JvmTestSuite>("gameTest") {
            useJUnitJupiter()

            targets.all {
                testTask.configure {
                    inputs.dir(testGames)
                        .withPropertyName("testGames")
                        .withPathSensitivity(PathSensitivity.RELATIVE)
                    systemProperty("games.dir", testGames.asFile.absolutePath)

                    dependencies {
                        implementation(project())
                        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0-RC")
                    }

                    // Turn on parallel execution
                    systemProperty("junit.jupiter.execution.parallel.enabled", "true")
                    // Methods within a class run concurrently
                    systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
                    // Different classes run concurrently too
                    systemProperty("junit.jupiter.execution.parallel.mode.classes.default", "concurrent")
                    // At most 4 tests at a time
                    systemProperty("junit.jupiter.execution.parallel.config.strategy", "fixed")
                    systemProperty("junit.jupiter.execution.parallel.config.fixed.parallelism", "4")
                }
            }
        }
    }
}

application {
    mainClass = "com.mrpowergamerbr.starfait.StarfaitCLIKt"
}