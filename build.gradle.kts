plugins {
    id("my-plugin")
}

group = "com.alikhachev"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    workClasspath("com.alikhachev.api:impl")
}