plugins {
    kotlin("jvm") version "1.8.10"
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("com.alikhachev.api:api")
}

kotlin {
    jvmToolchain(17)
}