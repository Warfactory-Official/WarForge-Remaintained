import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

val Project.sc: StonecutterBuildExtension
    get() = extensions.getByType<StonecutterBuildExtension>()

/** A property from stonecutter.properties.toml for this node. Fails if it is missing. */
fun Project.prop(key: String): String = sc.properties.get<String>(key)

/** A property from stonecutter.properties.toml for this node, or null if this node does not set it. */
fun Project.propOrNull(key: String): String? =
    runCatching { sc.properties.get<String>(key) }.getOrNull()?.takeIf { it.isNotBlank() }

/** The Minecraft version of this node, for example "1.21.1". */
val Project.mcVersion: String
    get() = sc.current.version

/** The loader of this node: "forge" or "neoforge". */
val Project.loader: String
    get() = sc.current.project.substringAfterLast('-')
