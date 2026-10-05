plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":starfait-wad"))

    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0-RC")

    implementation(kotlin("reflect"))
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)

    compilerOptions.freeCompilerArgs.add("-Xannotation-default-target=param-property")
}

val testGames = layout.projectDirectory.dir("../test-games")

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }

        register<JvmTestSuite>("gameTest") {
            useJUnitJupiter()

            dependencies {
                implementation(project())
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0-RC")
            }

            targets.all {
                testTask.configure {
                    inputs.dir(testGames)
                        .withPropertyName("testGames")
                        .withPathSensitivity(PathSensitivity.RELATIVE)
                    systemProperty("games.dir", testGames.asFile.absolutePath)

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