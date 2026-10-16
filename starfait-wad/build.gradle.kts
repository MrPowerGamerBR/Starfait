plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
}

kotlin {
    jvm()
    wasmJs()

    sourceSets {
        commonMain.dependencies {
            api(project(":starfait-common"))
        }

        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.junit.jupiter:junit-jupiter:5.11.0")
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}