package net.spooncast.openmocker.lib.data.adapter

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import net.spooncast.openmocker.lib.model.CachedResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KtorAdapterTest {

    private val adapter = KtorAdapter()

    /**
     * MockEngine 을 통해 실제 요청을 한 번 보내고, 핸들러가 받은 [HttpRequestData] 와
     * 클라이언트가 받은 [HttpResponse] 를 함께 반환한다.
     */
    private fun exchange(
        url: String,
        method: HttpMethod = HttpMethod.Get,
        responseCode: HttpStatusCode = HttpStatusCode.OK,
        responseBody: String = "server-body",
        headers: Map<String, String> = emptyMap()
    ): Pair<HttpRequestData, HttpResponse> = runBlocking {
        lateinit var captured: HttpRequestData
        val engine = MockEngine { req ->
            captured = req
            respond(
                content = ByteReadChannel(responseBody),
                status = responseCode,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val client = HttpClient(engine)
        val response = client.request(url) {
            this.method = method
            headers.forEach { (k, v) -> header(k, v) }
        }
        client.close()
        captured to response
    }

    @Test
    fun `extractRequestData 는 method, path, url 을 추출한다`() {
        val (requestData, _) = exchange("https://api.example.com/v1/items?q=1", HttpMethod.Post)

        val req = adapter.extractRequestData(requestData)

        assertEquals("POST", req.method)
        assertEquals("/v1/items", req.path)
        assertTrue(req.url.startsWith("https://api.example.com/v1/items"))
    }

    @Test
    fun `extractRequestData 는 헤더를 추출한다`() {
        val (requestData, _) = exchange(
            "https://api.example.com/x",
            headers = mapOf("X-Custom" to "value-1")
        )

        val req = adapter.extractRequestData(requestData)

        assertEquals(listOf("value-1"), req.headers["X-Custom"])
    }

    @Test
    fun `extractResponseData 는 code, body, isSuccessful 을 추출한다`() {
        val (_, response) = exchange("https://api.example.com/x", responseBody = "hello")

        val resp = adapter.extractResponseData(response)

        assertEquals(200, resp.code)
        assertEquals("hello", resp.body)
        assertTrue(resp.isSuccessful)
    }

    @Test
    fun `extractResponseData 는 5xx 응답을 실패로 표시한다`() {
        val (_, response) = exchange(
            "https://api.example.com/x",
            responseCode = HttpStatusCode.InternalServerError
        )

        val resp = adapter.extractResponseData(response)

        assertEquals(500, resp.code)
        assertFalse(resp.isSuccessful)
    }

    @Test
    fun `createMockResponse 는 CachedResponse 로 mock 응답을 만든다`() {
        val (requestData, _) = exchange("https://api.example.com/x")

        val mock = adapter.createMockResponse(requestData, CachedResponse(code = 200, body = "{\"mocked\":true}"))

        assertEquals(200, mock.status.value)
        val body = runBlocking { mock.bodyAsText() }
        assertEquals("{\"mocked\":true}", body)
    }

    @Test
    fun `createMockResponse 는 4xx mock 을 throw 없이 반환한다`() {
        val (requestData, _) = exchange("https://api.example.com/x")

        val mock = adapter.createMockResponse(requestData, CachedResponse(code = 404, body = "{\"error\":\"not found\"}"))

        assertEquals(404, mock.status.value)
        val body = runBlocking { mock.bodyAsText() }
        assertEquals("{\"error\":\"not found\"}", body)
    }

    @Test
    fun `createMockResponse 는 5xx mock 을 throw 없이 반환한다`() {
        val (requestData, _) = exchange("https://api.example.com/x")

        val mock = adapter.createMockResponse(requestData, CachedResponse(code = 500, body = "server error"))

        assertEquals(500, mock.status.value)
        val body = runBlocking { mock.bodyAsText() }
        assertEquals("server error", body)
    }

    @Test
    fun `createMockResponse 반복 호출은 예외 없이 안정적으로 동작한다`() {
        val (requestData, _) = exchange("https://api.example.com/x")

        repeat(100) { i ->
            val mock = adapter.createMockResponse(requestData, CachedResponse(code = 200, body = "body-$i"))
            assertEquals(200, mock.status.value)
            assertEquals("body-$i", runBlocking { mock.bodyAsText() })
        }
    }

    @Test
    fun `extractResponseData 호출 후에도 원본 응답 본문을 다시 읽을 수 있다`() {
        val (_, response) = exchange("https://api.example.com/x", responseBody = "downstream-body")

        val resp = adapter.extractResponseData(response)
        assertEquals("downstream-body", resp.body)

        // call.save() 로 비소비 읽기를 했으므로 다운스트림이 본문을 다시 읽을 수 있어야 한다.
        val reread = runBlocking { response.bodyAsText() }
        assertEquals("downstream-body", reread)
    }
}
