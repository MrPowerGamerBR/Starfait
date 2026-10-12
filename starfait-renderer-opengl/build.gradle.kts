plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

val lwjglVersion = "3.4.3"
val lwjglNatives = findProperty("lwjglNativesPlatform") ?: "natives-linux"

dependencies {
    api(project(":starfait-runner-core"))

    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
    implementation("org.lwjgl:lwjgl-opengl")
    implementation("org.lwjgl:lwjgl-stb")

    implementation("org.lwjgl:lwjgl-opengl::$lwjglNatives")
    implementation("org.lwjgl:lwjgl-stb::$lwjglNatives")

    implementation(kotlin("reflect"))
    testImplementation(kotlin("test"))
}