---
layout: page
title: "Examples"
permalink: /examples/
---

# Examples

This page provides practical examples of using GMAI in real-world scenarios.

## Basic Examples

### Simple AI-Powered Tests

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm")
    id("se.premex.gmai") version "0.0.2"
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
import kotlin.test.assertTrue
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import kotlinx.serialization.json.*

class AiTest {
    
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    
    @Test
    fun testCodeGeneration() {
        val prompt = "Write a simple Kotlin function that adds two numbers"
        val response = callOllama("llama3:8b", prompt)
        
        assertTrue(response.contains("fun"), "Response should contain a Kotlin function")
        assertTrue(response.contains("Int") || response.contains("Number"), "Response should mention number types")
    }
    
    @Test
    fun testCodeExplanation() {
        val code = """
            fun fibonacci(n: Int): Int {
                return if (n <= 1) n else fibonacci(n - 1) + fibonacci(n - 2)
            }
        """.trimIndent()
        
        val prompt = "Explain this Kotlin code: $code"
        val response = callOllama("llama3:8b", prompt)
        
        assertTrue(response.contains("fibonacci"), "Response should mention fibonacci")
        assertTrue(response.contains("recursive"), "Response should mention recursion")
    }
    
    private fun callOllama(model: String, prompt: String): String {
        val requestBody = """
            {
                "model": "$model",
                "prompt": "$prompt",
                "stream": false
            }
        """.trimIndent()
        
        val request = Request.Builder()
            .url("http://localhost:11434/api/generate")
            .post(RequestBody.create("application/json".toMediaType(), requestBody))
            .build()
        
        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: throw RuntimeException("Empty response")
            val jsonResponse = json.parseToJsonElement(body).jsonObject
            return jsonResponse["response"]?.jsonPrimitive?.content ?: ""
        }
    }
}
```

### Code Generation Task

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm")
    id("se.premex.gmai") version "0.0.2"
}

managedAi {
    models {
        "codellama" {
            version = "7b-instruct"
        }
    }
}

tasks.register("generateCode") {
    useManagedAi()
    
    doLast {
        val prompt = "Generate a Kotlin data class for a User with name, email, and age"
        val response = callOllamaApi("codellama:7b-instruct", prompt)
        
        // Save generated code to file
        file("src/main/kotlin/Generated.kt").writeText(response)
        println("Generated code saved to Generated.kt")
    }
}

fun callOllamaApi(model: String, prompt: String): String {
    // Implementation similar to above
    return "// Generated code would be here"
}
```

## Advanced Examples

### Multi-Model AI Pipeline

```kotlin
// build.gradle.kts
managedAi {
    models {
        "llama3" {
            version = "8b"
            preload = true
        }
        "codellama" {
            version = "7b-instruct"
            preload = true
        }
        "mistral" {
            version = "7b"
            preload = false
        }
    }
}

tasks.register("aiPipeline") {
    useManagedAi()
    
    doLast {
        // Step 1: Generate requirements with general model
        val requirements = callOllama("llama3:8b", "Generate requirements for a simple calculator app")
        
        // Step 2: Generate code with specialized model
        val code = callOllama("codellama:7b-instruct", "Create Kotlin code for: $requirements")
        
        // Step 3: Review with different model
        val review = callOllama("mistral:7b", "Review this code for issues: $code")
        
        println("Requirements: $requirements")
        println("Code: $code")
        println("Review: $review")
    }
}
```

### CI/CD Integration

```kotlin
// build.gradle.kts
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
            preload = isCI  // Preload in CI for faster execution
        }
    }
}

tasks.register("ciAiValidation") {
    useManagedAi()
    
    doLast {
        val sourceFiles = fileTree("src/main/kotlin") { include("**/*.kt") }
        
        sourceFiles.forEach { file ->
            val content = file.readText()
            val prompt = "Review this Kotlin code for potential issues: $content"
            val review = callOllama("llama3:8b", prompt)
            
            if (review.contains("issue") || review.contains("problem")) {
                println("⚠️  Potential issues found in ${file.name}:")
                println(review)
            }
        }
    }
}
```

