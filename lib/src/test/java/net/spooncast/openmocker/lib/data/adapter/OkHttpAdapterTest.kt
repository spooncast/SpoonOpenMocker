package net.spooncast.openmocker.lib.data.adapter

import net.spooncast.openmocker.lib.model.CachedResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OkHttpAdapterTest {

    private val adapter = OkHttpAdapter()

    private fun request(url: String, method: String = "GET"): Request {
        val builder = Request.Builder().url(url)
        if (method != "GET") {
            builder.method(method, "".toRequestBody())
        }
        return builder.build()
    }

    private fun response(request: Request, code: Int, body: String): Response {
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("msg")
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()
    }

    @Test
    fun `extractRequestData 는 method, path, url 을 추출한다`() {
        val req = adapter.extractRequestData(request("https://api.example.com/v1/users?page=1", "POST"))

        assertEquals("POST", req.method)
        assertEquals("/v1/users", req.path)
        assertEquals("https://api.example.com/v1/users?page=1", req.url)
    }

    @Test
    fun `extractRequestData 는 헤더를 추출한다`() {
        val request = Request.Builder()
            .url("https://api.example.com/x")
            .header("Authorization", "Bearer token")
            .build()

        val req = adapter.extractRequestData(request)

        assertEquals(listOf("Bearer token"), req.headers["Authorization"])
    }

    @Test
    fun `extractResponseData 는 code, body, isSuccessful 을 추출한다`() {
        val request = request("https://api.example.com/x")
        val resp = adapter.extractResponseData(response(request, 200, "{\"ok\":true}"))

        assertEquals(200, resp.code)
        assertEquals("{\"ok\":true}", resp.body)
        assertTrue(resp.isSuccessful)
    }

    @Test
    fun `extractResponseData 는 4xx 응답을 실패로 표시한다`() {
        val request = request("https://api.example.com/x")
        val resp = adapter.extractResponseData(response(request, 404, "not found"))

        assertEquals(404, resp.code)
        assertFalse(resp.isSuccessful)
    }

    @Test
    fun `extractResponseData 는 peekBody 를 사용하므로 원본 응답 body 를 소비하지 않는다`() {
        val request = request("https://api.example.com/x")
        val original = response(request, 200, "payload")

        adapter.extractResponseData(original)

        // peekBody 로 읽었으므로 원본 body 는 그대로 다시 읽을 수 있어야 한다.
        assertEquals("payload", original.body!!.string())
    }

    @Test
    fun `createMockResponse 는 CachedResponse 로 mock 응답을 만든다`() {
        val request = request("https://api.example.com/x")
        val mock = adapter.createMockResponse(request, CachedResponse(code = 503, body = "maintenance"))

        assertEquals(503, mock.code)
        assertEquals("maintenance", mock.body!!.string())
        assertEquals(OkHttpAdapter.MOCKER_MESSAGE, mock.message)
        assertEquals(request, mock.request)
    }
}
