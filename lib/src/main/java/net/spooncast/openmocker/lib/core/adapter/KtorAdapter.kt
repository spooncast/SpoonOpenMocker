package net.spooncast.openmocker.lib.core.adapter

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import net.spooncast.openmocker.lib.model.CachedResponse
import net.spooncast.openmocker.lib.model.HttpRequestData
import net.spooncast.openmocker.lib.model.HttpResponseData

/**
 * Ktor-specific adapter implementation
 *
 * This adapter handles the conversion between Ktor's HttpRequestData/HttpResponse objects
 * and the generic HttpRequestData/HttpResponseData models used by the mocking engine.
 */
internal class KtorAdapter : HttpClientAdapter<io.ktor.client.request.HttpRequestData, HttpResponse> {

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

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = false
                    prettyPrint = true
                    coerceInputValues = true
                })
            }
        }

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
}