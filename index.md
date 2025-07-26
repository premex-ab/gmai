---
layout: page
title: "GMAI"
---

# Gradle Managed AI

**A Gradle plugin that automatically manages Ollama LLM instances for your build tasks**

GMAI (Gradle Managed AI) is a Gradle plugin that seamlessly integrates AI capabilities into your build process by automatically managing Ollama instances. It handles the entire lifecycle of AI services - from installation and startup to model management and cleanup - so you can focus on using AI in your tasks.

## 🚀 GitHub Repository

[![View on GitHub](https://img.shields.io/badge/View%20on-GitHub-blue?style=for-the-badge&logo=github)](https://github.com/premex-ab/gmai)
[![Stars](https://img.shields.io/github/stars/premex-ab/gmai?style=for-the-badge)](https://github.com/premex-ab/gmai/stargazers)
[![Fork](https://img.shields.io/github/forks/premex-ab/gmai?style=for-the-badge)](https://github.com/premex-ab/gmai/fork)

**👆 Star, fork, or contribute to GMAI on GitHub! Found an issue? [Report it here](https://github.com/premex-ab/gmai/issues)**

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
    id("se.premex.gmai") version "0.0.1"
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

## Get Started

1. **[Getting Started](getting-started.md)** - Add GMAI to your project and run your first AI-powered task
2. **[Configuration](configuration.md)** - Customize installation strategies and model configurations
3. **[Examples](examples.md)** - Real-world usage patterns and best practices
4. **[API Reference](api-reference.md)** - Complete API documentation

## Available Tasks

GMAI provides several built-in tasks for managing AI services:

- `setupManagedAi` - Start Ollama and ensure all models are available
- `teardownManagedAi` - Stop Ollama and cleanup resources
- `startOllama` - Start the Ollama service
- `stopOllama` - Stop the Ollama service
- `ollamaStatus` - Check Ollama status and list available models
- `pullModel{ModelName}` - Pull specific models (auto-generated for each configured model)

## Community & Support

**Love GMAI? Help us grow the community!**

[![GitHub stars](https://img.shields.io/github/stars/premex-ab/gmai?style=social)](https://github.com/premex-ab/gmai/stargazers)
[![GitHub forks](https://img.shields.io/github/forks/premex-ab/gmai?style=social)](https://github.com/premex-ab/gmai/fork)
[![GitHub issues](https://img.shields.io/github/issues/premex-ab/gmai)](https://github.com/premex-ab/gmai/issues)

- **⭐ [Star us on GitHub](https://github.com/premex-ab/gmai)** - Show your support and help others discover GMAI
- **🍴 [Fork the repository](https://github.com/premex-ab/gmai/fork)** - Contribute to the project or create your own version
- **🐛 [Report issues](https://github.com/premex-ab/gmai/issues)** - Found a bug or have a feature request? Let us know!
- **💬 [View the source code](https://github.com/premex-ab/gmai)** - Explore the implementation and contribute

## Product by Premex

GMAI is developed and maintained by [Premex](https://premex.se), a company specializing in innovative software solutions.

---

[Get Started →](getting-started.md){: .btn .btn-primary}
[View Examples →](examples.md){: .btn .btn-outline}
[⭐ Star on GitHub →](https://github.com/premex-ab/gmai){: .btn .btn-outline}
