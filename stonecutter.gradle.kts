plugins {
    id("mod-common") apply false
    id("dev.kikugie.stonecutter")
    id("net.neoforged.moddev") version "2.0.148" apply false
    id("net.neoforged.moddev.legacyforge") version "2.0.148" apply false
}

stonecutter active file(".sc_active_version")

// Text replacements run on src/main for each node. Every node must see the same list (with its own direction), so
// Stonecutter can undo a replacement when the active node changes. Only true one-to-one renames go here; anything
// else is a shim class in a src/variant folder (see build-logic Variants.kt).
stonecutter parameters {
    val loader = current.project.substringAfterLast('-')

    // Shared code imports NeoForge packages. Forge 1.20.1 has the same classes under these names.
    val forge = loader == "forge"
    FORGE_RENAMES.forEach { (from, to) -> replacements.string(forge) { replace(from, to) } }

    // Renamed in 1.21.11.
    replacements.string(current.parsed >= "1.21.11") { replace("ResourceLocation", "Identifier") }
}

tasks.register<CheckNoVersionGuards>("checkNoVersionGuards") {
    group = "verification"
    description = "Fails if a Stonecutter version comment is in src/main or src/variant."
    sources.from(fileTree("src/main"), fileTree("src/variant"))
}

// Builds every node.
tasks.register("buildAll") {
    group = "build"
    description = "Builds the jar for every Minecraft version and loader."
    dependsOn(stonecutter.versions.map { ":${it.project}:build" })
}

// Snapshot maven (repo.warfactory.co) exists for NeoForge nodes only.
tasks.register("publishAll") {
    group = "publishing"
    description = "Publishes every node's snapshot maven artifact."
    dependsOn(stonecutter.versions.map { ":${it.project}:publish" })
}

tasks.register("publishAllMods") {
    group = "publishing"
    description = "Uploads every node's jar to Modrinth and CurseForge."
    dependsOn(stonecutter.versions.map { ":${it.project}:publishMods" })
}
