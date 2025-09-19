package net.spooncast.openmocker.lib.core

import net.spooncast.openmocker.lib.model.CachedResponse

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
     * Client type identifier for debugging and logging
     */
    val clientType: String

    /**
     * Extracts client-agnostic request data from the client-specific request
     *
     * @param clientRequest The original client-specific request
     * @return Client-agnostic request data
     */
    fun extractRequestData(clientRequest: TRequest): HttpRequestData

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

    /**
     * Extracts client-agnostic response data from the client-specific response
     *
     * @param clientResponse The original client-specific response
     * @return Client-agnostic response data
     */
    fun extractResponseData(clientResponse: TResponse): HttpResponseData

    /**
     * Validates if the given request and response types are supported by this adapter
     *
     * @param request The request object to validate
     * @param response The response object to validate
     * @return true if both objects are supported, false otherwise
     */
    fun isSupported(request: Any?, response: Any?): Boolean {
        return try {
            request != null && response != null &&
                    canHandleRequest(request) && canHandleResponse(response)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if this adapter can handle the given request type
     *
     * @param request The request object to check
     * @return true if the request can be handled, false otherwise
     */
    fun canHandleRequest(request: Any): Boolean {
        return try {
            @Suppress("UNCHECKED_CAST")
            extractRequestData(request as TRequest)
            true
        } catch (e: ClassCastException) {
            false
        }
    }

    /**
     * Checks if this adapter can handle the given response type
     *
     * @param response The response object to check
     * @return true if the response can be handled, false otherwise
     */
    fun canHandleResponse(response: Any): Boolean {
        return try {
            @Suppress("UNCHECKED_CAST")
            extractResponseData(response as TResponse)
            true
        } catch (e: ClassCastException) {
            false
        }
    }
}