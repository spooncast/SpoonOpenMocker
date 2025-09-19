package net.spooncast.openmocker.lib.client.okhttp

import net.spooncast.openmocker.lib.core.HttpClientAdapter
import net.spooncast.openmocker.lib.core.HttpRequestData
import net.spooncast.openmocker.lib.core.HttpResponseData
import net.spooncast.openmocker.lib.model.CachedResponse
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * OkHttp-specific adapter implementation
 *
 * This adapter handles the conversion between OkHttp's Request/Response objects
 * and the generic HttpRequestData/HttpResponseData models used by the mocking engine.
 */
internal class OkHttpAdapter : HttpClientAdapter<Request, Response> {

    override val clientType: String = "OkHttp"

    /**
     * Extracts client-agnostic request data from OkHttp Request
     */
    override fun extractRequestData(clientRequest: Request): HttpRequestData {
        return HttpRequestData(
            method = clientRequest.method,
            path = clientRequest.url.encodedPath,
            url = clientRequest.url.toString(),
            headers = clientRequest.headers.toMultimap()
        )
    }

    /**
     * Creates an OkHttp Response from cached response data
     *
     * This method constructs a mock OkHttp Response that matches the original request
     * but contains the mocked status code, body, and other properties from the cache.
     */
    override fun createMockResponse(originalRequest: Request, mockResponse: CachedResponse): Response {
        return Response.Builder()
            .protocol(Protocol.HTTP_2)
            .request(originalRequest)
            .code(mockResponse.code)
            .message(MOCKER_MESSAGE)
            .body(mockResponse.body.toResponseBody())
            .build()
    }

    /**
     * Extracts client-agnostic response data from OkHttp Response
     */
    override fun extractResponseData(clientResponse: Response): HttpResponseData {
        // Use peekBody to avoid consuming the response body
        val body = try {
            clientResponse.peekBody(Long.MAX_VALUE).string()
        } catch (e: Exception) {
            // Fallback to empty body if reading fails
            ""
        }

        return HttpResponseData(
            code = clientResponse.code,
            body = body,
            headers = clientResponse.headers.toMultimap(),
            isSuccessful = clientResponse.isSuccessful
        )
    }

    /**
     * Validates if the given objects are OkHttp Request and Response types
     */
    override fun isSupported(request: Any?, response: Any?): Boolean {
        return request is Request && response is Response
    }

    /**
     * Checks if the given request is an OkHttp Request
     */
    override fun canHandleRequest(request: Any): Boolean {
        return request is Request
    }

    /**
     * Checks if the given response is an OkHttp Response
     */
    override fun canHandleResponse(response: Any): Boolean {
        return response is Response
    }

    companion object {
        const val MOCKER_MESSAGE = "OpenMocker enabled"
    }
}

