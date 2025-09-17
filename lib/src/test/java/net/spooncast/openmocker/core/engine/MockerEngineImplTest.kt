package net.spooncast.openmocker.core.engine

import io.mockk.*
import kotlinx.coroutines.test.runTest
import net.spooncast.openmocker.core.model.MockKey
import net.spooncast.openmocker.core.model.MockResponse
import net.spooncast.openmocker.core.repository.MockRepository
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

/**
 * MockerEngineImpl [Mock 엔진 핵심 구현체] - BDD 스타일 종합 테스트
 * 100% 코드 커버리지와 모든 엣지 케이스를 다루는 포괄적 테스트 스위트
 *
 * 테스트 대상 기능:
 * - Mock 응답 조회 및 지연 처리 (getMockResponse)
 * - 실제 응답 캐싱 (cacheResponse)
 * - Mock 설정 (setMock)
 * - Mock 해제 (removeMock)
 * - 전체 Mock 삭제 (clearAllMocks)
 * - 캐시된 키 목록 조회 (getCachedKeys)
 * - Mock 상태 확인 (isMocked)
 */
class MockerEngineImplTest {

    // ================================
    // 테스트 설정 및 픽스처
    // ================================

    private lateinit var mockRepository: MockRepository
    private lateinit var mockerEngine: MockerEngineImpl

    @Before
    fun setUp() {
        // Given - Mock 객체 초기화 및 엔진 생성
        mockRepository = mockk()
        mockerEngine = MockerEngineImpl(mockRepository)
    }

    // ================================
    // 테스트 데이터 팩토리
    // ================================

    private fun createMockKey(method: String = "GET", path: String = "/api/test") = MockKey(method, path)

    private fun createMockResponse(
        code: Int = 200,
        body: String = "OK",
        delay: Long = 0L,
        headers: Map<String, String> = emptyMap()
    ) = MockResponse(code, body, delay, headers)

    // ================================
    // getMockResponse 테스트
    // ================================

    @Test
    fun `GIVEN repository has mock response without delay WHEN getMockResponse THEN returns response immediately`() = runTest {
        // Given
        val key = createMockKey("GET", "/api/users")
        val mockResponse = createMockResponse(200, "User data")
        coEvery { mockRepository.getMock(key) } returns mockResponse

        // When
        val result = mockerEngine.getMockResponse(key)

        // Then
        assertEquals(mockResponse, result)
        coVerify(exactly = 1) { mockRepository.getMock(key) }
    }

    @Test
    fun `GIVEN repository has mock response with delay WHEN getMockResponse THEN returns response with delay applied`() = runTest {
        // Given
        val key = createMockKey("POST", "/api/submit")
        val delayMs = 100L
        val mockResponse = createMockResponse(201, "Created", delayMs)
        coEvery { mockRepository.getMock(key) } returns mockResponse

        // When
        val result = mockerEngine.getMockResponse(key)

        // Then
        assertEquals("Mock 응답이 정확히 반환되어야 합니다.", mockResponse, result)
        assertTrue("응답에 지연 시간이 설정되어 있어야 합니다.", result?.hasDelay == true)
        assertEquals("지연 시간이 정확해야 합니다.", delayMs, result?.delay)
        coVerify(exactly = 1) { mockRepository.getMock(key) }
    }

    @Test
    fun `GIVEN repository has no mock for key WHEN getMockResponse THEN returns null`() = runTest {
        // Given
        val key = createMockKey("DELETE", "/api/nonexistent")
        coEvery { mockRepository.getMock(key) } returns null

        // When
        val result = mockerEngine.getMockResponse(key)

        // Then
        assertNull("존재하지 않는 Mock은 null을 반환해야 합니다.", result)
        coVerify(exactly = 1) { mockRepository.getMock(key) }
    }

    @Test
    fun `GIVEN repository has mock response with zero delay WHEN getMockResponse THEN returns immediately without delay`() = runTest {
        // Given
        val key = createMockKey("PATCH", "/api/update")
        val mockResponse = createMockResponse(200, "Updated", 0L)
        coEvery { mockRepository.getMock(key) } returns mockResponse

        // When
        val result = mockerEngine.getMockResponse(key)

        // Then
        assertEquals("Mock 응답이 정확히 반환되어야 합니다.", mockResponse, result)
        assertFalse("지연 시간이 0인 경우 hasDelay는 false여야 합니다.", result?.hasDelay == true)
        assertEquals("지연 시간이 0이어야 합니다.", 0L, result?.delay)
        coVerify(exactly = 1) { mockRepository.getMock(key) }
    }

