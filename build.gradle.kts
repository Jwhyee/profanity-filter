plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jmh)
    `maven-publish`
}

group = "io.github.jwhyee.profanity"
version = libs.versions.profanity.filter.get()

repositories {
    mavenCentral()
}

dependencies {
    api(libs.ahocorasick)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    
    jmh(libs.jmh.core)
    jmh(libs.jmh.generator.annprocess)
}

jmh {
    jmhVersion.set(libs.versions.jmhVersion.get())
    duplicateClassesStrategy.set(DuplicatesStrategy.EXCLUDE)
    warmupIterations.set(5)
    iterations.set(5)
    fork.set(1)
    failOnError.set(true)
    resultFormat.set("JSON") // Or JSON
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