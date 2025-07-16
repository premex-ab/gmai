package se.premex.gmai.plugin.extensions

import javax.inject.Inject

open class OllamaModelConfiguration @Inject constructor(
    private val name: String
) {
    var version: String = "latest"
    var preload: Boolean = false

    fun getName(): String = name
}