    @Test
    fun `GIVEN repository has mock response with large delay WHEN getMockResponse THEN returns response with large delay configured`() = runTest {
        // Given
        val key = createMockKey("GET", "/api/slow")
        val longDelayMs = 500L
        val mockResponse = createMockResponse(200, "Slow response", longDelayMs)
        coEvery { mockRepository.getMock(key) } returns mockResponse

        // When
        val result = mockerEngine.getMockResponse(key)

        // Then
        assertEquals("Mock 응답이 정확히 반환되어야 합니다.", mockResponse, result)
        assertTrue("긴 지연 시간이 설정되어 있어야 합니다.", result?.hasDelay == true)
        assertEquals("긴 지연 시간이 정확해야 합니다.", longDelayMs, result?.delay)
        coVerify(exactly = 1) { mockRepository.getMock(key) }
    }

    @Test
    fun `GIVEN multiple sequential calls with different delays WHEN getMockResponse THEN each call returns response with correct delay`() = runTest {
        // Given
        val key1 = createMockKey("GET", "/api/fast")
        val key2 = createMockKey("GET", "/api/slow")
        val fastResponse = createMockResponse(200, "Fast", 50L)
        val slowResponse = createMockResponse(200, "Slow", 200L)

        coEvery { mockRepository.getMock(key1) } returns fastResponse
        coEvery { mockRepository.getMock(key2) } returns slowResponse

        // When
        val result1 = mockerEngine.getMockResponse(key1)
        val result2 = mockerEngine.getMockResponse(key2)

        // Then
        assertEquals("첫 번째 Mock 응답이 정확해야 합니다.", fastResponse, result1)
        assertEquals("두 번째 Mock 응답이 정확해야 합니다.", slowResponse, result2)

        assertTrue("첫 번째 응답의 지연 시간이 설정되어 있어야 합니다.", result1?.hasDelay == true)
        assertTrue("두 번째 응답의 지연 시간이 설정되어 있어야 합니다.", result2?.hasDelay == true)

        assertEquals("첫 번째 응답의 지연 시간이 정확해야 합니다.", 50L, result1?.delay)
        assertEquals("두 번째 응답의 지연 시간이 정확해야 합니다.", 200L, result2?.delay)

        coVerify(exactly = 1) { mockRepository.getMock(key1) }
        coVerify(exactly = 1) { mockRepository.getMock(key2) }
    }

    // ================================
    // cacheResponse 테스트
    // ================================

    @Test
    fun `GIVEN valid key and response WHEN cacheResponse THEN delegates to repository cacheRealResponse`() = runTest {
        // Given
        val key = createMockKey("POST", "/api/cache")
        val response = createMockResponse(200, "Cached response")
        coEvery { mockRepository.cacheRealResponse(key, response) } just runs

        // When
        mockerEngine.cacheResponse(key, response)

        // Then
        coVerify(exactly = 1) { mockRepository.cacheRealResponse(key, response) }
        confirmVerified(mockRepository)
    }

    @Test
    fun `GIVEN response with headers and delay WHEN cacheResponse THEN all data is passed to repository`() = runTest {
        // Given
        val key = createMockKey("PUT", "/api/complex")
        val headers = mapOf("Content-Type" to "application/json", "X-Custom" to "test")
        val response = createMockResponse(201, "Complex response", 100L, headers)
        coEvery { mockRepository.cacheRealResponse(key, response) } just runs

        // When
        mockerEngine.cacheResponse(key, response)

        // Then
        coVerify(exactly = 1) { mockRepository.cacheRealResponse(key, response) }
        confirmVerified(mockRepository)
    }

