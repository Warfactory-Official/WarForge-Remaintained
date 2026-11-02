import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.plugins.ide.idea.model.IdeaModel

/**
 * Build setup shared by every node: Java toolchain, repositories, common dependencies, resources, jar and publishing.
 * The loader plugin (ModDevGradle or its legacyforge variant) and the runs are set up in build.<loader>.gradle.kts.
 */
class ModCommonPlugin : Plugin<Project> {
    override fun apply(project: Project) = with(project) {
        // Read [<loader>] and [<loader>."<mc>"] sections of stonecutter.properties.toml for this node.
        sc.properties.tags(mcVersion, loader)

        listOf("java-library", "idea", "maven-publish").forEach { apply(plugin = it) }

        group = prop("mod.group")
        version = prop("mod.version")

        extensions.configure<JavaPluginExtension> {
            toolchain.languageVersion.set(JavaLanguageVersion.of(prop("java").toInt()))
            withSourcesJar()
        }
        extensions.configure<org.gradle.api.plugins.BasePluginExtension> {
            archivesName.set("${prop("mod.archives_base_name")}-$mcVersion-$loader")
        }

        applyVariants()
        extensions.getByType<SourceSetContainer>().named("main") {
            // Resources made by the data generators.
            resources.srcDir(rootProject.file("src/generated/resources"))
            resources.exclude("**/*.bbmodel") // BlockBench project files
            resources.exclude(".cache")
        }

        configureRepositories()
        configureDependencies()
        configureResources()
        configureJar()
        useNeoFormMutex()

        tasks.named("check") { dependsOn(":checkNoVersionGuards") }

        tasks.withType<Test>().configureEach {
            // Level.OVERWORLD needs bootstrapped vanilla registries, which only 1.21.1 tolerates without a launcher.
            useJUnitPlatform { if (mcVersion != "1.21.1") excludeTags("needs-registries") }
        }

        tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
            options.compilerArgs.addAll(listOf("-Xmaxerrs", "2000", "-Xlint:unchecked,rawtypes"))
        }

        extensions.configure<IdeaModel> {
            module {
                isDownloadSources = true
                isDownloadJavadoc = true
            }
        }

        extensions.configure<PublishingExtension> {
            publications.register<MavenPublication>("mavenJava") {
                artifactId = "${prop("mod.archives_base_name")}-$mcVersion-$loader"
                from(components.getByName("java"))
            }
            repositories.maven(rootProject.uri("repo"))
        }
        if (loader == "neoforge") configureSnapshotPublishing()

        configurePlatformPublishing()
    }

    private fun Project.configureRepositories() = repositories.apply {
        maven("https://maven.neoforged.net/releases") { name = "NeoForged" }
        maven("https://maven.firstdark.dev/snapshots") { name = "FirstDark" } // LDLib (UI framework) + its libs
        maven("https://maven.firstdark.dev/releases") { name = "FirstDark Releases" }
        maven("https://modmaven.dev") { name = "ModMaven" } // AE2 Yoga (LDLib2 transitive) and other ecosystem libs
        maven("https://maven.parchmentmc.org") { name = "ParchmentMC" }
        maven("https://jm.gserv.me/repository/maven-public/") { // the only place the JourneyMap client API is published
            name = "JourneyMap"
            content { includeGroup("info.journeymap") }
        }
        maven("https://api.modrinth.com/maven") { // map mods at dev runtime. Scoped so it cannot shadow anything.
            name = "Modrinth"
            content { includeGroup("maven.modrinth") }
        }
        mavenCentral()
    }

    private fun Project.configureDependencies() {
        // localRuntime: dev-only runtime mods (map mods) that are NOT shipped.
        configurations.create("localRuntime")

        dependencies.apply {
            add("compileOnly", "org.jetbrains:annotations:24.1.0")
            add("compileOnly", "org.projectlombok:lombok:1.18.42")
            add("annotationProcessor", "org.projectlombok:lombok:1.18.42")
            add("testImplementation", platform("org.junit:junit-bom:5.11.4"))
            add("testImplementation", "org.junit.jupiter:junit-jupiter")
            add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
        }
    }

    private fun Project.configureResources() {
        val replaceProperties = mapOf(
            "mod_id" to prop("mod.id"),
            "mod_name" to prop("mod.name"),
            "mod_version" to prop("mod.version"),
            "mod_group" to prop("mod.group"),
            "mod_authors" to prop("mod.authors"),
            "mod_description" to prop("mod.description"),
            "minecraft_version" to mcVersion,
            "minecraft_version_range" to prop("minecraft_version_range"),
            "loader_version_range" to prop("loader_version_range"),
            "loader_dependency_range" to prop("loader_dependency_range"),
            "ldlib_dependency_range" to prop("ldlib_dependency_range"),
            // Only the pre-26 pack.mcmeta has a pack_format; 26.x declares no format (NeoForge then accepts any).
            "pack_format" to (propOrNull("pack_format") ?: ""),
            "mixin_java" to prop("mixin_java"),
            "obj_loader" to prop("obj_loader"),
        )
        tasks.named<ProcessResources>("processResources") {
            inputs.properties(replaceProperties)
            // Only these files hold ${...} tokens; other JSON may contain '$' and must stay untouched.
            filesMatching(listOf(
                "META-INF/neoforge.mods.toml", "META-INF/mods.toml", "pack.mcmeta",
                "mixins.*.json", "assets/*/models/block/pole.json",
            )) {
                expand(replaceProperties)
            }
        }
    }

    private fun Project.configureJar() {
        tasks.named<Jar>("jar") {
            manifest.attributes(
                mapOf(
                    "Specification-Title" to prop("mod.id"),
                    "Specification-Vendor" to prop("mod.authors"),
                    "Specification-Version" to "1",
                    "Implementation-Title" to rootProject.name,
                    "Implementation-Version" to prop("mod.version"),
                    "Implementation-Vendor" to prop("mod.authors"),
                )
            )
        }
    }

    private fun Project.configureSnapshotPublishing() {
        extensions.configure<PublishingExtension> {
            publications.named<MavenPublication>("mavenJava") { version = "${prop("mod.version")}-SNAPSHOT" }
            repositories.maven("https://repo.warfactory.co/snapshots") {
                name = "WarfactorySnapshots"
                credentials {
                    username = findProperty("warfactoryUser") as String? ?: System.getenv("WARFACTORY_MAVEN_USER")
                    password = findProperty("warfactoryToken") as String? ?: System.getenv("WARFACTORY_MAVEN_TOKEN")
                }
            }
        }
    }
}
