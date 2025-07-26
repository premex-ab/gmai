
// Demo showing the installation functionality
val currentOS = se.premex.gmai.plugin.utils.OSUtils.getOperatingSystem()
println("Detected OS: $currentOS")

val userHome = System.getProperty("user.home")
val installPath = when (currentOS) {
    se.premex.gmai.plugin.utils.OSUtils.OperatingSystem.WINDOWS -> "$userHome/.gradle/ollama/bin/ollama.exe"
    else -> "$userHome/.gradle/ollama/bin/ollama"
}
println("Default install path: $installPath")

val downloadUrl = when (currentOS) {
    se.premex.gmai.plugin.utils.OSUtils.OperatingSystem.MACOS -> "https://github.com/ollama/ollama/releases/download/v0.9.6/ollama-darwin.tgz"
    se.premex.gmai.plugin.utils.OSUtils.OperatingSystem.LINUX -> "https://github.com/ollama/ollama/releases/download/v0.9.6/ollama-linux-amd64.tgz"
    se.premex.gmai.plugin.utils.OSUtils.OperatingSystem.WINDOWS -> "https://github.com/ollama/ollama/releases/download/v0.9.6/ollama-windows-amd64.zip"
}
println("Download URL: $downloadUrl")

