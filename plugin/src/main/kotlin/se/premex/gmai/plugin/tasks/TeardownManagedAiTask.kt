package se.premex.gmai.plugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.CacheableTask

@CacheableTask
abstract class TeardownManagedAiTask : DefaultTask() {

    @get:Input
    abstract val gracefulShutdown: Property<Boolean>

    @get:Input
    abstract val timeoutSeconds: Property<Int>

    @get:Input
    abstract val port: Property<Int>

    init {
        group = "ai"
        description = "Teardown the managed AI environment (stop Ollama and cleanup)"

        // Set default values
        gracefulShutdown.convention(true)
        timeoutSeconds.convention(30)
        port.convention(11434)
    }

    @TaskAction
    fun teardownManagedAi() {
        logger.lifecycle("Tearing down managed AI environment...")

        // This task will depend on stopOllama task
        // The actual work is done by the dependent tasks
        logger.lifecycle("Managed AI environment teardown complete")
    }
}
