package net.spooncast.openmocker.core

/**
 * Core interface for HTTP mocking engine that abstracts platform-specific HTTP clients.
 *
 * This interface provides a unified API for mocking HTTP requests and responses,
 * supporting both OkHttp and Ktor implementations.
 *
 * All methods are suspend functions to support asynchronous operations.
 */
interface MockerEngine {

    /**
     * Checks if a mock response should be returned for the given request.
     *
     * @param method HTTP method (GET, POST, etc.)
     * @param path URL path without query parameters
     * @return MockResponse if a mock is configured, null otherwise
     */
    suspend fun shouldMock(method: String, path: String): MockResponse?

    /**
     * Caches an actual HTTP response for potential mocking later.
     *
     * @param method HTTP method
     * @param path URL path
     * @param code HTTP status code
     * @param body Response body as string
     */
    suspend fun cacheResponse(method: String, path: String, code: Int, body: String)

    /**
     * Enables mocking for the specified request with the given response.
     *
     * @param key MockKey identifying the request
     * @param response MockResponse to return for matching requests
     * @return true if mock was successfully enabled, false otherwise
     */
    suspend fun mock(key: MockKey, response: MockResponse): Boolean

    /**
     * Disables mocking for the specified request.
     *
     * @param key MockKey identifying the request to unmock
     * @return true if mock was successfully disabled, false otherwise
     */
    suspend fun unmock(key: MockKey): Boolean
}