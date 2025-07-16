package se.premex.gmai.plugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.CacheableTask
import se.premex.gmai.plugin.utils.ProcessManager

@CacheableTask
abstract class StopOllamaTask : DefaultTask() {

    @get:Input
    abstract val gracefulShutdown: Property<Boolean>

    @get:Input
    abstract val timeoutSeconds: Property<Int>

    @get:Input
    abstract val port: Property<Int>

    init {
        group = "ai"
        description = "Stop the Ollama service"

        // Set default values
        gracefulShutdown.convention(true)
        timeoutSeconds.convention(30)
        port.convention(11434)
    }

    @TaskAction
    fun stopOllama() {
        val processManager = ProcessManager(project.logger)

        if (!processManager.isOllamaRunning(port.get())) {
            project.logger.info("Ollama is not running")
            return
        }

        project.logger.info("Stopping Ollama...")

        val success = if (gracefulShutdown.get()) {
            processManager.stopOllamaGracefully(timeoutSeconds.get())
        } else {
            processManager.stopOllama()
        }

        if (success) {
            project.logger.lifecycle("Ollama stopped successfully")
        } else {
            project.logger.warn("Failed to stop Ollama cleanly")
        }
    }
}
