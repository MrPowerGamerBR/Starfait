plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":starfait-annotations"))
    implementation("com.google.devtools.ksp:symbol-processing-api:2.3.12")
}