package net.spooncast.openmocker.okhttp

import net.spooncast.openmocker.core.MockKey
import net.spooncast.openmocker.core.MockResponse
import net.spooncast.openmocker.core.MockRepository
import net.spooncast.openmocker.core.MockerEngine

/**
 * OkHttp-specific implementation of MockerEngine.
 *
 * This implementation provides a bridge between OkHttp's interceptor pattern and the core
 * MockerEngine interface, enabling seamless integration with the multi-module architecture.
 *
 * The engine delegates all mock and cache operations to the provided MockRepository,
 * ensuring consistency with other platform implementations.
 *
 * Thread Safety:
 * - All operations are thread-safe as they delegate to the thread-safe MockRepository
 * - Uses suspend functions to support asynchronous operations without blocking threads
 *
 * Performance Considerations:
 * - Minimal overhead on each request (single repository lookup)
 * - Efficient key-based mock retrieval with O(1) lookup time
 * - No unnecessary object creation during normal operation
 *
 * @property repository The repository for managing mocks and cached responses
 */
class OkHttpMockerEngine(
    private val repository: MockRepository
) : MockerEngine {

    /**
     * Checks if a mock response should be returned for the given request.
     *
     * This method performs a fast O(1) lookup in the mock repository to determine
     * if the request should be mocked. It's designed to have minimal performance
     * impact on regular HTTP requests.
     *
     * @param method HTTP method (GET, POST, etc.)
     * @param path URL path without query parameters
     * @return MockResponse if a mock is configured, null otherwise
     */
    override suspend fun shouldMock(method: String, path: String): MockResponse? {
        val key = MockKey(method, path)
        return repository.getMock(key)
    }

    /**
     * Caches an actual HTTP response for potential mocking later.
     *
     * This method is typically called after receiving a real HTTP response
     * to store it for future use in mocking scenarios.
     *
     * @param method HTTP method
     * @param path URL path
     * @param code HTTP status code
     * @param body Response body as string
     */
    override suspend fun cacheResponse(method: String, path: String, code: Int, body: String) {
        val key = MockKey(method, path)
        val response = MockResponse(code, body)
        repository.cacheRealResponse(key, response)
    }

    /**
     * Enables mocking for the specified request with the given response.
     *
     * Once a mock is enabled, subsequent requests matching the mock key will return
     * the specified mock response instead of making actual HTTP calls.
     *
     * @param key MockKey identifying the request
     * @param response MockResponse to return for matching requests
     * @return true if mock was successfully enabled, false otherwise
     */
    override suspend fun mock(key: MockKey, response: MockResponse): Boolean {
        return try {
            repository.saveMock(key, response)
            true
        } catch (e: Exception) {
            // Log error in production; for now return false
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
     * @return true if mock was successfully disabled, false if no mock existed
     */
    override suspend fun unmock(key: MockKey): Boolean {
        return repository.removeMock(key)
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
     * Useful for debugging and management UI purposes.
     *
     * @return Map of all mock keys to their corresponding responses
     */
    suspend fun getAllMocks(): Map<MockKey, MockResponse> {
        return repository.getAllMocks()
    }

    /**
     * Retrieves all cached responses.
     *
     * Useful for debugging and providing options in management UI.
     *
     * @return Map of all cached response keys to their responses
     */
    suspend fun getAllCachedResponses(): Map<MockKey, MockResponse> {
        return repository.getAllCachedResponses()
    }

    /**
     * Clears all mocks and cached responses.
     *
     * This is a destructive operation that cannot be undone.
     */
    suspend fun clearAll() {
        repository.clearAll()
    }
}