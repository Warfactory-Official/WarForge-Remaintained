plugins {
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
    constants.match(loader, "forge", "neoforge")

    // Shared code imports NeoForge packages. Forge 1.20.1 has the same classes under these names.
    val forge = loader == "forge"
    replacements.string(forge) { replace("net.neoforged.neoforge.", "net.minecraftforge.") }
    replacements.string(forge) { replace("net.neoforged.fml.", "net.minecraftforge.fml.") }
    replacements.string(forge) { replace("net.neoforged.api.distmarker.", "net.minecraftforge.api.distmarker.") }
    replacements.string(forge) { replace("net.neoforged.bus.api.", "net.minecraftforge.eventbus.api.") }
    replacements.string(forge) { replace("ModConfigSpec", "ForgeConfigSpec") }

    // Renamed in 1.21.11.
    replacements.string(current.parsed >= "1.21.11") { replace("ResourceLocation", "Identifier") }
}

// Builds every node. CI and release builds use this.
tasks.register("buildAll") {
    group = "build"
    description = "Builds the jar for every Minecraft version and loader."
    dependsOn(stonecutter.versions.map { ":${it.project}:build" })
}

tasks.register("releaseBuild") {
    group = "build"
    description = "Builds the release jars for CurseForge/Modrinth publishing."
    dependsOn("buildAll")
}
