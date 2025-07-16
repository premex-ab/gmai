---
layout: page
title: "Changelog"
permalink: /changelog/
---

# Changelog

All notable changes to the GMAI project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2025-07-15

### Added
- Initial release of GMAI (Gradle Managed AI) plugin
- Task-dependent lifecycle management for Ollama LLM instances
- Automatic model pulling and caching with Gradle build cache integration
- Core tasks: `setupManagedAi`, `teardownManagedAi`, `startOllama`, `stopOllama`, `ollamaStatus`
- Dynamic model tasks: `pullModel{ModelName}` for each configured model
- Convenience method `useManagedAi()` for easy task integration
- Environment-specific configurations for development, CI, and production
- Resource management with memory, CPU, and GPU configuration
- Lifecycle hooks for custom behavior during AI service management
- Support for custom model parameters and configurations
- Robust error handling and graceful shutdown mechanisms
- Integration with Gradle's build cache for optimal performance
- Reference counting for multiple tasks sharing the same AI instance
- Detailed logging and debugging capabilities

### Features
- **Task Integration**: Seamless integration with existing Gradle tasks
- **Model Management**: Automatic pulling and caching of Ollama models
- **Environment Support**: Different configurations for different environments
- **Resource Control**: Fine-grained resource allocation and management
- **Status Monitoring**: Real-time status checking and health monitoring
- **Lifecycle Management**: Complete lifecycle control of AI services
- **Performance Optimization**: Build cache integration and parallel processing
- **Error Handling**: Comprehensive error handling and recovery mechanisms

### Documentation
- Complete Jekyll-based documentation site
- Getting started guide with installation and basic usage
- Comprehensive configuration guide with all available options
- Advanced features documentation with integration examples
- API reference with detailed class and method documentation
- Practical examples for common use cases
- CI/CD integration examples for GitHub Actions and Jenkins

### Requirements
- Gradle 7.0 or higher
- Java 8 or higher
- Kotlin 1.7 or higher
- Ollama 0.1.0 or higher

### Plugin Details
- **Plugin ID**: `se.premex.gmai`
- **Plugin Class**: `se.premex.gmai.plugin.GradleManagedAiPlugin`
- **Group**: `se.premex`
- **Version**: `1.0.0`

---

## Product Information

**GMAI** is developed and maintained by [Premex AB](https://premex.se), a Swedish software company specializing in innovative development tools and solutions.

For support, issues, or feature requests, please visit our [GitHub repository](https://github.com/premex-ab/gmai).
