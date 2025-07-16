---
layout: page
title: "Getting Started"
permalink: /getting-started/
---

# Getting Started with GMAI

This guide will help you set up and use the Gradle Managed AI plugin in your project.

## Installation

### Step 1: Apply the Plugin

Add the GMAI plugin to your `build.gradle.kts`:

```kotlin
plugins {
    id("se.premex.gmai") version "0.0.1"
}
```

Or using the legacy plugin syntax:

```kotlin
buildscript {
    dependencies {
        classpath("se.premex:gmai-plugin:1.0.0")
    }
}

apply(plugin = "se.premex.gmai")
```

### Step 2: Configure Models

Configure the AI models you want to use:

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

### Step 3: Use AI in Your Tasks

The simplest way to use GMAI is with the `useManagedAi()` extension:

```kotlin
tasks.withType<Test> {
    useManagedAi()
    systemProperty("ollama.url", "http://localhost:11434")
}
```

## How GMAI Works

GMAI automatically manages the lifecycle of Ollama and your AI models:

1. **Installation**: Finds existing Ollama or installs it locally in your project
2. **Startup**: Starts Ollama service when your tasks need it
3. **Model Management**: Downloads and manages the models you've configured
4. **Cleanup**: Stops Ollama and cleans up after your tasks complete

## Basic Usage

### Using AI in Tasks

The `useManagedAi()` extension automatically handles the AI lifecycle:

```kotlin
tasks.withType<Test> {
    useManagedAi()
    // Your task now has access to Ollama at http://localhost:11434
}
```

This automatically:
- Starts Ollama before the task runs
- Ensures all configured models are available
- Stops Ollama after the task completes

### Manual Task Dependencies

For more control, you can manually declare dependencies:

```kotlin
tasks.named("myAiTask") {
    dependsOn("setupManagedAi")
    finalizedBy("teardownManagedAi")
}
```

### Available Tasks

GMAI provides several built-in tasks:

- `setupManagedAi` - Start Ollama and ensure all models are available
- `teardownManagedAi` - Stop Ollama and cleanup
- `startOllama` - Start Ollama service only
- `stopOllama` - Stop Ollama service only
- `ollamaStatus` - Check Ollama status and list models
- `pullModel{ModelName}` - Pull specific model (auto-generated for each configured model)
- `preloadModels` - Preload models marked with `preload = true`

## First Steps

### 1. Run a Status Check

After applying the plugin, check if everything is working:

```bash
./gradlew ollamaStatus
```

This will show you:
- Whether Ollama is installed and running
- Which models are available
- Connection information

### 2. Pull Your Models

Pull the models you've configured:

```bash
./gradlew pullModelLlama3
```

Or pull all models at once:

```bash
./gradlew setupManagedAi
```

### 3. Use AI in Your Code

With Ollama running, you can use it in your application code:

```kotlin
// In your test or application code
val client = OkHttpClient()
val request = Request.Builder()
    .url("http://localhost:11434/api/generate")
    .post(RequestBody.create(
        MediaType.parse("application/json"),
        """{"model": "llama3:8b", "prompt": "Hello, world!"}"""
    ))
    .build()

val response = client.newCall(request).execute()
```

## Example Project

Here's a complete example of a project using GMAI:

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") version "1.9.0"
    id("se.premex.gmai") version "0.0.1"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
}

managedAi {
    models {
        "llama3" {
            version = "8b"
        }
    }
}

tasks.withType<Test> {
    useManagedAi()
    systemProperty("ollama.url", "http://localhost:11434")
}
```

```kotlin
// src/test/kotlin/AiTest.kt
import kotlin.test.Test
import kotlin.test.assertNotNull
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType

class AiTest {
    
    @Test
    fun testAiConnection() {
        val client = OkHttpClient()
        val json = """{"model": "llama3:8b", "prompt": "Say hello"}"""
        
        val request = Request.Builder()
            .url("http://localhost:11434/api/generate")
            .post(RequestBody.create("application/json".toMediaType(), json))
            .build()
        
        val response = client.newCall(request).execute()
        assertNotNull(response.body)
        println("AI Response: ${response.body?.string()}")
    }
}
```

## Project Structure

After running GMAI, your project structure will look like this:

```
your-project/
├── .ollama/                    # Project-local Ollama (if using default strategy)
│   ├── bin/
│   │   └── ollama             # Ollama executable
│   └── models/                # Downloaded models
├── build.gradle.kts
├── src/
│   └── test/
│       └── kotlin/
│           └── AiTest.kt
└── .gitignore                 # Should include .ollama/
```

## Common Commands

### Development Commands

```bash
# Start Ollama and pull all models
./gradlew setupManagedAi

# Check status
./gradlew ollamaStatus

# Run tests with AI
./gradlew test

# Stop Ollama
./gradlew stopOllama
```

### Model Management

```bash
# Pull specific model
./gradlew pullModelLlama3

# Pull all models
./gradlew setupManagedAi

# Preload models marked with preload=true
./gradlew preloadModels
```

## Configuration Tips

### For Development

Use the default configuration for development:

```kotlin
managedAi {
    models {
        "llama3" { version = "8b" }
    }
}
```

### For CI/CD

Use isolated installation for CI:

```kotlin
managedAi {
    ollama {
        installationStrategy = if (System.getenv("CI") == "true") {
            OllamaInstallationStrategy.ISOLATED_ONLY
        } else {
            OllamaInstallationStrategy.PREFER_EXISTING
        }
    }
    
    models {
        "llama3" {
            version = "8b"
            preload = System.getenv("CI") == "true"  // Preload in CI
        }
    }
}
```

## Troubleshooting

### Ollama Not Starting

1. Check if port 11434 is available
2. Run `./gradlew ollamaStatus --info` for detailed logs
3. Try a different port in configuration

### Models Not Downloading

1. Check internet connection
2. Verify model names are correct
3. Increase timeout in configuration

### Permission Issues

1. Ensure `.ollama/` directory is writable
2. Check file permissions on project directory
3. Try running with different installation strategy

## Next Steps

1. **[Configuration](configuration.md)** - Learn about advanced configuration options
2. **[Examples](examples.md)** - See real-world usage examples
3. **[API Reference](api-reference.md)** - Complete API documentation
4. **[Advanced Features](advanced-features.md)** - Custom integrations and advanced usage

## Need Help?

- Check the [examples](examples.md) for common usage patterns
- Review the [configuration guide](configuration.md) for advanced options
- See the [API reference](api-reference.md) for complete documentation
