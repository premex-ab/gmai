package se.premex.gmai.plugin.extensions

import se.premex.gmai.plugin.models.OllamaEnvironmentType
import se.premex.gmai.plugin.models.OllamaInstallationStrategy
import javax.inject.Inject

open class OllamaConfiguration @Inject constructor() {
    var host: String = "localhost"
    var port: Int = 11434
    var protocol: String = "http"
    var installPath: String? = null
    var dataPath: String? = null
    var logLevel: String = "info"
    var additionalArgs: List<String> = emptyList()

    /**
     * Environment type: SYSTEM_WIDE or ISOLATED
     */
    var environmentType: OllamaEnvironmentType = OllamaEnvironmentType.SYSTEM_WIDE

    /**
     * Path for isolated environment (if environmentType is ISOLATED)
     */
    var isolatedPath: String? = null

    /**
     * Installation strategy - defines how Ollama should be found or installed
     */
    var installationStrategy: OllamaInstallationStrategy = OllamaInstallationStrategy.PREFER_EXISTING

    /**
     * Use graceful shutdown when stopping Ollama
     */
    var gracefulShutdown: Boolean = true

    /**
     * Timeout in seconds for graceful shutdown
     */
    var shutdownTimeout: Int = 30

    /**
     * Allow automatic port change when port conflicts occur (Phase 3 feature)
     */
    var allowPortChange: Boolean = true
}