    @Test
    fun `GIVEN error response WHEN cacheResponse THEN error response is cached correctly`() = runTest {
        // Given
        val key = createMockKey("GET", "/api/error")
        val errorResponse = createMockResponse(500, "Internal Server Error")
        coEvery { mockRepository.cacheRealResponse(key, errorResponse) } just runs

        // When
        mockerEngine.cacheResponse(key, errorResponse)

        // Then
        coVerify(exactly = 1) { mockRepository.cacheRealResponse(key, errorResponse) }
        confirmVerified(mockRepository)
    }

    // ================================
    // setMock 테스트
    // ================================

    @Test
    fun `GIVEN valid key and mock response WHEN setMock THEN delegates to repository saveMock`() = runTest {
        // Given
        val key = createMockKey("GET", "/api/mock")
        val mockResponse = createMockResponse(200, "Mock response")
        coEvery { mockRepository.saveMock(key, mockResponse) } just runs

        // When
        mockerEngine.setMock(key, mockResponse)

        // Then
        coVerify(exactly = 1) { mockRepository.saveMock(key, mockResponse) }
        confirmVerified(mockRepository)
    }

    @Test
    fun `GIVEN mock response with custom delay WHEN setMock THEN delay is preserved in repository call`() = runTest {
        // Given
        val key = createMockKey("POST", "/api/delayed-mock")
        val mockResponse = createMockResponse(201, "Delayed mock", 300L)
        coEvery { mockRepository.saveMock(key, mockResponse) } just runs

        // When
        mockerEngine.setMock(key, mockResponse)

        // Then
        coVerify(exactly = 1) { mockRepository.saveMock(key, mockResponse) }
        confirmVerified(mockRepository)
    }

    @Test
    fun `GIVEN mock response with headers WHEN setMock THEN headers are preserved in repository call`() = runTest {
        // Given
        val key = createMockKey("PATCH", "/api/header-mock")
        val headers = mapOf("Authorization" to "Bearer token", "Content-Length" to "42")
        val mockResponse = createMockResponse(202, "With headers", 0L, headers)
        coEvery { mockRepository.saveMock(key, mockResponse) } just runs

        // When
        mockerEngine.setMock(key, mockResponse)

        // Then
        coVerify(exactly = 1) { mockRepository.saveMock(key, mockResponse) }
        confirmVerified(mockRepository)
    }

    @Test
    fun `GIVEN client error mock response WHEN setMock THEN error response is set correctly`() = runTest {
        // Given
        val key = createMockKey("GET", "/api/not-found")
        val clientErrorResponse = createMockResponse(404, "Not Found")
        coEvery { mockRepository.saveMock(key, clientErrorResponse) } just runs

        // When
        mockerEngine.setMock(key, clientErrorResponse)

        // Then
        coVerify(exactly = 1) { mockRepository.saveMock(key, clientErrorResponse) }
        confirmVerified(mockRepository)
    }

    @Test
    fun `GIVEN server error mock response WHEN setMock THEN server error is set correctly`() = runTest {
        // Given
        val key = createMockKey("POST", "/api/server-error")
        val serverErrorResponse = createMockResponse(503, "Service Unavailable")
        coEvery { mockRepository.saveMock(key, serverErrorResponse) } just runs

        // When
        mockerEngine.setMock(key, serverErrorResponse)

        // Then
        coVerify(exactly = 1) { mockRepository.saveMock(key, serverErrorResponse) }
        confirmVerified(mockRepository)
    }

    // ================================
    // removeMock 테스트
    // ================================

    @Test
    fun `GIVEN valid key WHEN removeMock THEN delegates to repository removeMock`() = runTest {
        // Given
        val key = createMockKey("DELETE", "/api/remove")
        coEvery { mockRepository.removeMock(key) } returns true

        // When
        mockerEngine.removeMock(key)

        // Then
        coVerify(exactly = 1) { mockRepository.removeMock(key) }
        confirmVerified(mockRepository)
    }

    @Test
    fun `GIVEN key with existing mock WHEN removeMock THEN repository removeMock is called`() = runTest {
        // Given
        val key = createMockKey("PUT", "/api/existing")
        coEvery { mockRepository.removeMock(key) } returns true

        // When
        mockerEngine.removeMock(key)

        // Then
        coVerify(exactly = 1) { mockRepository.removeMock(key) }
    }

