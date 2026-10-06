plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

repositories {
    mavenCentral()
}

val lwjglVersion = "3.4.2"
val jomlVersion = "1.10.9"
val lwjglNatives = "natives-linux"

dependencies {
    api(project(":starfait-runner-core"))

    implementation("com.github.ajalt.clikt:clikt:5.1.0")

    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))

    implementation("org.lwjgl:lwjgl")
    implementation("org.lwjgl:lwjgl-opengl")
    implementation("org.lwjgl:lwjgl-sdl")
    implementation("org.lwjgl:lwjgl-stb")
    implementation("org.lwjgl:lwjgl-tinyfd")
    implementation("org.lwjgl:lwjgl::$lwjglNatives")
    implementation("org.lwjgl:lwjgl-opengl::$lwjglNatives")
    implementation("org.lwjgl:lwjgl-sdl::$lwjglNatives")
    implementation("org.lwjgl:lwjgl-stb::$lwjglNatives")
    implementation("org.lwjgl:lwjgl-tinyfd::$lwjglNatives")
    implementation("org.joml:joml:$jomlVersion")

    implementation(kotlin("reflect"))
    testImplementation(kotlin("test"))
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }
    }
}

application {
    mainClass = "com.mrpowergamerbr.starfait.StarfaitCLIKt"
}