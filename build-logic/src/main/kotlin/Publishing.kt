import com.modrinth.minotaur.ModrinthExtension
import com.modrinth.minotaur.dependencies.ModDependency
import com.modrinth.minotaur.dependencies.VersionDependency
import net.darkhax.curseforgegradle.TaskPublishCurseForge
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register

/*
 * CurseForge and Modrinth upload tasks ("curseforge", "modrinth") for one node. They are only created when an API key
 * is set, or when DEPLOYMENT_DEBUG=true (which makes both plugins print the request instead of sending it).
 *
 * Environment:
 *   CURSEFORGE_API_KEY / CF_API_TOKEN, CURSEFORGE_PROJECT_ID, CURSEFORGE_RELATIONS  ("req:slug;opt:slug")
 *   MODRINTH_API_KEY / MPR_TOKEN,      MODRINTH_PROJECT_ID,   MODRINTH_RELATIONS    ("req:id;opt-ver:id")
 *   CURSEFORGE_RELATIONS_<LOADER> / MODRINTH_RELATIONS_<LOADER> (LOADER = FORGE or NEOFORGE) override the relations
 *   for one loader, because the dependencies differ (LDLib 1 on Forge, LDLib2 on NeoForge).
 *   RELEASE_TYPE (release|beta|alpha), CHANGELOG_LOCATION, DEPLOYMENT_DEBUG
 */

private val Project.loaderDisplayName: String
    get() = if (loader == "neoforge") "NeoForge" else "Forge"

private fun Project.env(name: String): String? = providers.environmentVariable(name).orNull?.takeIf { it.isNotEmpty() }

/** The relations for this node's loader, else the ones for every loader. */
private fun Project.relations(platform: String): String =
    env("${platform}_RELATIONS_${loader.uppercase()}") ?: env("${platform}_RELATIONS") ?: ""

private fun Project.releaseType(): String {
    val t = (env("RELEASE_TYPE") ?: "release").lowercase()
    if (t !in listOf("release", "beta", "alpha")) {
        throw GradleException("RELEASE_TYPE must be release, beta, or alpha. Got: $t")
    }
    return t
}

private fun Project.platformChangelog(): String {
    val f = rootProject.file(env("CHANGELOG_LOCATION") ?: "build/platform-changelog.md")
    return if (f.exists()) f.readText(Charsets.UTF_8)
    else "Automated ${releaseType()} release of ${prop("mod.name")} ${prop("mod.version")} for Minecraft $mcVersion."
}

private fun Project.platformVersionName(): String =
    "${prop("mod.name")} ${prop("mod.version")} ($loaderDisplayName $mcVersion)"

fun Project.configurePlatformPublishing() {
    val debug = (env("DEPLOYMENT_DEBUG") ?: "false").toBoolean()
    val cfApiKey = env("CURSEFORGE_API_KEY") ?: env("CF_API_TOKEN")
    val mrApiKey = env("MODRINTH_API_KEY") ?: env("MPR_TOKEN")

    if (cfApiKey != null || debug) configureCurseForge(cfApiKey ?: "debug_token", debug)
    if (mrApiKey != null || debug) configureModrinth(mrApiKey ?: "debug_token", debug)
}

private fun Project.configureCurseForge(apiKey: String, debug: Boolean) {
    val projectId = env("CURSEFORGE_PROJECT_ID") ?: ""
    val relations = relations("CURSEFORGE")
    tasks.register<TaskPublishCurseForge>("curseforge") {
        group = "publishing"
        dependsOn("build")
        disableVersionDetection()
        debugMode = debug
        apiToken = apiKey
        doFirst {
            if (projectId.isEmpty()) throw GradleException("CURSEFORGE_PROJECT_ID is not set")
            val mainFile = upload(projectId, project.tasks.named("jar"))
            mainFile.displayName = platformVersionName()
            mainFile.releaseType = releaseType()
            mainFile.changelog = platformChangelog()
            mainFile.changelogType = "markdown"
            mainFile.addModLoader(loaderDisplayName)
            mainFile.addJavaVersion("Java ${prop("java")}")
            mainFile.addGameVersion(mcVersion)
            val typeMap = mapOf(
                "req" to "requiredDependency", "required" to "requiredDependency",
                "opt" to "optionalDependency", "optional" to "optionalDependency",
                "embed" to "embeddedLibrary", "embedded" to "embeddedLibrary",
                "incomp" to "incompatible", "fail" to "incompatible", "tool" to "tool",
            )
            relations.split(';').filter { it.isNotEmpty() }.forEach { dep ->
                val parts = dep.split(':')
                val type = typeMap[parts[0]] ?: parts[0]
                if (type !in typeMap.values) throw GradleException("Invalid CurseForge dependency type: ${parts[0]}")
                mainFile.addRelation(parts[1], type)
            }
            val sources = mainFile.withAdditionalFile(project.tasks.named("sourcesJar"))
            sources.changelog = platformChangelog()
        }
    }
}

private fun Project.configureModrinth(apiKey: String, debug: Boolean) {
    apply(plugin = "com.modrinth.minotaur")
    val relations = relations("MODRINTH")
    extensions.configure<ModrinthExtension> {
        token.set(apiKey)
        projectId.set(env("MODRINTH_PROJECT_ID") ?: "")
        versionName.set(platformVersionName())
        versionNumber.set("${prop("mod.version")}+$mcVersion-$loader")
        versionType.set(releaseType())
        changelog.set(platformChangelog())
        gameVersions.set(listOf(mcVersion))
        loaders.set(listOf(loader))
        debugMode.set(debug)
        uploadFile.set(tasks.named("jar"))
        additionalFiles.set(listOf(tasks.named("sourcesJar")))

        val scopeMap = mapOf(
            "req" to "required", "required" to "required", "opt" to "optional", "optional" to "optional",
            "embed" to "embedded", "embedded" to "embedded", "incomp" to "incompatible", "fail" to "incompatible",
        )
        relations.split(';').filter { it.isNotEmpty() }.forEach { dep ->
            val parts = dep.split(':')
            val qual = parts[0].split('-')
            val scope = scopeMap[qual[0]] ?: qual[0]
            if (scope !in scopeMap.values) throw GradleException("Invalid Modrinth dependency scope: ${qual[0]}")
            when (val kind = qual.getOrElse(1) { "project" }) {
                "proj", "p", "project" -> dependencies.add(ModDependency(parts[1], scope))
                "ver", "v", "version" -> dependencies.add(VersionDependency(parts[1], scope))
                else -> throw GradleException("Invalid Modrinth dependency kind: $kind")
            }
        }
    }
    tasks.named("modrinth") { dependsOn("build") }
}
