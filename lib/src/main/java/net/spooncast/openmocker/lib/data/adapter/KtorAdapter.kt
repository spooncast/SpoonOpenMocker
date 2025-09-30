package net.spooncast.openmocker.lib.data.adapter

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
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
import net.spooncast.openmocker.lib.model.HttpResp

internal class KtorAdapter: HttpClientAdapter<HttpRequestData, HttpResponse> {

    override fun extractRequestData(clientRequest: HttpRequestData): net.spooncast.openmocker.lib.model.HttpReq {
        return net.spooncast.openmocker.lib.model.HttpReq(
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

    override fun extractResponseData(clientResponse: HttpResponse): HttpResp {
        val body = try {
            runBlocking {
                clientResponse.bodyAsText()
            }
        } catch (e: Exception) {
            // Fallback to empty body if reading fails
            ""
        }

        return HttpResp(
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

    override fun createMockResponse(originalRequest: HttpRequestData, mockResponse: CachedResponse): HttpResponse {
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

            // ResponseValidator를 이용하여, HttpStatusCode에 따른 Exception 유발
            expectSuccess = true
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