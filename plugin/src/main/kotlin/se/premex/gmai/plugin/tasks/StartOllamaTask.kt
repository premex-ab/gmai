package se.premex.gmai.plugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.CacheableTask
import org.gradle.process.ExecOperations
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import se.premex.gmai.plugin.models.OllamaInstance
import se.premex.gmai.plugin.models.OllamaInstallationStrategy
import se.premex.gmai.plugin.services.OllamaService
import se.premex.gmai.plugin.utils.ProcessManager
import se.premex.gmai.plugin.utils.OllamaInstaller
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

    @get:Inject
    abstract val execOperations: ExecOperations

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
        val processManager = ProcessManager(project.logger)
        val installer = OllamaInstaller(project.logger, project)
        val portManager = PortManager(project.logger)
        val errorHandler = ErrorHandler(project.logger)

        try {
            // Phase 3: Resolve port conflicts
            val portResolution = portManager.resolvePortConflict(
                preferredPort = port.get(),
                host = host.get(),
                allowPortChange = allowPortChange.get()
            )

            val actualPort = when (portResolution.status) {
                PortManager.PortResolution.Status.AVAILABLE -> {
                    project.logger.info("Port ${port.get()} is available")
                    portResolution.port
                }
                PortManager.PortResolution.Status.SERVICE_RUNNING -> {
                    project.logger.info("Ollama is already running on port ${portResolution.port}")
                    return // Service already running, nothing to do
                }
                PortManager.PortResolution.Status.ALTERNATIVE_FOUND -> {
                    project.logger.lifecycle("Port ${port.get()} is in use, using alternative port ${portResolution.port}")
                    portResolution.port
                }
                PortManager.PortResolution.Status.CONFLICT -> {
                    errorHandler.handlePortConflict(port.get(), host.get(), allowPortChange.get())
                }
                PortManager.PortResolution.Status.NO_ALTERNATIVE -> {
                    throw GradleException("No alternative port available")
                }
            }

            // Find or install Ollama with enum-based strategy
            val installationResult = try {
                if (autoInstall.get()) {
                    project.logger.info("Finding or installing Ollama with strategy: ${installationStrategy.get()}")
                    installer.findOrInstallOllama(
                        strategy = installationStrategy.get(),
                        isolatedPath = installPath.orNull
                    )
                } else {
                    // Only try to find existing installation if auto-install is disabled
                    val existingPath = installer.findOllamaExecutable()
                    if (existingPath != null) {
                        OllamaInstaller.InstallationResult(
                            success = true,
                            executablePath = existingPath,
                            installationType = OllamaInstaller.InstallationType.EXISTING_SYSTEM,
                            message = "Using existing Ollama installation"
                        )
                    } else {
                        OllamaInstaller.InstallationResult(
                            success = false,
                            executablePath = null,
                            installationType = OllamaInstaller.InstallationType.EXISTING_SYSTEM,
                            message = "Ollama not found and auto-install is disabled"
                        )
                    }
                }
            } catch (e: Exception) {
                errorHandler.handleInstallationError(e, autoInstall.get())
            }

            if (!installationResult.success) {
                throw GradleException("Failed to find or install Ollama: ${installationResult.message}")
            }

            val executablePath = installationResult.executablePath
                ?: throw GradleException("Installation succeeded but executable path is null")

            // Log the installation type for user information
            when (installationResult.installationType) {
                OllamaInstaller.InstallationType.EXISTING_SYSTEM -> {
                    project.logger.lifecycle("Using existing system-wide Ollama installation")
                }
                OllamaInstaller.InstallationType.EXISTING_ISOLATED -> {
                    project.logger.lifecycle("Using existing isolated Ollama installation")
                }
                OllamaInstaller.InstallationType.NEW_ISOLATED -> {
                    project.logger.lifecycle("Installed Ollama in isolated environment")
                }
                OllamaInstaller.InstallationType.NEW_SYSTEM_WIDE -> {
                    project.logger.lifecycle("Installed Ollama system-wide")
                }
            }

            project.logger.info("Ollama executable path: $executablePath")

            // Start Ollama with enhanced error handling
            val success = try {
                processManager.startOllama(
                    executablePath = executablePath,
                    host = host.get(),
                    port = actualPort,
                    additionalArgs = additionalArgs.get()
                )
            } catch (e: Exception) {
                errorHandler.handleStartupError(e, actualPort, host.get())
            }

            if (!success) {
                throw GradleException("Failed to start Ollama")
            }

            // Wait for service to be ready with timeout
            try {
                waitForOllamaReady(actualPort)
            } catch (e: Exception) {
                errorHandler.handleStartupError(e, actualPort, host.get())
            }

            project.logger.lifecycle("Ollama started successfully on ${host.get()}:$actualPort")

            // Store the actual port used for other tasks
            project.extensions.extraProperties.set("ollama.actualPort", actualPort)

        } catch (e: GradleException) {
            throw e
        } catch (e: Exception) {
            throw errorHandler.createContextualError(
                "Start Ollama",
                e,
                mapOf(
                    "host" to host.get(),
                    "port" to port.get(),
                    "autoInstall" to autoInstall.get()
                )
            )
        }
    }

    private fun waitForOllamaReady(actualPort: Int) {
        val ollamaInstance = OllamaInstance(
            host = host.get(),
            port = actualPort,
            protocol = "http",
            timeout = Duration.ofSeconds(30),
            isIsolated = false,
            isolatedPath = null
        )
        val service = OllamaService(ollamaInstance, project.logger)

        val startTime = System.currentTimeMillis()
        val timeoutMs = 60000 // 60 seconds timeout

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                val isHealthy = runBlocking {
                    service.isHealthy()
                }
                if (isHealthy) {
                    project.logger.info("Ollama is ready and healthy at ${ollamaInstance.baseUrl}")
                    return // Exit the method when Ollama is ready
                }
            } catch (e: Exception) {
                project.logger.debug("Health check failed: ${e.message}")
            }
            Thread.sleep(2000) // Check every 2 seconds
        }

        throw GradleException("Ollama did not become ready within 60 seconds")
    }
}
