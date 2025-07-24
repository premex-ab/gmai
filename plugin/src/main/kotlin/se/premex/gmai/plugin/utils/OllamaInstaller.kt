package se.premex.gmai.plugin.utils

import org.gradle.api.Project
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import se.premex.gmai.plugin.models.OllamaInstallationStrategy
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit
import se.premex.gmai.plugin.utils.OSUtils.platformName

class OllamaInstaller(
    private val logger: Logger = LoggerFactory.getLogger(OllamaInstaller::class.java),
    private val project: Project
) {

    data class InstallationResult(
        val success: Boolean,
        val executablePath: String?,
        val installationType: InstallationType,
        val message: String
    )

    enum class InstallationType {
        EXISTING_SYSTEM,
        EXISTING_ISOLATED,
        NEW_ISOLATED,
        NEW_SYSTEM_WIDE
    }

    /**
     * Find or install Ollama based on the specified strategy
     */
    fun findOrInstallOllama(
        strategy: OllamaInstallationStrategy,
        isolatedPath: String? = null
    ): InstallationResult {

        return when (strategy) {
            OllamaInstallationStrategy.PREFER_EXISTING -> {
                // Try existing first, then isolated if not found
                findExistingOrInstallIsolated(isolatedPath)
            }

            OllamaInstallationStrategy.ISOLATED_ONLY -> {
                // Only install isolated, ignore existing
                installIsolated(isolatedPath)
            }

            OllamaInstallationStrategy.PREFER_EXISTING_THEN_SYSTEM_WIDE -> {
                // Try existing first, then system-wide if not found
                findExistingOrInstallSystemWide()
            }

            OllamaInstallationStrategy.SYSTEM_WIDE_ONLY -> {
                // Only install system-wide, ignore existing
                installSystemWide()
            }

            OllamaInstallationStrategy.FULL_PRIORITY -> {
                // Try all options in priority order
                findOrInstallWithFullPriority(isolatedPath)
            }
        }
    }

    private fun findExistingOrInstallIsolated(isolatedPath: String?): InstallationResult {
        // Check for existing installation first
        val existingPath = findOllamaExecutable()
        if (existingPath != null) {
            val installationType = if (isSystemWideInstallation(existingPath)) {
                InstallationType.EXISTING_SYSTEM
            } else {
                InstallationType.EXISTING_ISOLATED
            }
            logger.info("Found existing Ollama installation at: $existingPath")
            return InstallationResult(
                success = true,
                executablePath = existingPath,
                installationType = installationType,
                message = "Using existing Ollama installation"
            )
        }

        // No existing installation, try isolated
        logger.info("No existing installation found, installing in isolated environment...")
        return installIsolated(isolatedPath)
    }

    private fun findExistingOrInstallSystemWide(): InstallationResult {
        // Check for existing installation first
        val existingPath = findOllamaExecutable()
        if (existingPath != null) {
            val installationType = if (isSystemWideInstallation(existingPath)) {
                InstallationType.EXISTING_SYSTEM
            } else {
                InstallationType.EXISTING_ISOLATED
            }
            logger.info("Found existing Ollama installation at: $existingPath")
            return InstallationResult(
                success = true,
                executablePath = existingPath,
                installationType = installationType,
                message = "Using existing Ollama installation"
            )
        }

        // No existing installation, try system-wide
        logger.info("No existing installation found, installing system-wide...")
        return installSystemWide()
    }

    private fun findOrInstallWithFullPriority(isolatedPath: String?): InstallationResult {
        // Priority 1: Check for existing installations
        val existingPath = findOllamaExecutable()
        if (existingPath != null) {
            val installationType = if (isSystemWideInstallation(existingPath)) {
                InstallationType.EXISTING_SYSTEM
            } else {
                InstallationType.EXISTING_ISOLATED
            }
            logger.info("Found existing Ollama installation at: $existingPath")
            return InstallationResult(
                success = true,
                executablePath = existingPath,
                installationType = installationType,
                message = "Using existing Ollama installation"
            )
        }

        // Priority 2: Try isolated installation
        logger.info("No existing installation found, attempting isolated installation...")
        val isolatedResult = installIsolated(isolatedPath)
        if (isolatedResult.success) {
            return isolatedResult
        }
        logger.warn("Isolated installation failed: ${isolatedResult.message}")

        // Priority 3: Try system-wide installation
        logger.info("Attempting system-wide installation...")
        val systemWideResult = installSystemWide()
        if (systemWideResult.success) {
            return systemWideResult
        }
        logger.error("System-wide installation failed: ${systemWideResult.message}")

        // All installation attempts failed
        return InstallationResult(
            success = false,
            executablePath = null,
            installationType = InstallationType.NEW_ISOLATED,
            message = "All installation attempts failed. Consider installing Ollama manually."
        )
    }

    private fun isSystemWideInstallation(path: String): Boolean {
        val systemWidePaths = when (OSUtils.getOperatingSystem()) {
            OSUtils.OperatingSystem.MACOS -> listOf(
                "/usr/local/bin/",
                "/opt/homebrew/bin/",
                "/usr/bin/"
            )
            OSUtils.OperatingSystem.LINUX -> listOf(
                "/usr/local/bin/",
                "/usr/bin/",
                "/bin/"
            )
            OSUtils.OperatingSystem.WINDOWS -> listOf(
                "C:\\Program Files\\",
                "C:\\Program Files (x86)\\"
            )
        }

        return systemWidePaths.any { systemPath ->
            path.startsWith(systemPath, ignoreCase = true)
        }
    }

    private fun installIsolated(isolatedPath: String?): InstallationResult {
        return try {
            val targetPath = isolatedPath ?: getDefaultIsolatedPath()
            val success = downloadAndInstallDirect(OSUtils.getOperatingSystem().platformName, targetPath)

            if (success) {
                InstallationResult(
                    success = true,
                    executablePath = targetPath,
                    installationType = InstallationType.NEW_ISOLATED,
                    message = "Ollama installed in isolated environment at: $targetPath"
                )
            } else {
                InstallationResult(
                    success = false,
                    executablePath = null,
                    installationType = InstallationType.NEW_ISOLATED,
                    message = "Failed to download and install Ollama in isolated environment"
                )
            }
        } catch (e: Exception) {
            InstallationResult(
                success = false,
                executablePath = null,
                installationType = InstallationType.NEW_ISOLATED,
                message = "Isolated installation failed: ${e.message}"
            )
        }
    }

    private fun installSystemWide(): InstallationResult {
        return try {
            val success = when (OSUtils.getOperatingSystem()) {
                OSUtils.OperatingSystem.MACOS -> installSystemWideOnMacOS()
                OSUtils.OperatingSystem.LINUX -> installSystemWideOnLinux()
                OSUtils.OperatingSystem.WINDOWS -> installSystemWideOnWindows()
            }

            if (success) {
                val executablePath = findOllamaExecutable()
                InstallationResult(
                    success = true,
                    executablePath = executablePath,
                    installationType = InstallationType.NEW_SYSTEM_WIDE,
                    message = "Ollama installed system-wide"
                )
            } else {
                InstallationResult(
                    success = false,
                    executablePath = null,
                    installationType = InstallationType.NEW_SYSTEM_WIDE,
                    message = "System-wide installation failed"
                )
            }
        } catch (e: Exception) {
            InstallationResult(
                success = false,
                executablePath = null,
                installationType = InstallationType.NEW_SYSTEM_WIDE,
                message = "System-wide installation failed: ${e.message}"
            )
        }
    }

    private fun installSystemWideOnMacOS(): Boolean {
        return try {
            // Try Homebrew first
            val brewResult = ProcessBuilder("brew", "install", "ollama")
                .redirectErrorStream(true)
                .start()

            if (brewResult.waitFor(300, TimeUnit.SECONDS) && brewResult.exitValue() == 0) {
                logger.info("Ollama installed via Homebrew")
                true
            } else {
                logger.warn("Homebrew installation failed")
                false
            }
        } catch (e: Exception) {
            logger.warn("Homebrew not available or failed: ${e.message}")
            false
        }
    }

    private fun installSystemWideOnLinux(): Boolean {
        return try {
            // Use the official install script
            val installScript = "curl -fsSL https://ollama.com/install.sh | sh"
            val bashProcess = ProcessBuilder("bash", "-c", installScript)
                .redirectErrorStream(true)
                .start()

            val result = bashProcess.waitFor(300, TimeUnit.SECONDS)
            if (result && bashProcess.exitValue() == 0) {
                logger.info("Ollama installed via install script")
                true
            } else {
                logger.warn("Install script failed")
                false
            }
        } catch (e: Exception) {
            logger.error("Failed to install Ollama via script", e)
            false
        }
    }

    private fun installSystemWideOnWindows(): Boolean {
        logger.warn("Automatic system-wide Ollama installation on Windows is not yet supported")
        logger.info("Please install Ollama manually from https://ollama.com/download")
        return false
    }

    private fun getDefaultIsolatedPath(): String {
        return when (OSUtils.getOperatingSystem()) {
            OSUtils.OperatingSystem.MACOS -> ".ollama/bin/ollama"
            OSUtils.OperatingSystem.LINUX -> ".ollama/bin/ollama"
            OSUtils.OperatingSystem.WINDOWS -> ".ollama\\bin\\ollama.exe"
        }
    }

    fun findOllamaExecutable(): String? {
        val possiblePaths = when (OSUtils.getOperatingSystem()) {
            OSUtils.OperatingSystem.MACOS -> listOf(
                // Check project directory first (isolated installation)
                ".ollama/bin/ollama",
                // Then check system-wide locations
                "/usr/local/bin/ollama",
                "/opt/homebrew/bin/ollama",
                System.getProperty("user.home") + "/.ollama/bin/ollama"
            )
            OSUtils.OperatingSystem.LINUX -> listOf(
                // Check project directory first (isolated installation)
                ".ollama/bin/ollama",
                // Then check system-wide locations
                "/usr/local/bin/ollama",
                "/usr/bin/ollama",
                System.getProperty("user.home") + "/.local/bin/ollama"
            )
            OSUtils.OperatingSystem.WINDOWS -> listOf(
                // Check project directory first (isolated installation)
                ".ollama\\bin\\ollama.exe",
                // Then check system-wide locations
                System.getenv("LOCALAPPDATA") + "\\Programs\\Ollama\\ollama.exe",
                System.getenv("PROGRAMFILES") + "\\Ollama\\ollama.exe"
            )
        }

        return possiblePaths.firstOrNull { path ->
            File(path).let { it.exists() && it.canExecute() }
        }
    }

    private fun downloadAndInstallDirect(platform: String, installPath: String?): Boolean {
        return try {
            val downloadUrl = getDownloadUrl(platform)
            val targetPath = installPath ?: getDefaultInstallPath(platform)
            val targetFile = File(targetPath)
            targetFile.parentFile?.mkdirs()

            logger.info("Downloading Ollama from: $downloadUrl")

            // Create temporary file for download
            val tempFile = File.createTempFile("ollama-download",
                if (platform == "windows") ".zip" else ".tgz")

            try {
                // Download the archive
                @Suppress("DEPRECATION")
                URL(downloadUrl).openStream().use { input ->
                    Files.copy(input, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }

                logger.info("Download completed, extracting archive...")

                // Extract the archive and install using Gradle's built-in capabilities
                val success = when (platform) {
                    "windows" -> extractZipAndInstallWithGradle(tempFile, targetFile)
                    else -> extractTarGzAndInstallWithGradle(tempFile, targetFile)
                }

                if (success) {
                    logger.info("Ollama installed successfully at: $targetPath")
                    true
                } else {
                    logger.error("Failed to extract and install Ollama")
                    false
                }
            } finally {
                // Clean up temporary file
                tempFile.delete()
            }
        } catch (e: Exception) {
            logger.error("Failed to download and install Ollama", e)
            false
        }
    }

    private fun extractTarGzAndInstallWithGradle(archiveFile: File, targetFile: File): Boolean {
        return try {
            logger.info("Using Gradle's tarTree to extract archive")
            val tarTree = project.tarTree(archiveFile)

            // Extract all files to a temporary directory
            val tempExtractDir = File.createTempFile("ollama-extract", "").apply {
                delete()
                mkdirs()
            }

            try {
                // Copy the tar tree to the temporary directory
                project.copy { spec ->
                    spec.from(tarTree)
                    spec.into(tempExtractDir)
                }

                // Find the ollama executable in the extracted files
                val ollamaExecutable = findOllamaExecutableInDir(tempExtractDir)
                if (ollamaExecutable != null && ollamaExecutable.exists()) {
                    // Move the executable to the target location
                    ollamaExecutable.renameTo(targetFile)
                    targetFile.setExecutable(true)
                    true
                } else {
                    logger.error("Ollama executable not found in extracted archive")
                    false
                }
            } finally {
                // Clean up temporary extraction directory
                tempExtractDir.deleteRecursively()
            }
        } catch (e: Exception) {
            logger.error("Failed to extract tar.gz archive using Gradle", e)
            // Fallback to system command
            extractTarGzAndInstall(archiveFile, targetFile)
        }
    }

    private fun extractZipAndInstallWithGradle(archiveFile: File, targetFile: File): Boolean {
        return try {
            logger.info("Using Gradle's zipTree to extract archive")
            val zipTree = project.zipTree(archiveFile)

            // Extract all files to a temporary directory
            val tempExtractDir = File.createTempFile("ollama-extract", "").apply {
                delete()
                mkdirs()
            }

            try {
                // Copy the zip tree to the temporary directory
                project.copy { spec ->
                    spec.from(zipTree)
                    spec.into(tempExtractDir)
                }

                // Find the ollama executable in the extracted files
                val ollamaExecutable = findOllamaExecutableInDir(tempExtractDir)
                if (ollamaExecutable != null && ollamaExecutable.exists()) {
                    // Move the executable to the target location
                    ollamaExecutable.renameTo(targetFile)
                    true
                } else {
                    logger.error("Ollama executable not found in extracted archive")
                    false
                }
            } finally {
                // Clean up temporary extraction directory
                tempExtractDir.deleteRecursively()
            }
        } catch (e: Exception) {
            logger.error("Failed to extract ZIP archive using Gradle", e)
            // Fallback to system command
            extractZipAndInstall(archiveFile, targetFile)
        }
    }

    private fun findOllamaExecutableInDir(dir: File): File? {
        // Look for the ollama executable in the extracted directory
        val executableName = if (OSUtils.getOperatingSystem() == OSUtils.OperatingSystem.WINDOWS) "ollama.exe" else "ollama"

        return dir.walkTopDown()
            .firstOrNull { file ->
                file.name == executableName && file.canExecute()
            }
    }

    // Keep the fallback system command methods for when Project is not available
    private fun extractTarGzAndInstall(archiveFile: File, targetFile: File): Boolean {
        return try {
            // Use tar command to extract
            val extractProcess = ProcessBuilder("tar", "-xzf", archiveFile.absolutePath, "-C", targetFile.parent)
                .directory(targetFile.parentFile)
                .redirectErrorStream(true)
                .start()

            val result = extractProcess.waitFor(60, TimeUnit.SECONDS)
            if (result && extractProcess.exitValue() == 0) {
                // The extracted file should be named 'ollama', move it to target location
                val extractedFile = File(targetFile.parent, "ollama")
                if (extractedFile.exists()) {
                    extractedFile.renameTo(targetFile)
                    targetFile.setExecutable(true)
                    true
                } else {
                    logger.error("Extracted file 'ollama' not found in archive")
                    false
                }
            } else {
                logger.error("tar extraction failed")
                false
            }
        } catch (e: Exception) {
            logger.error("Failed to extract tar.gz archive", e)
            false
        }
    }

    private fun extractZipAndInstall(archiveFile: File, targetFile: File): Boolean {
        return try {
            // Use PowerShell/native tools to extract ZIP on Windows
            val extractProcess = ProcessBuilder("powershell", "-Command",
                "Expand-Archive -Path '${archiveFile.absolutePath}' -DestinationPath '${targetFile.parent}' -Force")
                .directory(targetFile.parentFile)
                .redirectErrorStream(true)
                .start()

            val result = extractProcess.waitFor(60, TimeUnit.SECONDS)
            if (result && extractProcess.exitValue() == 0) {
                // The extracted file should be named 'ollama.exe', move it to target location
                val extractedFile = File(targetFile.parent, "ollama.exe")
                if (extractedFile.exists()) {
                    extractedFile.renameTo(targetFile)
                    true
                } else {
                    logger.error("Extracted file 'ollama.exe' not found in archive")
                    false
                }
            } else {
                logger.error("ZIP extraction failed")
                false
            }
        } catch (e: Exception) {
            logger.error("Failed to extract ZIP archive", e)
            false
        }
    }

    private fun getDownloadUrl(platform: String): String {
        // Use the latest release URL pattern with proper file extensions
        val version = "v0.9.6" // This should ideally be configurable or fetched dynamically
        return when (platform) {
            "darwin" -> "https://github.com/ollama/ollama/releases/download/$version/ollama-darwin.tgz"
            "linux" -> "https://github.com/ollama/ollama/releases/download/$version/ollama-linux-amd64.tgz"
            "windows" -> "https://github.com/ollama/ollama/releases/download/$version/ollama-windows-amd64.zip"
            else -> throw IllegalArgumentException("Unsupported platform: $platform")
        }
    }

    private fun getDefaultInstallPath(platform: String): String {
        return when (platform) {
            "darwin" -> System.getProperty("user.home") + "/.ollama/bin/ollama"
            "linux" -> System.getProperty("user.home") + "/.local/bin/ollama"
            "windows" -> System.getenv("LOCALAPPDATA") + "\\Programs\\Ollama\\ollama.exe"
            else -> throw IllegalArgumentException("Unsupported platform: $platform")
        }
    }
}
