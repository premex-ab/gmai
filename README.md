# Gradle Managed AI

[![Latest Release](https://img.shields.io/github/v/release/premex-ab/gmai?style=flat-square&logo=github)](https://github.com/premex-ab/gmai/releases/latest)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/se.premex.gmai?style=flat-square&logo=gradle)](https://plugins.gradle.org/plugin/se.premex.gmai)

**A Gradle plugin that automatically manages Ollama LLM instances for your build tasks**

GMAI (Gradle Managed AI) is a Gradle plugin that seamlessly integrates AI capabilities into your build process by automatically managing Ollama instances. It handles the entire lifecycle of AI services - from installation and startup to model management and cleanup - so you can focus on using AI in your tasks.

## What GMAI Does

- **Automatic Ollama Management**: Installs, starts, and stops Ollama automatically based on your build needs
- **Task Integration**: Simple API to make any Gradle task depend on AI services with `useManagedAi()`
- **Model Management**: Automatically pulls and manages AI models defined in your configuration
- **Lifecycle Management**: Ensures AI services are available when needed and cleaned up afterward
- **Cross-Platform**: Works on macOS, Linux, and Windows with automatic platform detection

## Key Features

### Task-Dependent AI Services
Tasks can declare dependencies on AI services, and GMAI handles everything automatically:

```kotlin
tasks.withType<Test> {
    useManagedAi()  // AI services start before tests, stop after
}
```

### Smart Installation
GMAI finds existing Ollama installations or installs per-project for isolation:
- Uses existing installations when available
- Falls back to project-local installation (`.ollama/bin/ollama`)
- Configurable installation strategies for different environments

### Automatic Model Management
Define models in your build script and GMAI handles the rest:

```kotlin
managedAi {
    models {
        "llama3" {
            version = "8b"
        }
        "codellama" {
            version = "7b"
        }
    }
}
```

## Quick Start

```kotlin
// build.gradle.kts
plugins {
    id("se.premex.gmai") version "0.0.2"
}

managedAi {
    models {
        "llama3" {
            version = "8b"
        }
    }
}

// Use AI in your tasks
tasks.withType<Test> {
    useManagedAi()
    systemProperty("ollama.url", "http://localhost:11434")
}
```

## Use Cases

- **AI-Powered Testing**: Use LLMs in your test suites for dynamic test generation or validation
- **Code Generation**: Generate code during build time using AI models
- **Documentation**: Generate or validate documentation with AI assistance
- **CI/CD Integration**: Run AI-powered tasks in continuous integration environments

## Why GMAI?

- **Zero Configuration**: Works out of the box with sensible defaults
- **Build Integration**: Native Gradle task dependencies and lifecycle management
- **Team Consistency**: Same AI environment for all team members
- **CI/CD Ready**: Designed for continuous integration environments
- **Isolation**: Project-specific installations don't interfere with system setup

## Available Tasks

GMAI provides several built-in tasks for managing AI services:

- `setupManagedAi` - Start Ollama and ensure all models are available
- `teardownManagedAi` - Stop Ollama and cleanup resources
- `startOllama` - Start the Ollama service
- `stopOllama` - Stop the Ollama service
- `ollamaStatus` - Check Ollama status and list available models
- `pullModel{ModelName}` - Pull specific models (auto-generated for each configured model)

## Development

### Project Structure

- `plugin/` - Main plugin source code
- `plugin/src/main/kotlin/` - Plugin implementation
- `plugin/src/test/kotlin/` - Unit tests
- `plugin/src/functionalTest/kotlin/` - Integration tests

### Local Development

To test the plugin locally:

```bash
# Run tests
./gradlew :plugin:check

# Validate plugin
./gradlew :plugin:validatePlugins

# Publish to local repository
./gradlew :plugin:publishToMavenLocal
```

### Building

```bash
# Build the plugin
./gradlew :plugin:build

# Run all tests
./gradlew :plugin:check
```

## Community & Support

[![GitHub stars](https://img.shields.io/github/stars/premex-ab/gmai?style=social)](https://github.com/premex-ab/gmai/stargazers)
[![GitHub forks](https://img.shields.io/github/forks/premex-ab/gmai?style=social)](https://github.com/premex-ab/gmai/fork)
[![GitHub issues](https://img.shields.io/github/issues/premex-ab/gmai)](https://github.com/premex-ab/gmai/issues)

- **⭐ [Star this repository](https://github.com/premex-ab/gmai)** if you find GMAI useful
- **🍴 [Fork the repository](https://github.com/premex-ab/gmai/fork)** to contribute or customize
- **🐛 [Report issues](https://github.com/premex-ab/gmai/issues)** for bugs or feature requests
- **📖 [Read the documentation](https://gmai.premex.se/)** for comprehensive guides

## Product by Premex

GMAI is developed and maintained by [Premex](https://premex.se), a company specializing in innovative software solutions.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