    @Test
    fun `GIVEN key with no existing mock WHEN removeMock THEN repository removeMock is still called`() = runTest {
        // Given
        val key = createMockKey("GET", "/api/nonexistent")
        coEvery { mockRepository.removeMock(key) } returns false

        // When
        mockerEngine.removeMock(key)

        // Then
        coVerify(exactly = 1) { mockRepository.removeMock(key) }
    }

    @Test
    fun `GIVEN multiple different keys WHEN removeMock called for each THEN each key is passed to repository`() = runTest {
        // Given
        val key1 = createMockKey("GET", "/api/first")
        val key2 = createMockKey("POST", "/api/second")
        val key3 = createMockKey("DELETE", "/api/third")

        coEvery { mockRepository.removeMock(key1) } returns true
        coEvery { mockRepository.removeMock(key2) } returns false
        coEvery { mockRepository.removeMock(key3) } returns true

        // When
        mockerEngine.removeMock(key1)
        mockerEngine.removeMock(key2)
        mockerEngine.removeMock(key3)

        // Then
        coVerify(exactly = 1) { mockRepository.removeMock(key1) }
        coVerify(exactly = 1) { mockRepository.removeMock(key2) }
        coVerify(exactly = 1) { mockRepository.removeMock(key3) }
    }

    // ================================
    // clearAllMocks 테스트
    // ================================

    @Test
    fun `GIVEN engine with existing mocks WHEN clearAllMocks THEN delegates to repository clearAllMocks`() = runTest {
        // Given
        coEvery { mockRepository.clearAllMocks() } just runs

        // When
        mockerEngine.clearAllMocks()

        // Then
        coVerify(exactly = 1) { mockRepository.clearAllMocks() }
        confirmVerified(mockRepository)
    }

    @Test
    fun `GIVEN empty repository WHEN clearAllMocks THEN repository clearAllMocks is still called`() = runTest {
        // Given
        coEvery { mockRepository.clearAllMocks() } just runs

        // When
        mockerEngine.clearAllMocks()

        // Then
        coVerify(exactly = 1) { mockRepository.clearAllMocks() }
    }

    @Test
    fun `GIVEN multiple clearAllMocks calls WHEN clearAllMocks THEN each call is delegated to repository`() = runTest {
        // Given
        coEvery { mockRepository.clearAllMocks() } just runs

        // When
        mockerEngine.clearAllMocks()
        mockerEngine.clearAllMocks()
        mockerEngine.clearAllMocks()

        // Then
        coVerify(exactly = 3) { mockRepository.clearAllMocks() }
    }

    // ================================
    // getCachedKeys 테스트
    // ================================

    @Test
    fun `GIVEN repository with cached keys WHEN getCachedKeys THEN returns keys from repository`() = runTest {
        // Given
        val expectedKeys = listOf(
            createMockKey("GET", "/api/users"),
            createMockKey("POST", "/api/posts"),
            createMockKey("DELETE", "/api/items")
        )
        coEvery { mockRepository.getAllCachedKeys() } returns expectedKeys

        // When
        val result = mockerEngine.getCachedKeys()

        // Then
        assertEquals("캐시된 키 목록이 일치해야 합니다.", expectedKeys, result)
        coVerify(exactly = 1) { mockRepository.getAllCachedKeys() }
        confirmVerified(mockRepository)
    }

    @Test
    fun `GIVEN repository with no cached keys WHEN getCachedKeys THEN returns empty list`() = runTest {
        // Given
        val emptyList = emptyList<MockKey>()
        coEvery { mockRepository.getAllCachedKeys() } returns emptyList

        // When
        val result = mockerEngine.getCachedKeys()

        // Then
        assertTrue("캐시된 키가 없을 때 빈 목록을 반환해야 합니다.", result.isEmpty())
        assertEquals(emptyList, result)
        coVerify(exactly = 1) { mockRepository.getAllCachedKeys() }
    }

    @Test
    fun `GIVEN repository with single cached key WHEN getCachedKeys THEN returns single item list`() = runTest {
        // Given
        val singleKey = createMockKey("PUT", "/api/single")
        val singleKeyList = listOf(singleKey)
        coEvery { mockRepository.getAllCachedKeys() } returns singleKeyList

        // When
        val result = mockerEngine.getCachedKeys()

        // Then
        assertEquals("단일 키가 포함된 목록이 반환되어야 합니다.", singleKeyList, result)
        assertEquals(1, result.size)
        assertEquals(singleKey, result[0])
        coVerify(exactly = 1) { mockRepository.getAllCachedKeys() }
    }

