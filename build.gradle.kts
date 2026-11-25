// exchange-rates-uy for Kotlin: the Android and JVM client of an exchange-rates-uy server. A plain JVM library (no
// Android dependency), so it works in an Android app, a Ktor or Spring server, or a script. Published from tags through
// JitPack: com.github.guillecura:exchange-rates-uy-kotlin:<tag>.
plugins {
    kotlin("jvm") version "2.4.21"
    kotlin("plugin.serialization") version "2.4.21"
    `java-library`
    `maven-publish`
}

group = "com.github.guillecura"
version = "1.0.0"

repositories { mavenCentral() }

kotlin { jvmToolchain(17) }

java { withSourcesJar() }

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    testImplementation(kotlin("test"))
}

tasks.test { useJUnitPlatform() }

publishing {
    publications {
        create<MavenPublication>("release") {
            from(components["java"])
            artifactId = "exchange-rates-uy-kotlin"
        }
    }
}
