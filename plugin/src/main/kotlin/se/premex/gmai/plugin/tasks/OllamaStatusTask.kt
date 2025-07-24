package se.premex.gmai.plugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.slf4j.LoggerFactory
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

    private val logger = LoggerFactory.getLogger(OllamaStatusTask::class.java)

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

        // Use project.logger for consistent logging approach
        project.logger.lifecycle("Ollama Status:")
        project.logger.lifecycle("  Process Running: $isProcessRunning")
        project.logger.lifecycle("  Service Healthy: $isServiceHealthy")
        project.logger.lifecycle("  Endpoint: http://${host.get()}:${port.get()}")

        if (verbose.get()) {
            project.logger.info("Detailed status check completed for Ollama at ${host.get()}:${port.get()}")
            project.logger.info("Process running: $isProcessRunning, Service healthy: $isServiceHealthy")
        }
    }
}
