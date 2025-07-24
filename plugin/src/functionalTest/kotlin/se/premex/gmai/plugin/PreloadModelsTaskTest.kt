package se.premex.gmai.plugin

import java.io.File
import kotlin.test.assertTrue
import kotlin.test.Test
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir

/**
 * Functional tests for the preloadModels task
 */
class PreloadModelsTaskTest {

    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle") }

    @Test fun `preloadModels task is registered and depends on correct model tasks`() {
        // Set up the test build
        settingsFile.writeText("")
        buildFile.writeText("""
            plugins {
                id('se.premex.gmai')
            }
            
            managedAi {
                autoInstall = false
                autoStart = false
                
                models {
                    llama3 {
                        version = "latest"
                        preload = true
                    }
                    codellama {
                        version = "7b"
                        preload = false
                    }
                    mistral {
                        version = "7b"
                        preload = true
                    }
                }
            }
        """.trimIndent())

        // Run the build with dry run to see task dependencies
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("preloadModels", "--dry-run")
        runner.withProjectDir(projectDir)
        val result = runner.build()

        // Verify the preloadModels task exists and runs
        assertTrue(result.output.contains(":preloadModels"), "preloadModels task should be in the execution plan")

        // Verify it depends on startOllama
        assertTrue(result.output.contains(":startOllama"), "preloadModels should depend on startOllama")

        // Verify it depends on preload model tasks
        assertTrue(result.output.contains(":pullModelLlama3"), "preloadModels should depend on pullModelLlama3 (preload=true)")
        assertTrue(result.output.contains(":pullModelMistral"), "preloadModels should depend on pullModelMistral (preload=true)")

        // Verify it does NOT depend on non-preload models
        assertTrue(!result.output.contains(":pullModelCodellama"), "preloadModels should NOT depend on pullModelCodellama (preload=false)")
    }

    @Test fun `preloadModels task works with no preload models`() {
        // Set up the test build
        settingsFile.writeText("")
        buildFile.writeText("""
            plugins {
                id('se.premex.gmai')
            }
            
            managedAi {
                autoInstall = false
                autoStart = false
                
                models {
                    llama3 {
                        version = "8b"
                        preload = false
                    }
                    codellama {
                        version = "7b"
                        // preload defaults to false
                    }
                }
            }
        """.trimIndent())

        // Run the build with dry run to see task dependencies
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("preloadModels", "--dry-run")
        runner.withProjectDir(projectDir)
        val result = runner.build()

        // Verify the preloadModels task exists
        assertTrue(result.output.contains(":preloadModels"), "preloadModels task should be in the execution plan")

        // Verify it depends on startOllama
        assertTrue(result.output.contains(":startOllama"), "preloadModels should depend on startOllama")

        // Verify it doesn't depend on any model tasks since none have preload=true
        assertTrue(!result.output.contains(":pullModelLlama3"), "preloadModels should NOT depend on pullModelLlama3 (preload=false)")
        assertTrue(!result.output.contains(":pullModelCodellama"), "preloadModels should NOT depend on pullModelCodellama (preload=false)")
    }

    @Test fun `preloadModels task sanitizes model names correctly`() {
        // Set up the test build
        settingsFile.writeText("")
        buildFile.writeText("""
            plugins {
                id('se.premex.gmai')
            }
            
            managedAi {
                autoInstall = false
                autoStart = false
                
                models {
                    "llama3:8b" {
                        preload = true
                    }
                    "my/custom-model" {
                        preload = true
                    }
                }
            }
        """.trimIndent())

        // Run the build with dry run to see task dependencies
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("preloadModels", "--dry-run")
        runner.withProjectDir(projectDir)
        val result = runner.build()

        // Verify the preloadModels task exists
        assertTrue(result.output.contains(":preloadModels"), "preloadModels task should be in the execution plan")

        // Verify it depends on sanitized task names
        assertTrue(result.output.contains(":pullModelLlama3_8b"), "preloadModels should depend on sanitized task name pullModelLlama3_8b")
        assertTrue(result.output.contains(":pullModelMy_custom-model"), "preloadModels should depend on sanitized task name pullModelMy_custom-model")
    }

    @Test fun `preloadModels task has correct group and description`() {
        // Set up the test build
        settingsFile.writeText("")
        buildFile.writeText("""
            plugins {
                id('se.premex.gmai')
            }
            
            managedAi {
                autoInstall = false
                autoStart = false
            }
        """.trimIndent())

        // Run the build to list tasks
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("tasks", "--group=ai")
        runner.withProjectDir(projectDir)
        val result = runner.build()

        // Verify the preloadModels task is in the ai group
        assertTrue(result.output.contains("preloadModels"), "preloadModels task should be listed in ai group")
        assertTrue(result.output.contains("Preload models marked with preload = true"),
                  "preloadModels should have correct description")
    }
}
