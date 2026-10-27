plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
}

kotlin {
    jvm()
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain {
            dependencies {
                api(project(":starfait-runner-core"))
                api("net.perfectdreams.harmony:harmony-gl:0.0.1-SNAPSHOT")
            }
        }
    }
}