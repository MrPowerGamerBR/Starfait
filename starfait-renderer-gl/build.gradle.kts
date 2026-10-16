plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":starfait-runner-core"))

    implementation(kotlin("reflect"))
    testImplementation(kotlin("test"))
}
