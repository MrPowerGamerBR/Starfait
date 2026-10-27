plugins {
    kotlin("multiplatform")
}

kotlin {
    wasmJs {
        compilerOptions {
            freeCompilerArgs.add("-Xwasm-generate-sourcemap")   // on by default for dev builds in recent versions
        }

        browser()
        binaries.library()
    }

    sourceSets {
        wasmJsMain {
            dependencies {
                api(project(":starfait-runner-core"))
                api(project(":starfait-renderer-gl"))
                api("net.perfectdreams.harmony:harmony-wasm-utils:0.0.1-SNAPSHOT")
                api("net.perfectdreams.harmony:harmony-webgl2:0.0.1-SNAPSHOT")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-browser:2026.10.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
            }
        }
    }
}

repositories {
    mavenCentral()
}
