import org.gradle.api.Project
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register

/** Variant folders written with NeoForge names; the Forge node compiles a copy with [FORGE_RENAMES] applied. */
val RENAMED_FOR_FORGE: Set<String> = setOf("mc-1.20.1-1.21.1")

/** Forge 1.20.1 names of NeoForge classes; the same renames Stonecutter applies to src/main. */
val FORGE_RENAMES: List<Pair<String, String>> = listOf(
    "net.neoforged.neoforge." to "net.minecraftforge.",
    "net.neoforged.fml." to "net.minecraftforge.fml.",
    "net.neoforged.api.distmarker." to "net.minecraftforge.api.distmarker.",
    "net.neoforged.bus.api." to "net.minecraftforge.eventbus.api.",
    "ModConfigSpec" to "ForgeConfigSpec",
)

/** The one table that says which version-specific source folders each node compiles. See docs/multi-version.md. */
val VARIANTS: Map<String, List<String>> = mapOf(
    "1.20.1-forge" to listOf("forge-1.20.1", "mc-1.20.1-1.21.1", "ui-ldlib1"),
    "1.21.1-neoforge" to listOf("neoforge", "neoforge-1.21.1", "mc-1.20.1-1.21.1", "ui-ldlib2"),
    "26.1.2-neoforge" to listOf("neoforge", "neoforge-26", "neoforge-26.1", "ui-ldlib2"),
    "26.2-neoforge" to listOf("neoforge", "neoforge-26", "neoforge-26.2", "ui-ldlib2"),
)

fun Project.applyVariants() {
    val node = sc.current.project
    val folders = VARIANTS[node] ?: error("No variant folders declared for node '$node' in Variants.kt")
    extensions.getByType<SourceSetContainer>().named("main") {
        folders.forEach { folder ->
            if (loader == "forge" && folder in RENAMED_FOR_FORGE) {
                java.srcDir(renamedForForge(folder))
            } else {
                java.srcDir(rootProject.file("src/variant/$folder/java"))
            }
            resources.srcDir(rootProject.file("src/variant/$folder/resources"))
        }
    }
}

private fun Project.renamedForForge(folder: String) =
    tasks.register<Sync>("renameVariant-$folder") {
        from(rootProject.file("src/variant/$folder/java"))
        into(layout.buildDirectory.dir("generated/variant/$folder/java"))
        filter { line -> FORGE_RENAMES.fold(line) { text, (from, to) -> text.replace(from, to) } }
    }
