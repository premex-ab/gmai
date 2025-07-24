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
}
