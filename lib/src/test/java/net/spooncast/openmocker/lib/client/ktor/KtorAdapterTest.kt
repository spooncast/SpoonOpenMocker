package net.spooncast.openmocker.lib.client.ktor

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import net.spooncast.openmocker.lib.model.CachedResponse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Assertions.*
import io.mockk.mockk
import io.mockk.every
import io.mockk.coEvery
import kotlinx.coroutines.test.runTest

@DisplayName("KtorAdapter 테스트")
class KtorAdapterTest {

    private val adapter = KtorAdapter()

    @Nested
    @DisplayName("주어진 조건: KtorAdapter 인스턴스")
    inner class AdapterPropertiesTests {

        @Test
        @DisplayName("""
        [주어진 조건: KtorAdapter 인스턴스]
        [실행: 클라이언트 타입 조회]
        [예상 결과: 'Ktor' 반환]
        """)
        fun `should return Ktor as client type`() {
            assertEquals("Ktor", adapter.clientType)
        }
    }

    @Nested
    @DisplayName("주어진 조건: Ktor HttpRequestData 객체")
    inner class RequestExtractionTests {

        @Test
        @DisplayName("""
        [주어진 조건: 메소드, URL, 헤더를 포함한 Ktor HttpRequestData]
        [실행: 요청 데이터 추출]
        [예상 결과: HttpRequestData로 올바르게 변환]
        """)
        fun `should extract request data correctly`() = runTest {
            // Given
            val builder = HttpRequestBuilder().apply {
                method = HttpMethod.Get
                url("https://api.example.com/api/v1/users?page=1")
                headers {
                    append("Authorization", "Bearer token123")
                    append("Accept", "application/json")
                }
            }
            val requestData = builder.build()

            // When
            val extractedData = adapter.extractRequestData(requestData)

            // Then
            assertEquals("GET", extractedData.method)
            assertEquals("/api/v1/users", extractedData.path)
            assertEquals("https://api.example.com/api/v1/users?page=1", extractedData.url)
            assertEquals("Bearer token123", extractedData.getHeader("Authorization"))
            assertEquals("application/json", extractedData.getHeader("Accept"))
        }

        @Test
        @DisplayName("""
        [주어진 조건: 동일한 이름의 다중 헤더를 포함한 Ktor HttpRequestData]
        [실행: 요청 데이터 추출]
        [예상 결과: 다중 헤더를 올바르게 처리]
        """)
        fun `should handle request with multiple headers`() = runTest {
            // Given
            val builder = HttpRequestBuilder().apply {
                method = HttpMethod.Post
                url("https://api.example.com/api/v1/data")
                headers {
                    append("Accept", "application/json")
                    append("Accept", "text/plain")
                    append("Custom-Header", "value1")
                }
            }
            val requestData = builder.build()

            // When
            val extractedData = adapter.extractRequestData(requestData)

            // Then
            assertEquals("POST", extractedData.method)
            assertEquals("/api/v1/data", extractedData.path)

            val acceptHeaders = extractedData.getHeaders("Accept")
            assertEquals(2, acceptHeaders.size)
            assertTrue(acceptHeaders.contains("application/json"))
            assertTrue(acceptHeaders.contains("text/plain"))

            assertEquals("value1", extractedData.getHeader("Custom-Header"))
        }

        @Test
        @DisplayName("""
        [주어진 조건: 루트 경로를 가진 Ktor HttpRequestData]
        [실행: 요청 데이터 추출]
        [예상 결과: 루트 경로를 올바르게 처리]
        """)
        fun `should handle root path correctly`() = runTest {
            // Given
            val builder = HttpRequestBuilder().apply {
                method = HttpMethod.Get
                url("https://api.example.com/")
            }
            val requestData = builder.build()

            // When
            val extractedData = adapter.extractRequestData(requestData)

            // Then
            assertEquals("GET", extractedData.method)
            assertEquals("/", extractedData.path)
            assertEquals("https://api.example.com/", extractedData.url)
        }
    }

