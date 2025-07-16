package se.premex.gmai.plugin.models

/**
 * Defines the strategy for finding or installing Ollama
 */
enum class OllamaInstallationStrategy {
    /**
     * Use existing installation if available, otherwise install isolated.
     * Never install system-wide automatically.
     */
    PREFER_EXISTING,

    /**
     * Always install in isolated/per-project environment.
     * Ignore existing installations.
     */
    ISOLATED_ONLY,

    /**
     * Use existing installation if available, otherwise install system-wide.
     * Skip isolated installation.
     */
    PREFER_EXISTING_THEN_SYSTEM_WIDE,

    /**
     * Always install system-wide (e.g., via homebrew).
     * Ignore existing installations and skip isolated.
     */
    SYSTEM_WIDE_ONLY,

    /**
     * Try all options in priority order:
     * 1. Use existing installation
     * 2. Install isolated
     * 3. Install system-wide
     */
    FULL_PRIORITY
}