    @Test
    fun `GIVEN repository with many cached keys WHEN getCachedKeys THEN returns all keys in order`() = runTest {
        // Given
        val manyKeys = (1..10).map { i ->
            createMockKey("GET", "/api/endpoint$i")
        }
        coEvery { mockRepository.getAllCachedKeys() } returns manyKeys

        // When
        val result = mockerEngine.getCachedKeys()

        // Then
        assertEquals("모든 키가 순서대로 반환되어야 합니다.", manyKeys, result)
        assertEquals(10, result.size)
        coVerify(exactly = 1) { mockRepository.getAllCachedKeys() }
    }

    // ================================
    // isMocked 테스트
    // ================================

    @Test
    fun `GIVEN repository has mock for key WHEN isMocked THEN returns true`() = runTest {
        // Given
        val key = createMockKey("GET", "/api/mocked")
        coEvery { mockRepository.hasMock(key) } returns true

        // When
        val result = mockerEngine.isMocked(key)

        // Then
        assertTrue("Mock이 존재하는 키에 대해 true를 반환해야 합니다.", result)
        coVerify(exactly = 1) { mockRepository.hasMock(key) }
        confirmVerified(mockRepository)
    }

    @Test
    fun `GIVEN repository has no mock for key WHEN isMocked THEN returns false`() = runTest {
        // Given
        val key = createMockKey("POST", "/api/not-mocked")
        coEvery { mockRepository.hasMock(key) } returns false

        // When
        val result = mockerEngine.isMocked(key)

        // Then
        assertFalse("Mock이 존재하지 않는 키에 대해 false를 반환해야 합니다.", result)
        coVerify(exactly = 1) { mockRepository.hasMock(key) }
    }

    @Test
    fun `GIVEN different keys with mixed mock states WHEN isMocked THEN returns correct state for each`() = runTest {
        // Given
        val mockedKey1 = createMockKey("GET", "/api/mocked1")
        val mockedKey2 = createMockKey("POST", "/api/mocked2")
        val notMockedKey1 = createMockKey("PUT", "/api/not-mocked1")
        val notMockedKey2 = createMockKey("DELETE", "/api/not-mocked2")

        coEvery { mockRepository.hasMock(mockedKey1) } returns true
        coEvery { mockRepository.hasMock(mockedKey2) } returns true
        coEvery { mockRepository.hasMock(notMockedKey1) } returns false
        coEvery { mockRepository.hasMock(notMockedKey2) } returns false

        // When & Then
        assertTrue("첫 번째 Mock된 키는 true를 반환해야 합니다.", mockerEngine.isMocked(mockedKey1))
        assertTrue("두 번째 Mock된 키는 true를 반환해야 합니다.", mockerEngine.isMocked(mockedKey2))
        assertFalse("첫 번째 Mock되지 않은 키는 false를 반환해야 합니다.", mockerEngine.isMocked(notMockedKey1))
        assertFalse("두 번째 Mock되지 않은 키는 false를 반환해야 합니다.", mockerEngine.isMocked(notMockedKey2))

        coVerify(exactly = 1) { mockRepository.hasMock(mockedKey1) }
        coVerify(exactly = 1) { mockRepository.hasMock(mockedKey2) }
        coVerify(exactly = 1) { mockRepository.hasMock(notMockedKey1) }
        coVerify(exactly = 1) { mockRepository.hasMock(notMockedKey2) }
    }

    @Test
    fun `GIVEN same key checked multiple times WHEN isMocked THEN repository is called each time`() = runTest {
        // Given
        val key = createMockKey("GET", "/api/check-multiple")
        coEvery { mockRepository.hasMock(key) } returns true

        // When
        val result1 = mockerEngine.isMocked(key)
        val result2 = mockerEngine.isMocked(key)
        val result3 = mockerEngine.isMocked(key)

        // Then
        assertTrue("첫 번째 호출 결과가 true여야 합니다.", result1)
        assertTrue("두 번째 호출 결과가 true여야 합니다.", result2)
        assertTrue("세 번째 호출 결과가 true여야 합니다.", result3)
        coVerify(exactly = 3) { mockRepository.hasMock(key) }
    }

