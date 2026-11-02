plugins {
    id("mod-common")
    id("net.neoforged.moddev.legacyforge")
}

legacyForge {
    version = "$mcVersion-${prop("deps.forge")}"

    parchment {
        mappingsVersion = prop("deps.parchment")
        minecraftVersion = prop("deps.parchment_minecraft")
    }

    configureRuns(runs, "forge.enabledGameTestNamespaces", configurations["localRuntime"]) { data() }

    mods {
        register(prop("mod.id")) {
            sourceSet(sourceSets["main"])
        }
    }

    addModdingDependenciesTo(sourceSets["test"])
}

mixin {
    add(sourceSets["main"], "mixins.${prop("mod.id")}.refmap.json")
    config("mixins.${prop("mod.id")}.json")
    config("mixins.${prop("mod.id")}.compat.json")
}

val modLocalRuntime = obfuscation.createRemappingConfiguration(configurations["localRuntime"])

// JourneyMap 6.x on 1.20.1 ships its v2 API only inside the mod jar. Extract it to compile against; JourneyMap
// supplies it at runtime. Its classes name Minecraft types only by class name (Mojang names on Forge 1.20.1 too),
// so no remapping is needed.
val journeyMapMod: Configuration by configurations.creating { isTransitive = false }
val extractJourneyMapApi by tasks.registering(Copy::class) {
    val entry = prop("deps.journeymap_api_jarjar")
    from(journeyMapMod.elements.map { files -> files.map { zipTree(it) } }) { include(entry) }
    into(layout.buildDirectory.dir("journeymap-api"))
    eachFile { path = name }
    includeEmptyDirs = false
}

dependencies {
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")

    // LDLib v1: UI framework on Forge 1.20.1 (remapped from SRG by modImplementation).
    "modImplementation"(prop("deps.ldlib"))

    journeyMapMod(prop("deps.journeymap"))
    compileOnly(files(extractJourneyMapApi.map { it.destinationDir.resolve(prop("deps.journeymap_api_jarjar").substringAfterLast('/')) }).builtBy(extractJourneyMapApi))

    // Xaero's World Map and Minimap for the claim highlighters. Their 1.20.1 jars call Minecraft by SRG names, so they
    // go through the remapping mod* configurations.
    "modCompileOnly"(prop("deps.xaero_worldmap"))
    "modCompileOnly"(prop("deps.xaero_minimap"))
    "modCompileOnly"(prop("deps.carryon"))

    // The JourneyMap and Xaero mods themselves, dev-runtime only (client runs).
    "modLocalRuntime"(prop("deps.journeymap"))
    "modLocalRuntime"(prop("deps.xaero_worldmap"))
    "modLocalRuntime"(prop("deps.xaero_minimap"))
    "modLocalRuntime"(prop("deps.carryon"))
}

tasks.named<Jar>("jar") {
    manifest.attributes(
        "MixinConfigs" to listOf("json", "compat.json").joinToString(",") { "mixins.${prop("mod.id")}.$it" }
    )
}
