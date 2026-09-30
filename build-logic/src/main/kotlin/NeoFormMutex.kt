import org.gradle.api.Project
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

/**
 * Lets only one node decompile/recompile Minecraft at a time. Several MDG decompiles in parallel use a lot of memory.
 */
abstract class NeoFormMutex : BuildService<BuildServiceParameters.None>

fun Project.useNeoFormMutex() {
    val mutex = gradle.sharedServices.registerIfAbsent("neoFormMutex", NeoFormMutex::class.java) {
        maxParallelUsages.set(1)
    }
    tasks.matching { it.name == "createMinecraftArtifacts" }.configureEach {
        usesService(mutex)
        // MDG reads the Stonecutter-generated sources (for the IDE source jar); order the two tasks.
        dependsOn("stonecutterGenerate")
    }
}