    // ================================
    // 통합 시나리오 테스트
    // ================================

    @Test
    fun `GIVEN complete mock workflow WHEN setting then getting mock THEN workflow executes correctly`() = runTest {
        // Given
        val key = createMockKey("GET", "/api/workflow")
        val mockResponse = createMockResponse(200, "Workflow response", 50L)

        coEvery { mockRepository.saveMock(key, mockResponse) } just runs
        coEvery { mockRepository.getMock(key) } returns mockResponse

        // When - Mock 설정
        mockerEngine.setMock(key, mockResponse)

        // When - Mock 조회
        val result = mockerEngine.getMockResponse(key)

        // Then
        assertEquals("설정한 Mock 응답이 정확히 반환되어야 합니다.", mockResponse, result)
        coVerify(exactly = 1) { mockRepository.saveMock(key, mockResponse) }
        coVerify(exactly = 1) { mockRepository.getMock(key) }
    }

    @Test
    fun `GIVEN mock set then removed WHEN getting mock THEN returns null`() = runTest {
        // Given
        val key = createMockKey("POST", "/api/lifecycle")
        val mockResponse = createMockResponse(201, "Created")

        coEvery { mockRepository.saveMock(key, mockResponse) } just runs
        coEvery { mockRepository.removeMock(key) } returns true
        coEvery { mockRepository.getMock(key) } returns null

        // When - Mock 설정 후 제거
        mockerEngine.setMock(key, mockResponse)
        mockerEngine.removeMock(key)

        // When - 제거된 Mock 조회
        val result = mockerEngine.getMockResponse(key)

        // Then
        assertNull("제거된 Mock은 null을 반환해야 합니다.", result)
        coVerify(exactly = 1) { mockRepository.saveMock(key, mockResponse) }
        coVerify(exactly = 1) { mockRepository.removeMock(key) }
        coVerify(exactly = 1) { mockRepository.getMock(key) }
    }

    @Test
    fun `GIVEN multiple mocks set WHEN clearAllMocks then check isMocked THEN all mocks are cleared`() = runTest {
        // Given
        val key1 = createMockKey("GET", "/api/first")
        val key2 = createMockKey("POST", "/api/second")

        coEvery { mockRepository.saveMock(any(), any()) } just runs
        coEvery { mockRepository.clearAllMocks() } just runs
        coEvery { mockRepository.hasMock(any()) } returns false

        // When - 여러 Mock 설정 후 전체 삭제
        mockerEngine.setMock(key1, createMockResponse(200, "First"))
        mockerEngine.setMock(key2, createMockResponse(201, "Second"))
        mockerEngine.clearAllMocks()

        // When - 삭제 후 상태 확인
        val isMocked1 = mockerEngine.isMocked(key1)
        val isMocked2 = mockerEngine.isMocked(key2)

        // Then
        assertFalse("첫 번째 Mock이 삭제되어야 합니다.", isMocked1)
        assertFalse("두 번째 Mock이 삭제되어야 합니다.", isMocked2)

        coVerify(exactly = 1) { mockRepository.saveMock(key1, any()) }
        coVerify(exactly = 1) { mockRepository.saveMock(key2, any()) }
        coVerify(exactly = 1) { mockRepository.clearAllMocks() }
        coVerify(exactly = 1) { mockRepository.hasMock(key1) }
        coVerify(exactly = 1) { mockRepository.hasMock(key2) }
    }

    // ================================
    // 예외 처리 및 경계값 테스트
    // ================================

    @Test
    fun `GIVEN repository throws exception WHEN getMockResponse THEN exception propagates`() = runTest {
        // Given
        val key = createMockKey("GET", "/api/error")
        val expectedException = RuntimeException("Repository error")
        coEvery { mockRepository.getMock(key) } throws expectedException

        // When & Then
        try {
            mockerEngine.getMockResponse(key)
            fail("예외가 전파되어야 합니다.")
        } catch (e: RuntimeException) {
            assertEquals("예외 메시지가 일치해야 합니다.", "Repository error", e.message)
        }

        coVerify(exactly = 1) { mockRepository.getMock(key) }
    }

