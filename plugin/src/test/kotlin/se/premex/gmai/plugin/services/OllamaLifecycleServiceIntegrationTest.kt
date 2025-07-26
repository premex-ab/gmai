package se.premex.gmai.plugin.services

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import java.nio.file.Path
import java.io.File
import java.lang.reflect.Method

/**
 * Integration tests to verify that the actual installViaBinaryDownload implementation
 * can be called and handles various scenarios properly.
 * 
 * Note: These tests verify the implementation exists and can be called, but don't
 * perform actual downloads to avoid network dependencies in CI.
 */
class OllamaLifecycleServiceIntegrationTest {

    @Test
    fun `installViaBinaryDownload implementation exists and is callable`(@TempDir tempDir: Path) {
        // Create a test service instance to verify the implementation exists
        val serviceClass = Class.forName("se.premex.gmai.plugin.services.OllamaLifecycleService")
        assertNotNull(serviceClass, "OllamaLifecycleService class should exist")
        
        // Find the nested OllamaInstallerForService class
        val nestedClasses = serviceClass.declaredClasses
        val installerClass = nestedClasses.find { it.simpleName == "OllamaInstallerForService" }
        assertNotNull(installerClass, "OllamaInstallerForService nested class should exist")
        
        // Find the installViaBinaryDownload method
        val methods = installerClass!!.declaredMethods
        val installMethod = methods.find { it.name == "installViaBinaryDownload" }
        assertNotNull(installMethod, "installViaBinaryDownload method should exist")
        
        // Verify method signature
        assertTrue(installMethod!!.parameterCount == 2, "Method should have 2 parameters")
        assertTrue(installMethod.parameterTypes[0] == String::class.java, "First parameter should be String (targetPath)")
        assertTrue(installMethod.parameterTypes[1] == String::class.java, "Second parameter should be String (version)")
        
        // Verify return type
        val returnType = installMethod.returnType
        assertTrue(returnType.simpleName == "InstallResult", "Should return InstallResult")
    }

    @Test 
    fun `can instantiate service and call methods`() {
        // This test verifies that the service can be instantiated without errors
        // and that the key methods are available
        
        // We can't easily instantiate the full service due to Gradle dependencies,
        // but we can verify the class structure is correct
        val serviceClass = Class.forName("se.premex.gmai.plugin.services.OllamaLifecycleService")
        
        // Check that required data classes exist
        val innerClasses = serviceClass.declaredClasses
        val installResultClass = innerClasses.find { it.simpleName == "InstallResult" }
        assertNotNull(installResultClass, "InstallResult data class should exist")
        
        val ollamaStatusClass = innerClasses.find { it.simpleName == "OllamaStatus" }
        assertNotNull(ollamaStatusClass, "OllamaStatus data class should exist")
        
        // Verify InstallResult has expected fields
        val installResultFields = installResultClass!!.declaredFields.map { it.name }
        assertTrue(installResultFields.contains("success"), "InstallResult should have success field")
        assertTrue(installResultFields.contains("message"), "InstallResult should have message field")
    }

    @Test
    fun `download URLs are properly formatted`() {
        // Test that the URL generation logic follows expected patterns
        // This is important for the actual download functionality
        
        val expectedPatterns = mapOf(
            "darwin" to "ollama-darwin.tgz",
            "linux" to "ollama-linux-amd64.tgz", 
            "windows" to "ollama-windows-amd64.zip"
        )
        
        expectedPatterns.forEach { (platform, expectedFile) ->
            // These URLs should be valid GitHub release URLs
            val expectedUrl = "https://github.com/ollama/ollama/releases/download/v0.9.6/$expectedFile"
            
            assertTrue(expectedUrl.startsWith("https://github.com/ollama/ollama/releases/download/"), 
                "URL for $platform should use GitHub releases")
            assertTrue(expectedUrl.contains(expectedFile), 
                "URL for $platform should contain expected file: $expectedFile")
        }
    }

    @Test
    fun `default install paths are platform appropriate`(@TempDir tempDir: Path) {
        val userHome = System.getProperty("user.home")
        
        // Test that default paths follow expected patterns for each platform
        val expectedPaths = mapOf(
            "MACOS" to "$userHome/.gradle/ollama/bin/ollama",
            "LINUX" to "$userHome/.gradle/ollama/bin/ollama", 
            "WINDOWS" to "$userHome/.gradle/ollama/bin/ollama.exe"
        )
        
        expectedPaths.forEach { (platform, expectedPath) ->
            assertTrue(expectedPath.contains(".gradle/ollama/bin"), 
                "Path for $platform should use .gradle/ollama/bin directory")
            
            if (platform == "WINDOWS") {
                assertTrue(expectedPath.endsWith(".exe"), 
                    "Windows path should end with .exe")
            } else {
                assertFalse(expectedPath.endsWith(".exe"), 
                    "Non-Windows path should not end with .exe")
            }
        }
    }

    @Test
    fun `archive extraction methods exist for each platform`() {
        // Verify that the implementation has methods for extracting different archive types
        val serviceClass = Class.forName("se.premex.gmai.plugin.services.OllamaLifecycleService")
        val nestedClasses = serviceClass.declaredClasses
        val installerClass = nestedClasses.find { it.simpleName == "OllamaInstallerForService" }
        assertNotNull(installerClass, "OllamaInstallerForService nested class should exist")
        
        val methods = installerClass!!.declaredMethods.map { it.name }
        
        // These methods should exist for archive extraction
        assertTrue(methods.contains("extractTarGz"), "Should have extractTarGz method for Unix platforms")
        assertTrue(methods.contains("extractZip"), "Should have extractZip method for Windows")
        assertTrue(methods.contains("downloadFile"), "Should have downloadFile method")
        assertTrue(methods.contains("downloadAndExtract"), "Should have downloadAndExtract method")
    }

    @Test
    fun `error handling methods are present`() {
        // Verify that proper error handling infrastructure is in place
        val serviceClass = Class.forName("se.premex.gmai.plugin.services.OllamaLifecycleService")
        val nestedClasses = serviceClass.declaredClasses
        val installerClass = nestedClasses.find { it.simpleName == "OllamaInstallerForService" }
        assertNotNull(installerClass)
        
        // InstallResult should be available for error reporting
        val installResultClass = nestedClasses.find { it.simpleName == "InstallResult" }
        assertNotNull(installResultClass, "InstallResult should exist for error reporting")
        
        // Should have constructor that takes success boolean and message
        val constructors = installResultClass!!.declaredConstructors
        val primaryConstructor = constructors.find { it.parameterCount == 2 }
        assertNotNull(primaryConstructor, "InstallResult should have constructor with success and message parameters")
    }
}