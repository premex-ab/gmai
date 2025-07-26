package se.premex.gmai.plugin.services

import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.slf4j.LoggerFactory
import se.premex.gmai.plugin.models.OllamaInstallationStrategy
import se.premex.gmai.plugin.utils.OllamaInstaller
import se.premex.gmai.plugin.utils.ProcessManager
import java.io.Serializable
import java.util.concurrent.TimeUnit

/**
 * Configuration cache compatible build service for managing Ollama lifecycle.
 * Build services are isolated from project state and can be safely cached.
 */
abstract class OllamaLifecycleService : BuildService<OllamaLifecycleService.Parameters>, AutoCloseable {

    interface Parameters : BuildServiceParameters {
        val autoInstall: Property<Boolean>
        val autoStart: Property<Boolean>
        val host: Property<String>
        val port: Property<Int>
        val installationStrategy: Property<OllamaInstallationStrategy>
        val installPath: Property<String>
        val version: Property<String>
    }

    private val logger = LoggerFactory.getLogger(OllamaLifecycleService::class.java)
    private val processManager = ProcessManager(logger)

    // Lazy initialization to avoid configuration cache issues
    private val installer by lazy {
        // Create a minimal installer without project dependency for configuration cache compatibility
        OllamaInstallerForService(logger)
    }

    /**
     * Ensures Ollama is installed if auto-install is enabled.
     * This method is configuration cache safe.
     */
    fun ensureOllamaInstalled(): Boolean {
        if (!parameters.autoInstall.get()) {
            return true
        }

        val existingExecutable = installer.findOllamaExecutable()
        if (existingExecutable != null) {
            logger.info("Ollama already installed at: $existingExecutable")
            return true
        }

        logger.info("Ollama not found, attempting auto-install...")
        val result = installer.findOrInstallOllama(
            strategy = parameters.installationStrategy.getOrElse(OllamaInstallationStrategy.PREFER_EXISTING),
            isolatedPath = parameters.installPath.orNull,
            version = parameters.version.get()
        )

        if (result.success) {
            logger.info("Ollama installed successfully: ${result.message}")
            return true
        } else {
            logger.warn("Failed to install Ollama automatically: ${result.message}")
            return false
        }
    }

    /**
     * Checks if Ollama is running on the configured port.
     */
    fun isOllamaRunning(): Boolean {
        return processManager.isOllamaRunning(parameters.port.get())
    }

    /**
     * Gets the current Ollama process status.
     */
    fun getOllamaStatus(): OllamaStatus {
        val isRunning = isOllamaRunning()
        val executable = installer.findOllamaExecutable()

        return OllamaStatus(
            isRunning = isRunning,
            port = parameters.port.get(),
            host = parameters.host.get(),
            executablePath = executable
        )
    }

    override fun close() {
        // Cleanup when build service is disposed
        if (parameters.autoStart.get() && isOllamaRunning()) {
            logger.info("Build service closing, stopping Ollama process...")
            processManager.stopOllamaGracefully(30)
        }
    }

    /**
     * Simplified Ollama installer that doesn't depend on Project for configuration cache compatibility.
     */
    private class OllamaInstallerForService(private val logger: org.slf4j.Logger) {

        fun findOllamaExecutable(): String? {
            // Check common installation paths
            val commonPaths = when (se.premex.gmai.plugin.utils.OSUtils.getOperatingSystem()) {
                se.premex.gmai.plugin.utils.OSUtils.OperatingSystem.MACOS -> listOf(
                    "/usr/local/bin/ollama",
                    "/opt/homebrew/bin/ollama",
                    System.getProperty("user.home") + "/.local/bin/ollama"
                )
                se.premex.gmai.plugin.utils.OSUtils.OperatingSystem.LINUX -> listOf(
                    "/usr/local/bin/ollama",
                    "/usr/bin/ollama",
                    System.getProperty("user.home") + "/.local/bin/ollama"
                )
                se.premex.gmai.plugin.utils.OSUtils.OperatingSystem.WINDOWS -> listOf(
                    System.getenv("LOCALAPPDATA") + "\\Programs\\Ollama\\ollama.exe",
                    System.getenv("PROGRAMFILES") + "\\Ollama\\ollama.exe",
                    System.getenv("PROGRAMFILES(X86)") + "\\Ollama\\ollama.exe"
                )
            }

            for (path in commonPaths) {
                val file = java.io.File(path)
                if (file.exists() && file.canExecute()) {
                    return path
                }
            }

            // Check PATH using cross-platform method
            return se.premex.gmai.plugin.utils.OSUtils.findExecutableInPath("ollama")?.takeIf { it.isNotBlank() }
                ?: run {
                    logger.debug("Could not find ollama in PATH")
                    null
                }
        }

