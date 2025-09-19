package net.spooncast.openmocker.lib.client.okhttp

import net.spooncast.openmocker.lib.model.CachedResponse
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Assertions.*

@DisplayName("OkHttpAdapter 테스트")
class OkHttpAdapterTest {

    private val adapter = OkHttpAdapter()

    @Nested
    @DisplayName("주어진 조건: OkHttpAdapter 인스턴스")
    inner class AdapterPropertiesTests {

        @Test
        @DisplayName("""
        [주어진 조건: OkHttpAdapter 인스턴스]
        [실행: 클라이언트 타입 조회]
        [예상 결과: 'OkHttp' 반환]
        """)
        fun `should return OkHttp as client type`() {
            assertEquals("OkHttp", adapter.clientType)
        }
    }

    @Nested
    @DisplayName("주어진 조건: OkHttp Request 객체")
    inner class RequestExtractionTests {

        @Test
        @DisplayName("""
        [주어진 조건: 메소드, URL, 헤더를 포함한 OkHttp Request]
        [실행: 요청 데이터 추출]
        [예상 결과: HttpRequestData로 올바르게 변환]
        """)
        fun `should extract request data correctly`() {
            // Given
            val request = Request.Builder()
                .url("https://api.example.com/api/v1/users?page=1")
                .method("GET", null)
                .addHeader("Authorization", "Bearer token123")
                .addHeader("Accept", "application/json")
                .build()

            // When
            val requestData = adapter.extractRequestData(request)

            // Then
            assertEquals("GET", requestData.method)
            assertEquals("/api/v1/users", requestData.path)
            assertEquals("https://api.example.com/api/v1/users?page=1", requestData.url)
            assertEquals("Bearer token123", requestData.getHeader("Authorization"))
            assertEquals("application/json", requestData.getHeader("Accept"))
        }

        @Test
        @DisplayName("""
        [주어진 조건: 동일한 이름의 다중 헤더를 포함한 OkHttp Request]
        [실행: 요청 데이터 추출]
        [예상 결과: 다중 헤더를 올바르게 처리]
        """)
        fun `should handle request with multiple headers`() {
            // Given
            val requestBody = "test body".toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://api.example.com/api/v1/data")
                .method("POST", requestBody)
                .addHeader("Accept", "application/json")
                .addHeader("Accept", "text/plain")
                .addHeader("Custom-Header", "value1")
                .build()

            // When
            val requestData = adapter.extractRequestData(request)

            // Then
            assertEquals("POST", requestData.method)
            assertEquals("/api/v1/data", requestData.path)

            val acceptHeaders = requestData.getHeaders("Accept")
            assertEquals(2, acceptHeaders.size)
            assertTrue(acceptHeaders.contains("application/json"))
            assertTrue(acceptHeaders.contains("text/plain"))

            assertEquals("value1", requestData.getHeader("Custom-Header"))
        }
    }

    @Nested
    @DisplayName("주어진 조건: 목 응답 생성")
    inner class MockResponseCreationTests {

        @Test
        @DisplayName("""
        [주어진 조건: 원본 OkHttp Request와 CachedResponse]
        [실행: 목 응답 생성]
        [예상 결과: 적절한 OkHttp Response 생성]
        """)
        fun `should create proper mock response`() {
            // Given
            val originalRequest = Request.Builder()
                .url("https://api.example.com/api/v1/test")
                .build()

            val mockResponse = CachedResponse(
                code = 201,
                body = """{"id": 123, "name": "test"}""",
                duration = 1000L
            )

            // When
            val response = adapter.createMockResponse(originalRequest, mockResponse)

            // Then
            assertEquals(201, response.code)
            assertEquals("OpenMocker enabled", response.message)
            assertEquals(Protocol.HTTP_2, response.protocol)
            assertEquals(originalRequest, response.request)
            assertEquals("""{"id": 123, "name": "test"}""", response.body?.string())
        }
    }

