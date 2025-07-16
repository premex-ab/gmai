package se.premex.gmai.plugin.models

import java.time.Duration

data class OllamaInstance(
    val host: String = "localhost",
    val port: Int = 11434,
    val protocol: String = "http",
    val timeout: Duration = Duration.ofSeconds(30),
    val isIsolated: Boolean = false,
    val isolatedPath: String? = null
) {
    val baseUrl: String get() = "$protocol://$host:$port"
}
