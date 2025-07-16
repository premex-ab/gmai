package se.premex.gmai.plugin.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ThreadLocalRandom

/**
 * Utility class for managing port availability and conflict resolution
 */
class PortManager(
    private val logger: Logger = LoggerFactory.getLogger(PortManager::class.java)
) {

    /**
     * Check if a port is available for use
     */
    fun isPortAvailable(port: Int, host: String = "localhost"): Boolean {
        return try {
            ServerSocket(port).use {
                // Port is available
                true
            }
        } catch (e: Exception) {
            // Port is in use or not available
            false
        }
    }

    /**
     * Check if a service is already running on the specified port
     */
    fun isServiceRunning(port: Int, host: String = "localhost"): Boolean {
        return try {
            Socket(host, port).use {
                // Successfully connected, service is running
                true
            }
        } catch (e: Exception) {
            // Connection failed, service not running
            false
        }
    }

    /**
     * Find an available port starting from the preferred port
     */
    fun findAvailablePort(preferredPort: Int, host: String = "localhost", maxAttempts: Int = 100): Int {
        // First try the preferred port
        if (isPortAvailable(preferredPort, host)) {
            return preferredPort
        }

        logger.info("Port $preferredPort is not available, searching for alternative...")

        // Try ports incrementally from preferred port
        for (port in (preferredPort + 1)..(preferredPort + maxAttempts)) {
            if (isPortAvailable(port, host)) {
                logger.info("Found available port: $port")
                return port
            }
        }

        // If no sequential port found, try random ports in valid range
        repeat(maxAttempts) {
            val randomPort = ThreadLocalRandom.current().nextInt(49152, 65535)
            if (isPortAvailable(randomPort, host)) {
                logger.info("Found available random port: $randomPort")
                return randomPort
            }
        }

        throw IllegalStateException("Unable to find available port after $maxAttempts attempts")
    }

    /**
     * Resolve port conflicts by finding the best available port
     */
    fun resolvePortConflict(
        preferredPort: Int,
        host: String = "localhost",
        allowPortChange: Boolean = true
    ): PortResolution {
        // Check if preferred port is available
        if (isPortAvailable(preferredPort, host)) {
            return PortResolution(preferredPort, PortResolution.Status.AVAILABLE)
        }

        // Check if Ollama is already running on preferred port
        if (isServiceRunning(preferredPort, host)) {
            return PortResolution(preferredPort, PortResolution.Status.SERVICE_RUNNING)
        }

        // Port is occupied by something else
        if (!allowPortChange) {
            return PortResolution(preferredPort, PortResolution.Status.CONFLICT)
        }

        // Try to find alternative port
        return try {
            val alternativePort = findAvailablePort(preferredPort, host)
            PortResolution(alternativePort, PortResolution.Status.ALTERNATIVE_FOUND)
        } catch (e: Exception) {
            logger.error("Failed to find alternative port", e)
            PortResolution(preferredPort, PortResolution.Status.NO_ALTERNATIVE)
        }
    }

    data class PortResolution(
        val port: Int,
        val status: Status
    ) {
        enum class Status {
            AVAILABLE,           // Preferred port is available
            SERVICE_RUNNING,     // Ollama already running on preferred port
            CONFLICT,            // Port occupied by another service
            ALTERNATIVE_FOUND,   // Alternative port found
            NO_ALTERNATIVE       // No alternative port available
        }
    }
}
