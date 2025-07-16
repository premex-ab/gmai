---
layout: page
title: "API Reference"
permalink: /api-reference/
---

# API Reference

This page provides detailed API documentation for the GMAI plugin.

## Plugin Extension

### ManagedAiExtension

The main configuration extension for the GMAI plugin.

```kotlin
managedAi {
    // Configuration options
}
```

#### Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `models` | `NamedDomainObjectContainer<OllamaModelConfiguration>` | Empty | Model configurations |
| `ollama` | `OllamaConfiguration` | Default config | Ollama service configuration |
| `timeout` | `Duration` | 5 minutes | Global timeout for operations |
| `autoStart` | `Boolean` | `true` | Auto-start Ollama service |
| `autoInstall` | `Boolean` | `true` | Auto-install Ollama if not present |

## Configuration Classes

### OllamaConfiguration

Configuration for the Ollama service.

```kotlin
managedAi {
    ollama {
        host = "localhost"
        port = 11434
        installationStrategy = OllamaInstallationStrategy.PREFER_EXISTING
    }
}
```

#### Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `host` | `String` | `"localhost"` | Ollama server host |
| `port` | `Int` | `11434` | Ollama server port |
| `installationStrategy` | `OllamaInstallationStrategy` | `PREFER_EXISTING` | Installation strategy |
| `installPath` | `String?` | `null` | Custom installation path |
| `additionalArgs` | `List<String>` | Empty | Additional arguments for Ollama |
| `allowPortChange` | `Boolean` | `true` | Allow automatic port changes if port is busy |
| `gracefulShutdown` | `Boolean` | `true` | Use graceful shutdown |
| `shutdownTimeout` | `Int` | `30` | Shutdown timeout in seconds |

### OllamaModelConfiguration

Configuration for individual AI models.

```kotlin
managedAi {
    models {
        "llama3" {
            version = "8b"
            preload = true
        }
        "codellama" {
            version = "7b"
            preload = false
        }
    }
}
```

#### Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `version` | `String` | `"latest"` | Model version |
| `preload` | `Boolean` | `false` | Whether to preload during configuration |

#### Methods

| Method | Description |
|--------|-------------|
| `getName()` | Returns the model name |

## Installation Strategies

### OllamaInstallationStrategy

Enumeration of available installation strategies:

| Strategy | Description |
|----------|-------------|
| `PREFER_EXISTING` | Use existing installation if available, otherwise install isolated |
| `ISOLATED_ONLY` | Always install in isolated/per-project environment |
| `PREFER_EXISTING_THEN_SYSTEM_WIDE` | Use existing installation, otherwise install system-wide |
| `SYSTEM_WIDE_ONLY` | Always install system-wide |
| `FULL_PRIORITY` | Try all options: existing → isolated → system-wide |

## Task Extensions

### useManagedAi()

Extension function to make tasks depend on managed AI services.

```kotlin
tasks.withType<Test> {
    useManagedAi()
}
```

This automatically:
- Adds dependency on `setupManagedAi` task
- Adds `teardownManagedAi` as finalizer
- Ensures AI services are available during task execution

### useManagedAi(configure)

Extension function with custom configuration.

```kotlin
tasks.withType<Test> {
    useManagedAi {
        autoTeardown = false  // Don't automatically stop after task
    }
}
```

## Built-in Tasks

### Core Lifecycle Tasks

#### setupManagedAi
- **Group**: `ai`
- **Description**: Start Ollama and ensure all models are available
- **Dependencies**: `startOllama`, all `pullModel*` tasks
- **Usage**: Automatically used by `useManagedAi()`

#### teardownManagedAi
- **Group**: `ai`
- **Description**: Stop Ollama and cleanup resources
- **Dependencies**: `stopOllama`
- **Usage**: Automatically used by `useManagedAi()`

### Ollama Service Tasks

#### startOllama
- **Group**: `ai`
- **Description**: Start the Ollama service
- **Configuration**: Uses `ollama` configuration block

#### stopOllama
- **Group**: `ai`
- **Description**: Stop the Ollama service
- **Configuration**: Uses `ollama` configuration block

#### ollamaStatus
- **Group**: `ai`
- **Description**: Check Ollama status and list available models
- **Output**: Displays service status and model information

### Model Management Tasks

#### pullModel{ModelName}
- **Group**: `ai`
- **Description**: Pull specific model (auto-generated for each configured model)
- **Dependencies**: `startOllama`
- **Example**: `pullModelLlama3` for a model named "llama3"

#### preloadModels
- **Group**: `ai`
- **Description**: Preload models marked with `preload = true`
- **Dependencies**: `startOllama`, relevant `pullModel*` tasks

## Environment Variables

GMAI respects several environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `OLLAMA_HOST` | Ollama server host | `localhost` |
| `OLLAMA_PORT` | Ollama server port | `11434` |
| `CI` | CI environment detection | Used for strategy selection |

## Examples

### Basic Configuration

```kotlin
managedAi {
    models {
        "llama3" {
            version = "8b"
        }
    }
}
```

### Advanced Configuration

```kotlin
managedAi {
    timeout = Duration.ofMinutes(10)
    autoStart = true
    autoInstall = true
    
    ollama {
        host = "localhost"
        port = 11434
        installationStrategy = OllamaInstallationStrategy.PREFER_EXISTING
        allowPortChange = true
        gracefulShutdown = true
        shutdownTimeout = 30
    }
    
    models {
        "llama3" {
            version = "8b"
            preload = true
        }
        "codellama" {
            version = "7b"
            preload = false
        }
    }
}
```

### CI/CD Configuration

```kotlin
managedAi {
    ollama {
        installationStrategy = if (System.getenv("CI") == "true") {
            OllamaInstallationStrategy.ISOLATED_ONLY
        } else {
            OllamaInstallationStrategy.PREFER_EXISTING
        }
    }
}
```
