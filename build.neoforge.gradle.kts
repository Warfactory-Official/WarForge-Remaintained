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

    runs {
        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
            logLevel = org.slf4j.event.Level.DEBUG
        }
        register("client") {
            client()
            systemProperty("neoforge.enabledGameTestNamespaces", prop("mod.id"))
        }
        register("server") {
            server()
            programArgument("--nogui")
            systemProperty("neoforge.enabledGameTestNamespaces", prop("mod.id"))
        }
        // Second client for local multiplayer testing.
        register("client2") {
            client()
            gameDirectory = file("run_client2")
            systemProperty("neoforge.enabledGameTestNamespaces", prop("mod.id"))
        }
        register("data") {
            // "data" before NeoForge 1.21.4, "clientData" (client and server providers) after.
            type = prop("data_run_type")
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

    if (gradle.startParameter.taskNames.none { it.lowercase().contains("server") }) {
        "localRuntime"(prop("deps.journeymap"))
        "localRuntime"(prop("deps.xaero_worldmap"))
        "localRuntime"(prop("deps.xaero_minimap"))
    }

    // Soft / compat dependencies, re-enabled in their porting phases:
    // SuperbWarfare (curse.maven:superb-warfare-1218165) and AshVehicle (curse.maven:ashvehicle-1291715): the compat
    // mixin @targets are strings, so they compile without the jars. EffortlessBuilding is not used.
}

publishing {
    publications {
        named<MavenPublication>("mavenJava") {
            version = "${prop("mod.version")}-SNAPSHOT"
        }
    }
    repositories {
        maven {
            name = "WarfactorySnapshots"
            url = uri("https://repo.warfactory.co/snapshots")
            credentials {
                username = findProperty("warfactoryUser") as String? ?: System.getenv("WARFACTORY_MAVEN_USER")
                password = findProperty("warfactoryToken") as String? ?: System.getenv("WARFACTORY_MAVEN_TOKEN")
            }
        }
    }
}