    @Nested
    @DisplayName("주어진 조건: 목 응답 생성")
    inner class MockResponseCreationTests {

        @Test
        @DisplayName("""
        [주어진 조건: 원본 Ktor HttpRequestData와 CachedResponse]
        [실행: 목 응답 생성]
        [예상 결과: 적절한 Ktor HttpResponse 생성]
        """)
        fun `should create proper mock response`() = runTest {
            // Given
            val builder = HttpRequestBuilder().apply {
                method = HttpMethod.Get
                url("https://api.example.com/api/v1/test")
            }
            val originalRequest = builder.build()

            val mockResponse = CachedResponse(
                code = 201,
                body = """{"id": 123, "name": "test"}""",
                duration = 1000L
            )

            // When
            val response = adapter.createMockResponse(originalRequest, mockResponse)

            // Then
            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals("""{"id": 123, "name": "test"}""", response.bodyAsText())
            assertEquals(originalRequest.url, response.call.request.url)
        }

        @Test
        @DisplayName("""
        [주어진 조건: 빈 바디를 가진 CachedResponse]
        [실행: 목 응답 생성]
        [예상 결과: 빈 바디를 포함한 적절한 응답 생성]
        """)
        fun `should create mock response with empty body`() = runTest {
            // Given
            val builder = HttpRequestBuilder().apply {
                method = HttpMethod.Delete
                url("https://api.example.com/api/v1/test/123")
            }
            val originalRequest = builder.build()

            val mockResponse = CachedResponse(
                code = 204,
                body = "",
                duration = 500L
            )

            // When
            val response = adapter.createMockResponse(originalRequest, mockResponse)

            // Then
            assertEquals(HttpStatusCode.NoContent, response.status)
            assertEquals("", response.bodyAsText())
            assertEquals(originalRequest.url, response.call.request.url)
        }

        @Test
        @DisplayName("""
        [주어진 조건: 에러 상태 코드를 가진 CachedResponse]
        [실행: 목 응답 생성]
        [예상 결과: 에러 상태 코드를 포함한 적절한 응답 생성]
        """)
        fun `should create mock response with error status code`() = runTest {
            // Given
            val builder = HttpRequestBuilder().apply {
                method = HttpMethod.Get
                url("https://api.example.com/api/v1/notfound")
            }
            val originalRequest = builder.build()

            val mockResponse = CachedResponse(
                code = 404,
                body = """{"error": "Not Found"}""",
                duration = 100L
            )

            // When
            val response = adapter.createMockResponse(originalRequest, mockResponse)

            // Then
            assertEquals(HttpStatusCode.NotFound, response.status)
            assertEquals("""{"error": "Not Found"}""", response.bodyAsText())
        }
    }

