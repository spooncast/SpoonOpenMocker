package net.spooncast.openmocker.ktor

import net.spooncast.openmocker.core.MockRepository
import net.spooncast.openmocker.core.MemoryMockRepository

/**
 * Configuration class for OpenMocker Ktor client plugin.
 *
 * Provides DSL-style configuration for the OpenMocker plugin with sensible defaults
 * and customization options for mock repository and behavior.
 *
 * Example usage:
 * ```
 * val client = HttpClient {
 *     install(OpenMocker) {
 *         repository = CustomMockRepository()
 *         isEnabled = true
 *         interceptAll = false
 *     }
 * }
 * ```
 */
class OpenMockerConfig {

    /**
     * Mock repository for storing and retrieving cached responses and mocks.
     * Defaults to [MemoryMockRepository] which provides thread-safe in-memory storage.
     */
    var repository: MockRepository = MemoryMockRepository()

    /**
     * Whether the OpenMocker plugin is enabled.
     * When disabled, requests will pass through without interception.
     * Defaults to true.
     */
    var isEnabled: Boolean = true

    /**
     * Whether to intercept all requests regardless of mock configuration.
     * When true, all requests will be cached for potential mocking.
     * When false, only requests with configured mocks will be intercepted.
     * Defaults to true for comprehensive request caching.
     */
    var interceptAll: Boolean = true

    /**
     * Maximum number of cached responses to keep in memory.
     * Set to -1 for unlimited caching (default).
     * Positive values will implement LRU eviction.
     */
    var maxCacheSize: Int = -1

    /**
     * Whether to automatically enable mocking for development builds.
     * This can be useful for automatically enabling mocks in debug builds.
     * Defaults to false.
     */
    var autoEnableInDebug: Boolean = false

    internal fun validate() {
        require(maxCacheSize == -1 || maxCacheSize > 0) {
            "maxCacheSize must be either -1 (unlimited) or a positive number"
        }
    }
}