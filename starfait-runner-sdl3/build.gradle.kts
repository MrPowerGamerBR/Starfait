plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

repositories {
    mavenCentral()
}

val lwjglVersion = "3.4.3"
val lwjglNatives = findProperty("lwjglNativesPlatform") ?: "natives-linux"

dependencies {
    api(project(":starfait-runner-core"))
    api(project(":starfait-renderer-gl"))
    api("net.perfectdreams.harmony:harmony-opengl-lwjgl:0.0.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")

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
    implementation("org.jspecify:jspecify:1.0.1")

    implementation(kotlin("reflect"))
    testImplementation(kotlin("test"))
}

testing {
    suites {
        getByName<JvmTestSuite>("test") {
            useJUnitJupiter()
        }
    }
}

application {
    mainClass = "com.mrpowergamerbr.starfait.StarfaitCLIKt"
}