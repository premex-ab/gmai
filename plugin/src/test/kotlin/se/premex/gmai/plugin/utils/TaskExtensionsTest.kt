package se.premex.gmai.plugin.utils

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals

/**
 * Unit tests for TaskExtensions
 */
class TaskExtensionsTest {

    @Test
    fun `useManagedAi adds setup dependency and teardown finalizer`() {
        // Create a test project
        val project = ProjectBuilder.builder().build()

        // Manually register the required tasks for testing
        val setupTask = project.tasks.register("setupManagedAi") {
            it.doLast { println("Setup task") }
        }
        val teardownTask = project.tasks.register("teardownManagedAi") {
            it.doLast { println("Teardown task") }
        }

        // Create a test task
        val testTask = project.tasks.register("testTask") { task ->
            task.doLast {
                // Test task action
            }
        }

        // Apply the useManagedAi extension
        testTask.get().useManagedAi()

        // Verify the task has dependencies (providers will resolve to task names)
        assertTrue(testTask.get().dependsOn.isNotEmpty())
        assertTrue(testTask.get().finalizedBy.getDependencies(testTask.get()).isNotEmpty())

        // Verify the dependencies resolve correctly when evaluated
        val resolvedDependencies = testTask.get().taskDependencies.getDependencies(testTask.get())
        assertTrue(resolvedDependencies.contains(setupTask.get()))

        val resolvedFinalizers = testTask.get().finalizedBy.getDependencies(testTask.get())
        assertTrue(resolvedFinalizers.contains(teardownTask.get()))
    }

    @Test
    fun `useManagedAi with configuration and autoTeardown true adds teardown finalizer`() {
        // Create a test project
        val project = ProjectBuilder.builder().build()

        // Manually register the required tasks for testing
        val setupTask = project.tasks.register("setupManagedAi") {
            it.doLast { println("Setup task") }
        }
        val teardownTask = project.tasks.register("teardownManagedAi") {
            it.doLast { println("Teardown task") }
        }

        // Create a test task
        val testTask = project.tasks.register("testTask") { task ->
            task.doLast {
                // Test task action
            }
        }

        // Apply the useManagedAi extension with configuration
        testTask.get().useManagedAi {
            autoTeardown = true
            requiredModels = listOf("llama2", "codellama")
        }

        // Verify the task has dependencies
        assertTrue(testTask.get().dependsOn.isNotEmpty())
        assertTrue(testTask.get().finalizedBy.getDependencies(testTask.get()).isNotEmpty())

        // Verify the dependencies resolve correctly when evaluated
        val resolvedDependencies = testTask.get().taskDependencies.getDependencies(testTask.get())
        assertTrue(resolvedDependencies.contains(setupTask.get()))

        val resolvedFinalizers = testTask.get().finalizedBy.getDependencies(testTask.get())
        assertTrue(resolvedFinalizers.contains(teardownTask.get()))
    }

    @Test
    fun `useManagedAi with configuration and autoTeardown false does not add teardown finalizer`() {
        // Create a test project
        val project = ProjectBuilder.builder().build()

        // Manually register the required tasks for testing
        val setupTask = project.tasks.register("setupManagedAi") {
            it.doLast { println("Setup task") }
        }
        val teardownTask = project.tasks.register("teardownManagedAi") {
            it.doLast { println("Teardown task") }
        }

        // Create a test task
        val testTask = project.tasks.register("testTask") { task ->
            task.doLast {
                // Test task action
            }
        }

        // Apply the useManagedAi extension with configuration
        testTask.get().useManagedAi {
            autoTeardown = false
            requiredModels = listOf("llama2")
        }

        // Verify the task has setup dependency but not teardown finalizer
        assertTrue(testTask.get().dependsOn.isNotEmpty())

        // Verify the dependencies resolve correctly when evaluated
        val resolvedDependencies = testTask.get().taskDependencies.getDependencies(testTask.get())
        assertTrue(resolvedDependencies.contains(setupTask.get()))

        // Verify no teardown finalizer was added
        val resolvedFinalizers = testTask.get().finalizedBy.getDependencies(testTask.get())
        assertFalse(resolvedFinalizers.contains(teardownTask.get()))
    }

    @Test
    fun `ManagedAiTaskConfiguration has correct defaults`() {
        val config = ManagedAiTaskConfiguration()

        assertTrue(config.autoTeardown)
        assertEquals(emptyList<String>(), config.requiredModels)
    }

    @Test
    fun `ManagedAiTaskConfiguration can be configured`() {
        val config = ManagedAiTaskConfiguration()
        config.autoTeardown = false
        config.requiredModels = listOf("llama2", "codellama")

        assertFalse(config.autoTeardown)
        assertEquals(listOf("llama2", "codellama"), config.requiredModels)
    }
}
