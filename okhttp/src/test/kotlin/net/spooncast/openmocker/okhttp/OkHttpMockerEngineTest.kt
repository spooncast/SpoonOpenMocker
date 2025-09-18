package net.spooncast.openmocker.okhttp

import kotlinx.coroutines.test.runTest
import net.spooncast.openmocker.core.MemoryMockRepository
import net.spooncast.openmocker.core.MockKey
import net.spooncast.openmocker.core.MockResponse
import net.spooncast.openmocker.core.MockRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class OkHttpMockerEngineTest {

    @Mock
    private lateinit var mockRepository: MockRepository

    private lateinit var realRepository: MemoryMockRepository
    private lateinit var engine: OkHttpMockerEngine
    private lateinit var engineWithRealRepo: OkHttpMockerEngine

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        // Create engine with mock repository for testing with mocks
        engine = OkHttpMockerEngine(mockRepository)

        // Create engine with real repository for integration testing
        realRepository = MemoryMockRepository()
        engineWithRealRepo = OkHttpMockerEngine(realRepository)
    }

    // shouldMock() tests
    // BDD: Given no mock exists, When shouldMock is called, Then return null
    @Test
    fun `shouldMock returns null when no mock exists`() = runTest {
        // Arrange
        val method = "GET"
        val path = "/api/users"
        val expectedKey = MockKey(method, path)
        whenever(mockRepository.getMock(expectedKey)).thenReturn(null)

        // Act
        val result = engine.shouldMock(method, path)

        // Assert
        assertNull(result)
        verify(mockRepository).getMock(expectedKey)
    }

    // BDD: Given mock exists, When shouldMock is called, Then return mock response
    @Test
    fun `shouldMock returns mock response when mock exists`() = runTest {
        // Arrange
        val method = "POST"
        val path = "/api/login"
        val expectedKey = MockKey(method, path)
        val mockResponse = MockResponse(200, """{"success": true}""", 100L)
        whenever(mockRepository.getMock(expectedKey)).thenReturn(mockResponse)

        // Act
        val result = engine.shouldMock(method, path)

        // Assert
        assertNotNull(result)
        assertEquals(mockResponse, result)
        verify(mockRepository).getMock(expectedKey)
    }

    // BDD: Given method and path, When shouldMock is called, Then create correct MockKey
    @Test
    fun `shouldMock creates correct MockKey for request`() = runTest {
        // Arrange
        val method = "PUT"
        val path = "/api/users/123"
        val expectedKey = MockKey(method, path)
        whenever(mockRepository.getMock(expectedKey)).thenReturn(null)

        // Act
        engine.shouldMock(method, path)

        // Assert
        verify(mockRepository).getMock(expectedKey)
    }

    // cacheResponse() tests
    // BDD: Given response parameters, When cacheResponse is called, Then store in repository
    @Test
    fun `cacheResponse stores response in repository`() = runTest {
        // Arrange
        val method = "GET"
        val path = "/api/products"
        val code = 200
        val body = """{"products": []}"""
        val expectedKey = MockKey(method, path)
        val expectedResponse = MockResponse(code, body)

        // Act
        engine.cacheResponse(method, path, code, body)

        // Assert
        verify(mockRepository).cacheRealResponse(expectedKey, expectedResponse)
    }

    // BDD: Given empty response body, When cacheResponse is called, Then handle gracefully
    @Test
    fun `cacheResponse handles empty body`() = runTest {
        // Arrange
        val method = "DELETE"
        val path = "/api/users/123"
        val code = 204
        val body = ""
        val expectedKey = MockKey(method, path)
        val expectedResponse = MockResponse(code, body)

        // Act
        engine.cacheResponse(method, path, code, body)

        // Assert
        verify(mockRepository).cacheRealResponse(expectedKey, expectedResponse)
    }

    // mock() tests
    // BDD: Given valid mock data, When mock is called, Then return true for success
    @Test
    fun `mock returns true when successfully saved`() = runTest {
        // Arrange
        val key = MockKey("GET", "/api/test")
        val response = MockResponse(200, "test response")

        // Act
        val result = engine.mock(key, response)

        // Assert
        assertTrue(result)
        verify(mockRepository).saveMock(key, response)
    }

    // BDD: Given repository failure, When mock is called, Then return false for failure
    @Test
    fun `mock returns false when repository throws exception`() = runTest {
        // Arrange
        val key = MockKey("GET", "/api/test")
        val response = MockResponse(200, "test response")
        whenever(mockRepository.saveMock(key, response)).thenThrow(RuntimeException("Save failed"))

        // Act
        val result = engine.mock(key, response)

        // Assert
        assertFalse(result)
        verify(mockRepository).saveMock(key, response)
    }

    // unmock() tests
    // BDD: Given existing mock, When unmock is called, Then return true for successful removal
    @Test
    fun `unmock returns true when mock was removed`() = runTest {
        // Arrange
        val key = MockKey("GET", "/api/test")
        whenever(mockRepository.removeMock(key)).thenReturn(true)

        // Act
        val result = engine.unmock(key)

        // Assert
        assertTrue(result)
        verify(mockRepository).removeMock(key)
    }

    // BDD: Given no existing mock, When unmock is called, Then return false for no removal
    @Test
    fun `unmock returns false when no mock existed`() = runTest {
        // Arrange
        val key = MockKey("GET", "/api/test")
        whenever(mockRepository.removeMock(key)).thenReturn(false)

        // Act
        val result = engine.unmock(key)

        // Assert
        assertFalse(result)
        verify(mockRepository).removeMock(key)
    }

    // Utility method tests
    // BDD: Given method and path parameters, When createMockKey is called, Then create correct MockKey
    @Test
    fun `createMockKey creates correct key from parameters`() {
        // Arrange
        val method = "PATCH"
        val path = "/api/settings"
        val expected = MockKey(method, path)

        // Act
        val result = engine.createMockKey(method, path)

        // Assert
        assertEquals(expected, result)
    }

    // BDD: When getAllMocks is called, Then delegate to repository
    @Test
    fun `getAllMocks delegates to repository`() = runTest {
        // Arrange
        val expectedMocks = mapOf(
            MockKey("GET", "/api/test") to MockResponse(200, "test")
        )
        whenever(mockRepository.getAllMocks()).thenReturn(expectedMocks)

        // Act
        val result = engine.getAllMocks()

        // Assert
        assertEquals(expectedMocks, result)
        verify(mockRepository).getAllMocks()
    }

    // BDD: When getAllCachedResponses is called, Then delegate to repository
    @Test
    fun `getAllCachedResponses delegates to repository`() = runTest {
        // Arrange
        val expectedCached = mapOf(
            MockKey("POST", "/api/login") to MockResponse(200, """{"token": "abc123"}""")
        )
        whenever(mockRepository.getAllCachedResponses()).thenReturn(expectedCached)

        // Act
        val result = engine.getAllCachedResponses()

        // Assert
        assertEquals(expectedCached, result)
        verify(mockRepository).getAllCachedResponses()
    }

    // BDD: When clearAll is called, Then delegate to repository
    @Test
    fun `clearAll delegates to repository`() = runTest {
        // Act
        engine.clearAll()

        // Assert
        verify(mockRepository).clearAll()
    }

    // Integration tests with real repository
    // BDD: 통합 테스트 - 완전한 모킹 워크플로우가 정상 동작해야 한다
    @Test
    fun `integration test - complete mock workflow`() = runTest {
        // Arrange
        val method = "GET"
        val path = "/api/integration"
        val mockResponse = MockResponse(201, """{"created": true}""", 50L)
        val mockKey = MockKey(method, path)

        // Act & Assert - Initially no mock exists
        val initialResult = engineWithRealRepo.shouldMock(method, path)
        assertNull(initialResult)

        // Act & Assert - Cache a real response
        engineWithRealRepo.cacheResponse(method, path, 200, """{"original": true}""")
        val cachedResponses = engineWithRealRepo.getAllCachedResponses()
        assertEquals(1, cachedResponses.size)
        assertTrue(cachedResponses.containsKey(mockKey))

        // Act & Assert - Add a mock
        val mockResult = engineWithRealRepo.mock(mockKey, mockResponse)
        assertTrue(mockResult)

        // Act & Assert - Mock should now be returned
        val mockedResult = engineWithRealRepo.shouldMock(method, path)
        assertEquals(mockResponse, mockedResult)

        // Act & Assert - Verify all mocks
        val allMocks = engineWithRealRepo.getAllMocks()
        assertEquals(1, allMocks.size)
        assertEquals(mockResponse, allMocks[mockKey])

        // Act & Assert - Remove mock
        val unmockResult = engineWithRealRepo.unmock(mockKey)
        assertTrue(unmockResult)

        // Act & Assert - Mock should no longer exist
        val finalResult = engineWithRealRepo.shouldMock(method, path)
        assertNull(finalResult)

        // Act & Assert - But cached response should still exist
        val finalCached = engineWithRealRepo.getAllCachedResponses()
        assertEquals(1, finalCached.size)
    }

    // BDD: 통합 테스트 - clearAll 호출 시 모든 데이터가 제거되어야 한다
    @Test
    fun `integration test - clearAll removes everything`() = runTest {
        // Arrange
        val key1 = MockKey("GET", "/api/test1")
        val key2 = MockKey("POST", "/api/test2")
        val response1 = MockResponse(200, "response1")
        val response2 = MockResponse(201, "response2")

        // Act - Add mocks and cached responses
        engineWithRealRepo.mock(key1, response1)
        engineWithRealRepo.cacheResponse(key2.method, key2.path, response2.code, response2.body)

        // Assert - Items exist
        assertEquals(1, engineWithRealRepo.getAllMocks().size)
        assertEquals(1, engineWithRealRepo.getAllCachedResponses().size)

        // Act - Clear all
        engineWithRealRepo.clearAll()

        // Assert - Everything cleared
        assertEquals(0, engineWithRealRepo.getAllMocks().size)
        assertEquals(0, engineWithRealRepo.getAllCachedResponses().size)
    }
}