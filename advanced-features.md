---
layout: page
title: "Advanced Features"
permalink: /advanced-features/
---

# Advanced Features

This guide covers advanced features and integrations available in the GMAI plugin.

## Custom Task Integration

### Creating AI-Powered Tasks

Create custom tasks that leverage managed AI services:

```kotlin
abstract class AiGenerationTask : DefaultTask() {
    @get:Input
    abstract val prompt: Property<String>
    
    @get:Input
    abstract val model: Property<String>
    
    @get:OutputFile
    abstract val outputFile: RegularFileProperty
    
    @TaskAction
    fun generate() {
        // Use Ollama API to generate content
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("http://localhost:11434/api/generate")
            .post("""
                {
                    "model": "${model.get()}",
                    "prompt": "${prompt.get()}",
                    "stream": false
                }
            """.trimIndent().toRequestBody("application/json".toMediaType()))
            .build()
            
        client.newCall(request).execute().use { response ->
            val result = response.body?.string()
            outputFile.get().asFile.writeText(result ?: "")
        }
    }
}

// Register the task
tasks.register<AiGenerationTask>("generateCode") {
    prompt.set("Generate a Kotlin data class for a User")
    model.set("codellama:7b")
    outputFile.set(layout.buildDirectory.file("generated/User.kt"))
    
    // Ensure AI is available
    useManagedAi()
}
```

### Custom Model Management

Implement custom model management strategies:

```kotlin
managedAi {
    // Custom model resolver
    modelResolver = { modelName ->
        when {
            modelName.startsWith("custom:") -> {
                CustomModelConfiguration(
                    name = modelName.removePrefix("custom:"),
                    source = "https://my-models.com/",
                    verificationStrategy = CustomVerificationStrategy()
                )
            }
            else -> DefaultModelConfiguration(modelName)
        }
    }
    
    // Model lifecycle hooks
    lifecycle {
        onModelPull = { modelName ->
            // Custom model pull logic
            if (modelName.startsWith("custom:")) {
                downloadCustomModel(modelName)
            }
        }
        
        onModelReady = { modelName ->
            // Warm up model
            warmUpModel(modelName)
        }
    }
}
```

## Integration with Testing Frameworks

### JUnit 5 Integration

Create a JUnit 5 extension for AI testing:

```kotlin
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(ManagedAiExtension::class)
annotation class WithManagedAi(
    val models: Array<String> = [],
    val environment: String = "default"
)

class ManagedAiExtension : BeforeAllCallback, AfterAllCallback {
    override fun beforeAll(context: ExtensionContext) {
        // Ensure AI is running before tests
        ensureAiIsRunning()
    }
    
    override fun afterAll(context: ExtensionContext) {
        // Optional: cleanup after tests
        cleanupAiResources()
    }
}

// Usage in tests
@WithManagedAi(models = ["llama3:8b"])
class AiPoweredTest {
    
    @Test
    fun testAiGeneration() {
        // Test uses managed AI
        val client = OllamaClient("http://localhost:11434")
        val response = client.generate("llama3:8b", "Hello, AI!")
        assertThat(response).isNotEmpty()
    }
}
```

### Gradle Test Integration

Configure test tasks to use different AI environments:

```kotlin
tasks.test {
    useManagedAi()
    
    // Use development environment for unit tests
    systemProperty("gmai.environment", "development")
    
    // Pass AI configuration to tests
    systemProperty("ollama.url", "http://localhost:11434")
    systemProperty("ollama.model", "llama3:8b")
}

tasks.register<Test>("integrationTest") {
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    
    // Use production-like environment for integration tests
    useManagedAi()
    systemProperty("gmai.environment", "integration")
}
```

## CI/CD Integration

### GitHub Actions

Configure GitHub Actions to use GMAI:

```yaml
# .github/workflows/test.yml
name: Test with AI

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 11
      uses: actions/setup-java@v3
      with:
        java-version: '11'
        distribution: 'temurin'
    
    - name: Install Ollama
      run: |
        curl -fsSL https://ollama.ai/install.sh | sh
        ollama serve &
    
    - name: Run tests with AI
      run: |
        ./gradlew test -Dgmai.environment=ci
      env:
        GMAI_OLLAMA_HOST: localhost
        GMAI_OLLAMA_PORT: 11434
```

### Jenkins Pipeline

Configure Jenkins to use GMAI:

```groovy
pipeline {
    agent any
    
    environment {
        OLLAMA_HOST = 'localhost'
        OLLAMA_PORT = '11434'
    }
    
    stages {
        stage('Setup') {
            steps {
                sh 'curl -fsSL https://ollama.ai/install.sh | sh'
                sh 'ollama serve &'
            }
        }
        
        stage('Test') {
            steps {
                sh './gradlew test'
            }
        }
    }
    
    post {
        always {
            sh 'pkill -f ollama || true'
        }
    }
}
```

## Custom Integrations

### Spring Boot Integration

Create a Spring Boot starter for GMAI:

