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
        val processManager = ProcessManager(logger)
        val ollamaInstance = OllamaInstance(
            host = host.get(),
            port = port.get(),
            protocol = "http",
            timeout = Duration.ofSeconds(30),
            isIsolated = false,
            isolatedPath = null
        )
        val service = OllamaService(ollamaInstance, logger)

        val isProcessRunning = processManager.isOllamaRunning(port.get())
        val isServiceHealthy = runBlocking { service.isHealthy() }

        // Use task.logger for configuration cache compatibility
        logger.lifecycle("Ollama Status:")
        logger.lifecycle("  Process Running: $isProcessRunning")
        logger.lifecycle("  Service Healthy: $isServiceHealthy")
        logger.lifecycle("  Endpoint: http://${host.get()}:${port.get()}")

        if (verbose.get()) {
            logger.info("Detailed status check completed for Ollama at ${host.get()}:${port.get()}")
            logger.info("Process running: $isProcessRunning, Service healthy: $isServiceHealthy")
        }
    }
}
