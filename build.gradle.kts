plugins {
    kotlin("jvm") version "2.3.10"
}

group = "org.gang"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    compileOnly("org.robolectric:android-all:14-robolectric-10818077")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}