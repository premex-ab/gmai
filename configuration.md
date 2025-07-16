---
layout: page
title: "Configuration"
permalink: /configuration/
---

# Configuration Guide

This guide covers all configuration options available in the GMAI plugin.

## Basic Configuration

The simplest configuration just defines the models you want to use:

```kotlin
managedAi {
    models {
        "llama3" {
            version = "8b"
        }
    }
}
```

## Global Settings

### Auto-Installation and Auto-Start

```kotlin
managedAi {
    autoInstall = true   // Automatically install Ollama if not found
    autoStart = true     // Automatically start Ollama when needed
    timeout = Duration.ofMinutes(10)  // Global timeout for operations
}
```

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `autoInstall` | `Boolean` | `true` | Automatically install Ollama if not present |
| `autoStart` | `Boolean` | `true` | Automatically start Ollama service |
| `timeout` | `Duration` | 5 minutes | Global timeout for operations |

## Ollama Configuration

### Basic Ollama Settings

```kotlin
managedAi {
    ollama {
        host = "localhost"
        port = 11434
        allowPortChange = true
        gracefulShutdown = true
        shutdownTimeout = 30
    }
}
```

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `host` | `String` | `"localhost"` | Ollama server host |
| `port` | `Int` | `11434` | Ollama server port |
| `allowPortChange` | `Boolean` | `true` | Allow automatic port changes if port is busy |
| `gracefulShutdown` | `Boolean` | `true` | Use graceful shutdown |
| `shutdownTimeout` | `Int` | `30` | Shutdown timeout in seconds |
| `additionalArgs` | `List<String>` | Empty | Additional arguments for Ollama |

### Installation Strategy Configuration

GMAI supports multiple installation strategies for Ollama:

```kotlin
managedAi {
    ollama {
        installationStrategy = OllamaInstallationStrategy.PREFER_EXISTING
        installPath = "custom-ollama-location/ollama"  // Optional custom path
    }
}
```

#### Available Installation Strategies

| Strategy | Description | Best For |
|----------|-------------|----------|
| `PREFER_EXISTING` | Use existing installation, fall back to project-local | Most development scenarios |
| `ISOLATED_ONLY` | Always install in project directory | CI/CD environments, strict isolation |
| `PREFER_EXISTING_THEN_SYSTEM_WIDE` | Use existing, fall back to system-wide | Development with system-wide preference |
| `SYSTEM_WIDE_ONLY` | Always install system-wide | Shared development machines |
| `FULL_PRIORITY` | Try existing → isolated → system-wide | Maximum flexibility |

#### Strategy Details

**`PREFER_EXISTING` (Default - Recommended)**
- Uses existing Ollama installations when available
- Falls back to project directory installation (`.ollama/bin/ollama`)
- Best for most development scenarios - clean and efficient

**`ISOLATED_ONLY`**
- Always installs in project directory
- Ignores existing installations
- Best for CI/CD environments and strict isolation requirements

**`PREFER_EXISTING_THEN_SYSTEM_WIDE`**
- Uses existing installation first
- Falls back to system-wide installation (homebrew, package manager)
- Best for development environments where system-wide installation is preferred

**`SYSTEM_WIDE_ONLY`**
- Always installs system-wide
- Ignores existing installations and project-local options
- Best for shared development machines or system administration

**`FULL_PRIORITY`**
- Tries all options in order: existing → isolated → system-wide
- Maximum flexibility and compatibility
- Best for complex environments with varying requirements

## Model Configuration

### Basic Model Setup

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

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `version` | `String` | `"latest"` | Model version/tag |
| `preload` | `Boolean` | `false` | Whether to preload during configuration phase |

### Model Names and Versioning

Model names follow Ollama's naming convention:
- `"llama3"` - Uses latest version
- `"llama3:8b"` - Specific version tag
- `"codellama:7b-instruct"` - Specific variant

### Preloading Models

Models marked with `preload = true` are pulled during the configuration phase:

```kotlin
managedAi {
    models {
        "llama3" {
            version = "8b"
            preload = true  // Downloaded during build configuration
        }
    }
}
```

This is useful for:
- Ensuring models are available before any tasks run
- CI/CD environments where you want to fail fast if models can't be downloaded
- Speeding up subsequent task executions

## Environment-Specific Configuration

### CI/CD Configuration

```kotlin
managedAi {
    ollama {
        installationStrategy = if (System.getenv("CI") == "true") {
            OllamaInstallationStrategy.ISOLATED_ONLY  // CI: Always isolated
        } else {
            OllamaInstallationStrategy.PREFER_EXISTING  // Dev: Use existing
        }
    }
}
```

### Development vs Production

```kotlin
managedAi {
    timeout = if (System.getenv("CI") == "true") {
        Duration.ofMinutes(15)  // Longer timeout for CI
    } else {
        Duration.ofMinutes(5)   // Shorter timeout for development
    }
    
    models {
        "llama3" {
            version = "8b"
            preload = System.getenv("CI") == "true"  // Preload in CI only
        }
    }
}
```

## Project Directory Structure

With default settings, GMAI creates this structure:

```
your-project/
├── .ollama/
│   ├── bin/
│   │   └── ollama          # Project-specific Ollama binary
│   └── models/             # Downloaded models (managed by Ollama)
├── build.gradle.kts
└── src/
```

### Benefits of Project-Local Installation

- **True Project Isolation**: Each project gets its own Ollama instance
- **Version Control Friendly**: Add `.ollama/` to `.gitignore`
- **Easy Cleanup**: Delete project directory to remove everything
- **Team Consistency**: Same structure for all team members
- **No System Pollution**: Doesn't affect system-wide installations

## Advanced Configuration

### Custom Installation Path

```kotlin
managedAi {
    ollama {
        installationStrategy = OllamaInstallationStrategy.ISOLATED_ONLY
        installPath = "tools/ai/ollama"  // Custom location
    }
}
```

### Additional Ollama Arguments

```kotlin
managedAi {
    ollama {
        additionalArgs = listOf("--verbose", "--debug")
    }
}
```

### Port Configuration

```kotlin
managedAi {
    ollama {
        port = 11434
        allowPortChange = true  // If port is busy, try next available
    }
}
```

## Configuration Examples

### Minimal Configuration

```kotlin
managedAi {
    models {
        "llama3" { version = "8b" }
    }
}
```

### Full Configuration

```kotlin
managedAi {
    autoInstall = true
    autoStart = true
    timeout = Duration.ofMinutes(10)
    
    ollama {
        host = "localhost"
        port = 11434
        installationStrategy = OllamaInstallationStrategy.PREFER_EXISTING
        allowPortChange = true
        gracefulShutdown = true
        shutdownTimeout = 30
        additionalArgs = listOf("--verbose")
    }
    
    models {
        "llama3" {
            version = "8b"
            preload = true
        }
        "codellama" {
            version = "7b-instruct"
            preload = false
        }
    }
}
```

### Multi-Environment Configuration

```kotlin
managedAi {
    val isCI = System.getenv("CI") == "true"
    
    timeout = if (isCI) Duration.ofMinutes(15) else Duration.ofMinutes(5)
    
    ollama {
        installationStrategy = if (isCI) {
            OllamaInstallationStrategy.ISOLATED_ONLY
        } else {
            OllamaInstallationStrategy.PREFER_EXISTING
        }
        allowPortChange = true
    }
    
    models {
        "llama3" {
            version = "8b"
            preload = isCI  // Only preload in CI
        }
    }
}
```
