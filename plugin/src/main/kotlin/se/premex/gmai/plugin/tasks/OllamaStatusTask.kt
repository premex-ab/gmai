package se.premex.gmai.plugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import kotlinx.coroutines.runBlocking
import se.premex.gmai.plugin.models.OllamaInstance
import se.premex.gmai.plugin.services.OllamaService
import se.premex.gmai.plugin.utils.ProcessManager
import java.time.Duration

abstract class OllamaStatusTask : DefaultTask() {

    @get:Input
    abstract val host: Property<String>

    @get:Input
    abstract val port: Property<Int>

    @get:Input
    abstract val verbose: Property<Boolean>

    init {
        group = "ai"
        description = "Check the status of the Ollama service"

        // Set default values
        host.convention("localhost")
        port.convention(11434)
        verbose.convention(false)
    }

    @TaskAction
    fun checkStatus() {
        val processManager = ProcessManager(project.logger)
        val ollamaInstance = OllamaInstance(
            host = host.get(),
            port = port.get(),
            protocol = "http",
            timeout = Duration.ofSeconds(30),
            isIsolated = false,
            isolatedPath = null
        )
        val service = OllamaService(ollamaInstance, project.logger)

        val isProcessRunning = processManager.isOllamaRunning(port.get())
        val isServiceHealthy = runBlocking { service.isHealthy() }

        project.logger.lifecycle("Ollama Status:")
        project.logger.lifecycle("  Process Running: $isProcessRunning")
        project.logger.lifecycle("  Service Healthy: $isServiceHealthy")
        project.logger.lifecycle("  Endpoint: ${ollamaInstance.baseUrl}")

        if (verbose.get() && isServiceHealthy) {
            showDetailedStatus(service)
        }
    }

    private fun showDetailedStatus(service: OllamaService) {
        runBlocking {
            try {
                val models = service.listModels()
                project.logger.lifecycle("  Installed Models: ${models.size}")
                models.forEach { model ->
                    project.logger.lifecycle("    - ${model.name} (${model.version})")
                }
            } catch (e: Exception) {
                project.logger.warn("Failed to get detailed status: ${e.message}")
            }
        }
    }
}