```kotlin
@Configuration
@EnableConfigurationProperties(GmaiProperties::class)
class GmaiAutoConfiguration {
    
    @Bean
    @ConditionalOnProperty(prefix = "gmai", name = ["enabled"], havingValue = "true")
    fun ollamaClient(properties: GmaiProperties): OllamaClient {
        return OllamaClient(
            host = properties.host,
            port = properties.port,
            timeout = properties.timeout
        )
    }
    
    @Bean
    @ConditionalOnBean(OllamaClient::class)
    fun aiService(client: OllamaClient): AiService {
        return AiService(client)
    }
}

@ConfigurationProperties(prefix = "gmai")
data class GmaiProperties(
    val enabled: Boolean = true,
    val host: String = "localhost",
    val port: Int = 11434,
    val timeout: Duration = Duration.ofSeconds(30),
    val defaultModel: String = "llama3:8b"
)
```

### Micronaut Integration

Create a Micronaut integration:

```kotlin
@Factory
class GmaiFactory {
    
    @Singleton
    @Requires(property = "gmai.enabled", value = "true")
    fun ollamaClient(@ConfigurationProperties("gmai") config: GmaiConfiguration): OllamaClient {
        return OllamaClient(config.host, config.port)
    }
}

@ConfigurationProperties("gmai")
class GmaiConfiguration {
    var enabled: Boolean = true
    var host: String = "localhost"
    var port: Int = 11434
    var defaultModel: String = "llama3:8b"
}
```

## Performance Optimization

### Model Caching Strategies

Implement advanced model caching:

```kotlin
managedAi {
    caching {
        // Cache models locally
        localCache = true
        localCacheDir = file("~/.gmai/models")
        
        // Share cache across projects
        sharedCache = true
        sharedCacheDir = file("~/.gradle/gmai-cache")
        
        // Cache validation
        validateCacheOnStartup = true
        cacheExpiryDays = 30
        
        // Parallel model loading
        parallelModelLoading = true
        maxParallelDownloads = 3
    }
}
```

## Monitoring and Observability

### Metrics Collection

Integrate with monitoring systems:

```kotlin
managedAi {
    monitoring {
        // Prometheus metrics
        prometheus {
            enabled = true
            port = 9090
            path = "/metrics"
        }
        
        // Custom metrics
        customMetrics = listOf(
            MetricDefinition("ai_task_duration", "Duration of AI tasks"),
            MetricDefinition("model_usage", "Model usage statistics")
        )
        
        // Health checks
        healthChecks {
            enabled = true
            interval = Duration.ofSeconds(30)
            timeout = Duration.ofSeconds(10)
        }
    }
}
```

### Distributed Tracing

Enable distributed tracing:

```kotlin
managedAi {
    tracing {
        // OpenTelemetry integration
        openTelemetry {
            enabled = true
            serviceName = "gmai-gradle-plugin"
            endpoint = "http://jaeger:14268/api/traces"
        }
        
        // Custom spans
        customSpans = listOf(
            "model_pull",
            "ai_generation",
            "model_inference"
        )
    }
}
```

## Security Features

### API Key Management

Secure API key handling:

```kotlin
managedAi {
    security {
        // API key configuration
        apiKey {
            source = "environment" // environment, file, vault
            environmentVariable = "OLLAMA_API_KEY"
            keyFile = file("~/.gmai/api-key")
        }
        
        // SSL/TLS configuration
        ssl {
            enabled = true
            trustStore = file("~/.gmai/truststore.jks")
            trustStorePassword = "changeit"
        }
        
        // Authentication
        authentication {
            type = "bearer" // bearer, basic, custom
            tokenProvider = CustomTokenProvider()
        }
    }
}
```

### Network Security

Configure network security:

```kotlin
managedAi {
    network {
        // Firewall rules
        firewall {
            allowedHosts = listOf("localhost", "127.0.0.1")
            allowedPorts = listOf(11434)
        }
        
        // Proxy configuration
        proxy {
            host = "proxy.company.com"
            port = 8080
            username = "user"
            password = "pass"
        }
        
        // Rate limiting
        rateLimiting {
            enabled = true
            requestsPerSecond = 10
            burstSize = 20
        }
    }
}
```

## Plugin Development

### Custom Extensions

Create custom plugin extensions:

```kotlin
interface GmaiExtension {
    fun configure(project: Project, managedAi: ManagedAiExtension)
}

class CustomGmaiExtension : GmaiExtension {
    override fun configure(project: Project, managedAi: ManagedAiExtension) {
        // Custom configuration logic
        managedAi.models {
            "custom-model" {
                version = "1.0"
                // Custom model configuration
            }
        }
    }
}

// Apply extension
managedAi {
    extensions.add(CustomGmaiExtension())
}
```

### Plugin Hooks

Implement plugin hooks for custom behavior:

```kotlin
class CustomGmaiPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply("se.premex.gmai")
        
        val managedAi = project.extensions.getByType<ManagedAiExtension>()
        
        // Add custom hooks
        managedAi.lifecycle {
            beforeStart = {
                // Custom pre-start logic
                setupCustomEnvironment()
            }
            
            afterStop = {
                // Custom post-stop logic
                cleanupCustomResources()
            }
        }
    }
}
```

## Next Steps

- Check the [API reference](api-reference.md) for detailed API documentation
- Learn about [plugin development](plugin-development.md) for extending GMAI
- Explore [examples](examples.md) for real-world use cases
