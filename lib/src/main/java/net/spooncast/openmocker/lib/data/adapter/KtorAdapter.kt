package net.spooncast.openmocker.lib.data.adapter

import io.ktor.client.HttpClient
import io.ktor.client.call.save
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
                // call.save() 로 응답을 버퍼링한 사본에서 본문을 읽어 원본 채널을 소비하지
                // 않는다. onResponse 캐싱 경로에서 앱 다운스트림이 본문을 재독할 수 있다(KA-C).
                clientResponse.call.save().response.bodyAsText()
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

        // MockEngine 핸들러가 per-call CachedResponse(body/code)를 캡처하므로 단일
        // HttpClient 재사용은 불가능하다. per-call 클라이언트를 만들고 finally 로 close 를 보장한다.
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

            // expectSuccess=true 는 4xx/5xx 에서 ResponseValidator 예외를 던져 mock 에러를
            // 그대로 반환하지 못하게 한다(KA-B). 제거해 mock 응답을 status 그대로 반환한다.
        }

        return runBlocking {
            try {
                client.request {
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
            } finally {
                client.close()
            }
        }
    }
}