### Custom Task with AI Integration

```kotlin
// build.gradle.kts
abstract class DocumentationTask : DefaultTask() {
    
    @get:InputDirectory
    abstract val sourceDir: DirectoryProperty
    
    @get:OutputFile
    abstract val outputFile: RegularFileProperty
    
    @TaskAction
    fun generateDocumentation() {
        val kotlinFiles = sourceDir.asFileTree.matching { include("**/*.kt") }
        val documentation = StringBuilder()
        
        kotlinFiles.forEach { file ->
            val content = file.readText()
            val prompt = "Generate documentation for this Kotlin code: $content"
            val docs = callOllama("llama3:8b", prompt)
            
            documentation.append("# ${file.name}\n\n")
            documentation.append("$docs\n\n")
        }
        
        outputFile.get().asFile.writeText(documentation.toString())
    }
    
    private fun callOllama(model: String, prompt: String): String {
        // Implementation here
        return "Generated documentation"
    }
}

tasks.register<DocumentationTask>("generateDocs") {
    useManagedAi()
    
    sourceDir.set(layout.projectDirectory.dir("src/main/kotlin"))
    outputFile.set(layout.buildDirectory.file("docs/api-docs.md"))
}
```

## Testing Examples

### AI-Powered Test Generation

```kotlin
// build.gradle.kts
managedAi {
    models {
        "codellama" {
            version = "7b-instruct"
        }
    }
}

tasks.register("generateTests") {
    useManagedAi()
    
    doLast {
        val sourceFiles = fileTree("src/main/kotlin") { include("**/*.kt") }
        
        sourceFiles.forEach { file ->
            val content = file.readText()
            val prompt = """
                Generate JUnit 5 tests for this Kotlin code:
                
                $content
                
                Include edge cases and error conditions.
            """.trimIndent()
            
            val tests = callOllama("codellama:7b-instruct", prompt)
            
            val testFile = file("src/test/kotlin/${file.nameWithoutExtension}Test.kt")
            testFile.writeText(tests)
        }
    }
}
```

### Property-Based Testing with AI

```kotlin
// build.gradle.kts
dependencies {
    testImplementation("io.kotest:kotest-property:5.8.0")
}

managedAi {
    models {
        "llama3" {
            version = "8b"
        }
    }
}

// src/test/kotlin/PropertyBasedTest.kt
import io.kotest.core.spec.style.FunSpec
import io.kotest.property.checkAll
import io.kotest.property.arbitrary.arbitrary

class PropertyBasedTest : FunSpec({
    
    test("AI-generated test cases should be valid") {
        checkAll(aiGeneratedArbitrary()) { testCase ->
            // Use AI to validate test case
            val isValid = validateWithAi(testCase)
            isValid shouldBe true
        }
    }
})

fun aiGeneratedArbitrary() = arbitrary { rs ->
    val prompt = "Generate a random test case for string validation"
    val response = callOllama("llama3:8b", prompt)
    parseTestCase(response)
}

fun validateWithAi(testCase: String): Boolean {
    val prompt = "Is this a valid test case: $testCase"
    val response = callOllama("llama3:8b", prompt)
    return response.contains("valid", ignoreCase = true)
}
```

## Integration Examples

### Spring Boot Integration

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("se.premex.gmai") version "0.0.2"
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
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
// src/test/kotlin/AiIntegrationTest.kt
@SpringBootTest
class AiIntegrationTest {
    