    @Nested
    @DisplayName("주어진 조건: Ktor HttpResponse 객체")
    inner class ResponseExtractionTests {

        @Test
        @DisplayName("""
        [주어진 조건: 코드, 바디, 헤더를 포함한 Ktor HttpResponse]
        [실행: 응답 데이터 추출]
        [예상 결과: HttpResponseData로 올바르게 변환]
        """)
        fun `should extract response data correctly`() = runTest {
            // Given - Create a real response using MockEngine
            val mockEngine = MockEngine { request ->
                respond(
                    content = """{"message": "success"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(
                        "Content-Type" to listOf("application/json; charset=utf-8"),
                        "Server" to listOf("nginx/1.18"),
                        "Custom-Header" to listOf("value1", "value2")
                    )
                )
            }

            val client = HttpClient(mockEngine)
            val response = client.get("https://example.com/test")
            client.close()

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
        [주어진 조건: 바디가 없는 Ktor HttpResponse]
        [실행: 응답 데이터 추출]
        [예상 결과: 빈 바디를 올바르게 처리]
        """)
        fun `should handle response with no body`() = runTest {
            // Given - Create a real response using MockEngine
            val mockEngine = MockEngine { request ->
                respond(
                    content = "",
                    status = HttpStatusCode.NoContent,
                    headers = headersOf()
                )
            }

            val client = HttpClient(mockEngine)
            val response = client.get("https://example.com/test")
            client.close()

            // When
            val responseData = adapter.extractResponseData(response)

            // Then
            assertEquals(204, responseData.code)
            assertEquals("", responseData.body)
            assertTrue(responseData.isSuccessful)
        }

        @Test
        @DisplayName("""
        [주어진 조건: 에러 상태의 Ktor HttpResponse]
        [실행: 응답 데이터 추출]
        [예상 결과: 에러 상태를 올바르게 처리]
        """)
        fun `should handle error response correctly`() = runTest {
            // Given - Create a real response using MockEngine
            val mockEngine = MockEngine { request ->
                respond(
                    content = """{"error": "Internal Server Error"}""",
                    status = HttpStatusCode.InternalServerError,
                    headers = headersOf(
                        "Content-Type" to listOf("application/json")
                    )
                )
            }

            val client = HttpClient(mockEngine)
            val response = client.get("https://example.com/test")
            client.close()

            // When
            val responseData = adapter.extractResponseData(response)

            // Then
            assertEquals(500, responseData.code)
            assertEquals("""{"error": "Internal Server Error"}""", responseData.body)
            assertFalse(responseData.isSuccessful)
            assertEquals("application/json", responseData.getHeader("Content-Type"))
        }
    }

    @Nested
    @DisplayName("주어진 조건: 타입 검증 요구사항")
    inner class TypeValidationTests {

        @Test
        @DisplayName("""
        [주어진 조건: 다양한 객체 타입]
        [실행: 어댑터가 요청 및 응답 타입을 지원하는지 확인]
        [예상 결과: Ktor 타입을 올바르게 검증]
        """)
        fun `should validate Ktor types correctly`() = runTest {
            // Given
            val builder = HttpRequestBuilder().apply {
                method = HttpMethod.Get
                url("https://example.com")
            }
            val request = builder.build()

            val response: HttpResponse = mockk {
                every { status } returns HttpStatusCode.OK
                every { headers } returns headersOf()
            }

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
        [예상 결과: Ktor HttpRequestData 타입을 올바르게 검증]
        """)
        fun `should validate Ktor HttpRequestData type`() = runTest {
            // Given
            val builder = HttpRequestBuilder().apply {
                method = HttpMethod.Get
                url("https://example.com")
            }
            val request = builder.build()

            // When & Then
            assertTrue(adapter.canHandleRequest(request))
            assertFalse(adapter.canHandleRequest("not a request"))
            assertFalse(adapter.canHandleRequest(123))
        }

        @Test
        @DisplayName("""
        [주어진 조건: 다양한 응답 객체 타입]
        [실행: 어댑터가 응답을 처리할 수 있는지 확인]
        [예상 결과: Ktor HttpResponse 타입을 올바르게 검증]
        """)
        fun `should validate Ktor HttpResponse type`() = runTest {
            // Given
            val response: HttpResponse = mockk {
                every { status } returns HttpStatusCode.OK
                every { headers } returns headersOf()
            }

            // When & Then
            assertTrue(adapter.canHandleResponse(response))
            assertFalse(adapter.canHandleResponse("not a response"))
            assertFalse(adapter.canHandleResponse(456))
        }
    }

    @Nested
    @DisplayName("주어진 조건: 헤더 처리")
    inner class HeaderHandlingTests {

        @Test
        @DisplayName("""
        [주어진 조건: 대소문자 혼용 헤더 이름을 가진 Ktor 요청]
        [실행: 헤더 추출]
        [예상 결과: 대소문자 구분 없이 헤더를 올바르게 처리]
        """)
        fun `should handle case-insensitive header names in request`() = runTest {
            // Given
            val builder = HttpRequestBuilder().apply {
                method = HttpMethod.Get
                url("https://api.example.com/test")
                headers {
                    append("content-type", "application/json")
                    append("AUTHORIZATION", "Bearer token")
                    append("User-Agent", "Test-Client")
                }
            }
            val requestData = builder.build()

            // When
            val extractedData = adapter.extractRequestData(requestData)

            // Then
            assertEquals("application/json", extractedData.getHeader("Content-Type"))
            assertEquals("application/json", extractedData.getHeader("content-type"))
            assertEquals("Bearer token", extractedData.getHeader("authorization"))
            assertEquals("Bearer token", extractedData.getHeader("AUTHORIZATION"))
            assertEquals("Test-Client", extractedData.getHeader("user-agent"))
        }

        @Test
        @DisplayName("""
        [주어진 조건: 헤더가 없는 Ktor 요청]
        [실행: 헤더 추출]
        [예상 결과: 빈 헤더 맵을 올바르게 처리]
        """)
        fun `should handle request with no headers`() = runTest {
            // Given
            val builder = HttpRequestBuilder().apply {
                method = HttpMethod.Get
                url("https://api.example.com/test")
            }
            val requestData = builder.build()

            // When
            val extractedData = adapter.extractRequestData(requestData)

            // Then
            assertNull(extractedData.getHeader("Authorization"))
            assertTrue(extractedData.getHeaders("Accept").isEmpty())
        }
    }

    @Nested
    @DisplayName("주어진 조건: 예외 상황 처리")
    inner class ErrorHandlingTests {

        @Test
        @DisplayName("""
        [주어진 조건: 바디 읽기 실패 시나리오]
        [실행: 응답 데이터 추출]
        [예상 결과: 예외 상황에서 빈 바디 반환]
        """)
        fun `should handle body reading failure gracefully`() = runTest {
            // Given - Mock a problematic response by using a mock directly
            val response: HttpResponse = mockk(relaxed = true) {
                every { status } returns HttpStatusCode.OK
                every { headers } returns headersOf()
                coEvery { bodyAsText() } throws RuntimeException("Body reading failed")
            }

            // When
            val responseData = adapter.extractResponseData(response)

            // Then
            assertEquals(200, responseData.code)
            assertEquals("", responseData.body) // Should fallback to empty string
            assertTrue(responseData.isSuccessful)
        }
    }
}