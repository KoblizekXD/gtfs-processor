plugins {
    kotlin("jvm") version "2.2.0"
}

group = "dev.aa55h"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jsoup:jsoup:1.21.1")
    implementation("com.opencsv:opencsv:5.12.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.2")
    implementation("com.squareup.okhttp3:okhttp:5.1.0")
    implementation("org.ahocorasick:ahocorasick:0.6.3")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(24)
}

tasks.register<JavaExec>("updateGtfs") {
    group = "application"
    description = "Update GTFS data"
    
    mainClass.set("dev.aa55h.gtfs.MainKt")
    
    classpath = sourceSets["main"].runtimeClasspath

    // Allow passing program args with: ./gradlew runGtfs --args="foo bar"
    if (project.hasProperty("args")) {
        args = (project.property("args") as String).split("\\s+".toRegex())
    }
}