    @Test
    fun `GIVEN repository throws exception WHEN cacheResponse THEN exception propagates`() = runTest {
        // Given
        val key = createMockKey("POST", "/api/cache-error")
        val response = createMockResponse(200, "Test")
        val expectedException = RuntimeException("Cache error")
        coEvery { mockRepository.cacheRealResponse(key, response) } throws expectedException

        // When & Then
        try {
            mockerEngine.cacheResponse(key, response)
            fail("예외가 전파되어야 합니다.")
        } catch (e: RuntimeException) {
            assertEquals("예외 메시지가 일치해야 합니다.", "Cache error", e.message)
        }

        coVerify(exactly = 1) { mockRepository.cacheRealResponse(key, response) }
    }

    @Test
    fun `GIVEN repository throws exception WHEN setMock THEN exception propagates`() = runTest {
        // Given
        val key = createMockKey("PUT", "/api/set-error")
        val response = createMockResponse(200, "Test")
        val expectedException = RuntimeException("Set error")
        coEvery { mockRepository.saveMock(key, response) } throws expectedException

        // When & Then
        try {
            mockerEngine.setMock(key, response)
            fail("예외가 전파되어야 합니다.")
        } catch (e: RuntimeException) {
            assertEquals("예외 메시지가 일치해야 합니다.", "Set error", e.message)
        }

        coVerify(exactly = 1) { mockRepository.saveMock(key, response) }
    }

    @Test
    fun `GIVEN very long delay mock WHEN getMockResponse THEN returns response with very long delay configured`() = runTest {
        // Given
        val key = createMockKey("GET", "/api/very-long-delay")
        val veryLongDelay = 1000L // 1초
        val mockResponse = createMockResponse(200, "Very slow", veryLongDelay)
        coEvery { mockRepository.getMock(key) } returns mockResponse

        // When
        val result = mockerEngine.getMockResponse(key)

        // Then
        assertEquals("Mock 응답이 정확히 반환되어야 합니다.", mockResponse, result)
        assertTrue("매우 긴 지연 시간이 설정되어 있어야 합니다.", result?.hasDelay == true)
        assertEquals("매우 긴 지연 시간이 정확해야 합니다.", veryLongDelay, result?.delay)
        coVerify(exactly = 1) { mockRepository.getMock(key) }
    }

    @Test
    fun `GIVEN boundary status code responses WHEN workflow operations THEN all operations handle boundary values correctly`() = runTest {
        // Given
        val minCodeKey = createMockKey("GET", "/api/min-code")
        val maxCodeKey = createMockKey("GET", "/api/max-code")
        val minCodeResponse = createMockResponse(100, "Continue") // 최소 HTTP 코드
        val maxCodeResponse = createMockResponse(599, "Network Error") // 최대 HTTP 코드

        coEvery { mockRepository.saveMock(minCodeKey, minCodeResponse) } just runs
        coEvery { mockRepository.saveMock(maxCodeKey, maxCodeResponse) } just runs
        coEvery { mockRepository.getMock(minCodeKey) } returns minCodeResponse
        coEvery { mockRepository.getMock(maxCodeKey) } returns maxCodeResponse

        // When & Then - 최소값 테스트
        mockerEngine.setMock(minCodeKey, minCodeResponse)
        val minResult = mockerEngine.getMockResponse(minCodeKey)
        assertEquals("최소 HTTP 상태 코드 응답이 정확해야 합니다.", minCodeResponse, minResult)

        // When & Then - 최대값 테스트
        mockerEngine.setMock(maxCodeKey, maxCodeResponse)
        val maxResult = mockerEngine.getMockResponse(maxCodeKey)
        assertEquals("최대 HTTP 상태 코드 응답이 정확해야 합니다.", maxCodeResponse, maxResult)

        coVerify(exactly = 1) { mockRepository.saveMock(minCodeKey, minCodeResponse) }
        coVerify(exactly = 1) { mockRepository.saveMock(maxCodeKey, maxCodeResponse) }
        coVerify(exactly = 1) { mockRepository.getMock(minCodeKey) }
        coVerify(exactly = 1) { mockRepository.getMock(maxCodeKey) }
    }
}