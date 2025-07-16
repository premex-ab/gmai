package se.premex.gmai.plugin.utils

import org.gradle.api.GradleException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Enhanced error handling utility for Phase 3 production-ready features
 */
class ErrorHandler(
    private val logger: Logger = LoggerFactory.getLogger(ErrorHandler::class.java)
) {

    /**
     * Handle Ollama installation errors with helpful recovery suggestions
     */
    fun handleInstallationError(error: Exception, autoInstall: Boolean): Nothing {
        logger.error("Ollama installation failed", error)

        val message = when {
            !autoInstall -> """
                Ollama is not installed and auto-installation is disabled.
                
                To resolve this:
                1. Install Ollama manually from https://ollama.com/download
                2. Or enable auto-installation with: managedAi { autoInstall = true }
                3. Or specify a custom installation path
            """.trimIndent()

            error.message?.contains("permission", ignoreCase = true) == true -> """
                Ollama installation failed due to permission issues.
                
                To resolve this:
                1. Run Gradle with elevated permissions
                2. Or install Ollama manually: https://ollama.com/download
                3. Or specify a user-writable installation path
            """.trimIndent()

            error.message?.contains("network", ignoreCase = true) == true -> """
                Ollama installation failed due to network issues.
                
                To resolve this:
                1. Check your internet connection
                2. Check if you're behind a corporate firewall
                3. Try installing manually: https://ollama.com/download
            """.trimIndent()

            else -> """
                Ollama installation failed: ${error.message}
                
                To resolve this:
                1. Install Ollama manually from https://ollama.com/download
                2. Check system requirements and compatibility
                3. Verify disk space and permissions
            """.trimIndent()
        }

        throw GradleException(message, error)
    }

    /**
     * Handle Ollama startup errors with diagnostic information
     */
    fun handleStartupError(error: Exception, port: Int, host: String): Nothing {
        logger.error("Ollama startup failed", error)

        val message = when {
            error.message?.contains("port", ignoreCase = true) == true -> """
                Ollama failed to start on port $port.
                
                Possible causes:
                1. Port $port is already in use by another service
                2. Insufficient permissions to bind to port $port
                3. Firewall blocking the port
                
                To resolve this:
                1. Use a different port: managedAi { ollama { port = 11435 } }
                2. Stop other services using port $port
                3. Check firewall settings
            """.trimIndent()

            error.message?.contains("executable", ignoreCase = true) == true -> """
                Ollama executable not found or not executable.
                
                To resolve this:
                1. Reinstall Ollama from https://ollama.com/download
                2. Check file permissions on Ollama executable
                3. Verify PATH environment variable
            """.trimIndent()

            error.message?.contains("timeout", ignoreCase = true) == true -> """
                Ollama startup timed out.
                
                Possible causes:
                1. System is under heavy load
                2. Insufficient system resources
                3. Ollama is taking longer than expected to start
                
                To resolve this:
                1. Increase startup timeout
                2. Check system resources (CPU, memory)
                3. Try starting manually to diagnose issues
            """.trimIndent()

            else -> """
                Ollama startup failed: ${error.message}
                
                To diagnose:
                1. Try starting Ollama manually: ollama serve
                2. Check system logs for additional error details
                3. Verify system requirements are met
            """.trimIndent()
        }

        throw GradleException(message, error)
    }

    /**
     * Handle model pulling errors with recovery strategies
     */
    fun handleModelError(error: Exception, modelName: String): Nothing {
        logger.error("Model operation failed for $modelName", error)

        val message = when {
            error.message?.contains("not found", ignoreCase = true) == true -> """
                Model '$modelName' not found.
                
                To resolve this:
                1. Check model name spelling
                2. Browse available models at https://ollama.com/library
                3. Try a different model version or tag
            """.trimIndent()

            error.message?.contains("network", ignoreCase = true) == true -> """
                Failed to download model '$modelName' due to network issues.
                
                To resolve this:
                1. Check internet connection
                2. Verify firewall/proxy settings
                3. Try downloading manually: ollama pull $modelName
            """.trimIndent()

            error.message?.contains("space", ignoreCase = true) == true -> """
                Insufficient disk space to download model '$modelName'.
                
                To resolve this:
                1. Free up disk space
                2. Change Ollama models directory
                3. Use a smaller model variant
            """.trimIndent()

            else -> """
                Model operation failed for '$modelName': ${error.message}
                
                To diagnose:
                1. Check Ollama service status
                2. Try the operation manually: ollama pull $modelName
                3. Check Ollama logs for more details
            """.trimIndent()
        }

        throw GradleException(message, error)
    }

    /**
     * Handle port conflicts with suggested resolutions
     */
    fun handlePortConflict(port: Int, host: String, allowPortChange: Boolean): Nothing {
        val message = if (allowPortChange) {
            """
                Port $port is already in use on $host.
                
                The plugin will attempt to find an alternative port automatically.
                To use a specific port, configure it in your build script:
                
                managedAi {
                    ollama {
                        port = 11435  // or another available port
                    }
                }
            """.trimIndent()
        } else {
            """
                Port $port is already in use on $host and port changes are not allowed.
                
                To resolve this:
                1. Stop the service using port $port
                2. Configure a different port in your build script
                3. Enable automatic port resolution
            """.trimIndent()
        }

        throw GradleException(message)
    }

    /**
     * Create a user-friendly error message with context
     */
    fun createContextualError(
        operation: String,
        error: Exception,
        context: Map<String, Any> = emptyMap()
    ): GradleException {
        val contextInfo = if (context.isNotEmpty()) {
            context.entries.joinToString("\n") { (key, value) -> "  $key: $value" }
        } else {
            ""
        }

        val message = """
            Operation '$operation' failed: ${error.message}
            
            ${if (contextInfo.isNotEmpty()) "Context:\n$contextInfo\n" else ""}
            
            For more information, run with --debug or --stacktrace
        """.trimIndent()

        return GradleException(message, error)
    }
}
