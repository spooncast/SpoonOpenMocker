package net.spooncast.openmocker.ktor

import net.spooncast.openmocker.core.MockerEngine
import net.spooncast.openmocker.core.MockRepository
import net.spooncast.openmocker.core.MemoryMockRepository
import net.spooncast.openmocker.core.MockKey
import net.spooncast.openmocker.core.MockResponse

/**
 * Ktor-specific implementation of the MockerEngine interface.
 *
 * This engine provides HTTP mocking capabilities specifically for Ktor client,
 * implementing the core MockerEngine interface to ensure consistency with
 * other HTTP client implementations (like OkHttp).
 *
 * The engine handles:
 * - Mock response lookup for incoming requests
 * - Response caching for potential future mocking
 * - Mock configuration and management through the repository
 * - Integration with Ktor client request/response pipeline
 *
 * @param repository The MockRepository implementation for storing and retrieving mocks
 *
 * @see net.spooncast.openmocker.core.MockerEngine
 * @see net.spooncast.openmocker.core.MockRepository
 */
class KtorMockerEngine(
    private val repository: MockRepository
) : MockerEngine {

    /**
     * Checks if a mock response should be returned for the given request.
     *
     * This method is called by the OpenMocker plugin during request processing
     * to determine if a mock response is configured for the current request.
     *
     * Performance: O(1) lookup time using key-based repository access.
     * Thread Safety: Repository operations are thread-safe.
     *
     * @param method HTTP method (GET, POST, etc.)
     * @param path URL path without query parameters
     * @return MockResponse if a mock is configured, null otherwise
     */
    override suspend fun shouldMock(method: String, path: String): MockResponse? {
        return try {
            val key = MockKey(method, path)
            repository.getMock(key)
        } catch (e: Exception) {
            // Log error in production; return null to proceed with real request
            null
        }
    }

    /**
     * Caches an actual HTTP response for potential mocking later.
     *
     * This method is called by the OpenMocker plugin after successful HTTP responses
     * to store them for potential future mocking through the UI or programmatic API.
     *
     * Memory Efficiency: Stores only essential response data (code, body, delay).
     * Thread Safety: Repository operations are thread-safe.
     *
     * @param method HTTP method
     * @param path URL path
     * @param code HTTP status code
     * @param body Response body as string
     */
    override suspend fun cacheResponse(method: String, path: String, code: Int, body: String) {
        try {
            val key = MockKey(method, path)
            val response = MockResponse(code = code, body = body, delay = 0L)
            repository.cacheRealResponse(key, response)
        } catch (e: Exception) {
            // Log error in production; continue execution without caching
            // Caching failures should not affect normal operation
        }
    }

    /**
     * Enables mocking for the specified request with the given response.
     *
     * Once a mock is enabled, subsequent requests matching the mock key will return
     * the specified mock response instead of making actual HTTP calls.
     *
     * Validation: Validates parameters before storage.
     * Atomicity: Mock is either fully saved or operation fails.
     *
     * @param key MockKey identifying the request
     * @param response MockResponse to return for matching requests
     * @return true if mock was successfully enabled, false otherwise
     */
    override suspend fun mock(key: MockKey, response: MockResponse): Boolean {
        return try {
            // Validate parameters
            require(key.method.isNotBlank()) { "Mock key method cannot be blank" }
            require(key.path.isNotBlank()) { "Mock key path cannot be blank" }
            require(response.code in 100..599) { "Response code must be valid HTTP status code" }

            repository.saveMock(key, response)
            true
        } catch (e: Exception) {
            // Log error in production; return false to indicate failure
            false
        }
    }

    /**
     * Disables mocking for the specified request.
     *
     * After disabling a mock, subsequent requests will proceed with normal HTTP execution
     * instead of returning the mock response.
     *
     * @param key MockKey identifying the request to unmock
     * @return true if mock was successfully disabled, false if no mock existed or operation failed
     */
    override suspend fun unmock(key: MockKey): Boolean {
        return try {
            repository.removeMock(key)
        } catch (e: Exception) {
            // Log error in production; return false to indicate failure
            false
        }
    }

    /**
     * Convenience method to create a MockKey from HTTP method and path.
     *
     * @param method HTTP method
     * @param path URL path
     * @return MockKey for the request
     */
    fun createMockKey(method: String, path: String): MockKey {
        return MockKey(method, path)
    }

    /**
     * Retrieves all currently configured mocks.
     *
     * Useful for debugging, UI management, and administrative purposes.
     *
     * @return Map of all mock keys to their corresponding responses
     */
    suspend fun getAllMocks(): Map<MockKey, MockResponse> {
        return try {
            repository.getAllMocks()
        } catch (e: Exception) {
            // Return empty map on error to avoid breaking client code
            emptyMap()
        }
    }

    /**
     * Retrieves all cached responses.
     *
     * Useful for debugging, UI management, and providing options for mock creation.
     *
     * @return Map of all cached response keys to their responses
     */
    suspend fun getAllCachedResponses(): Map<MockKey, MockResponse> {
        return try {
            repository.getAllCachedResponses()
        } catch (e: Exception) {
            // Return empty map on error to avoid breaking client code
            emptyMap()
        }
    }

    /**
     * Clears all mocks and cached responses.
     *
     * This is a destructive operation that cannot be undone.
     * Useful for testing and cleanup scenarios.
     */
    suspend fun clearAll() {
        try {
            repository.clearAll()
        } catch (e: Exception) {
            // Log error in production; silently handle cleanup failures
        }
    }
}

/**
 * Factory function to create a KtorMockerEngine with the default MemoryMockRepository.
 *
 * This provides a convenient way to create a KtorMockerEngine without manually
 * constructing a repository, using the same singleton instance used by other components.
 *
 * @return KtorMockerEngine configured with the default repository
 */
fun createKtorMockerEngine(): KtorMockerEngine {
    return KtorMockerEngine(MemoryMockRepository())
}