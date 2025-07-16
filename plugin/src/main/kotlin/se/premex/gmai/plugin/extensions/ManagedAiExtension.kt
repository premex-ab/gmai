package se.premex.gmai.plugin.extensions

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import java.time.Duration
import javax.inject.Inject

@Suppress("UnstableApiUsage")
open class ManagedAiExtension @Inject constructor(
    private val project: Project,
    private val objects: ObjectFactory
) {
    /**
     * Ollama instance configuration
     */
    val ollama: OllamaConfiguration = objects.newInstance(OllamaConfiguration::class.java)

    /**
     * Models to manage
     */
    val models: NamedDomainObjectContainer<OllamaModelConfiguration> =
        objects.domainObjectContainer(OllamaModelConfiguration::class.java)

    /**
     * Global timeout for operations
     */
    var timeout: Duration = Duration.ofMinutes(5)

    /**
     * Auto-start Ollama service
     */
    var autoStart: Boolean = true

    /**
     * Auto-install Ollama if not present
     */
    var autoInstall: Boolean = true

    fun ollama(configure: Action<OllamaConfiguration>) {
        configure.execute(ollama)
    }

    fun models(configure: Action<NamedDomainObjectContainer<OllamaModelConfiguration>>) {
        configure.execute(models)
    }
}
