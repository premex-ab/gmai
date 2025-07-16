plugins {
    id("se.premex.gmai")
}

managedAi {
    // Ollama configuration
    ollama {
        host = "localhost"
        port = 11434
        protocol = "http"
        logLevel = "info"
        environmentType = OllamaEnvironmentType.SYSTEM_WIDE

        // Installation strategy - choose one:
        installationStrategy = OllamaInstallationStrategy.PREFER_EXISTING  // Default: Use existing, fallback to isolated
        // installationStrategy = OllamaInstallationStrategy.ISOLATED_ONLY  // Always install isolated
        // installationStrategy = OllamaInstallationStrategy.PREFER_EXISTING_THEN_SYSTEM_WIDE  // Use existing, fallback to system-wide
        // installationStrategy = OllamaInstallationStrategy.SYSTEM_WIDE_ONLY  // Always install system-wide
        // installationStrategy = OllamaInstallationStrategy.FULL_PRIORITY  // Try all: existing → isolated → system-wide

        // Optional: Custom path for isolated installation
        // isolatedPath = "/path/to/custom/ollama"
    }

    // Model configurations
    models {
        "llama3" {
            version = "8b"
            preload = true
            keepAlive = Duration.ofMinutes(5)
        }

        "codellama" {
            version = "7b"
            preload = false
            keepAlive = Duration.ofMinutes(2)
        }
    }

    // Global settings
    timeout = Duration.ofMinutes(10)
    autoStart = true
    autoInstall = true
}
