plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
}

val testGames = layout.projectDirectory.dir("../test-games")

kotlin {
    jvm {
        val main = compilations.getByName("main")

        val gameTestCompilation = compilations.create("gameTest") {
            associateWith(main)

            defaultSourceSet.dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0-RC")
                implementation(project.dependencies.platform("org.junit:junit-bom:5.13.4"))
                implementation("org.junit.jupiter:junit-jupiter")
                runtimeOnly("org.junit.platform:junit-platform-launcher")
            }
        }

        // A test run (instead of a plain Test task) so IntelliJ knows which task runs the jvmGameTest sources
        testRuns.create("game") {
            setExecutionSourceFrom(gameTestCompilation)

            executionTask.configure {
                useJUnitPlatform()

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

    wasmJs {
        browser()
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(layout.buildDirectory.dir("generated/ksp/metadata/commonMain/kotlin"))

            dependencies {
                api(project(":starfait-wad"))
                api(project(":starfait-annotations"))

                api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0-RC")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
            }
        }

        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation(project.dependencies.platform("org.junit:junit-bom:5.13.4"))
            implementation("org.junit.jupiter:junit-jupiter-params")
            runtimeOnly("org.junit.platform:junit-platform-launcher")
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", project(":starfait-annotation-processor"))
}

// The KSP output for commonMain is added as a source dir above, so every compilation (and every per-target KSP task, which also reads the sources) must wait for it
tasks.matching { (it is org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*> || it.name.startsWith("ksp")) && it.name != "kspCommonMainKotlinMetadata" }.configureEach {
    dependsOn("kspCommonMainKotlinMetadata")
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}

repositories {
    mavenCentral()
}
