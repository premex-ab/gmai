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

    fun startOllamaIsolated(
        executablePath: String,
        isolatedPath: String,
        host: String = "localhost",
        port: Int = 11434,
        additionalArgs: List<String> = emptyList()
    ): Boolean {
        return startOllama(
            executablePath = executablePath,
            host = host,
            port = port,
            additionalArgs = additionalArgs,
            isolatedPath = isolatedPath
        )
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
            val process = when (getOperatingSystem()) {
                OperatingSystem.MACOS, OperatingSystem.LINUX -> {
                    ProcessBuilder("lsof", "-i", ":$port").start()
                }
                OperatingSystem.WINDOWS -> {
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
            val process = when (getOperatingSystem()) {
                OperatingSystem.MACOS, OperatingSystem.LINUX -> {
                    ProcessBuilder("pkill", "-f", "ollama.*serve").start()
                }
                OperatingSystem.WINDOWS -> {
                    ProcessBuilder("taskkill", "/F", "/IM", "ollama.exe").start()
                }
            }

            process.waitFor() == 0
        } catch (e: Exception) {
            logger.error("Failed to kill existing Ollama processes", e)
            false
        }
    }

    private fun getOperatingSystem(): OperatingSystem {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("mac") -> OperatingSystem.MACOS
            os.contains("linux") -> OperatingSystem.LINUX
            os.contains("windows") -> OperatingSystem.WINDOWS
            else -> throw UnsupportedOperationException("Unsupported operating system: $os")
        }
    }

    enum class OperatingSystem {
        MACOS, LINUX, WINDOWS
    }
}
