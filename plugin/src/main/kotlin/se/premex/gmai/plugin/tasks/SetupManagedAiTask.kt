package se.premex.gmai.plugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.CacheableTask

@CacheableTask
abstract class SetupManagedAiTask : DefaultTask() {

    @get:Input
    abstract val host: Property<String>

    @get:Input
    abstract val port: Property<Int>

    @get:Input
    abstract val autoInstall: Property<Boolean>

    init {
        group = "ai"
        description = "Setup the managed AI environment (start Ollama and pull required models)"

        // Set default values
        host.convention("localhost")
        port.convention(11434)
        autoInstall.convention(true)
    }

    @TaskAction
    fun setupManagedAi() {
        logger.lifecycle("Setting up managed AI environment...")

        // This task will depend on startOllama and pullModel tasks
        // The actual work is done by the dependent tasks
        logger.lifecycle("Managed AI environment setup complete")
    }
}
