package net.spooncast.openmocker.core

/**
 * Platform-independent repository interface for managing mock responses and cached real responses.
 *
 * This interface provides a clean abstraction layer for storing and retrieving mock configurations
 * and cached HTTP responses. It supports both mock management (user-defined responses) and
 * response caching (storing real HTTP responses for later mocking).
 *
 * All operations are thread-safe and use coroutines for asynchronous execution.
 */
interface MockRepository {

    /**
     * Retrieves a mock response for the given key.
     *
     * @param key The mock key identifying the HTTP request
     * @return The mock response if exists, null otherwise
     */
    suspend fun getMock(key: MockKey): MockResponse?

    /**
     * Saves a mock response for the given key.
     *
     * This will overwrite any existing mock for the same key.
     *
     * @param key The mock key identifying the HTTP request
     * @param response The mock response to save
     */
    suspend fun saveMock(key: MockKey, response: MockResponse)

    /**
     * Removes a mock response for the given key.
     *
     * @param key The mock key identifying the HTTP request
     * @return true if a mock was removed, false if no mock existed
     */
    suspend fun removeMock(key: MockKey): Boolean

    /**
     * Retrieves all currently stored mocks.
     *
     * @return A map of all mock keys to their corresponding responses
     */
    suspend fun getAllMocks(): Map<MockKey, MockResponse>

    /**
     * Removes all stored mocks and cached responses.
     *
     * This is a destructive operation that cannot be undone.
     */
    suspend fun clearAll()

    /**
     * Caches a real HTTP response for potential future mocking.
     *
     * This method stores actual HTTP responses that were received from the server.
     * These cached responses can later be used as a base for creating mocks.
     *
     * @param key The mock key identifying the HTTP request
     * @param response The real HTTP response to cache
     */
    suspend fun cacheRealResponse(key: MockKey, response: MockResponse)

    /**
     * Retrieves a cached real response for the given key.
     *
     * @param key The mock key identifying the HTTP request
     * @return The cached real response if exists, null otherwise
     */
    suspend fun getCachedResponse(key: MockKey): MockResponse?

    /**
     * Retrieves all cached real responses.
     *
     * @return A map of all mock keys to their corresponding cached responses
     */
    suspend fun getAllCachedResponses(): Map<MockKey, MockResponse>
}