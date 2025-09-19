package net.spooncast.openmocker.lib.core

import kotlinx.coroutines.delay
import net.spooncast.openmocker.lib.model.CachedResponse
import net.spooncast.openmocker.lib.repo.CacheRepo

/**
 * Generic mocking engine that handles core mocking logic independent of HTTP client implementation
 *
 * This engine coordinates between the HTTP client adapter and cache repository to provide
 * unified mocking functionality across different HTTP clients (OkHttp, Ktor).
 *
 * @param TRequest The client-specific request type
 * @param TResponse The client-specific response type
 * @param cacheRepo Repository for storing and retrieving cached responses and mocks
 * @param clientAdapter Adapter for converting between client-specific and generic data models
 */
internal class MockingEngine<TRequest, TResponse>(
    private val cacheRepo: CacheRepo,
    private val clientAdapter: HttpClientAdapter<TRequest, TResponse>
) {

    /**
     * Checks if a mock response exists for the given request and returns it if found
     *
     * This method:
     * 1. Extracts request data using the client adapter
     * 2. Looks up any existing mock in the cache repository
     * 3. If a mock exists, applies the configured delay and creates a client-specific response
     *
     * @param clientRequest The original client-specific request
     * @return A mocked client-specific response, or null if no mock exists
     */
    suspend fun checkForMock(clientRequest: TRequest): TResponse? {
        val requestData = clientAdapter.extractRequestData(clientRequest)
        val cachedValue = cacheRepo.getCachedValue(requestData.method, requestData.path)

        return cachedValue?.mock?.let { mockResponse ->
            // Apply artificial delay if configured
            if (mockResponse.duration > 0) {
                delay(mockResponse.duration)
            }

            // Create client-specific mock response
            clientAdapter.createMockResponse(clientRequest, mockResponse)
        }
    }

    /**
     * Caches the response data for future mocking
     *
     * This method:
     * 1. Extracts both request and response data using the client adapter
     * 2. Stores the response in the cache repository for potential future mocking
     *
     * @param clientRequest The original client-specific request
     * @param clientResponse The original client-specific response
     */
    fun cacheResponse(clientRequest: TRequest, clientResponse: TResponse) {
        val requestData = clientAdapter.extractRequestData(clientRequest)
        val responseData = clientAdapter.extractResponseData(clientResponse)

        cacheRepo.cache(
            method = requestData.method,
            urlPath = requestData.path,
            responseCode = responseData.code,
            responseBody = responseData.body
        )
    }

    /**
     * Synchronous version of checkForMock for clients that don't support coroutines
     *
     * @param clientRequest The original client-specific request
     * @return A mocked client-specific response, or null if no mock exists
     */
    fun checkForMockSync(clientRequest: TRequest): TResponse? {
        val requestData = clientAdapter.extractRequestData(clientRequest)
        val mockResponse = cacheRepo.getMock(requestData.method, requestData.path)

        return mockResponse?.let { mock ->
            clientAdapter.createMockResponse(clientRequest, mock)
        }
    }

    /**
     * Applies artificial delay for mock responses
     *
     * This is a separate method to handle delay application consistently
     * across different usage patterns.
     *
     * @param mockResponse The mock response containing delay information
     */
    suspend fun applyMockDelay(mockResponse: CachedResponse) {
        if (mockResponse.duration > 0) {
            delay(mockResponse.duration)
        }
    }

    /**
     * Gets the client type from the adapter for debugging purposes
     */
    fun getClientType(): String = clientAdapter.clientType

    /**
     * Validates if this engine can handle the given request/response types
     */
    fun canHandle(request: Any?, response: Any?): Boolean {
        return clientAdapter.isSupported(request, response)
    }

    companion object {
        const val MOCKER_MESSAGE = "OpenMocker enabled"
    }
}

/**
 * Extension function to get cached value from repository
 */
private fun CacheRepo.getCachedValue(method: String, path: String): net.spooncast.openmocker.lib.model.CachedValue? {
    val key = net.spooncast.openmocker.lib.model.CachedKey(method, path)
    return cachedMap[key]
}