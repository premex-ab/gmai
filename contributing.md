---
layout: page
title: "Contributing"
permalink: /contributing/
---

# Contributing to GMAI

We welcome contributions to the GMAI project! This guide will help you get started with contributing to the Gradle Managed AI plugin.

## Getting Started

### Prerequisites

- Java 8 or higher
- Gradle 7.0 or higher
- Kotlin 1.7 or higher
- [Ollama](https://ollama.ai) installed locally for testing
- Git

### Development Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/premex-ab/gmai.git
   cd gmai
   ```

2. **Install Ollama**
   ```bash
   # macOS
   brew install ollama
   
   # Linux
   curl -fsSL https://ollama.ai/install.sh | sh
   
   # Windows - Download from https://ollama.ai/download
   ```

3. **Build the project**
   ```bash
   ./gradlew build
   ```

4. **Run tests**
   ```bash
   ./gradlew test
   ```

5. **Run functional tests**
   ```bash
   ./gradlew functionalTest
   ```

## Project Structure

```
gmai/
├── plugin/                 # Main plugin source code
│   ├── src/main/kotlin/    # Plugin implementation
│   ├── src/test/kotlin/    # Unit tests
│   └── src/functionalTest/kotlin/  # Functional tests
├── build-logic/            # Build configuration
├── test-project/           # Test project for manual testing
├── docs/                   # Development documentation
└── *.md                    # Jekyll documentation pages
```

## Development Guidelines

### Code Style

- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Add KDoc comments for public APIs
- Keep functions focused and small
- Use data classes for configuration objects

### Testing

- Write unit tests for all new functionality
- Include functional tests for end-to-end scenarios
- Test both success and failure cases
- Mock external dependencies in unit tests
- Use real Ollama instances in functional tests

### Documentation

- Update relevant documentation pages
- Add KDoc comments to public APIs
- Include usage examples in documentation
- Update the changelog for notable changes

## Types of Contributions

### Bug Reports

When reporting bugs, please include:
- GMAI version
- Gradle version
- Java/Kotlin version
- Ollama version
- Operating system
- Steps to reproduce
- Expected vs actual behavior
- Relevant logs or stack traces

### Feature Requests

For new features, please:
- Check if the feature already exists
- Describe the use case and motivation
- Provide examples of how it would be used
- Consider backwards compatibility

### Code Contributions

1. **Fork the repository**
2. **Create a feature branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

3. **Make your changes**
   - Follow the code style guidelines
   - Add or update tests
   - Update documentation

4. **Test your changes**
   ```bash
   ./gradlew test functionalTest
   ```

5. **Commit your changes**
   ```bash
   git commit -m "feat: add new feature description"
   ```

6. **Push to your fork**
   ```bash
   git push origin feature/your-feature-name
   ```

7. **Create a pull request**

### Commit Message Format

We use [Conventional Commits](https://www.conventionalcommits.org/):

```
type(scope): description

[optional body]

[optional footer]
```

Types:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

Examples:
```
feat(plugin): add support for custom model parameters
fix(lifecycle): resolve memory leak in service shutdown
docs(readme): update installation instructions
test(integration): add tests for multi-environment setup
```

## Pull Request Process

1. **Check the checklist**
   - [ ] Code follows style guidelines
   - [ ] Tests pass locally
   - [ ] Documentation is updated
   - [ ] Changelog is updated (for notable changes)
   - [ ] No merge conflicts

2. **Pull request description**
   - Describe what changed and why
   - Reference any related issues
   - Include testing instructions
   - Add screenshots if applicable

3. **Review process**
   - All PRs require at least one review
   - Address feedback and update as needed
   - Maintainers will merge approved PRs

## Testing Guidelines

### Unit Tests

```kotlin
class ModelManagerTest {
    @Test
    fun `should pull model successfully`() {
        // Given
        val mockService = mockk<OllamaService>()
        every { mockService.pullModel("llama3:8b") } returns true
        
        val manager = ModelManager(mockService, mockk())
        
        // When
        val result = manager.pullModel("llama3:8b")
        
        // Then
        assertTrue(result)
        verify { mockService.pullModel("llama3:8b") }
    }
}
```

### Functional Tests

```kotlin
class GmaiPluginFunctionalTest {
    @Test
    fun `should start and stop Ollama successfully`() {
        // Given
        val testProject = createTestProject()
        testProject.buildFile.appendText("""
            managedAi {
                models {
                    "llama3" {
                        version = "8b"
                    }
                }
            }
        """.trimIndent())
        
        // When
        val result = testProject.runner()
            .withArguments("setupManagedAi", "teardownManagedAi")
            .build()
        
        // Then
        assertEquals(TaskOutcome.SUCCESS, result.task(":setupManagedAi")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":teardownManagedAi")?.outcome)
    }
}
```

## Documentation Contributions

### Jekyll Documentation

The documentation is built using Jekyll and hosted on GitHub Pages.

1. **Local development**
   ```bash
   bundle install
   bundle exec jekyll serve
   ```

2. **File structure**
   - `index.md` - Homepage
   - `getting-started.md` - Getting started guide
   - `configuration.md` - Configuration reference
   - `advanced-features.md` - Advanced features
   - `api-reference.md` - API documentation
   - `examples.md` - Usage examples
   - `changelog.md` - Version history

3. **Writing guidelines**
   - Use clear, concise language
   - Include code examples
   - Test all examples
   - Follow the existing structure

### API Documentation

Use KDoc for API documentation:

```kotlin
/**
 * Manages the lifecycle of Ollama AI services.
 *
 * This class provides methods to start, stop, and monitor
 * Ollama services with proper resource management.
 *
 * @param configuration The AI configuration
 * @param project The Gradle project
 * 
 * @since 1.0.0
 */
class LifecycleManager(
    private val configuration: ManagedAiExtension,
    private val project: Project
) {
    /**
     * Starts the AI service with the configured models.
     *
     * @return true if the service started successfully
     * @throws ServiceTimeoutException if the service fails to start within the timeout
     */
    suspend fun start(): Boolean {
        // Implementation
    }
}
```

## Release Process

Releases are managed by the maintainers:

1. **Version bump** in `build.gradle.kts`
2. **Update changelog** with new features and fixes
3. **Create release tag**
4. **Publish to Gradle Plugin Portal**
5. **Update documentation**

## Community Guidelines

### Code of Conduct

- Be respectful and inclusive
- Welcome newcomers and help them learn
- Focus on constructive feedback
- Respect different perspectives and experiences

### Communication

- Use GitHub Issues for bug reports and feature requests
- Use GitHub Discussions for general questions
- Be patient and respectful in all interactions
- Provide clear and detailed information

## Getting Help

If you need help with contributing:

1. Check the [documentation](https://premex-ab.github.io/gmai)
2. Look at existing [issues](https://github.com/premex-ab/gmai/issues)
3. Create a new issue with your question
4. Contact the maintainers

## License

By contributing to GMAI, you agree that your contributions will be licensed under the same license as the project.

---

**Thank you for contributing to GMAI!** Your contributions help make the plugin better for everyone.

*GMAI is a product by [Premex AB](https://premex.se)*
