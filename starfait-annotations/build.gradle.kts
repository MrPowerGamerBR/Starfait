plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    wasmJs {
        browser()
    }
}