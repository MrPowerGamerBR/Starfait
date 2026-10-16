plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":starfait-runner-core"))
    api("org.joml:joml:1.10.9")
}