    @Test
    fun testAiServiceIntegration() {
        val response = callOllama("llama3:8b", "Generate a greeting message")
        assertThat(response).isNotEmpty()
    }
}
```

### Android Integration

```kotlin
// build.gradle.kts (Android project)
plugins {
    id("com.android.application")
    kotlin("android")
    id("se.premex.gmai") version "0.0.2"
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

// Unit tests can use AI for test data generation
tasks.register("generateTestData") {
    useManagedAi()
    
    doLast {
        val testData = callOllama("llama3:8b", "Generate JSON test data for a mobile app")
        file("src/test/resources/test-data.json").writeText(testData)
    }
}
```

## Environment-Specific Examples

### Development Environment

```kotlin
// build.gradle.kts
managedAi {
    // Use existing Ollama installation in development
    ollama {
        installationStrategy = OllamaInstallationStrategy.PREFER_EXISTING
        allowPortChange = true
    }
    
    models {
        "llama3" {
            version = "8b"
            preload = false  // Don't preload in development
        }
    }
}

tasks.register("devTest") {
    useManagedAi()
    
    doLast {
        println("Running in development mode...")
        // Development-specific AI tasks
    }
}
```

### Production Build

```kotlin
// build.gradle.kts
managedAi {
    val isProduction = project.hasProperty("production")
    
    ollama {
        installationStrategy = if (isProduction) {
            OllamaInstallationStrategy.ISOLATED_ONLY
        } else {
            OllamaInstallationStrategy.PREFER_EXISTING
        }
    }
    
    models {
        "llama3" {
            version = "8b"
            preload = isProduction
        }
    }
}
```

## Utility Functions

### Common AI Helper Functions

```kotlin
// buildSrc/src/main/kotlin/AiUtils.kt
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import kotlinx.serialization.json.*

object AiUtils {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    
    fun callOllama(model: String, prompt: String, stream: Boolean = false): String {
        val requestBody = """
            {
                "model": "$model",
                "prompt": "$prompt",
                "stream": $stream
            }
        """.trimIndent()
        
        val request = Request.Builder()
            .url("http://localhost:11434/api/generate")
            .post(RequestBody.create("application/json".toMediaType(), requestBody))
            .build()
        
        return client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: throw RuntimeException("Empty response")
            val jsonResponse = json.parseToJsonElement(body).jsonObject
            jsonResponse["response"]?.jsonPrimitive?.content ?: ""
        }
    }
    
    fun callOllamaChat(model: String, messages: List<Map<String, String>>): String {
        val requestBody = """
            {
                "model": "$model",
                "messages": ${json.encodeToString(messages)}
            }
        """.trimIndent()
        
        val request = Request.Builder()
            .url("http://localhost:11434/api/chat")
            .post(RequestBody.create("application/json".toMediaType(), requestBody))
            .build()
        
        return client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: throw RuntimeException("Empty response")
            val jsonResponse = json.parseToJsonElement(body).jsonObject
            jsonResponse["message"]?.jsonObject?.get("content")?.jsonPrimitive?.content ?: ""
        }
    }
}
```

## Best Practices

### Error Handling

```kotlin
tasks.register("robustAiTask") {
    useManagedAi()
    
    doLast {
        try {
            val response = callOllama("llama3:8b", "Generate code")
            processResponse(response)
        } catch (e: Exception) {
            logger.error("AI task failed", e)
            // Fallback logic
            handleAiFailure()
        }
    }
}

fun handleAiFailure() {
    // Implement fallback strategy
    println("AI service unavailable, using fallback...")
}
```

### Performance Optimization

```kotlin
managedAi {
    models {
        "llama3" {
            version = "8b"
            preload = true  // Preload frequently used models
        }
    }
    
    ollama {
        allowPortChange = true  // Allow port changes for parallel builds
    }
}

// Cache AI responses to avoid repeated calls
val aiCache = mutableMapOf<String, String>()

fun cachedAiCall(model: String, prompt: String): String {
    val key = "$model:${prompt.hashCode()}"
    return aiCache.getOrPut(key) {
        callOllama(model, prompt)
    }
}
```

These examples demonstrate various ways to integrate GMAI into your Gradle builds, from simple testing scenarios to complex CI/CD pipelines and production deployments.
