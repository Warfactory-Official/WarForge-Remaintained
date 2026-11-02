import net.neoforged.moddevgradle.dsl.RunModel
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

/**
 * The runs every node has. [gameTestProperty] is "<loader>.enabledGameTestNamespaces"; [data] picks the data run type.
 * Only the client runs get [localRuntime] (dev-only map mods); a server run never sees it.
 */
fun Project.configureRuns(
    runs: NamedDomainObjectContainer<RunModel>,
    gameTestProperty: String,
    localRuntime: Configuration,
    data: RunModel.() -> Unit,
) {
    runs.apply {
        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
            logLevel.set(org.slf4j.event.Level.DEBUG)
        }
        register("client") {
            client()
            systemProperty(gameTestProperty, prop("mod.id"))
            additionalRuntimeClasspathConfiguration.extendsFrom(localRuntime)
        }
        register("server") {
            server()
            programArgument("--nogui")
            systemProperty(gameTestProperty, prop("mod.id"))
        }
        register("client2") {
            client()
            gameDirectory.set(file("run_client2"))
            systemProperty(gameTestProperty, prop("mod.id"))
            additionalRuntimeClasspathConfiguration.extendsFrom(localRuntime)
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
}
