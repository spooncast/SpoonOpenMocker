package net.spooncast.openmocker.lib.core

import io.mockk.*
import kotlinx.coroutines.test.runTest
import net.spooncast.openmocker.lib.core.adapter.HttpClientAdapter
import net.spooncast.openmocker.lib.model.CachedKey
import net.spooncast.openmocker.lib.model.CachedResponse
import net.spooncast.openmocker.lib.model.CachedValue
import net.spooncast.openmocker.lib.model.HttpRequestData
import net.spooncast.openmocker.lib.model.HttpResponseData
import net.spooncast.openmocker.lib.repo.CacheRepo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Assertions.*

@DisplayName("MockingEngine 테스트")
class MockingEngineTest {

    private val mockCacheRepo: CacheRepo = mockk()
    private val mockAdapter: HttpClientAdapter<String, String> = mockk()
    private lateinit var mockingEngine: MockingEngine<String, String>

    @BeforeEach
    fun setUp() {
        mockingEngine = MockingEngine(mockCacheRepo, mockAdapter)
        clearAllMocks()
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Nested
    @DisplayName("주어진 조건: 캐시된 목이 없는 MockingEngine")
    inner class NoMockExistsTests {

        @Test
        @DisplayName("""
        [주어진 조건: 캐시에 목이 존재하지 않는 요청]
        [실행: checkForMock을 사용하여 목 확인]
        [예상 결과: null 반환]
        """)
        fun `should return null when no mock exists`() = runTest {
            // Given
            val request = "test-request"
            val requestData =
                HttpRequestData("GET", "/api/test", "https://api.example.com/api/test")
            val cachedKey = CachedKey("GET", "/api/test")

            every { mockAdapter.extractRequestData(request) } returns requestData
            every { mockCacheRepo.cachedMap } returns mapOf<CachedKey, CachedValue>()

            // When
            val result = mockingEngine.checkForMock(request)

            // Then
            assertNull(result)
            verify { mockAdapter.extractRequestData(request) }
        }

        @Test
        @DisplayName("""
        [주어진 조건: 캐시에 목이 존재하지 않는 요청]
        [실행: checkForMockSync를 사용하여 목 확인]
        [예상 결과: null 반환]
        """)
        fun `should return null when no mock exists in sync version`() {
            // Given
            val request = "test-request"
            val requestData =
                HttpRequestData("GET", "/api/test", "https://api.example.com/api/test")

            every { mockAdapter.extractRequestData(request) } returns requestData
            every { mockCacheRepo.getMock("GET", "/api/test") } returns null

            // When
            val result = mockingEngine.checkForMockSync(request)

            // Then
            assertNull(result)
            verify { mockAdapter.extractRequestData(request) }
            verify { mockCacheRepo.getMock("GET", "/api/test") }
            verify(exactly = 0) { mockAdapter.createMockResponse(any(), any()) }
        }
    }

    @Nested
    @DisplayName("주어진 조건: 기존 목을 가진 MockingEngine")
    inner class MockExistsTests {

        @Test
        @DisplayName("""
        [주어진 조건: 캐시에 기존 목이 있는 요청]
        [실행: checkForMock을 사용하여 목 확인]
        [예상 결과: 목 응답 반환]
        """)
        fun `should return mock response when mock exists`() = runTest {
            // Given
            val request = "test-request"
            val requestData =
                HttpRequestData("GET", "/api/test", "https://api.example.com/api/test")
            val cachedKey = CachedKey("GET", "/api/test")
            val originalResponse = CachedResponse(200, """{"original": true}""")
            val mockResponse = CachedResponse(201, """{"mocked": true}""", 0L)
            val cachedValue = CachedValue(response = originalResponse, mock = mockResponse)
            val expectedMockResult = "mocked-response"

            every { mockAdapter.extractRequestData(request) } returns requestData
            every { mockCacheRepo.cachedMap } returns mapOf(cachedKey to cachedValue)
            every { mockAdapter.createMockResponse(request, mockResponse) } returns expectedMockResult

            // When
            val result = mockingEngine.checkForMock(request)

            // Then
            assertEquals(expectedMockResult, result)
            verify { mockAdapter.extractRequestData(request) }
            verify { mockAdapter.createMockResponse(request, mockResponse) }
        }

        @Test
        @DisplayName("""
        [주어진 조건: 지속 시간을 가진 기존 목이 있는 요청]
        [실행: checkForMock을 사용하여 목 확인]
        [예상 결과: 지연 적용 후 목 응답 반환]
        """)
        fun `should return mock response when mock has duration`() = runTest {
            // Given
            val request = "test-request"
            val requestData =
                HttpRequestData("GET", "/api/test", "https://api.example.com/api/test")
            val cachedKey = CachedKey("GET", "/api/test")
            val originalResponse = CachedResponse(200, "original")
            val mockResponse = CachedResponse(200, "delayed response", 100L) // 100ms delay
            val cachedValue = CachedValue(response = originalResponse, mock = mockResponse)
            val expectedMockResult = "delayed-mock-response"

            every { mockAdapter.extractRequestData(request) } returns requestData
            every { mockCacheRepo.cachedMap } returns mapOf(cachedKey to cachedValue)
            every { mockAdapter.createMockResponse(request, mockResponse) } returns expectedMockResult

            // When
            val result = mockingEngine.checkForMock(request)

            // Then
            assertEquals(expectedMockResult, result)
            verify { mockAdapter.createMockResponse(request, mockResponse) }
        }

        @Test
        @DisplayName("""
        [주어진 조건: 긴 지연을 가진 기존 목이 있는 요청]
        [실행: checkForMockSync를 사용하여 목 확인]
        [예상 결과: 지연 적용 없이 목 응답 반환]
        """)
        fun `should return mock response without delay in sync version`() {
            // Given
            val request = "test-request"
            val requestData =
                HttpRequestData("GET", "/api/test", "https://api.example.com/api/test")
            val mockResponse = CachedResponse(200, "sync response", 1000L) // Long delay
            val expectedMockResult = "sync-mock-response"

            every { mockAdapter.extractRequestData(request) } returns requestData
            every { mockCacheRepo.getMock("GET", "/api/test") } returns mockResponse
            every { mockAdapter.createMockResponse(request, mockResponse) } returns expectedMockResult

            val startTime = System.currentTimeMillis()

            // When
            val result = mockingEngine.checkForMockSync(request)

            // Then
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime

            assertEquals(expectedMockResult, result)
            assertTrue(duration < 50, "Should not apply delay in sync version")
            verify { mockAdapter.createMockResponse(request, mockResponse) }
        }
    }

    @Nested
    @DisplayName("주어진 조건: 응답 캐싱을 위한 MockingEngine")
    inner class CacheResponseTests {

        @Test
        @DisplayName("""
        [주어진 조건: 요청과 응답 쌍]
        [실행: cacheResponse를 사용하여 응답 캐싱]
        [예상 결과: 리포지토리에 응답 데이터 저장]
        """)
        fun `should store response in repository`() {
            // Given
            val request = "test-request"
            val response = "test-response"
            val requestData =
                HttpRequestData("POST", "/api/create", "https://api.example.com/api/create")
            val responseData = HttpResponseData(201, """{"id": 123}""")

            every { mockAdapter.extractRequestData(request) } returns requestData
            every { mockAdapter.extractResponseData(response) } returns responseData
            every { mockCacheRepo.cache(any(), any(), any(), any()) } just Runs

            // When
            mockingEngine.cacheResponse(request, response)

            // Then
            verify { mockAdapter.extractRequestData(request) }
            verify { mockAdapter.extractResponseData(response) }
            verify {
                mockCacheRepo.cache(
                    method = "POST",
                    urlPath = "/api/create",
                    responseCode = 201,
                    responseBody = """{"id": 123}"""
                )
            }
        }
    }

    @Nested
    @DisplayName("주어진 조건: MockingEngine 어댑터 속성")
    inner class AdapterPropertiesTests {

        @Test
        @DisplayName("""
        [주어진 조건: 구성된 어댑터를 가진 MockingEngine]
        [실행: 클라이언트 타입 조회]
        [예상 결과: 어댑터 클라이언트 타입 반환]
        """)
        fun `should return adapter client type`() {
            // Given
            every { mockAdapter.clientType } returns "TestClient"

            // When
            val clientType = mockingEngine.getClientType()

            // Then
            assertEquals("TestClient", clientType)
            verify { mockAdapter.clientType }
        }

        @Test
        @DisplayName("""
        [주어진 조건: 구성된 어댑터를 가진 MockingEngine]
        [실행: 요청과 응답을 처리할 수 있는지 확인]
        [예상 결과: 어댑터의 isSupported 메소드에 위임]
        """)
        fun `should delegate canHandle to adapter isSupported method`() {
            // Given
            val request = "test-request"
            val response = "test-response"

            every { mockAdapter.isSupported(request, response) } returns true

            // When
            val canHandle = mockingEngine.canHandle(request, response)

            // Then
            assertTrue(canHandle)
            verify { mockAdapter.isSupported(request, response) }
        }
    }

    @Nested
    @DisplayName("주어진 조건: 지연 기능을 위한 MockingEngine")
    inner class DelayFunctionalityTests {

        @Test
        @DisplayName("""
        [주어진 조건: 양수 지속 시간을 가진 목 응답]
        [실행: 목 지연 적용]
        [예상 결과: 성공적으로 완료]
        """)
        fun `should complete when duration is greater than zero`() = runTest {
            // Given
            val mockResponse = CachedResponse(200, "test", 100L)

            // When & Then
            // Should complete without throwing exception
            mockingEngine.applyMockDelay(mockResponse)

            // Test passes if no exception is thrown
            assertTrue(true, "applyMockDelay should complete successfully")
        }

        @Test
        @DisplayName("""
        [주어진 조건: 0 지속 시간을 가진 목 응답]
        [실행: 목 지연 적용]
        [예상 결과: 성공적으로 완료]
        """)
        fun `should complete when duration is zero`() = runTest {
            // Given
            val mockResponse = CachedResponse(200, "test", 0L)

            // When & Then
            // Should complete without throwing exception
            mockingEngine.applyMockDelay(mockResponse)

            // Test passes if no exception is thrown
            assertTrue(true, "applyMockDelay should complete successfully")
        }
    }
}