        fun findOrInstallOllama(strategy: OllamaInstallationStrategy, isolatedPath: String?, version: String): InstallResult {
            val existing = findOllamaExecutable()
            if (existing != null) {
                return InstallResult(true, "Ollama already installed at: $existing")
            }

            return when (strategy) {
                OllamaInstallationStrategy.PREFER_EXISTING -> {
                    // Already checked for existing above, so try isolated download
                    installViaBinaryDownload(isolatedPath ?: System.getProperty("user.home") + "/.gradle/ollama", version)
                }
                OllamaInstallationStrategy.PREFER_EXISTING_THEN_SYSTEM_WIDE -> {
                    // Try system package manager first, then isolated
                    val systemResult = installViaPackageManager()
                    if (systemResult.success) systemResult else installViaBinaryDownload(isolatedPath, version)
                }
                OllamaInstallationStrategy.ISOLATED_ONLY -> installViaBinaryDownload(isolatedPath ?: System.getProperty("user.home") + "/.gradle/ollama", version)
                OllamaInstallationStrategy.SYSTEM_WIDE_ONLY -> installViaPackageManager()
                OllamaInstallationStrategy.FULL_PRIORITY -> {
                    // Try system package manager, then isolated download
                    val systemResult = installViaPackageManager()
                    if (systemResult.success) systemResult else installViaBinaryDownload(isolatedPath, version)
                }
            }
        }

        private fun installViaPackageManager(): InstallResult {
            return try {
                val os = System.getProperty("os.name").lowercase()
                when {
                    os.contains("mac") -> {
                        val process = ProcessBuilder("brew", "install", "ollama").start()
                        val finished = process.waitFor(300, TimeUnit.SECONDS)
                        if (finished && process.exitValue() == 0) {
                            InstallResult(true, "Installed via Homebrew")
                        } else {
                            if (!finished) process.destroyForcibly()
                            InstallResult(false, "Homebrew installation failed")
                        }
                    }
                    os.contains("linux") -> {
                        // Try curl install script
                        val process = ProcessBuilder("bash", "-c", "curl -fsSL https://ollama.com/install.sh | sh").start()
                        val finished = process.waitFor(300, TimeUnit.SECONDS)
                        if (finished && process.exitValue() == 0) {
                            InstallResult(true, "Installed via curl script")
                        } else {
                            if (!finished) process.destroyForcibly()
                            InstallResult(false, "Curl installation failed")
                        }
                    }
                    else -> InstallResult(false, "Unsupported operating system for package manager installation")
                }
            } catch (e: Exception) {
                InstallResult(false, "Package manager installation failed: ${e.message}")
            }
        }

        private fun installViaBinaryDownload(targetPath: String?, version: String): InstallResult {
            return try {
                val os = se.premex.gmai.plugin.utils.OSUtils.getOperatingSystem()
                val installPath = targetPath ?: getDefaultInstallPath(os)
                
                logger.info("Starting binary download installation to: $installPath")
                
                // Create target directory
                val targetFile = java.io.File(installPath)
                targetFile.parentFile?.mkdirs()
                
                // Get platform-specific download URL
                val downloadUrl = getDownloadUrl(os, version)
                logger.info("Downloading Ollama from: $downloadUrl")
                
                // Download and extract
                val success = downloadAndExtract(downloadUrl, targetFile, os)
                
                if (success) {
                    InstallResult(true, "Ollama installed successfully at: $installPath")
                } else {
                    InstallResult(false, "Failed to download and install Ollama")
                }
            } catch (e: Exception) {
                logger.error("Binary download installation failed", e)
                InstallResult(false, "Binary download installation failed: ${e.message}")
            }
        }
        
        private fun getDefaultInstallPath(os: se.premex.gmai.plugin.utils.OSUtils.OperatingSystem): String {
            return when (os) {
                se.premex.gmai.plugin.utils.OSUtils.OperatingSystem.MACOS -> 
                    System.getProperty("user.home") + "/.gradle/ollama/bin/ollama"
                se.premex.gmai.plugin.utils.OSUtils.OperatingSystem.LINUX -> 
                    System.getProperty("user.home") + "/.gradle/ollama/bin/ollama"
                se.premex.gmai.plugin.utils.OSUtils.OperatingSystem.WINDOWS -> 
                    System.getProperty("user.home") + "/.gradle/ollama/bin/ollama.exe"
            }
        }
        
        private fun getDownloadUrl(os: se.premex.gmai.plugin.utils.OSUtils.OperatingSystem, version: String): String {
            return when (os) {
                se.premex.gmai.plugin.utils.OSUtils.OperatingSystem.MACOS -> 
                    "https://github.com/ollama/ollama/releases/download/$version/ollama-darwin.tgz"
                se.premex.gmai.plugin.utils.OSUtils.OperatingSystem.LINUX -> 
                    "https://github.com/ollama/ollama/releases/download/$version/ollama-linux-amd64.tgz"
                se.premex.gmai.plugin.utils.OSUtils.OperatingSystem.WINDOWS -> 
                    "https://github.com/ollama/ollama/releases/download/$version/ollama-windows-amd64.zip"
            }
        }
        
