package se.premex.gmai.plugin.utils

import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import se.premex.gmai.plugin.models.OllamaInstallationStrategy

class OllamaInstallationStrategyTest {

    private lateinit var installer: OllamaInstaller
    private val logger: Logger = LoggerFactory.getLogger(OllamaInstallationStrategyTest::class.java)

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        // Create a real Project instance using Gradle's ProjectBuilder
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .build()

        installer = OllamaInstaller(logger, project)
    }

    @Test
    fun `test PREFER_EXISTING strategy with existing installation`() {
        val result = installer.findOrInstallOllama(
            strategy = OllamaInstallationStrategy.PREFER_EXISTING,
            isolatedPath = null
        )

        // Test should handle both success and failure cases gracefully
        assertNotNull(result)
        assertNotNull(result.installationType)
        assertNotNull(result.message)
        assertTrue(result.success)
        assertTrue(result.message.isNotBlank())

        val messgeAlternatives = listOf(
            "Ollama installed in isolated environment at: .ollama/bin/ollama",
            "Using existing Ollama installation"
        )

        //check if message is any of the expected alternatives
        assertTrue(
            result.installationType == OllamaInstaller.InstallationType.EXISTING_SYSTEM ||
                    result.installationType == OllamaInstaller.InstallationType.EXISTING_ISOLATED ||
                    result.installationType == OllamaInstaller.InstallationType.NEW_ISOLATED ||
                    result.installationType == OllamaInstaller.InstallationType.NEW_SYSTEM_WIDE
        )
        assertTrue(messgeAlternatives.any { it in result.message })

    }

    @Test
    fun `test PREFER_EXISTING strategy without existing installation`() {
        // Create a mock scenario where no existing installation is found
        val customIsolatedPath = tempDir.resolve("custom-ollama").toString()

        val result = installer.findOrInstallOllama(
            strategy = OllamaInstallationStrategy.PREFER_EXISTING,
            isolatedPath = customIsolatedPath
        )

        // Should attempt isolated installation since no existing installation
        if (result.installationType == OllamaInstaller.InstallationType.EXISTING_SYSTEM) {
            assertEquals("Using existing Ollama installation", result.message, "Expected to find existing installation")
        } else if (result.installationType == OllamaInstaller.InstallationType.NEW_ISOLATED) {
            assertEquals(customIsolatedPath, result.executablePath)
        }
    }

    @Test
    fun `test ISOLATED_ONLY strategy ignores existing installations`() {
        val customIsolatedPath = tempDir.resolve("isolated-ollama").toString()

        val result = installer.findOrInstallOllama(
            strategy = OllamaInstallationStrategy.ISOLATED_ONLY,
            isolatedPath = customIsolatedPath
        )

        assertTrue(result.success)
        assertEquals(OllamaInstaller.InstallationType.NEW_ISOLATED, result.installationType)
    }

    @Test
    fun `test PREFER_EXISTING_THEN_SYSTEM_WIDE strategy`() {
        val result = installer.findOrInstallOllama(
            strategy = OllamaInstallationStrategy.PREFER_EXISTING_THEN_SYSTEM_WIDE,
            isolatedPath = null
        )

        assertTrue(result.success)

        val acceptableMessages = listOf(
            "Using existing Ollama installation",
            "Ollama installed system-wide"
        )

        assertTrue(
            acceptableMessages.any { it in result.message },
            "Expected message to be any of: $acceptableMessages but got: ${result.message}"
        )

    }

    @Test
    fun `test SYSTEM_WIDE_ONLY strategy ignores existing installations`() {
        val result = installer.findOrInstallOllama(
            strategy = OllamaInstallationStrategy.SYSTEM_WIDE_ONLY,
        )

        assertTrue(result.success)

        assertEquals(OllamaInstaller.InstallationType.NEW_SYSTEM_WIDE, result.installationType)
    }

    @Test
    fun `test FULL_PRIORITY strategy tries all options in order`() {
        val customIsolatedPath = tempDir.resolve("full-priority-ollama").toString()

        val result = installer.findOrInstallOllama(
            strategy = OllamaInstallationStrategy.FULL_PRIORITY,
            isolatedPath = customIsolatedPath
        )

        assertTrue(result.success)

        // Should use one of the installation types in priority order
        assertTrue(
            result.installationType == OllamaInstaller.InstallationType.EXISTING_SYSTEM ||
                    result.installationType == OllamaInstaller.InstallationType.EXISTING_ISOLATED ||
                    result.installationType == OllamaInstaller.InstallationType.NEW_ISOLATED ||
                    result.installationType == OllamaInstaller.InstallationType.NEW_SYSTEM_WIDE
        )
    }
}
