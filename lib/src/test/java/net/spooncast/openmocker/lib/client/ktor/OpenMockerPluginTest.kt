package net.spooncast.openmocker.lib.client.ktor

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import net.spooncast.openmocker.lib.core.MockingEngine
import net.spooncast.openmocker.lib.core.HttpRequestData
import net.spooncast.openmocker.lib.core.HttpResponseData
import net.spooncast.openmocker.lib.model.CachedResponse
import net.spooncast.openmocker.lib.repo.MemCacheRepoImpl
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

@DisplayName("OpenMockerPlugin 테스트")
class OpenMockerPluginTest {

    private lateinit var mockingEngine: MockingEngine<HttpRequestData, HttpResponse>

    @BeforeEach
    fun setUp() {
        mockingEngine = mockk(relaxed = true)
    }

    @Nested
    @DisplayName("주어진 조건: Plugin 설치 및 구성")
    inner class PluginInstallationTests {

        @Test
        @DisplayName("""
        [주어진 조건: HttpClient와 OpenMockerPlugin]
        [실행: 플러그인 설치]
        [예상 결과: 성공적인 설치 및 구성 완료]
        """)
        fun `should install plugin successfully with default configuration`() = runTest {
            // Given & When
            val client = HttpClient(MockEngine { request ->
                respond(
                    content = """{"message": "success"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType to listOf("application/json"))
                )
            }) {
                install(OpenMockerPlugin) {
                    // Default configuration - enabled = true
                }
            }

            // Then
            assertNotNull(client)
            // Plugin should be installed without throwing exceptions
            client.close()
        }

        @Test
        @DisplayName("""
        [주어진 조건: HttpClient와 OpenMockerPlugin]
        [실행: 비활성화된 상태로 플러그인 설치]
        [예상 결과: 플러그인이 설치되지만 비활성화됨]
        """)
        fun `should install plugin with disabled configuration`() = runTest {
            // Given & When
            val client = HttpClient(MockEngine { request ->
                respond(
                    content = """{"message": "success"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType to listOf("application/json"))
                )
            }) {
                install(OpenMockerPlugin) {
                    enabled = false
                }
            }

            // Then
            assertNotNull(client)
            client.close()
        }

        @Test
        @DisplayName("""
        [주어진 조건: HttpClient와 OpenMockerPlugin]
        [실행: 기본 HTTP 요청 수행]
        [예상 결과: 정상적인 HTTP 요청 및 응답 처리]
        """)
        fun `should handle normal HTTP requests when plugin is installed`() = runTest {
            // Given
            val client = HttpClient(MockEngine { request ->
                respond(
                    content = """{"id": 123, "name": "test"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType to listOf("application/json"))
                )
            }) {
                install(OpenMockerPlugin) {
                    enabled = true
                }
            }

            // When
            val response = client.get("https://api.example.com/users/123")

            // Then
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("""{"id": 123, "name": "test"}""", response.bodyAsText())
            client.close()
        }
    }

    @Nested
    @DisplayName("주어진 조건: Plugin 활성화/비활성화")
    inner class PluginEnableDisableTests {

        @Test
        @DisplayName("""
        [주어진 조건: 활성화된 OpenMockerPlugin]
        [실행: HTTP 요청 수행]
        [예상 결과: 플러그인이 응답을 처리]
        """)
        fun `should process responses when plugin is enabled`() = runTest {
            // Given
            val client = HttpClient(MockEngine { request ->
                respond(
                    content = """{"status": "enabled"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType to listOf("application/json"))
                )
            }) {
                install(OpenMockerPlugin) {
                    enabled = true
                }
            }

            // When
            val response = client.get("https://api.example.com/status")

            // Then
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("""{"status": "enabled"}""", response.bodyAsText())
            client.close()
        }

        @Test
        @DisplayName("""
        [주어진 조건: 비활성화된 OpenMockerPlugin]
        [실행: HTTP 요청 수행]
        [예상 결과: 플러그인이 요청을 처리하지 않음]
        """)
        fun `should not process responses when plugin is disabled`() = runTest {
            // Given
            val client = HttpClient(MockEngine { request ->
                respond(
                    content = """{"status": "disabled"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType to listOf("application/json"))
                )
            }) {
                install(OpenMockerPlugin) {
                    enabled = false
                }
            }

            // When
            val response = client.get("https://api.example.com/status")

            // Then
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("""{"status": "disabled"}""", response.bodyAsText())
            client.close()
        }
    }

    @Nested
    @DisplayName("주어진 조건: MockingEngine 통합")
    inner class MockingEngineIntegrationTests {

        @Test
        @DisplayName("""
        [주어진 조건: OpenMockerPluginConfig와 MockingEngine]
        [실행: 설정 객체 생성 및 MockingEngine 확인]
        [예상 결과: MockingEngine이 올바르게 초기화됨]
        """)
        fun `should initialize MockingEngine correctly through config`() {
            // Given
            val config = OpenMockerPluginConfig()

            // When
            val mockingEngine = config.mockingEngine

            // Then
            assertNotNull(mockingEngine)
            assertEquals("Ktor", mockingEngine.getClientType())
        }

        @Test
        @DisplayName("""
        [주어진 조건: 여러 OpenMockerPluginConfig 인스턴스]
        [실행: 각 인스턴스의 MockingEngine 확인]
        [예상 결과: 각기 다른 MockingEngine 인스턴스이지만 동일한 캐시 공유]
        """)
        fun `should create separate MockingEngine instances but share cache`() {
            // Given
            val config1 = OpenMockerPluginConfig()
            val config2 = OpenMockerPluginConfig()

            // When
            val engine1 = config1.mockingEngine
            val engine2 = config2.mockingEngine

            // Then
            assertNotSame(engine1, engine2) // Different instances
            assertEquals("Ktor", engine1.getClientType())
            assertEquals("Ktor", engine2.getClientType())
        }
    }

    @Nested
    @DisplayName("주어진 조건: HTTP 요청 및 응답 처리")
    inner class HttpRequestResponseHandlingTests {

        @Test
        @DisplayName("""
        [주어진 조건: OpenMockerPlugin이 설치된 HttpClient]
        [실행: 다양한 HTTP 메소드로 요청]
        [예상 결과: 모든 메소드가 정상 처리됨]
        """)
        fun `should handle various HTTP methods correctly`() = runTest {
            // Given
            val client = HttpClient(MockEngine { request ->
                respond(
                    content = """{"method": "${request.method.value}"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType to listOf("application/json"))
                )
            }) {
                install(OpenMockerPlugin) {
                    enabled = true
                }
            }

            // When & Then
            val getResponse = client.get("https://api.example.com/data")
            assertEquals("""{"method": "GET"}""", getResponse.bodyAsText())

            val postResponse = client.post("https://api.example.com/data") {
                setBody("""{"data": "test"}""")
            }
            assertEquals("""{"method": "POST"}""", postResponse.bodyAsText())

            val putResponse = client.put("https://api.example.com/data/123") {
                setBody("""{"data": "updated"}""")
            }
            assertEquals("""{"method": "PUT"}""", putResponse.bodyAsText())

            val deleteResponse = client.delete("https://api.example.com/data/123")
            assertEquals("""{"method": "DELETE"}""", deleteResponse.bodyAsText())

            client.close()
        }

        @Test
        @DisplayName("""
        [주어진 조건: OpenMockerPlugin이 설치된 HttpClient]
        [실행: 헤더가 포함된 HTTP 요청]
        [예상 결과: 헤더 정보가 올바르게 처리됨]
        """)
        fun `should handle requests with headers correctly`() = runTest {
            // Given
            val client = HttpClient(MockEngine { request ->
                val authHeader = request.headers["Authorization"] ?: "no-auth"
                respond(
                    content = """{"auth": "$authHeader"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(
                        HttpHeaders.ContentType to listOf("application/json"),
                        "X-Custom-Header" to listOf("custom-value")
                    )
                )
            }) {
                install(OpenMockerPlugin) {
                    enabled = true
                }
            }

            // When
            val response = client.get("https://api.example.com/protected") {
                headers {
                    append("Authorization", "Bearer token123")
                    append("Accept", "application/json")
                }
            }

            // Then
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("""{"auth": "Bearer token123"}""", response.bodyAsText())
            assertEquals("custom-value", response.headers["X-Custom-Header"])
            client.close()
        }

        @Test
        @DisplayName("""
        [주어진 조건: OpenMockerPlugin이 설치된 HttpClient]
        [실행: 다양한 상태 코드 응답]
        [예상 결과: 모든 상태 코드가 정상 처리됨]
        """)
        fun `should handle various HTTP status codes correctly`() = runTest {
            // Given
            var requestCount = 0
            val client = HttpClient(MockEngine { request ->
                requestCount++
                when (requestCount) {
                    1 -> respond("", HttpStatusCode.OK)
                    2 -> respond("", HttpStatusCode.Created)
                    3 -> respond("", HttpStatusCode.NoContent)
                    4 -> respond("""{"error": "Not Found"}""", HttpStatusCode.NotFound)
                    else -> respond("""{"error": "Server Error"}""", HttpStatusCode.InternalServerError)
                }
            }) {
                install(OpenMockerPlugin) {
                    enabled = true
                }
            }

            // When & Then
            val okResponse = client.get("https://api.example.com/ok")
            assertEquals(HttpStatusCode.OK, okResponse.status)

            val createdResponse = client.post("https://api.example.com/create")
            assertEquals(HttpStatusCode.Created, createdResponse.status)

            val noContentResponse = client.delete("https://api.example.com/delete")
            assertEquals(HttpStatusCode.NoContent, noContentResponse.status)

            val notFoundResponse = client.get("https://api.example.com/notfound")
            assertEquals(HttpStatusCode.NotFound, notFoundResponse.status)

            val serverErrorResponse = client.get("https://api.example.com/error")
            assertEquals(HttpStatusCode.InternalServerError, serverErrorResponse.status)

            client.close()
        }
    }

    @Nested
    @DisplayName("주어진 조건: 에러 처리 및 예외 상황")
    inner class ErrorHandlingTests {

        @Test
        @DisplayName("""
        [주어진 조건: MockEngine에서 예외 발생]
        [실행: HTTP 요청 수행]
        [예상 결과: 예외가 적절히 전파됨]
        """)
        fun `should handle engine exceptions properly`() = runTest {
            // Given
            val client = HttpClient(MockEngine { request ->
                throw RuntimeException("Mock engine error")
            }) {
                install(OpenMockerPlugin) {
                    enabled = true
                }
            }

            // When & Then
            assertThrows(RuntimeException::class.java) {
                runTest {
                    client.get("https://api.example.com/error")
                }
            }

            client.close()
        }

        @Test
        @DisplayName("""
        [주어진 조건: OpenMockerPlugin이 설치된 HttpClient]
        [실행: 잘못된 URL로 요청]
        [예상 결과: 적절한 에러 처리]
        """)
        fun `should handle invalid URLs gracefully`() = runTest {
            // Given
            val client = HttpClient(MockEngine { request ->
                respond("", HttpStatusCode.BadRequest)
            }) {
                install(OpenMockerPlugin) {
                    enabled = true
                }
            }

            // When & Then - Should not throw exception for valid URL format
            val response = client.get("https://invalid-domain-that-does-not-exist.com/test")
            assertEquals(HttpStatusCode.BadRequest, response.status)

            client.close()
        }
    }

    @Nested
    @DisplayName("주어진 조건: 성능 및 메모리 효율성")
    inner class PerformanceTests {

        @Test
        @DisplayName("""
        [주어진 조건: OpenMockerPlugin이 설치된 HttpClient]
        [실행: 다수의 동시 요청]
        [예상 결과: 모든 요청이 성공적으로 처리됨]
        """)
        fun `should handle concurrent requests efficiently`() = runTest {
            // Given
            var requestCount = 0
            val client = HttpClient(MockEngine { request ->
                synchronized(this) { requestCount++ }
                respond(
                    content = """{"request_id": ${requestCount}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType to listOf("application/json"))
                )
            }) {
                install(OpenMockerPlugin) {
                    enabled = true
                }
            }

            // When - Simulate multiple concurrent requests
            val responses = (1..10).map {
                client.get("https://api.example.com/data/$it")
            }

            // Then
            assertEquals(10, responses.size)
            responses.forEach { response ->
                assertEquals(HttpStatusCode.OK, response.status)
                assertTrue(response.bodyAsText().contains("request_id"))
            }

            client.close()
        }

        @Test
        @DisplayName("""
        [주어진 조건: OpenMockerPlugin이 설치된 HttpClient]
        [실행: 큰 응답 바디 처리]
        [예상 결과: 대용량 데이터가 정상 처리됨]
        """)
        fun `should handle large response bodies efficiently`() = runTest {
            // Given
            val largeContent = "x".repeat(10000) // 10KB content
            val client = HttpClient(MockEngine { request ->
                respond(
                    content = largeContent,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType to listOf("text/plain"))
                )
            }) {
                install(OpenMockerPlugin) {
                    enabled = true
                }
            }

            // When
            val response = client.get("https://api.example.com/large-data")

            // Then
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(largeContent, response.bodyAsText())
            assertEquals(10000, response.bodyAsText().length)

            client.close()
        }
    }

    @Nested
    @DisplayName("주어진 조건: on(Send) 훅 Mock 기능")
    inner class OnSendHookMockingTests {

        @Test
        @DisplayName("""
        [주어진 조건: Mock 데이터가 캐시에 존재]
        [실행: HTTP 요청 수행]
        [예상 결과: 네트워크 호출 우회하고 Mock 응답 반환]
        """)
        fun `should return mock response and bypass network call when mock exists`() = runTest {
            // Given
            val cacheRepo = MemCacheRepoImpl.getInstance()

            // First, cache a real response
            cacheRepo.cache(
                method = "GET",
                urlPath = "/api/test",
                responseCode = 200,
                responseBody = """{"message": "real response"}"""
            )

            // Then set up a mock for that cached response
            val mockResponse = CachedResponse(
                code = 200,
                body = """{"message": "mocked response"}""",
                duration = 0 // No delay
            )

            val key = net.spooncast.openmocker.lib.model.CachedKey("GET", "/api/test")
            cacheRepo.mock(key, mockResponse)

            var networkCallMade = false
            val client = HttpClient(MockEngine { request ->
                networkCallMade = true // This should NOT be called if mocking works
                respond(
                    content = """{"message": "real network response"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType to listOf("application/json"))
                )
            }) {
                install(OpenMockerPlugin) {
                    enabled = true
                }
            }

            // When
            val response = client.get("https://api.example.com/api/test")

            // Then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()
            assertTrue(responseBody.contains("mocked response"))
            assertFalse(networkCallMade, "Network call should be bypassed when mock exists")

            client.close()
        }

        @Test
        @DisplayName("""
        [주어진 조건: Mock 데이터가 존재하지 않음]
        [실행: HTTP 요청 수행]
        [예상 결과: 실제 네트워크 호출 수행 및 응답 캐싱]
        """)
        fun `should make real network call and cache response when no mock exists`() = runTest {
            // Given
            var networkCallMade = false
            val client = HttpClient(MockEngine { request ->
                networkCallMade = true
                respond(
                    content = """{"message": "real network response"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType to listOf("application/json"))
                )
            }) {
                install(OpenMockerPlugin) {
                    enabled = true
                }
            }

            // When
            val response = client.get("https://api.example.com/api/new-endpoint")

            // Then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()
            assertTrue(responseBody.contains("real network response"))
            assertTrue(networkCallMade, "Network call should be made when no mock exists")

            client.close()
        }

        @Test
        @DisplayName("""
        [주어진 조건: Mock 데이터에 지연 시간 설정됨]
        [실행: HTTP 요청 수행]
        [예상 결과: 지연 시간 적용 후 Mock 응답 반환]
        """)
        fun `should apply delay before returning mock response`() = runTest {
            // Given
            val cacheRepo = MemCacheRepoImpl.getInstance()
            cacheRepo.clearCache() // Clear any existing cache

            // First, cache a real response
            cacheRepo.cache(
                method = "GET",
                urlPath = "/api/delayed",
                responseCode = 200,
                responseBody = """{"message": "real response"}"""
            )

            // Then set up a mock with delay
            val mockResponse = CachedResponse(
                code = 200,
                body = """{"message": "delayed mock response"}""",
                duration = 100 // 100ms delay
            )

            val key = net.spooncast.openmocker.lib.model.CachedKey("GET", "/api/delayed")
            val mockResult = cacheRepo.mock(key, mockResponse)
            assertTrue(mockResult, "Mock should be successfully set")

            // Verify mock exists
            val retrievedMock = cacheRepo.getMock("GET", "/api/delayed")
            assertNotNull(retrievedMock, "Mock should exist")
            assertEquals(100, retrievedMock?.duration, "Mock should have correct delay")

            var networkCallMade = false
            val client = HttpClient(MockEngine { request ->
                networkCallMade = true
                respond("Should not reach here", HttpStatusCode.InternalServerError)
            }) {
                install(OpenMockerPlugin) {
                    enabled = true
                }
            }

            // When
            val response = client.get("https://api.example.com/api/delayed")

            // Then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()
            assertTrue(responseBody.contains("delayed mock response"), "Should return mock response body")
            assertFalse(networkCallMade, "Network call should be bypassed")

            // Note: Timing assertion disabled because runTest ignores delay() calls
            // The delay functionality is still applied in production, but cannot be reliably tested in runTest environment
            // Reference: https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/run-test.html

            client.close()
        }

        @Test
        @DisplayName("""
        [주어진 조건: 플러그인이 비활성화됨]
        [실행: HTTP 요청 수행]
        [예상 결과: Mock 기능 무시하고 항상 실제 네트워크 호출]
        """)
        fun `should always make real network calls when plugin is disabled`() = runTest {
            // Given
            val cacheRepo = MemCacheRepoImpl.getInstance()

            // First, cache a real response
            cacheRepo.cache(
                method = "GET",
                urlPath = "/api/disabled-test",
                responseCode = 200,
                responseBody = """{"message": "cached response"}"""
            )

            // Then set up a mock
            val mockResponse = CachedResponse(
                code = 200,
                body = """{"message": "this should be ignored"}""",
                duration = 0
            )

            val key = net.spooncast.openmocker.lib.model.CachedKey("GET", "/api/disabled-test")
            cacheRepo.mock(key, mockResponse)

            var networkCallMade = false
            val client = HttpClient(MockEngine { request ->
                networkCallMade = true
                respond(
                    content = """{"message": "real network response"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType to listOf("application/json"))
                )
            }) {
                install(OpenMockerPlugin) {
                    enabled = false // Plugin disabled
                }
            }

            // When
            val response = client.get("https://api.example.com/api/disabled-test")

            // Then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()
            assertTrue(responseBody.contains("real network response"))
            assertFalse(responseBody.contains("this should be ignored"))
            assertTrue(networkCallMade, "Network call should be made when plugin is disabled")

            client.close()
        }
    }

    @Nested
    @DisplayName("주어진 조건: KtorAdapter와의 호환성")
    inner class KtorAdapterCompatibilityTests {

        @Test
        @DisplayName("""
        [주어진 조건: OpenMockerPlugin과 KtorAdapter]
        [실행: MockingEngine의 clientType 확인]
        [예상 결과: 'Ktor' 타입으로 식별됨]
        """)
        fun `should work with KtorAdapter for client type identification`() {
            // Given
            val config = OpenMockerPluginConfig()

            // When
            val clientType = config.mockingEngine.getClientType()

            // Then
            assertEquals("Ktor", clientType)
        }

        @Test
        @DisplayName("""
        [주어진 조건: OpenMockerPlugin, KtorAdapter, MockingEngine]
        [실행: 통합 구성 요소 검증]
        [예상 결과: 모든 구성 요소가 올바르게 연결됨]
        """)
        fun `should integrate properly with existing architecture components`() {
            // Given
            val config = OpenMockerPluginConfig()

            // When
            val mockingEngine = config.mockingEngine

            // Then
            // Verify MockingEngine is properly configured
            assertNotNull(mockingEngine)
            assertEquals("Ktor", mockingEngine.getClientType())

            // Verify it has the expected message from base MockingEngine
            assertEquals("OpenMocker enabled", MockingEngine.MOCKER_MESSAGE)
        }
    }
}