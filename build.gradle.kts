plugins {
    kotlin("jvm") version "2.3.10" apply false
    kotlin("plugin.serialization") version "2.3.10" apply false
}

allprojects {
    group = "com.mrpowergamerbr.starfait"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
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