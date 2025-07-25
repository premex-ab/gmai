package se.premex.gmai.plugin.utils

import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

/**
 * Extension functions to make it easy for users to add AI dependencies to their tasks
 */

/**
 * Convenience method to make a task depend on managed AI being available
 */
fun Task.useManagedAi() {
    // Use task references with proper error handling
    dependsOn(project.provider {
        try {
            project.tasks.named("setupManagedAi")
        } catch (e: Exception) {
            logger.error("setupManagedAi task not found for task ${name} - make sure the Gradle Managed AI plugin is applied", e)
            null
        }
    })

    finalizedBy(project.provider {
        try {
            project.tasks.named("teardownManagedAi")
        } catch (e: Exception) {
            logger.error("teardownManagedAi task not found for task ${name} - make sure the Gradle Managed AI plugin is applied", e)
            null
        }
    })
}

/**
 * Convenience method to make a task depend on managed AI being available with custom configuration
 */
fun Task.useManagedAi(configure: ManagedAiTaskConfiguration.() -> Unit) {
    val config = ManagedAiTaskConfiguration()
    config.configure()

    // Use task references with proper error handling
    dependsOn(project.provider {
        try {
            project.tasks.named("setupManagedAi")
        } catch (e: Exception) {
            logger.warn("setupManagedAi task not found for task ${name} - make sure the Gradle Managed AI plugin is applied")
            null
        }
    })

    if (config.autoTeardown) {
        finalizedBy(project.provider {
            try {
                project.tasks.named("teardownManagedAi")
            } catch (e: Exception) {
                logger.warn("teardownManagedAi task not found for task ${name} - make sure the Gradle Managed AI plugin is applied")
                null
            }
        })
    }
}

/**
 * Configuration class for managed AI task dependencies
 */
class ManagedAiTaskConfiguration {
    var autoTeardown: Boolean = true
    var requiredModels: List<String> = emptyList()
}
