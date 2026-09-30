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

    runs {
        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
            logLevel = org.slf4j.event.Level.DEBUG
        }
        register("client") {
            client()
            systemProperty("forge.enabledGameTestNamespaces", prop("mod.id"))
        }
        register("server") {
            server()
            programArgument("--nogui")
            systemProperty("forge.enabledGameTestNamespaces", prop("mod.id"))
        }
        // Second client for local multiplayer testing.
        register("client2") {
            client()
            gameDirectory = file("run_client2")
            systemProperty("forge.enabledGameTestNamespaces", prop("mod.id"))
        }
        register("data") {
            data()
            programArguments.addAll(
                "--mod", prop("mod.id"), "--all",
                "--output", rootProject.file("src/generated/resources/").absolutePath,
                "--existing", rootProject.file("src/main/resources/").absolutePath,
            )
        }
    }

    mods {
        register(prop("mod.id")) {
            sourceSet(sourceSets["main"])
        }
    }
}

mixin {
    add(sourceSets["main"], "mixins.${prop("mod.id")}.refmap.json")
    config("mixins.${prop("mod.id")}.json")
    config("mixins.${prop("mod.id")}.compat.json")
}

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

    // The JourneyMap and Xaero mods themselves, dev-runtime only (client runs; see build.neoforge.gradle.kts).
    if (gradle.startParameter.taskNames.none { it.lowercase().contains("server") }) {
        "modRuntimeOnly"(prop("deps.journeymap"))
        "modRuntimeOnly"(prop("deps.xaero_worldmap"))
        "modRuntimeOnly"(prop("deps.xaero_minimap"))
    }
}

tasks.named<Jar>("jar") {
    manifest.attributes(
        "MixinConfigs" to listOf("json", "compat.json").joinToString(",") { "mixins.${prop("mod.id")}.$it" }
    )
}
