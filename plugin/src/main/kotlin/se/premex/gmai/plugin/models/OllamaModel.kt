package se.premex.gmai.plugin.models

import kotlinx.serialization.Serializable

@Serializable
data class OllamaModel(
    val name: String,
    val version: String = "latest",
    val size: String? = null,
    val digest: String? = null,
    val modifiedAt: String? = null,
    val details: ModelDetails? = null
)

@Serializable
data class ModelDetails(
    val format: String,
    val family: String,
    val families: List<String>? = null,
    val parameterSize: String,
    val quantizationLevel: String
)