        private fun downloadAndExtract(
            downloadUrl: String, 
            targetFile: java.io.File, 
            os: se.premex.gmai.plugin.utils.OSUtils.OperatingSystem
        ): Boolean {
            return try {
                // Create temporary file for download
                val isWindows = os == se.premex.gmai.plugin.utils.OSUtils.OperatingSystem.WINDOWS
                val tempFile = java.io.File.createTempFile(
                    "ollama-download", 
                    if (isWindows) ".zip" else ".tgz"
                )
                
                try {
                    // Download the file
                    downloadFile(downloadUrl, tempFile)
                    logger.info("Download completed, extracting to: ${targetFile.absolutePath}")
                    
                    // Extract based on platform
                    if (isWindows) {
                        extractZip(tempFile, targetFile)
                    } else {
                        extractTarGz(tempFile, targetFile)
                    }
                } finally {
                    // Clean up temporary file
                    tempFile.delete()
                }
            } catch (e: Exception) {
                logger.error("Failed to download and extract Ollama", e)
                false
            }
        }
        
        private fun downloadFile(url: String, targetFile: java.io.File) {
            java.net.URL(url).openStream().use { input ->
                java.nio.file.Files.copy(
                    input, 
                    targetFile.toPath(), 
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
                )
            }
        }
        
        private fun extractTarGz(archiveFile: java.io.File, targetFile: java.io.File): Boolean {
            return try {
                // Use tar command for extraction on Unix-like systems
                val processBuilder = ProcessBuilder(
                    "tar", "-xzf", archiveFile.absolutePath, "-C", targetFile.parent
                )
                processBuilder.directory(targetFile.parentFile)
                
                val process = processBuilder.start()
                val finished = process.waitFor(60, TimeUnit.SECONDS)
                
                if (finished && process.exitValue() == 0) {
                    // Look for the extracted ollama executable
                    val extractedFile = findExtractedExecutable(targetFile.parentFile, "ollama")
                    if (extractedFile != null && extractedFile.exists()) {
                        // Move to target location and make executable
                        extractedFile.renameTo(targetFile)
                        targetFile.setExecutable(true)
                        true
                    } else {
                        logger.error("Ollama executable not found after extraction")
                        false
                    }
                } else {
                    if (!finished) process.destroyForcibly()
                    logger.error("tar extraction failed")
                    false
                }
            } catch (e: Exception) {
                logger.error("Failed to extract tar.gz archive", e)
                false
            }
        }
        
        private fun extractZip(archiveFile: java.io.File, targetFile: java.io.File): Boolean {
            return try {
                // Use Java's built-in ZIP support for Windows
                val extractDir = java.io.File(targetFile.parent, "extract_temp")
                extractDir.mkdirs()
                
                try {
                    java.util.zip.ZipInputStream(java.io.FileInputStream(archiveFile)).use { zis ->
                        var entry = zis.nextEntry
                        while (entry != null) {
                            val entryFile = java.io.File(extractDir, entry.name)
                            if (entry.isDirectory) {
                                entryFile.mkdirs()
                            } else {
                                entryFile.parentFile?.mkdirs()
                                java.io.FileOutputStream(entryFile).use { fos ->
                                    zis.copyTo(fos)
                                }
                            }
                            zis.closeEntry()
                            entry = zis.nextEntry
                        }
                    }
                    
                    // Look for the extracted ollama executable
                    val extractedFile = findExtractedExecutable(extractDir, "ollama.exe")
                    if (extractedFile != null && extractedFile.exists()) {
                        // Move to target location
                        extractedFile.renameTo(targetFile)
                        true
                    } else {
                        logger.error("Ollama executable not found after ZIP extraction")
                        false
                    }
                } finally {
                    // Clean up extraction directory
                    extractDir.deleteRecursively()
                }
            } catch (e: Exception) {
                logger.error("Failed to extract ZIP archive", e)
                false
            }
        }
        
        private fun findExtractedExecutable(dir: java.io.File, executableName: String): java.io.File? {
            return dir.walkTopDown().firstOrNull { file ->
                file.name == executableName || 
                (file.name.equals("ollama", ignoreCase = true) && file.canExecute())
            }
        }
    }

    data class OllamaStatus(
        val isRunning: Boolean,
        val port: Int,
        val host: String,
        val executablePath: String?
    ) : Serializable

    data class InstallResult(
        val success: Boolean,
        val message: String
    ) : Serializable
}
