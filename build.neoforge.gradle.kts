plugins {
    id("mod-common")
    id("net.neoforged.moddev")
}

neoForge {
    version = prop("deps.neoforge")

    // Official Mojang names + Parchment parameter/javadoc export. 26.x ships unobfuscated with parameter names.
    propOrNull("deps.parchment")?.let { mappings ->
        parchment {
            mappingsVersion = mappings
            minecraftVersion = prop("deps.parchment_minecraft")
        }
    }

    // MDG auto-detects src/main/resources/META-INF/accesstransformer.cfg (currently empty).

    configureRuns(runs, "neoforge.enabledGameTestNamespaces", configurations["localRuntime"]) {
        // "data" before NeoForge 1.21.4, "clientData" (client and server providers) after.
        type = prop("data_run_type")
    }

    mods {
        register(prop("mod.id")) {
            sourceSet(sourceSets["main"])
        }
    }

    addModdingDependenciesTo(sourceSets["test"])
}

dependencies {
    // LDLib2: UI framework. On 1.21.1 the ':all' classifier bundles the impl; ldlib >= 2.2.1 pulls Yoga transitively.
    val ldlib2Classifier = propOrNull("deps.ldlib2_classifier")?.let { ":$it" } ?: ""
    implementation("com.lowdragmc.ldlib2:${prop("deps.ldlib2_artifact")}:${prop("deps.ldlib2")}$ldlib2Classifier")

    // Upgrade-config and vein config parsing use NeoForge's bundled night-config TOML library.

    // JourneyMap client API (@JourneyMapPlugin): compileOnly. The JourneyMap mod supplies the impl, which it
    // jar-in-jars, so this must never be shaded.
    compileOnly(prop("deps.journeymap_api"))

    // The JourneyMap mod itself, dev-runtime only. Without it nothing proves the plugins are discovered: a plugin
    // whose annotation class is missing is never scanned, with no error anywhere.
    // Client runs only: JourneyMap jar-in-jars lwjgl-ktx, whose module descriptor upsets a dev server launch.
    // Xaero's World Map and Minimap: compileOnly for the claim highlighters (client/xaero, added by mixins).
    compileOnly(prop("deps.xaero_worldmap"))
    compileOnly(prop("deps.xaero_minimap"))
    // Carry On: compileOnly for compat/carryon (gated mixin + client check); never shipped.
    compileOnly(prop("deps.carryon"))

    "localRuntime"(prop("deps.journeymap"))
    "localRuntime"(prop("deps.xaero_worldmap"))
    "localRuntime"(prop("deps.xaero_minimap"))
    "localRuntime"(prop("deps.carryon"))
}
