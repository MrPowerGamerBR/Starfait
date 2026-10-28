plugins {
    kotlin("jvm") version "2.4.21" apply false
    kotlin("multiplatform") version "2.4.21" apply false
    kotlin("plugin.serialization") version "2.4.21" apply false
    id("com.google.devtools.ksp") version "2.3.12" apply false
}

allprojects {
    group = "com.mrpowergamerbr.starfait"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven("https://repo.perfectdreams.net/")
    }

    plugins.withType<org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin> {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension> {
            this.jvmToolchain(21)
        }
    }

    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
            compilerOptions.freeCompilerArgs.add("-Xannotation-default-target=param-property")
        }
    }
}