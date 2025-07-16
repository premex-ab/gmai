package se.premex.gmai.plugin.services

import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import se.premex.gmai.plugin.models.OllamaInstance
import se.premex.gmai.plugin.models.OllamaModel
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Serializable
data class ModelsResponse(
    val models: List<OllamaModel> = emptyList()
)

@Serializable
data class PullRequest(
    val name: String,
    val stream: Boolean = false
)

@Serializable
data class DeleteRequest(
    val name: String
)

@Serializable
data class PullProgress(
    val status: String,
    val digest: String? = null,
    val total: Long? = null,
    val completed: Long? = null
)

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = false
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ChatResponse(
    val model: String,
    val message: ChatMessage,
    val done: Boolean
)

@Serializable
data class GenerateRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean = false
)

@Serializable
data class GenerateResponse(
    val model: String,
    val response: String,
    val done: Boolean
)

class OllamaService(
    private val instance: OllamaInstance,
    private val logger: Logger = LoggerFactory.getLogger(OllamaService::class.java)
) {

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(instance.timeout)
        .build()

    suspend fun isHealthy(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("${instance.baseUrl}/api/tags"))
                .GET()
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            response.statusCode() in 200..299
        } catch (e: Exception) {
            logger.debug("Health check failed", e)
            false
        }
    }

    suspend fun listModels(): List<OllamaModel> = withContext(Dispatchers.IO) {
        try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("${instance.baseUrl}/api/tags"))
                .GET()
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() !in 200..299) {
                throw RuntimeException("Failed to list models: ${response.statusCode()}")
            }

            // Parse JSON response to extract model names
            val responseBody = response.body()
            val models = mutableListOf<OllamaModel>()

            // Basic JSON parsing for model names
            val modelPattern = """"name"\s*:\s*"([^"]+)"""".toRegex()
            val matches = modelPattern.findAll(responseBody)

            for (match in matches) {
                val modelName = match.groupValues[1]
                models.add(OllamaModel(name = modelName, size = "0", digest = "", modifiedAt = ""))
            }

            logger.debug("Found ${models.size} models: ${models.map { it.name }}")
            models
        } catch (e: Exception) {
            logger.error("Failed to list models", e)
            emptyList()
        }
    }

    suspend fun pullModel(modelName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val requestBody = """{"name": "$modelName"}"""
            val request = HttpRequest.newBuilder()
                .uri(URI.create("${instance.baseUrl}/api/pull"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            response.statusCode() in 200..299
        } catch (e: Exception) {
            logger.error("Failed to pull model: $modelName", e)
            false
        }
    }

    suspend fun pullModelWithProgress(
        modelName: String,
        onProgress: (PullProgress) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val requestBody = """{"name": "$modelName", "stream": true}"""
            val request = HttpRequest.newBuilder()
                .uri(URI.create("${instance.baseUrl}/api/pull"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(instance.timeout)
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())

            if (response.statusCode() !in 200..299) {
                logger.error("Failed to pull model $modelName: HTTP ${response.statusCode()}")
                return@withContext false
            }

            // Process streaming response with better error handling
            var lastProgressKey: String? = null
            var lastPercentage: Int = -1
            var hasReceivedData = false

            try {
                response.body().use { inputStream ->
                    inputStream.bufferedReader().use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val currentLine = line ?: continue
                            if (currentLine.isNotBlank()) {
                                hasReceivedData = true
                                try {
                                    // Parse JSON line - basic parsing for now
                                    val statusMatch = """"status"\s*:\s*"([^"]+)"""".toRegex().find(currentLine)
                                    val digestMatch = """"digest"\s*:\s*"([^"]+)"""".toRegex().find(currentLine)
                                    val totalMatch = """"total"\s*:\s*(\d+)""".toRegex().find(currentLine)
                                    val completedMatch = """"completed"\s*:\s*(\d+)""".toRegex().find(currentLine)

                                    val status = statusMatch?.groupValues?.get(1) ?: "unknown"
                                    val digest = digestMatch?.groupValues?.get(1)
                                    val total = totalMatch?.groupValues?.get(1)?.toLongOrNull()
                                    val completed = completedMatch?.groupValues?.get(1)?.toLongOrNull()

                                    val progress = PullProgress(
                                        status = status,
                                        digest = digest,
                                        total = total,
                                        completed = completed
                                    )

                                    // Create a unique key for this progress update
                                    val progressKey = "${status}_${digest ?: "none"}"

                                    // Calculate percentage if we have total and completed
                                    val currentPercentage = if (total != null && completed != null && total > 0) {
                                        (completed.toDouble() / total.toDouble() * 100).toInt()
                                    } else {
                                        -1
                                    }

                                    // Only call progress callback if:
                                    // 1. Status changed, OR
                                    // 2. We have meaningful progress (percentage changed by at least 5%), OR
                                    // 3. This is a new digest/layer
                                    val shouldReport = lastProgressKey != progressKey ||
                                                     (currentPercentage != -1 &&
                                                      (lastPercentage == -1 ||
                                                       kotlin.math.abs(currentPercentage - lastPercentage) >= 5))

                                    if (shouldReport) {
                                        onProgress(progress)
                                        lastProgressKey = progressKey
                                        if (currentPercentage != -1) {
                                            lastPercentage = currentPercentage
                                        }
                                    }

                                } catch (e: Exception) {
                                    logger.debug("Failed to parse progress line: $currentLine", e)
                                }
                            }
                        }
                    }
                }
            } catch (e: java.io.IOException) {
                logger.warn("Connection interrupted during model pull for $modelName, checking if model was pulled successfully", e)
                // Check if the model was actually pulled successfully despite the connection issue
                if (hasReceivedData) {
                    // Wait a bit and check if the model is now available
                    delay(2000)
                    return@withContext isModelAvailable(modelName)
                }
                throw e
            }

            // Final success callback
            onProgress(PullProgress(status = "success", digest = null, total = null, completed = null))
            true

        } catch (e: Exception) {
            logger.error("Failed to pull model with progress: $modelName", e)
            false
        }
    }

    suspend fun isModelAvailable(modelName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val models = listModels()
            models.any { it.name == modelName || it.name.startsWith("$modelName:") }
        } catch (e: Exception) {
            logger.debug("Failed to check if model is available: $modelName", e)
            false
        }
    }

    suspend fun deleteModel(modelName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val requestBody = """{"name": "$modelName"}"""
            val request = HttpRequest.newBuilder()
                .uri(URI.create("${instance.baseUrl}/api/delete"))
                .header("Content-Type", "application/json")
                .method("DELETE", HttpRequest.BodyPublishers.ofString(requestBody))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            response.statusCode() in 200..299
        } catch (e: Exception) {
            logger.error("Failed to delete model: $modelName", e)
            false
        }
    }

    suspend fun generateResponse(modelName: String, prompt: String): String? = withContext(Dispatchers.IO) {
        try {
            val requestBody = """{"model": "$modelName", "prompt": "$prompt", "stream": false}"""
            val request = HttpRequest.newBuilder()
                .uri(URI.create("${instance.baseUrl}/api/generate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() !in 200..299) {
                logger.error("Failed to generate response: ${response.statusCode()}")
                return@withContext null
            }

            // Parse the JSON response to extract the response text
            val responseBody = response.body()
            // For now, do a simple extraction - in a real implementation, we'd use proper JSON parsing
            val responsePattern = """"response"\s*:\s*"([^"]*)"""".toRegex()
            val match = responsePattern.find(responseBody)
            match?.groupValues?.get(1)
        } catch (e: Exception) {
            logger.error("Failed to generate response for model: $modelName", e)
            null
        }
    }

    suspend fun chatCompletion(modelName: String, messages: List<ChatMessage>): String? = withContext(Dispatchers.IO) {
        try {
            val messagesJson = messages.joinToString(",") {
                """{"role": "${it.role}", "content": "${it.content}"}"""
            }
            val requestBody = """{"model": "$modelName", "messages": [$messagesJson], "stream": false}"""
            val request = HttpRequest.newBuilder()
                .uri(URI.create("${instance.baseUrl}/api/chat"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() !in 200..299) {
                logger.error("Failed to chat completion: ${response.statusCode()}")
                return@withContext null
            }

            // Parse the JSON response to extract the message content
            val responseBody = response.body()
            val contentPattern = """"content"\s*:\s*"([^"]*)"""".toRegex()
            val match = contentPattern.find(responseBody)
            match?.groupValues?.get(1)
        } catch (e: Exception) {
            logger.error("Failed to chat completion for model: $modelName", e)
            null
        }
    }

    fun close() {
        // HttpClient doesn't need explicit closing in Java 11+
    }
}
