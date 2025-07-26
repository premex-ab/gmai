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
            isolatedPath = parameters.installPath.orNull
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

        fun findOrInstallOllama(strategy: OllamaInstallationStrategy, isolatedPath: String?): InstallResult {
            val existing = findOllamaExecutable()
            if (existing != null) {
                return InstallResult(true, "Ollama already installed at: $existing")
            }

            return when (strategy) {
                OllamaInstallationStrategy.PREFER_EXISTING -> {
                    // Already checked for existing above, so try isolated download
                    installViaBinaryDownload(isolatedPath ?: System.getProperty("user.home") + "/.gradle/ollama")
                }
                OllamaInstallationStrategy.PREFER_EXISTING_THEN_SYSTEM_WIDE -> {
                    // Try system package manager first, then isolated
                    val systemResult = installViaPackageManager()
                    if (systemResult.success) systemResult else installViaBinaryDownload(isolatedPath)
                }
                OllamaInstallationStrategy.ISOLATED_ONLY -> installViaBinaryDownload(isolatedPath ?: System.getProperty("user.home") + "/.gradle/ollama")
                OllamaInstallationStrategy.SYSTEM_WIDE_ONLY -> installViaPackageManager()
                OllamaInstallationStrategy.FULL_PRIORITY -> {
                    // Try system package manager, then isolated download
                    val systemResult = installViaPackageManager()
                    if (systemResult.success) systemResult else installViaBinaryDownload(isolatedPath)
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

        private fun installViaBinaryDownload(targetPath: String?): InstallResult {
            return InstallResult(false, "Binary download installation not yet implemented")
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
