package net.spooncast.openmocker.lib.core.adapter

import net.spooncast.openmocker.lib.model.CachedResponse
import net.spooncast.openmocker.lib.model.HttpRequestData
import net.spooncast.openmocker.lib.model.HttpResponseData

/**
 * Generic HTTP client adapter interface
 *
 * This interface abstracts different HTTP client implementations (OkHttp, Ktor)
 * behind a unified interface, enabling type-safe integration with the mocking engine.
 *
 * @param TRequest The client-specific request type (e.g., okhttp3.Request, io.ktor.client.request.HttpRequestData)
 * @param TResponse The client-specific response type (e.g., okhttp3.Response, io.ktor.client.statement.HttpResponse)
 */
internal interface HttpClientAdapter<TRequest, TResponse> {

    /**
     * Extracts client-agnostic request data from the client-specific request
     *
     * @param clientRequest The original client-specific request
     * @return Client-agnostic request data
     */
    fun extractRequestData(clientRequest: TRequest): HttpRequestData

    /**
     * Extracts client-agnostic response data from the client-specific response
     *
     * @param clientResponse The original client-specific response
     * @return Client-agnostic response data
     */
    fun extractResponseData(clientResponse: TResponse): HttpResponseData

    /**
     * Creates a client-specific mock response from cached response data
     *
     * This method constructs a response object that matches the client's expected type
     * and contains the mock data specified in the cached response.
     *
     * @param originalRequest The original client-specific request
     * @param mockResponse The cached response data to use for the mock
     * @return Client-specific mock response
     */
    fun createMockResponse(originalRequest: TRequest, mockResponse: CachedResponse): TResponse
}