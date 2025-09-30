package net.spooncast.openmocker.lib.core

import net.spooncast.openmocker.lib.core.adapter.HttpClientAdapter
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
     * Gets mock data for the given request without creating the response
     *
     * This method provides access to the raw CachedResponse data, including
     * delay information, allowing clients to handle delay processing optimally
     * for their execution context (synchronous vs asynchronous).
     *
     * @param clientRequest The original client-specific request
     * @return CachedResponse data if a mock exists, null otherwise
     */
    fun getMockData(clientRequest: TRequest): CachedResponse? {
        val requestData = clientAdapter.extractRequestData(clientRequest)
        return cacheRepo.getMock(requestData.method, requestData.path)
    }

    /**
     * Creates a mock response from cached response data
     *
     * This method separates response creation from mock detection,
     * allowing clients to handle delay processing separately.
     *
     * @param clientRequest The original client-specific request
     * @param cachedResponse The cached response data to use for mocking
     * @return Client-specific mock response
     */
    fun createMockResponse(clientRequest: TRequest, cachedResponse: CachedResponse): TResponse {
        return clientAdapter.createMockResponse(clientRequest, cachedResponse)
    }

    companion object {
        const val MOCKER_MESSAGE = "OpenMocker enabled"
    }
}