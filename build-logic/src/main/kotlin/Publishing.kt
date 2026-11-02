import me.modmuss50.mpp.ModPublishExtension
import me.modmuss50.mpp.ReleaseType
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.kotlin.dsl.configure

/*
 * Modrinth + CurseForge upload for one node (mod-publish-plugin); root `publishAllMods` runs every node.
 *
 * stonecutter.properties.toml keys (prop): mod.publish_modrinth_project, mod.publish_curseforge_project (blank skips
 * that site), publish_requires / publish_optional (comma separated slugs, both sites), publish_skip.
 *
 * Switches (-P): publish.dryRun=true (log, upload nothing, no tokens), publish.type=alpha|beta|release (default beta),
 * publish.changelog=text | publish.changelogFile=path | "## <mod.version>" section of CHANGELOG.md.
 * Tokens: MODRINTH_TOKEN / CURSEFORGE_TOKEN, or modrinth.token / curseforge.token in ~/.gradle/gradle.properties.
 */

private fun Project.list(key: String): List<String> =
    (propOrNull(key) ?: "").split(',').map { it.trim() }.filter { it.isNotEmpty() }

/** "## 2.4.3", "## v2.4.3" and "## [2.4.3] - date" all match. */
private fun versionSection(text: String, version: String): String {
    val lines = text.lines()
    val start = lines.indexOfFirst {
        it.startsWith("## ") &&
            it.substring(3).trim().substringBefore(' ').replace("[", "").replace("]", "").removePrefix("v") == version
    }
    if (start < 0) return ""
    val end = (start + 1 until lines.size).firstOrNull { lines[it].startsWith("## ") } ?: lines.size
    return lines.subList(start + 1, end).joinToString("\n").trim()
}

fun Project.configurePlatformPublishing() {
    apply(plugin = "me.modmuss50.mod-publish-plugin")

    val modVersion = prop("mod.version")
    val modName = prop("mod.name")
    val skipped = propOrNull("publish_skip")?.toBoolean() == true
    val required = list("publish_requires")
    val optionals = list("mod.publish_optional")
    val modrinthProject = propOrNull("mod.publish_modrinth_project")
    val curseforgeProject = propOrNull("mod.publish_curseforge_project")
    val javaVersion = JavaVersion.toVersion(prop("java"))
    val nodeLoader = loader
    val nodeMc = mcVersion

    val changelogText = providers.gradleProperty("publish.changelog").orElse(
        providers.gradleProperty("publish.changelogFile")
            .map { rootProject.file(it).let { f -> if (f.isFile) f.readText() else "" } }
            .orElse(providers.provider {
                rootProject.file("CHANGELOG.md").let { f -> if (f.isFile) versionSection(f.readText(), modVersion) else "" }
            })
    ).map { it.ifBlank { "$modName $modVersion." } }

    // The jar a player installs: ModDevGradle Legacy's reobfuscated jar on Forge, the plain jar on NeoForge.
    val releaseJar = provider { if (tasks.names.contains("reobfJar")) "reobfJar" else "jar" }
        .flatMap { tasks.named(it, AbstractArchiveTask::class.java) }
        .flatMap { it.archiveFile }

    extensions.configure<ModPublishExtension> {
        file.set(releaseJar)
        version.set("$modVersion+$nodeMc-$nodeLoader")
        displayName.set("$modName $modVersion (${if (nodeLoader == "neoforge") "NeoForge" else "Forge"} $nodeMc)")
        changelog.set(changelogText)
        type.set(
            when (providers.gradleProperty("publish.type").getOrElse("beta").lowercase()) {
                "alpha" -> ReleaseType.ALPHA
                "release", "stable" -> ReleaseType.STABLE
                else -> ReleaseType.BETA
            }
        )
        modLoaders.add(nodeLoader)
        dryRun.set(providers.gradleProperty("publish.dryRun").map { it.toBoolean() }.orElse(false))

        if (!skipped && modrinthProject != null) {
            modrinth {
                projectId.set(modrinthProject)
                accessToken.set(providers.environmentVariable("MODRINTH_TOKEN").orElse(providers.gradleProperty("modrinth.token")))
                minecraftVersions.add(nodeMc)
                if (required.isNotEmpty()) requires(*required.toTypedArray())
                if (optionals.isNotEmpty()) optional(*optionals.toTypedArray())
            }
        }
        if (!skipped && curseforgeProject != null) {
            curseforge {
                projectId.set(curseforgeProject)
                accessToken.set(providers.environmentVariable("CURSEFORGE_TOKEN").orElse(providers.gradleProperty("curseforge.token")))
                minecraftVersions.add(nodeMc)
                javaVersions.add(javaVersion)
                client.set(true)
                server.set(true)
                if (required.isNotEmpty()) requires(*required.toTypedArray())
                if (optionals.isNotEmpty()) optional(*optionals.toTypedArray())
            }
        }
    }
}
