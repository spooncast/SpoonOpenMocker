package net.spooncast.openmocker.lib.data

import net.spooncast.openmocker.lib.data.adapter.HttpClientAdapter
import net.spooncast.openmocker.lib.data.repo.CacheRepo
import net.spooncast.openmocker.lib.model.CachedResponse

internal class MockingEngine<TRequest, TResponse>(
    private val cacheRepo: CacheRepo,
    private val clientAdapter: HttpClientAdapter<TRequest, TResponse>
) {
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

    fun getMockData(clientRequest: TRequest): CachedResponse? {
        val requestData = clientAdapter.extractRequestData(clientRequest)
        return cacheRepo.getMock(requestData.method, requestData.path)
    }

    fun createMockResponse(clientRequest: TRequest, cachedResponse: CachedResponse): TResponse {
        return clientAdapter.createMockResponse(clientRequest, cachedResponse)
    }
}