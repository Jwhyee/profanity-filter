plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

group = "io.github.jwhyee.profanity"
version = libs.versions.profanity.filter.get()

repositories {
    mavenCentral()
}

dependencies {
    api(libs.ahocorasick)
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