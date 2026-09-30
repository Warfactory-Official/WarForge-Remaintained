pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.neoforged.net/releases") { name = "NeoForged" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
    includeBuild("build-logic")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.kikugie.stonecutter") version "0.9.8"
}

rootProject.name = "WarForge-Remaintained"

// One Stonecutter node per (Minecraft version, loader). The node name is "<mc>-<loader>" and the loader picks the
// build script (build.<loader>.gradle.kts). Which extra source folders each node compiles is in
// build-logic/src/main/kotlin/Variants.kt.
stonecutter {
    create(rootProject) {
        fun match(version: String, vararg loaders: String) =
            loaders.forEach { version("$version-$it", version).buildscript = "build.$it.gradle.kts" }

        match("1.20.1", "forge")
        match("1.21.1", "neoforge")
        match("26.1.2", "neoforge")
        match("26.2", "neoforge")

        // The shared sources in src/main are written for this node, so it needs no Stonecutter changes.
        vcsVersion = "1.21.1-neoforge"
    }
}