    @Nested
    @DisplayName("주어진 조건: OkHttp Response 객체")
    inner class ResponseExtractionTests {

        @Test
        @DisplayName("""
        [주어진 조건: 코드, 바디, 헤더를 포함한 OkHttp Response]
        [실행: 응답 데이터 추출]
        [예상 결과: HttpResponseData로 올바르게 변환]
        """)
        fun `should extract response data correctly`() {
            // Given
            val request = Request.Builder()
                .url("https://api.example.com/test")
                .build()

            val responseBody = """{"message": "success"}""".toResponseBody("application/json".toMediaType())
            val headers = Headers.Builder()
                .add("Content-Type", "application/json; charset=utf-8")
                .add("Server", "nginx/1.18")
                .add("Custom-Header", "value1")
                .add("Custom-Header", "value2")
                .build()

            val response = Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_2)
                .code(200)
                .message("OK")
                .headers(headers)
                .body(responseBody)
                .build()

            // When
            val responseData = adapter.extractResponseData(response)

            // Then
            assertEquals(200, responseData.code)
            assertEquals("""{"message": "success"}""", responseData.body)
            assertTrue(responseData.isSuccessful)
            assertEquals("application/json; charset=utf-8", responseData.getHeader("Content-Type"))
            assertEquals("nginx/1.18", responseData.getHeader("Server"))

            val customHeaders = responseData.getHeaders("Custom-Header")
            assertEquals(2, customHeaders.size)
            assertTrue(customHeaders.contains("value1"))
            assertTrue(customHeaders.contains("value2"))
        }

        @Test
        @DisplayName("""
        [주어진 조건: 바디가 없는 OkHttp Response]
        [실행: 응답 데이터 추출]
        [예상 결과: 빈 바디를 올바르게 처리]
        """)
        fun `should handle response with no body`() {
            // Given
            val request = Request.Builder()
                .url("https://api.example.com/test")
                .build()

            val response = Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_2)
                .code(204)
                .message("No Content")
                .body("".toResponseBody())
                .build()

            // When
            val responseData = adapter.extractResponseData(response)

            // Then
            assertEquals(204, responseData.code)
            assertEquals("", responseData.body)
            assertTrue(responseData.isSuccessful)
        }
    }

    @Nested
    @DisplayName("주어진 조건: 타입 검증 요구사항")
    inner class TypeValidationTests {

        @Test
        @DisplayName("""
        [주어진 조건: 다양한 객체 타입]
        [실행: 어댑터가 요청 및 응답 타입을 지원하는지 확인]
        [예상 결과: OkHttp 타입을 올바르게 검증]
        """)
        fun `should validate OkHttp types correctly`() {
            // Given
            val request = Request.Builder().url("https://example.com").build()
            val response = Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_2)
                .code(200)
                .message("OK")
                .body("".toResponseBody())
                .build()

            // When & Then
            assertTrue(adapter.isSupported(request, response))
            assertFalse(adapter.isSupported(request, "not a response"))
            assertFalse(adapter.isSupported("not a request", response))
            assertFalse(adapter.isSupported(null, response))
            assertFalse(adapter.isSupported(request, null))
        }

        @Test
        @DisplayName("""
        [주어진 조건: 다양한 요청 객체 타입]
        [실행: 어댑터가 요청을 처리할 수 있는지 확인]
        [예상 결과: OkHttp Request 타입을 올바르게 검증]
        """)
        fun `should validate OkHttp Request type`() {
            // Given
            val request = Request.Builder().url("https://example.com").build()

            // When & Then
            assertTrue(adapter.canHandleRequest(request))
            assertFalse(adapter.canHandleRequest("not a request"))
            assertFalse(adapter.canHandleRequest(123))
        }

        @Test
        @DisplayName("""
        [주어진 조건: 다양한 응답 객체 타입]
        [실행: 어댑터가 응답을 처리할 수 있는지 확인]
        [예상 결과: OkHttp Response 타입을 올바르게 검증]
        """)
        fun `should validate OkHttp Response type`() {
            // Given
            val request = Request.Builder().url("https://example.com").build()
            val response = Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_2)
                .code(200)
                .message("OK")
                .body("".toResponseBody())
                .build()

            // When & Then
            assertTrue(adapter.canHandleResponse(response))
            assertFalse(adapter.canHandleResponse("not a response"))
            assertFalse(adapter.canHandleResponse(456))
        }
    }
}