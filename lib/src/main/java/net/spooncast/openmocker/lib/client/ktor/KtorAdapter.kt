package net.spooncast.openmocker.lib.client.ktor

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.utils.io.*
import kotlinx.coroutines.runBlocking
import net.spooncast.openmocker.lib.core.HttpClientAdapter
import net.spooncast.openmocker.lib.core.HttpRequestData
import net.spooncast.openmocker.lib.core.HttpResponseData
import net.spooncast.openmocker.lib.model.CachedResponse

/**
 * Ktor-specific adapter implementation
 *
 * This adapter handles the conversion between Ktor's HttpRequestData/HttpResponse objects
 * and the generic HttpRequestData/HttpResponseData models used by the mocking engine.
 */
internal class KtorAdapter : HttpClientAdapter<io.ktor.client.request.HttpRequestData, HttpResponse> {

    override val clientType: String = "Ktor"

    /**
     * Extracts client-agnostic request data from Ktor HttpRequestData
     */
    override fun extractRequestData(clientRequest: io.ktor.client.request.HttpRequestData): HttpRequestData {
        return HttpRequestData(
            method = clientRequest.method.value,
            path = clientRequest.url.encodedPath,
            url = clientRequest.url.toString(),
            headers = buildMap {
                clientRequest.headers.forEach { key, values ->
                    put(key, values)
                }
            }
        )
    }

    /**
     * Creates a Ktor HttpResponse from cached response data
     *
     * This method constructs a mock Ktor HttpResponse that matches the original request
     * but contains the mocked status code, body, and other properties from the cache.
     */
    override fun createMockResponse(originalRequest: io.ktor.client.request.HttpRequestData, mockResponse: CachedResponse): HttpResponse {
        val mockEngine = MockEngine { requestData ->
            respond(
                content = ByteReadChannel(mockResponse.body),
                status = HttpStatusCode.fromValue(mockResponse.code),
                headers = headersOf(
                    HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString())
                )
            )
        }

        val client = HttpClient(mockEngine)

        return runBlocking {
            val response = client.request {
                url(originalRequest.url)
                method = originalRequest.method
                originalRequest.headers.forEach { key, values ->
                    headers {
                        values.forEach { value ->
                            append(key, value)
                        }
                    }
                }
            }
            client.close()
            response
        }
    }

    /**
     * Extracts client-agnostic response data from Ktor HttpResponse
     */
    override fun extractResponseData(clientResponse: HttpResponse): HttpResponseData {
        val body = try {
            runBlocking {
                clientResponse.bodyAsText()
            }
        } catch (e: Exception) {
            // Fallback to empty body if reading fails
            ""
        }

        return HttpResponseData(
            code = clientResponse.status.value,
            body = body,
            headers = buildMap {
                clientResponse.headers.forEach { key, values ->
                    put(key, values)
                }
            },
            isSuccessful = clientResponse.status.isSuccess()
        )
    }

    /**
     * Validates if the given objects are Ktor HttpRequestData and HttpResponse types
     */
    override fun isSupported(request: Any?, response: Any?): Boolean {
        return request is io.ktor.client.request.HttpRequestData && response is HttpResponse
    }

    /**
     * Checks if the given request is a Ktor HttpRequestData
     */
    override fun canHandleRequest(request: Any): Boolean {
        return request is io.ktor.client.request.HttpRequestData
    }

    /**
     * Checks if the given response is a Ktor HttpResponse
     */
    override fun canHandleResponse(response: Any): Boolean {
        return response is HttpResponse
    }

    companion object {
        const val MOCKER_MESSAGE = "OpenMocker enabled"
    }
}