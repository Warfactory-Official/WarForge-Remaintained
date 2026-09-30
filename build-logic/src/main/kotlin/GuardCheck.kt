import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Fails when a Stonecutter version comment appears in shared code (src/main).
 * Version-specific code goes in a src/variant folder instead (see Variants.kt).
 */
abstract class CheckNoVersionGuards : DefaultTask() {
    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @TaskAction
    fun check() {
        val guard = Regex("""//\?|/\*\?""")
        val hits = sources.asFileTree.files.flatMap { file ->
            file.readLines().mapIndexedNotNull { i, line ->
                if (guard.containsMatchIn(line)) "${file.path}:${i + 1}: ${line.trim()}" else null
            }
        }
        if (hits.isNotEmpty()) {
            throw GradleException(
                "Stonecutter version comments are not allowed in src/main. " +
                    "Move the version-specific code to a src/variant folder (see build-logic Variants.kt):\n" +
                    hits.joinToString("\n")
            )
        }
    }
}
