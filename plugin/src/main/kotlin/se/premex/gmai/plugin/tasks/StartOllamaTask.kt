package se.premex.gmai.plugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.CacheableTask
import org.slf4j.LoggerFactory
import kotlinx.coroutines.runBlocking
import se.premex.gmai.plugin.models.OllamaInstance
import se.premex.gmai.plugin.models.OllamaInstallationStrategy
import se.premex.gmai.plugin.services.OllamaService
import se.premex.gmai.plugin.utils.ProcessManager
import se.premex.gmai.plugin.utils.PortManager
import se.premex.gmai.plugin.utils.ErrorHandler
import java.time.Duration

@CacheableTask
abstract class StartOllamaTask : DefaultTask() {

    @get:Input
    abstract val host: Property<String>

    @get:Input
    abstract val port: Property<Int>

    @get:Input
    @get:Optional
    abstract val installPath: Property<String>

    @get:Input
    abstract val autoInstall: Property<Boolean>

    @get:Input
    abstract val additionalArgs: ListProperty<String>

    @get:Input
    abstract val allowPortChange: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val installationStrategy: Property<OllamaInstallationStrategy>

    private val logger = LoggerFactory.getLogger(StartOllamaTask::class.java)

    init {
        group = "ai"
        description = "Start Ollama service and ensure it's ready for use"

        // Set default values
        host.convention("localhost")
        port.convention(11434)
        autoInstall.convention(true)
        additionalArgs.convention(emptyList())
        allowPortChange.convention(true)
        installationStrategy.convention(OllamaInstallationStrategy.PREFER_EXISTING)
    }

    @TaskAction
    fun startOllama() {
        val processManager = ProcessManager(logger)
        val portManager = PortManager(logger)
        val errorHandler = ErrorHandler(logger)

        try {
            // Phase 3: Resolve port conflicts
            val portResolution = portManager.resolvePortConflict(
                preferredPort = port.get(),
                host = host.get(),
                allowPortChange = allowPortChange.get()
            )

            val actualPort = when (portResolution.status) {
                PortManager.PortResolution.Status.AVAILABLE -> {
                    logger.info("Port ${port.get()} is available")
                    portResolution.port
                }
                PortManager.PortResolution.Status.SERVICE_RUNNING -> {
                    logger.info("Ollama is already running on port ${portResolution.port}")
                    return // Service already running, nothing to do
                }
                PortManager.PortResolution.Status.ALTERNATIVE_FOUND -> {
                    println("Port ${port.get()} is in use, using alternative port ${portResolution.port}")
                    portResolution.port
                }
                PortManager.PortResolution.Status.CONFLICT -> {
                    errorHandler.handlePortConflict(port.get(), host.get(), allowPortChange.get())
                }
                PortManager.PortResolution.Status.NO_ALTERNATIVE -> {
                    throw GradleException("No alternative port available")
                }
            }

            // For configuration cache compatibility, we'll need to handle installation
            // through the build service or avoid project dependency
            if (autoInstall.get()) {
                logger.info("Auto-install is enabled, but installation logic has been moved to build service for configuration cache compatibility")
                // The actual installation should be handled by the OllamaLifecycleService
                // which is configuration cache compatible
            }

            // Create Ollama instance for service operations
            val ollamaInstance = OllamaInstance(
                host = host.get(),
                port = actualPort,
                protocol = "http",
                timeout = Duration.ofSeconds(30),
                isIsolated = false,
                isolatedPath = installPath.orNull
            )

            val service = OllamaService(ollamaInstance, logger)

            // Start the service if not already running
            if (!processManager.isOllamaRunning(actualPort)) {
                logger.info("Starting Ollama service on ${host.get()}:$actualPort")

                // Use a simplified start approach that doesn't require project access
                val startResult = processManager.startOllamaSimple(
                    host = host.get(),
                    port = actualPort,
                    additionalArgs = additionalArgs.get()
                )

                if (!startResult) {
                    throw GradleException("Failed to start Ollama service")
                }

                // Wait for service to be ready
                val maxWaitTime = 30000L // 30 seconds
                val startTime = System.currentTimeMillis()
                var isReady = false

                while (System.currentTimeMillis() - startTime < maxWaitTime) {
                    if (runBlocking { service.isHealthy() }) {
                        isReady = true
                        break
                    }
                    Thread.sleep(1000)
                }

                if (!isReady) {
                    throw GradleException("Ollama service failed to become ready within 30 seconds")
                }

                println("Ollama service started successfully on ${host.get()}:$actualPort")
            } else {
                println("Ollama service is already running on ${host.get()}:$actualPort")
            }

        } catch (e: Exception) {
            logger.error("Failed to start Ollama service: ${e.message}", e)
            throw GradleException("Failed to start Ollama service: ${e.message}", e)
        }
    }
}
