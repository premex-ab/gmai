package se.premex.gmai.plugin

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Tests for Configuration Cache compatibility of the Gradle Managed AI Plugin
 */
class ConfigurationCacheTest {

    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }

    @Test
    fun `plugin should support configuration cache for basic tasks`() {
        setupTestProject()

        // First run to populate the configuration cache
        val firstResult = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("ollamaStatus", "--configuration-cache")
            .build()

        assertTrue(firstResult.output.contains("Configuration cache entry stored"))

        // Second run should reuse the configuration cache
        val secondResult = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("ollamaStatus", "--configuration-cache")
            .build()

        assertTrue(secondResult.output.contains("Configuration cache entry reused"))
        assertEquals(TaskOutcome.SUCCESS, secondResult.task(":ollamaStatus")?.outcome)
    }

    @Test
    fun `plugin should support configuration cache for model tasks`() {
        setupTestProjectWithModels()

        // First run to populate the configuration cache
        val firstResult = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("tasks", "--configuration-cache")
            .build()

        assertTrue(firstResult.output.contains("Configuration cache entry stored"))

        // Second run should reuse the configuration cache
        val secondResult = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("tasks", "--configuration-cache")
            .build()

        assertTrue(secondResult.output.contains("Configuration cache entry reused"))
        // Verify model tasks are available
        assertTrue(secondResult.output.contains("pullModelTinyllama"))
    }

    @Test
    fun `plugin should support configuration cache with custom configuration`() {
        setupTestProjectWithCustomConfig()

        // Test with configuration cache
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("ollamaStatus", "--configuration-cache")
            .build()

        assertTrue(result.output.contains("Configuration cache entry stored"))
        assertEquals(TaskOutcome.SUCCESS, result.task(":ollamaStatus")?.outcome)
    }

    @Test
    fun `plugin should not break configuration cache when auto-install is disabled`() {
        setupTestProject()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("help", "--configuration-cache", "--configuration-cache-problems=warn")
            .build()

        // Should not have configuration cache problems
        assertFalse(result.output.contains("Configuration cache problems found"))
        assertTrue(result.output.contains("Configuration cache entry stored"))
    }

    @Test
    fun `plugin should handle configuration cache with lifecycle hooks`() {
        setupTestProjectWithLifecycleHooks()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("setupManagedAi", "--dry-run", "--configuration-cache")
            .build()

        assertTrue(result.output.contains("Configuration cache entry stored"))
        // The task should exist in the execution plan (either SKIPPED or in dry-run mode)
        assertTrue(result.output.contains(":setupManagedAi"), "setupManagedAi task should be in execution plan")
    }

    private fun setupTestProject() {
        settingsFile.writeText("""
            rootProject.name = "test-configuration-cache"
        """.trimIndent())

        buildFile.writeText("""
            plugins {
                id("se.premex.gmai")
            }

            managedAi {
                autoInstall = false
                autoStart = false
                
                ollama {
                    host = "localhost"
                    port = 11434
                }
            }
        """.trimIndent())
    }

    private fun setupTestProjectWithModels() {
        settingsFile.writeText("""
            rootProject.name = "test-configuration-cache-models"
        """.trimIndent())

        buildFile.writeText("""
            plugins {
                id("se.premex.gmai")
            }

            managedAi {
                autoInstall = false
                autoStart = false
                
                models {
                    create("tinyllama") {
                        version = "latest"
                        preload = false
                    }
                }
                
                ollama {
                    host = "localhost"
                    port = 11434
                }
            }
        """.trimIndent())
    }

    private fun setupTestProjectWithCustomConfig() {
        settingsFile.writeText("""
            rootProject.name = "test-configuration-cache-custom"
        """.trimIndent())

        buildFile.writeText("""
            plugins {
                id("se.premex.gmai")
            }

            managedAi {
                autoInstall = false
                autoStart = false
                
                ollama {
                    host = "127.0.0.1"
                    port = 9999
                    gracefulShutdown = true
                    shutdownTimeout = 10
                    allowPortChange = false
                }
            }
        """.trimIndent())
    }

    private fun setupTestProjectWithLifecycleHooks() {
        settingsFile.writeText("""
            rootProject.name = "test-configuration-cache-lifecycle"
        """.trimIndent())

        buildFile.writeText("""
            plugins {
                id("se.premex.gmai")
            }

            managedAi {
                autoInstall = false
                autoStart = false
                
                models {
                    create("llama3") {
                        version = "8b"
                        preload = true
                    }
                }
                
                ollama {
                    host = "localhost"
                    port = 11434
                }
            }
        """.trimIndent())
    }
}
