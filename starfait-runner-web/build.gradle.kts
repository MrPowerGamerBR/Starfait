plugins {
    kotlin("multiplatform")
}

kotlin {
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        wasmJsMain {
            dependencies {
                api(project(":starfait-runner-core"))
                implementation("org.jetbrains.kotlin-wrappers:kotlin-browser:2026.10.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
            }
        }
    }
}

repositories {
    mavenCentral()
}
