plugins {
    kotlin("jvm") version "2.2.20"
    `maven-publish`
}

group = "io.github.jwhyee.profanity"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    api("org.ahocorasick:ahocorasick:0.6.3")
    testImplementation(kotlin("test"))
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}