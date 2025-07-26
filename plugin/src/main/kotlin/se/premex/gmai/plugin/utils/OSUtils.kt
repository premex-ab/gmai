package se.premex.gmai.plugin.utils

import java.util.concurrent.TimeUnit

/**
 * Operating system detection utility shared across the plugin
 */
object OSUtils {

    enum class OperatingSystem {
        MACOS, LINUX, WINDOWS
    }

    fun getOperatingSystem(): OperatingSystem {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("mac") -> OperatingSystem.MACOS
            os.contains("linux") -> OperatingSystem.LINUX
            os.contains("windows") -> OperatingSystem.WINDOWS
            else -> throw UnsupportedOperationException("Unsupported operating system: $os")
        }
    }

    val OperatingSystem.platformName: String
        get() = when (this) {
            OperatingSystem.MACOS -> "darwin"
            OperatingSystem.LINUX -> "linux"
            OperatingSystem.WINDOWS -> "windows"
        }

    /**
     * Find an executable in system PATH using the appropriate command for the current OS.
     * Uses 'where' on Windows and 'which' on Unix-like systems.
     * Includes timeout to prevent hanging in CI environments.
     */
    fun findExecutableInPath(executableName: String): String? {
        return try {
            val processBuilder = when (getOperatingSystem()) {
                OperatingSystem.WINDOWS -> {
                    // Use cmd.exe to execute where command to avoid hanging
                    ProcessBuilder("cmd.exe", "/c", "where", executableName)
                }
                OperatingSystem.MACOS, OperatingSystem.LINUX -> {
                    ProcessBuilder("which", executableName)
                }
            }

            val process = processBuilder.start()

            // Add timeout to prevent hanging (especially important for Windows CI)
            val finished = process.waitFor(10, TimeUnit.SECONDS)

            if (finished && process.exitValue() == 0) {
                process.inputStream.bufferedReader().readText().trim().takeIf { it.isNotBlank() }
            } else {
                // If timeout or non-zero exit, destroy process and return null
                if (!finished) {
                    process.destroyForcibly()
                }
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
