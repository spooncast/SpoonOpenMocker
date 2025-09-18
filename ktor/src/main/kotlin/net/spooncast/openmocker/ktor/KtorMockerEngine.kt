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
     * @param method HTTP method (GET, POST, etc.)
     * @param path URL path without query parameters
     * @return MockResponse if a mock is configured, null otherwise
     */
    override suspend fun shouldMock(method: String, path: String): MockResponse? {
        // TODO: Implement mock lookup logic
        // This should:
        // 1. Create a MockKey from method and path
        // 2. Query the repository for an active mock
        // 3. Return the MockResponse if found, null otherwise

        val key = MockKey(method, path)
        return repository.getMock(key)
    }

    /**
     * Caches an actual HTTP response for potential mocking later.
     *
     * This method is called by the OpenMocker plugin after successful HTTP responses
     * to store them for potential future mocking through the UI or programmatic API.
     *
     * @param method HTTP method
     * @param path URL path
     * @param code HTTP status code
     * @param body Response body as string
     */
    override suspend fun cacheResponse(method: String, path: String, code: Int, body: String) {
        // TODO: Implement response caching logic
        // This should:
        // 1. Create a MockKey from method and path
        // 2. Create a MockResponse from code, body, and default delay
        // 3. Store the cached response in the repository

        val key = MockKey(method, path)
        val response = MockResponse(code = code, body = body, delay = 0L)
        repository.cacheRealResponse(key, response)
    }

    /**
     * Enables mocking for the specified request with the given response.
     *
     * @param key MockKey identifying the request
     * @param response MockResponse to return for matching requests
     * @return true if mock was successfully enabled, false otherwise
     */
    override suspend fun mock(key: MockKey, response: MockResponse): Boolean {
        // TODO: Implement mock enabling logic
        // This should:
        // 1. Validate the key and response parameters
        // 2. Store the mock configuration in the repository
        // 3. Return success status

        return try {
            repository.saveMock(key, response)
            true
        } catch (e: Exception) {
            // Log error in real implementation
            false
        }
    }

    /**
     * Disables mocking for the specified request.
     *
     * @param key MockKey identifying the request to unmock
     * @return true if mock was successfully disabled, false otherwise
     */
    override suspend fun unmock(key: MockKey): Boolean {
        // TODO: Implement mock disabling logic
        // This should:
        // 1. Validate the key parameter
        // 2. Remove the mock configuration from the repository
        // 3. Return success status

        return try {
            repository.removeMock(key)
            true
        } catch (e: Exception) {
            // Log error in real implementation
            false
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