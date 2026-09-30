import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.getByType

/**
 * The one table that says which version-specific source folders each node compiles.
 *
 * Shared code lives in src/main and is written for 1.21.1-neoforge. It must not contain Stonecutter comments
 * (see GuardCheck.kt). Code that differs between versions lives in src/variant/<folder>/{java,resources}. A class has
 * the same package and name in every folder that has a copy of it, so shared code calls it directly.
 *
 * Folder names say who uses them: a loader ("neoforge"), a loader + version range ("neoforge-26" = every NeoForge 26.x
 * node), vanilla code shared across loaders ("mc-1.20.1-1.21.1" = vanilla API before the 1.21.2 changes), or a UI
 * library ("ui-ldlib1", "ui-ldlib2").
 *
 * A folder used by nodes on both sides of 1.21.11 (for example "neoforge") must not name ResourceLocation, because
 * Stonecutter's ResourceLocation -> Identifier replacement only runs on src/main.
 */
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
            java.srcDir(rootProject.file("src/variant/$folder/java"))
            resources.srcDir(rootProject.file("src/variant/$folder/resources"))
        }
    }
}
