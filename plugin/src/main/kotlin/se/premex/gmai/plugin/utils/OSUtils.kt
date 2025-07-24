package se.premex.gmai.plugin.utils

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
     */
    fun findExecutableInPath(executableName: String): String? {
        return try {
            val command = when (getOperatingSystem()) {
                OperatingSystem.WINDOWS -> "where"
                OperatingSystem.MACOS, OperatingSystem.LINUX -> "which"
            }
            
            val process = ProcessBuilder(command, executableName).start()
            if (process.waitFor() == 0) {
                process.inputStream.bufferedReader().readText().trim()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
