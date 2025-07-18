/*
 * This source file was generated by the Gradle 'init' task
 */
package se.premex.gmai.plugin

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import se.premex.gmai.plugin.extensions.ManagedAiExtension
import se.premex.gmai.plugin.utils.OllamaInstaller
import se.premex.gmai.plugin.utils.ProcessManager
import se.premex.gmai.plugin.tasks.*

/**
 * Gradle Managed AI Plugin - Phase 2 Implementation
 *
 * Provides task-based lifecycle management for Ollama LLM instances in Gradle builds.
 */
class GradleManagedAiPlugin: Plugin<Project> {

    private val logger: Logger = LoggerFactory.getLogger(GradleManagedAiPlugin::class.java)

    override fun apply(project: Project) {
        // Create the extension
        val extension = project.extensions.create("managedAi", ManagedAiExtension::class.java, project)

        // Configure after evaluation to ensure all configurations are set
        project.afterEvaluate {
            configurePlugin(project, extension)
        }
    }

    private fun configurePlugin(project: Project, extension: ManagedAiExtension) {
        logger.info("Configuring Gradle Managed AI Plugin...")

        // Create services for lifecycle hooks - pass project to installer
        val processManager = ProcessManager(logger)
        val installer = OllamaInstaller(logger, project)

        // Register Phase 2 tasks
        registerPhase2Tasks(project, extension)

        // Set up lifecycle hooks
        setupLifecycleHooks(project, extension, processManager, installer)

        logger.info("Gradle Managed AI Plugin configured successfully")
    }

    private fun registerPhase2Tasks(project: Project, extension: ManagedAiExtension) {
        // Register core Ollama lifecycle tasks
        val startOllamaTask = project.tasks.register("startOllama", StartOllamaTask::class.java) { task ->
            task.host.set(extension.ollama.host)
            task.port.set(extension.ollama.port)
            task.autoInstall.set(extension.autoInstall)
            task.additionalArgs.set(extension.ollama.additionalArgs)
            task.allowPortChange.set(extension.ollama.allowPortChange)
            task.installationStrategy.set(extension.ollama.installationStrategy)
            extension.ollama.installPath?.let { task.installPath.set(it) }
        }

        val stopOllamaTask = project.tasks.register("stopOllama", StopOllamaTask::class.java) { task ->
            task.port.set(extension.ollama.port)
            task.gracefulShutdown.set(extension.ollama.gracefulShutdown)
            task.timeoutSeconds.set(extension.ollama.shutdownTimeout)
        }

        project.tasks.register("ollamaStatus", OllamaStatusTask::class.java) { task ->
            task.host.set(extension.ollama.host)
            task.port.set(extension.ollama.port)
            task.verbose.set(false)
        }

        // Register model management tasks
        val modelTasks = extension.models.map { model ->
            // Sanitize model name for task name - replace invalid characters with underscores
            val sanitizedTaskName = model.getName()
                .replace(Regex("[/\\\\:<>\"?*|]"), "_")
                .replaceFirstChar { it.uppercase() }

            project.tasks.register("pullModel${sanitizedTaskName}", PullModelTask::class.java) { task ->
                task.modelName.set(model.getName())
                task.modelVersion.set(model.version)
                task.host.set(extension.ollama.host)
                task.port.set(extension.ollama.port)
                task.timeoutMinutes.set(30)
                task.dependsOn(startOllamaTask)
            }
        }

        // Separate preload tasks from regular model tasks
        val preloadTasks = extension.models.filter { it.preload }.map { model ->
            val sanitizedTaskName = model.getName()
                .replace(Regex("[/\\\\:<>\"?*|]"), "_")
                .replaceFirstChar { it.uppercase() }

            project.tasks.named("pullModel${sanitizedTaskName}")
        }

        // Register high-level lifecycle tasks
        project.tasks.register("setupManagedAi", SetupManagedAiTask::class.java) { task ->
            task.host.set(extension.ollama.host)
            task.port.set(extension.ollama.port)
            task.autoInstall.set(extension.autoInstall)
            task.dependsOn(startOllamaTask)
            task.dependsOn(modelTasks)
        }

        // Register preload task that runs during configuration
        project.tasks.register("preloadModels") { task ->
            task.group = "ai"
            task.description = "Preload models marked with preload = true during configuration phase"
            task.dependsOn(startOllamaTask)
            task.dependsOn(preloadTasks)
        }

        project.tasks.register("teardownManagedAi", TeardownManagedAiTask::class.java) { task ->
            task.port.set(extension.ollama.port)
            task.gracefulShutdown.set(extension.ollama.gracefulShutdown)
            task.timeoutSeconds.set(extension.ollama.shutdownTimeout)
            task.dependsOn(stopOllamaTask)
        }

        // Register the legacy status task for backward compatibility
        project.tasks.register("managedAiStatus", OllamaStatusTask::class.java) { task ->
            task.host.set(extension.ollama.host)
            task.port.set(extension.ollama.port)
            task.verbose.set(true)
        }

        logger.info("Phase 2 tasks registered successfully")
    }

    private fun setupLifecycleHooks(
        project: Project,
        extension: ManagedAiExtension,
        processManager: ProcessManager,
        installer: OllamaInstaller
    ) {
        // Auto-install if needed and enabled
        if (extension.autoInstall && installer.findOllamaExecutable() == null) {
            logger.info("Ollama not found, auto-install is enabled")
            project.gradle.projectsEvaluated {
                logger.info("Attempting to install Ollama...")
                val result = installer.findOrInstallOllama(
                    strategy = extension.ollama.installationStrategy,
                    isolatedPath = extension.ollama.installPath
                )
                if (result.success) {
                    logger.info("Ollama installed successfully: ${result.message}")
                } else {
                    logger.warn("Failed to install Ollama automatically: ${result.message}")
                }
            }
        }

        // Set up cleanup on build completion
        project.gradle.projectsEvaluated {
            if (extension.autoStart) {
                // Register cleanup task to run at the end
                project.tasks.register("cleanupOllama") { task ->
                    task.doLast {
                        logger.info("Build finished, cleaning up Ollama process...")
                        processManager.stopOllamaGracefully(30)
                    }
                }
            }
        }
    }
}
