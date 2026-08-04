plugins {
    kotlin("jvm") version "2.3.10"
    application
}

group = "com.mrpowergamerbr.starfait"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.ajalt.clikt:clikt:5.1.0")
    implementation(kotlin("reflect"))
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass = "com.mrpowergamerbr.starfait.StarfaitCLIKt"
}