package se.premex.gmai.plugin.utils

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Task execution optimization utility for Phase 3
 * Provides performance improvements and reliability enhancements
 */
object TaskOptimizer {
    private val logger: Logger = Logging.getLogger(TaskOptimizer::class.java)
    private val executionCache = ConcurrentHashMap<String, TaskExecutionResult>()
    private val lockManager = ConcurrentHashMap<String, ReentrantLock>()

    /**
     * Execute a task with optimization features like caching and locking
     */
    fun <T> executeOptimized(
        taskKey: String,
        operation: () -> T,
        cacheDurationMs: Long = 60000, // 1 minute default cache
        allowConcurrent: Boolean = false
    ): T {
        // Check cache first
        val cachedResult = executionCache[taskKey]
        if (cachedResult != null && !cachedResult.isExpired(cacheDurationMs)) {
            logger.debug("Using cached result for task: $taskKey")
            @Suppress("UNCHECKED_CAST")
            return cachedResult.result as T
        }

        // Get or create lock for this task
        val lock = lockManager.computeIfAbsent(taskKey) { ReentrantLock() }

        return if (allowConcurrent) {
            // Execute without locking
            executeAndCache(taskKey, operation)
        } else {
            // Execute with locking to prevent concurrent execution
            lock.withLock {
                // Double-check cache after acquiring lock
                val recentResult = executionCache[taskKey]
                if (recentResult != null && !recentResult.isExpired(cacheDurationMs)) {
                    logger.debug("Using cached result after lock acquisition for task: $taskKey")
                    @Suppress("UNCHECKED_CAST")
                    recentResult.result as T
                } else {
                    executeAndCache(taskKey, operation)
                }
            }
        }
    }

    private fun <T> executeAndCache(taskKey: String, operation: () -> T): T {
        val startTime = System.currentTimeMillis()

        return try {
            val result = operation()
            val executionTime = System.currentTimeMillis() - startTime

            logger.debug("Task '$taskKey' completed in ${executionTime}ms")

            // Cache successful result
            executionCache[taskKey] = TaskExecutionResult(result, System.currentTimeMillis())

            result
        } catch (e: Exception) {
            val executionTime = System.currentTimeMillis() - startTime
            logger.debug("Task '$taskKey' failed after ${executionTime}ms: ${e.message}")
            throw e
        }
    }

    /**
     * Clear cache for a specific task
     */
    fun clearCache(taskKey: String) {
        executionCache.remove(taskKey)
        logger.debug("Cleared cache for task: $taskKey")
    }

    /**
     * Clear all cached results
     */
    fun clearAllCache() {
        executionCache.clear()
        logger.debug("Cleared all cached results")
    }

    /**
     * Get cache statistics
     */
    fun getCacheStats(): CacheStats {
        val currentTime = System.currentTimeMillis()
        val totalEntries = executionCache.size
        val expiredEntries = executionCache.values.count { it.isExpired(60000) }

        return CacheStats(
            totalEntries = totalEntries,
            activeEntries = totalEntries - expiredEntries,
            expiredEntries = expiredEntries
        )
    }

    /**
     * Clean up expired cache entries
     */
    fun cleanupExpiredEntries(maxAgeMs: Long = 300000) { // 5 minutes default
        val currentTime = System.currentTimeMillis()
        val expiredKeys = executionCache.entries
            .filter { it.value.isExpired(maxAgeMs) }
            .map { it.key }

        expiredKeys.forEach { key ->
            executionCache.remove(key)
            lockManager.remove(key)
        }

        if (expiredKeys.isNotEmpty()) {
            logger.debug("Cleaned up ${expiredKeys.size} expired cache entries")
        }
    }

    private data class TaskExecutionResult(
        val result: Any?,
        val timestamp: Long
    ) {
        fun isExpired(maxAgeMs: Long): Boolean {
            return System.currentTimeMillis() - timestamp > maxAgeMs
        }
    }

    data class CacheStats(
        val totalEntries: Int,
        val activeEntries: Int,
        val expiredEntries: Int
    )
}
