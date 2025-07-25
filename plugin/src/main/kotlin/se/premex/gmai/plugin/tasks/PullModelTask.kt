package se.premex.gmai.plugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import kotlinx.coroutines.runBlocking
import se.premex.gmai.plugin.models.OllamaInstance
import se.premex.gmai.plugin.services.OllamaService
import se.premex.gmai.plugin.utils.ErrorHandler
import java.time.Duration
import java.io.File

@CacheableTask
abstract class PullModelTask : DefaultTask() {

    @get:Input
    abstract val modelName: Property<String>

    @get:Input
    @get:Optional
    abstract val modelVersion: Property<String>

    @get:Input
    abstract val host: Property<String>

    @get:Input
    abstract val port: Property<Int>

    @get:Input
    abstract val timeoutMinutes: Property<Int>

    @get:OutputFile
    abstract val outputFile: Property<File>

    init {
        group = "ai"
        description = "Pull a model from Ollama registry"

        // Set default values
        host.convention("localhost")
        port.convention(11434)
        timeoutMinutes.convention(30)
        modelVersion.convention("latest")

        // Configure output file for caching - must be done in configuration phase
        outputFile.convention(
            project.layout.buildDirectory.file(
                modelName.zip(modelVersion) { name, version ->
                    "ollama/models/${name}-${version}.marker"
                }
            ).map { it.asFile }
        )
    }

    @TaskAction
    fun pullModel() {
        val errorHandler = ErrorHandler(logger)

        // Use actual port if available from StartOllamaTask
        val actualPort = project.extensions.extraProperties.get("ollama.actualPort") as? Int ?: port.get()

        val ollamaInstance = OllamaInstance(
            host = host.get(),
            port = actualPort,
            protocol = "http",
            timeout = Duration.ofMinutes(timeoutMinutes.get().toLong()),
            isIsolated = false,
            isolatedPath = null
        )
        val service = OllamaService(ollamaInstance, logger)

        val fullModelName = "${modelName.get()}:${modelVersion.get()}"

        logger.lifecycle("Checking for model: $fullModelName")

        runBlocking {
            try {
                // Check if model already exists
                val existingModels = service.listModels()
                if (existingModels.any { it.name == fullModelName }) {
                    logger.lifecycle("Model $fullModelName already exists, skipping pull")
                    createOutputMarker()
                    return@runBlocking
                }

                // Pull the model with progress tracking
                logger.lifecycle("Downloading model $fullModelName (this may take several minutes)...")
                logger.lifecycle("Progress: Starting download...")

                val success = service.pullModelWithProgress(fullModelName) { progress ->
                    when (progress.status) {
                        "pulling" -> {
                            if (progress.total != null && progress.completed != null) {
                                val percentage = (progress.completed.toDouble() / progress.total.toDouble() * 100).toInt()
                                logger.lifecycle("Progress: $percentage% (${progress.completed}/${progress.total} bytes)")
                            } else {
                                logger.lifecycle("Progress: Downloading...")
                            }
                        }
                        "downloading" -> logger.lifecycle("Progress: Downloading model data...")
                        "verifying sha256" -> logger.lifecycle("Progress: Verifying download...")
                        "writing manifest" -> logger.lifecycle("Progress: Writing manifest...")
                        "removing any unused layers" -> logger.lifecycle("Progress: Cleaning up...")
                        "success" -> logger.lifecycle("Progress: Download completed successfully!")
                        else -> logger.lifecycle("Progress: ${progress.status}")
                    }
                }

                if (success) {
                    logger.lifecycle("Successfully pulled model: $fullModelName")
                    createOutputMarker()
                } else {
                    throw GradleException("Failed to pull model: $fullModelName")
                }

            } catch (e: Exception) {
                errorHandler.handleModelError(e, fullModelName)
            }
        }
    }

    private fun createOutputMarker() {
        val markerFile = outputFile.get()
        markerFile.parentFile.mkdirs()
        markerFile.writeText("Model ${modelName.get()}:${modelVersion.get()} pulled at ${System.currentTimeMillis()}")
    }
}
