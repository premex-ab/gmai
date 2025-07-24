package se.premex.gmai.plugin.utils

import java.io.File
import java.util.concurrent.TimeUnit
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ProcessManager(private val logger: Logger = LoggerFactory.getLogger(ProcessManager::class.java)) {

    private var ollamaProcess: Process? = null

    fun startOllama(
        executablePath: String,
        host: String = "localhost",
        port: Int = 11434,
        additionalArgs: List<String> = emptyList(),
        isolatedPath: String? = null,
        dataPath: String? = null
    ): Boolean {
        if (isOllamaRunning(port)) {
            logger.info("Ollama is already running on port $port")
            return true
        }

        return try {
            val command = buildList {
                add(executablePath)
                add("serve")
                addAll(additionalArgs)
            }

            val processBuilder = ProcessBuilder(command)
                .redirectErrorStream(true)

            // Set environment variables
            processBuilder.environment().apply {
                put("OLLAMA_HOST", "$host:$port")

                // Set isolated environment paths if provided
                isolatedPath?.let { path ->
                    put("OLLAMA_HOME", path)
                    put("OLLAMA_MODELS", "$path/models")

                    // Ensure directories exist
                    File(path).mkdirs()
                    File("$path/models").mkdirs()
                }

                dataPath?.let { path ->
                    put("OLLAMA_MODELS", path)
                    File(path).mkdirs()
                }
            }

            logger.info("Starting Ollama with command: ${command.joinToString(" ")}")
            if (isolatedPath != null) {
                logger.info("Using isolated environment at: $isolatedPath")
            }

            ollamaProcess = processBuilder.start()

            // Wait a bit for startup
            Thread.sleep(2000)

            isOllamaRunning(port)
        } catch (e: Exception) {
            logger.error("Failed to start Ollama", e)
            false
        }
    }

    fun stopOllama(): Boolean {
        return try {
            ollamaProcess?.let { process ->
                logger.info("Stopping Ollama process...")
                process.destroyForcibly()
                process.waitFor(10, TimeUnit.SECONDS)
                ollamaProcess = null
                logger.info("Ollama process stopped")
                true
            } ?: run {
                // Try to find and kill existing Ollama processes
                killExistingOllamaProcesses()
            }
        } catch (e: Exception) {
            logger.error("Failed to stop Ollama", e)
            false
        }
    }

    fun stopOllamaGracefully(timeoutSeconds: Int = 30): Boolean {
        return try {
            ollamaProcess?.let { process ->
                logger.info("Gracefully stopping Ollama process...")
                process.destroy() // Use destroy() instead of destroyForcibly()

                val terminated = process.waitFor(timeoutSeconds.toLong(), TimeUnit.SECONDS)
                if (!terminated) {
                    logger.warn("Ollama did not stop gracefully within $timeoutSeconds seconds, forcing termination")
                    process.destroyForcibly()
                    process.waitFor(10, TimeUnit.SECONDS)
                }

                ollamaProcess = null
                logger.info("Ollama process stopped")
                true
            } ?: run {
                killExistingOllamaProcesses()
            }
        } catch (e: Exception) {
            logger.error("Failed to gracefully stop Ollama", e)
            false
        }
    }

    fun isOllamaRunning(port: Int = 11434): Boolean {
        return try {
            // Check if process is listening on the specific port
            val process = when (OSUtils.getOperatingSystem()) {
                OSUtils.OperatingSystem.MACOS, OSUtils.OperatingSystem.LINUX -> {
                    ProcessBuilder("lsof", "-i", ":$port").start()
                }
                OSUtils.OperatingSystem.WINDOWS -> {
                    ProcessBuilder("netstat", "-an").start()
                }
            }

            process.waitFor() == 0
        } catch (e: Exception) {
            // Fallback to generic process check
            try {
                val process = ProcessBuilder("pgrep", "-f", "ollama.*serve").start()
                process.waitFor() == 0
            } catch (e2: Exception) {
                false
            }
        }
    }

    private fun killExistingOllamaProcesses(): Boolean {
        return try {
            val process = when (OSUtils.getOperatingSystem()) {
                OSUtils.OperatingSystem.MACOS, OSUtils.OperatingSystem.LINUX -> {
                    ProcessBuilder("pkill", "-f", "ollama.*serve").start()
                }
                OSUtils.OperatingSystem.WINDOWS -> {
                    ProcessBuilder("taskkill", "/F", "/IM", "ollama.exe").start()
                }
            }

            process.waitFor() == 0
        } catch (e: Exception) {
            logger.error("Failed to kill existing Ollama processes", e)
            false
        }
    }

    /**
     * Simplified start method for configuration cache compatibility.
     * This method tries to find an existing Ollama executable and start it without project dependencies.
     */
    fun startOllamaSimple(
        host: String = "localhost",
        port: Int = 11434,
        additionalArgs: List<String> = emptyList()
    ): Boolean {
        if (isOllamaRunning(port)) {
            logger.info("Ollama is already running on port $port")
            return true
        }

        // Try to find ollama executable in common locations
        val executablePath = findOllamaExecutable()
        if (executablePath == null) {
            logger.error("Ollama executable not found. Please install Ollama first.")
            return false
        }

        return startOllama(
            executablePath = executablePath,
            host = host,
            port = port,
            additionalArgs = additionalArgs
        )
    }

    /**
     * Find Ollama executable in common system locations
     */
    private fun findOllamaExecutable(): String? {
        val possiblePaths = when (OSUtils.getOperatingSystem()) {
            OSUtils.OperatingSystem.MACOS -> listOf(
                "/usr/local/bin/ollama",
                "/opt/homebrew/bin/ollama",
                System.getProperty("user.home") + "/.local/bin/ollama"
            )
            OSUtils.OperatingSystem.LINUX -> listOf(
                "/usr/local/bin/ollama",
                "/usr/bin/ollama",
                System.getProperty("user.home") + "/.local/bin/ollama"
            )
            OSUtils.OperatingSystem.WINDOWS -> listOf(
                System.getenv("LOCALAPPDATA") + "\\Programs\\Ollama\\ollama.exe",
                System.getenv("PROGRAMFILES") + "\\Ollama\\ollama.exe",
                System.getenv("PROGRAMFILES(X86)") + "\\Ollama\\ollama.exe"
            )
        }

        return possiblePaths.firstOrNull { path ->
            File(path).let { it.exists() && it.canExecute() }
        } ?: findOllamaInPath()
    }

    /**
     * Try to find Ollama in system PATH
     */
    private fun findOllamaInPath(): String? {
        return OSUtils.findExecutableInPath("ollama")?.takeIf { it.isNotBlank() }
            ?: run {
                logger.debug("Could not find ollama in PATH")
                null
            }
    }

    enum class OperatingSystem {
        MACOS, LINUX, WINDOWS
    }
}
