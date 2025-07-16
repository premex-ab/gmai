package se.premex.gmai.plugin.models

enum class ModelStatus {
    NOT_INSTALLED,
    INSTALLING,
    INSTALLED,
    RUNNING,
    ERROR
}

enum class OllamaServiceStatus {
    STOPPED,
    STARTING,
    RUNNING,
    ERROR
}

enum class OllamaEnvironmentType {
    SYSTEM_WIDE,
    ISOLATED
}
