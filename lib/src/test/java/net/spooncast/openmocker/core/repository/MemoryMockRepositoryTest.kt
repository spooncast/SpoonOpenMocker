package net.spooncast.openmocker.core.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import net.spooncast.openmocker.core.model.MockKey
import net.spooncast.openmocker.core.model.MockResponse
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * MemoryMockRepository [메모리 기반 Mock 저장소] - BDD 스타일 종합 테스트
 * 100% 코드 커버리지와 모든 엣지 케이스를 다루는 포괄적 테스트 스위트
 *
 * 테스트 대상 기능:
 * - Mock 응답 저장 및 조회 (saveMock, getMock)
 * - Mock 응답 삭제 (removeMock)
 * - 실제 응답 캐싱 (cacheRealResponse, getCachedResponse)
 * - 전체 데이터 삭제 (clearAllMocks, clearAllCache)
 * - 키 목록 조회 (getAllMockKeys, getAllCachedKeys)
 * - 존재 여부 확인 (hasMock, hasCachedResponse)
 * - 상태 정보 조회 (getStatus)
 * - 동시성 안전 및 Mutex 동기화
 * - 스레드 안전성
 */
class MemoryMockRepositoryTest {

    // ================================
    // 테스트 설정 및 픽스처
    // ================================

    private lateinit var repository: MemoryMockRepository

    @Before
    fun setUp() {
        // Given - 새로운 저장소 인스턴스 생성
        repository = MemoryMockRepository()
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
    // saveMock / getMock 테스트
    // ================================

    @Test
    fun `GIVEN empty repository WHEN saving mock THEN mock is saved successfully`() = runTest {
        // Given
        val key = createMockKey("GET", "/api/users")
        val response = createMockResponse(200, "User list")

        // When
        repository.saveMock(key, response)

        // Then
        val retrieved = repository.getMock(key)
        assertNotNull(retrieved)
        assertEquals(200, retrieved!!.code)
        assertEquals("User list", retrieved.body)
        assertTrue(repository.hasMock(key))
    }

    @Test
    fun `GIVEN repository with mock WHEN getting existing mock THEN returns correct mock response`() = runTest {
        // Given
        val key = createMockKey("POST", "/api/login")
        val response = createMockResponse(201, "Login successful", 100L, mapOf("Authorization" to "Bearer token"))
        repository.saveMock(key, response)

        // When
        val result = repository.getMock(key)

        // Then
        assertNotNull(result)
        assertEquals(201, result!!.code)
        assertEquals("Login successful", result.body)
        assertEquals(100L, result.delay)
        assertEquals("Bearer token", result.headers["Authorization"])
    }

    @Test
    fun `GIVEN empty repository WHEN getting non-existent mock THEN returns null`() = runTest {
        // Given
        val key = createMockKey("GET", "/api/non-existent")

        // When
        val result = repository.getMock(key)

        // Then
        assertNull(result)
        assertFalse(repository.hasMock(key))
    }

    @Test
    fun `GIVEN repository with mock WHEN overwriting existing mock THEN returns updated mock`() = runTest {
        // Given
        val key = createMockKey("PUT", "/api/users/123")
        val originalResponse = createMockResponse(200, "Original user")
        val updatedResponse = createMockResponse(204, "Updated user", 50L)

        // When
        repository.saveMock(key, originalResponse)
        repository.saveMock(key, updatedResponse)

        // Then
        val result = repository.getMock(key)
        assertNotNull(result)
        assertEquals(204, result!!.code)
        assertEquals("Updated user", result.body)
        assertEquals(50L, result.delay)
    }

    @Test
    fun `GIVEN multiple mocks with different keys WHEN saving THEN all mocks stored independently`() = runTest {
        // Given
        val key1 = createMockKey("GET", "/api/users")
        val key2 = createMockKey("POST", "/api/users")
        val key3 = createMockKey("GET", "/api/posts")
        val response1 = createMockResponse(200, "Get users")
        val response2 = createMockResponse(201, "Create user")
        val response3 = createMockResponse(200, "Get posts")

        // When
        repository.saveMock(key1, response1)
        repository.saveMock(key2, response2)
        repository.saveMock(key3, response3)

        // Then
        assertEquals("Get users", repository.getMock(key1)!!.body)
        assertEquals("Create user", repository.getMock(key2)!!.body)
        assertEquals("Get posts", repository.getMock(key3)!!.body)
        assertTrue(repository.hasMock(key1))
        assertTrue(repository.hasMock(key2))
        assertTrue(repository.hasMock(key3))
    }

    // ================================
    // removeMock 테스트
    // ================================

    @Test
    fun `GIVEN repository with mock WHEN removing existing mock THEN mock is removed and returns true`() = runTest {
        // Given
        val key = createMockKey("DELETE", "/api/users/123")
        val response = createMockResponse(204, "Deleted")
        repository.saveMock(key, response)

        // When
        val result = repository.removeMock(key)

        // Then
        assertTrue(result)
        assertNull(repository.getMock(key))
        assertFalse(repository.hasMock(key))
    }

    @Test
    fun `GIVEN empty repository WHEN removing non-existent mock THEN returns false`() = runTest {
        // Given
        val key = createMockKey("DELETE", "/api/non-existent")

        // When
        val result = repository.removeMock(key)

        // Then
        assertFalse(result)
    }

    @Test
    fun `GIVEN repository with multiple mocks WHEN removing one mock THEN only target mock is removed`() = runTest {
        // Given
        val key1 = createMockKey("GET", "/api/users")
        val key2 = createMockKey("GET", "/api/posts")
        val response1 = createMockResponse(200, "Users")
        val response2 = createMockResponse(200, "Posts")
        repository.saveMock(key1, response1)
        repository.saveMock(key2, response2)

        // When
        val result = repository.removeMock(key1)

        // Then
        assertTrue(result)
        assertNull(repository.getMock(key1))
        assertNotNull(repository.getMock(key2))
        assertFalse(repository.hasMock(key1))
        assertTrue(repository.hasMock(key2))
        assertEquals("Posts", repository.getMock(key2)!!.body)
    }

    @Test
    fun `GIVEN mock already removed WHEN removing same mock again THEN returns false`() = runTest {
        // Given
        val key = createMockKey("PATCH", "/api/users/123")
        val response = createMockResponse(200, "Updated")
        repository.saveMock(key, response)
        repository.removeMock(key)

        // When
        val result = repository.removeMock(key)

        // Then
        assertFalse(result)
        assertNull(repository.getMock(key))
    }

    // ================================
    // cacheRealResponse / getCachedResponse 테스트
    // ================================

    @Test
    fun `GIVEN empty repository WHEN caching real response THEN response is cached successfully`() = runTest {
        // Given
        val key = createMockKey("GET", "/api/data")
        val response = createMockResponse(200, "Real data", 0L, mapOf("Content-Type" to "application/json"))

        // When
        repository.cacheRealResponse(key, response)

        // Then
        val cached = repository.getCachedResponse(key)
        assertNotNull(cached)
        assertEquals(200, cached!!.code)
        assertEquals("Real data", cached.body)
        assertEquals("application/json", cached.headers["Content-Type"])
        assertTrue(repository.hasCachedResponse(key))
    }

    @Test
    fun `GIVEN repository with cached response WHEN getting cached response THEN returns correct response`() = runTest {
        // Given
        val key = createMockKey("POST", "/api/submit")
        val response = createMockResponse(202, "Accepted", 200L)
        repository.cacheRealResponse(key, response)

        // When
        val result = repository.getCachedResponse(key)

        // Then
        assertNotNull(result)
        assertEquals(202, result!!.code)
        assertEquals("Accepted", result.body)
        assertEquals(200L, result.delay)
    }

    @Test
    fun `GIVEN empty repository WHEN getting non-existent cached response THEN returns null`() = runTest {
        // Given
        val key = createMockKey("GET", "/api/missing")

        // When
        val result = repository.getCachedResponse(key)

        // Then
        assertNull(result)
        assertFalse(repository.hasCachedResponse(key))
    }

    @Test
    fun `GIVEN repository with cached response WHEN overwriting cached response THEN returns updated response`() = runTest {
        // Given
        val key = createMockKey("PUT", "/api/update")
        val originalResponse = createMockResponse(200, "Original")
        val updatedResponse = createMockResponse(201, "Updated")

        // When
        repository.cacheRealResponse(key, originalResponse)
        repository.cacheRealResponse(key, updatedResponse)

        // Then
        val result = repository.getCachedResponse(key)
        assertNotNull(result)
        assertEquals(201, result!!.code)
        assertEquals("Updated", result.body)
    }

    @Test
    fun `GIVEN multiple cached responses WHEN caching THEN all responses stored independently`() = runTest {
        // Given
        val key1 = createMockKey("GET", "/api/users")
        val key2 = createMockKey("GET", "/api/posts")
        val key3 = createMockKey("POST", "/api/users")
        val response1 = createMockResponse(200, "Users cache")
        val response2 = createMockResponse(200, "Posts cache")
        val response3 = createMockResponse(201, "User created cache")

        // When
        repository.cacheRealResponse(key1, response1)
        repository.cacheRealResponse(key2, response2)
        repository.cacheRealResponse(key3, response3)

        // Then
        assertEquals("Users cache", repository.getCachedResponse(key1)!!.body)
        assertEquals("Posts cache", repository.getCachedResponse(key2)!!.body)
        assertEquals("User created cache", repository.getCachedResponse(key3)!!.body)
        assertTrue(repository.hasCachedResponse(key1))
        assertTrue(repository.hasCachedResponse(key2))
        assertTrue(repository.hasCachedResponse(key3))
    }

    // ================================
    // clearAllMocks / clearAllCache 테스트
    // ================================

    @Test
    fun `GIVEN repository with multiple mocks WHEN clearing all mocks THEN all mocks are removed`() = runTest {
        // Given
        val key1 = createMockKey("GET", "/api/users")
        val key2 = createMockKey("POST", "/api/posts")
        val response1 = createMockResponse(200, "Users")
        val response2 = createMockResponse(201, "Posts")
        repository.saveMock(key1, response1)
        repository.saveMock(key2, response2)

        // When
        repository.clearAllMocks()

        // Then
        assertNull(repository.getMock(key1))
        assertNull(repository.getMock(key2))
        assertFalse(repository.hasMock(key1))
        assertFalse(repository.hasMock(key2))
        assertTrue(repository.getAllMockKeys().isEmpty())
    }

    @Test
    fun `GIVEN empty repository WHEN clearing all mocks THEN operation completes without error`() = runTest {
        // Given - 빈 저장소

        // When
        repository.clearAllMocks()

        // Then
        assertTrue(repository.getAllMockKeys().isEmpty())
    }

    @Test
    fun `GIVEN repository with mocks and cache WHEN clearing mocks THEN only mocks are cleared`() = runTest {
        // Given
        val key1 = createMockKey("GET", "/api/users")
        val key2 = createMockKey("GET", "/api/posts")
        val mockResponse = createMockResponse(200, "Mock")
        val cacheResponse = createMockResponse(200, "Cache")
        repository.saveMock(key1, mockResponse)
        repository.cacheRealResponse(key2, cacheResponse)

        // When
        repository.clearAllMocks()

        // Then
        assertNull(repository.getMock(key1))
        assertNotNull(repository.getCachedResponse(key2))
        assertFalse(repository.hasMock(key1))
        assertTrue(repository.hasCachedResponse(key2))
        assertEquals("Cache", repository.getCachedResponse(key2)!!.body)
    }

    @Test
    fun `GIVEN repository with multiple cached responses WHEN clearing all cache THEN all cache is removed`() = runTest {
        // Given
        val key1 = createMockKey("GET", "/api/data1")
        val key2 = createMockKey("GET", "/api/data2")
        val response1 = createMockResponse(200, "Data1")
        val response2 = createMockResponse(200, "Data2")
        repository.cacheRealResponse(key1, response1)
        repository.cacheRealResponse(key2, response2)

        // When
        repository.clearAllCache()

        // Then
        assertNull(repository.getCachedResponse(key1))
        assertNull(repository.getCachedResponse(key2))
        assertFalse(repository.hasCachedResponse(key1))
        assertFalse(repository.hasCachedResponse(key2))
        assertTrue(repository.getAllCachedKeys().isEmpty())
    }

    @Test
    fun `GIVEN empty repository WHEN clearing all cache THEN operation completes without error`() = runTest {
        // Given - 빈 저장소

        // When
        repository.clearAllCache()

        // Then
        assertTrue(repository.getAllCachedKeys().isEmpty())
    }

    @Test
    fun `GIVEN repository with mocks and cache WHEN clearing cache THEN only cache is cleared`() = runTest {
        // Given
        val key1 = createMockKey("GET", "/api/users")
        val key2 = createMockKey("GET", "/api/posts")
        val mockResponse = createMockResponse(200, "Mock")
        val cacheResponse = createMockResponse(200, "Cache")
        repository.saveMock(key1, mockResponse)
        repository.cacheRealResponse(key2, cacheResponse)

        // When
        repository.clearAllCache()

        // Then
        assertNotNull(repository.getMock(key1))
        assertNull(repository.getCachedResponse(key2))
        assertTrue(repository.hasMock(key1))
        assertFalse(repository.hasCachedResponse(key2))
        assertEquals("Mock", repository.getMock(key1)!!.body)
    }

    // ================================
    // getAllMockKeys / getAllCachedKeys 테스트
    // ================================

    @Test
    fun `GIVEN empty repository WHEN getting all mock keys THEN returns empty list`() = runTest {
        // Given - 빈 저장소

        // When
        val keys = repository.getAllMockKeys()

        // Then
        assertTrue(keys.isEmpty())
        assertEquals(0, keys.size)
    }

    @Test
    fun `GIVEN repository with multiple mocks WHEN getting all mock keys THEN returns all keys`() = runTest {
        // Given
        val key1 = createMockKey("GET", "/api/users")
        val key2 = createMockKey("POST", "/api/users")
        val key3 = createMockKey("DELETE", "/api/users/123")
        val response = createMockResponse(200, "Test")
        repository.saveMock(key1, response)
        repository.saveMock(key2, response)
        repository.saveMock(key3, response)

        // When
        val keys = repository.getAllMockKeys()

        // Then
        assertEquals(3, keys.size)
        assertTrue(keys.contains(key1))
        assertTrue(keys.contains(key2))
        assertTrue(keys.contains(key3))
    }

    @Test
    fun `GIVEN repository with mock keys WHEN getting mock keys multiple times THEN returns consistent results`() = runTest {
        // Given
        val key1 = createMockKey("GET", "/api/test1")
        val key2 = createMockKey("POST", "/api/test2")
        val response = createMockResponse(200, "Test")
        repository.saveMock(key1, response)
        repository.saveMock(key2, response)

        // When
        val keys1 = repository.getAllMockKeys()
        val keys2 = repository.getAllMockKeys()

        // Then
        assertEquals(keys1.size, keys2.size)
        assertTrue(keys1.containsAll(keys2))
        assertTrue(keys2.containsAll(keys1))
    }

    @Test
    fun `GIVEN empty repository WHEN getting all cached keys THEN returns empty list`() = runTest {
        // Given - 빈 저장소

        // When
        val keys = repository.getAllCachedKeys()

        // Then
        assertTrue(keys.isEmpty())
        assertEquals(0, keys.size)
    }

    @Test
    fun `GIVEN repository with multiple cached responses WHEN getting all cached keys THEN returns all keys`() = runTest {
        // Given
        val key1 = createMockKey("GET", "/api/data1")
        val key2 = createMockKey("POST", "/api/data2")
        val key3 = createMockKey("PUT", "/api/data3")
        val response = createMockResponse(200, "Cached")
        repository.cacheRealResponse(key1, response)
        repository.cacheRealResponse(key2, response)
        repository.cacheRealResponse(key3, response)

        // When
        val keys = repository.getAllCachedKeys()

        // Then
        assertEquals(3, keys.size)
        assertTrue(keys.contains(key1))
        assertTrue(keys.contains(key2))
        assertTrue(keys.contains(key3))
    }

    @Test
    fun `GIVEN repository with cached keys WHEN getting cached keys multiple times THEN returns consistent results`() = runTest {
        // Given
        val key1 = createMockKey("GET", "/api/cache1")
        val key2 = createMockKey("POST", "/api/cache2")
        val response = createMockResponse(200, "Cache")
        repository.cacheRealResponse(key1, response)
        repository.cacheRealResponse(key2, response)

        // When
        val keys1 = repository.getAllCachedKeys()
        val keys2 = repository.getAllCachedKeys()

        // Then
        assertEquals(keys1.size, keys2.size)
        assertTrue(keys1.containsAll(keys2))
        assertTrue(keys2.containsAll(keys1))
    }

    @Test
    fun `GIVEN repository with mocks and cache WHEN getting keys THEN returns separate key lists`() = runTest {
        // Given
        val mockKey1 = createMockKey("GET", "/api/mock1")
        val mockKey2 = createMockKey("POST", "/api/mock2")
        val cacheKey1 = createMockKey("GET", "/api/cache1")
        val cacheKey2 = createMockKey("PUT", "/api/cache2")
        val response = createMockResponse(200, "Test")

        repository.saveMock(mockKey1, response)
        repository.saveMock(mockKey2, response)
        repository.cacheRealResponse(cacheKey1, response)
        repository.cacheRealResponse(cacheKey2, response)

        // When
        val mockKeys = repository.getAllMockKeys()
        val cacheKeys = repository.getAllCachedKeys()

        // Then
        assertEquals(2, mockKeys.size)
        assertEquals(2, cacheKeys.size)
        assertTrue(mockKeys.contains(mockKey1))
        assertTrue(mockKeys.contains(mockKey2))
        assertTrue(cacheKeys.contains(cacheKey1))
        assertTrue(cacheKeys.contains(cacheKey2))
        assertFalse(mockKeys.contains(cacheKey1))
        assertFalse(cacheKeys.contains(mockKey1))
    }

    // ================================
    // hasMock / hasCachedResponse 테스트
    // ================================

    @Test
    fun `GIVEN repository with mock WHEN checking if mock exists THEN returns true`() = runTest {
        // Given
        val key = createMockKey("GET", "/api/exists")
        val response = createMockResponse(200, "Exists")
        repository.saveMock(key, response)

        // When & Then
        assertTrue(repository.hasMock(key))
    }

    @Test
    fun `GIVEN empty repository WHEN checking if mock exists THEN returns false`() = runTest {
        // Given
        val key = createMockKey("GET", "/api/not-exists")

        // When & Then
        assertFalse(repository.hasMock(key))
    }

    @Test
    fun `GIVEN repository with mock WHEN removing mock and checking existence THEN returns false`() = runTest {
        // Given
        val key = createMockKey("DELETE", "/api/removed")
        val response = createMockResponse(204, "Deleted")
        repository.saveMock(key, response)
        repository.removeMock(key)

        // When & Then
        assertFalse(repository.hasMock(key))
    }

    @Test
    fun `GIVEN repository with cached response WHEN checking if cached response exists THEN returns true`() = runTest {
        // Given
        val key = createMockKey("GET", "/api/cached")
        val response = createMockResponse(200, "Cached")
        repository.cacheRealResponse(key, response)

        // When & Then
        assertTrue(repository.hasCachedResponse(key))
    }

    @Test
    fun `GIVEN empty repository WHEN checking if cached response exists THEN returns false`() = runTest {
        // Given
        val key = createMockKey("GET", "/api/not-cached")

        // When & Then
        assertFalse(repository.hasCachedResponse(key))
    }

    @Test
    fun `GIVEN repository with cached response WHEN clearing cache and checking existence THEN returns false`() = runTest {
        // Given
        val key = createMockKey("GET", "/api/will-be-cleared")
        val response = createMockResponse(200, "Will be cleared")
        repository.cacheRealResponse(key, response)
        repository.clearAllCache()

        // When & Then
        assertFalse(repository.hasCachedResponse(key))
    }

    @Test
    fun `GIVEN repository with both mock and cache for same key WHEN checking existence THEN both return true`() = runTest {
        // Given
        val key = createMockKey("GET", "/api/both")
        val mockResponse = createMockResponse(200, "Mock")
        val cacheResponse = createMockResponse(200, "Cache")
        repository.saveMock(key, mockResponse)
        repository.cacheRealResponse(key, cacheResponse)

        // When & Then
        assertTrue(repository.hasMock(key))
        assertTrue(repository.hasCachedResponse(key))
    }

    // ================================
    // getStatus 테스트
    // ================================

    @Test
    fun `GIVEN empty repository WHEN getting status THEN returns empty status`() {
        // Given - 빈 저장소

        // When
        val status = repository.getStatus()

        // Then
        assertEquals(0, status.mockCount)
        assertEquals(0, status.cacheCount)
        assertEquals(0, status.totalItems)
        assertTrue(status.isEmpty)
    }

    @Test
    fun `GIVEN repository with mocks only WHEN getting status THEN returns correct mock count`() = runTest {
        // Given
        val key1 = createMockKey("GET", "/api/mock1")
        val key2 = createMockKey("POST", "/api/mock2")
        val key3 = createMockKey("PUT", "/api/mock3")
        val response = createMockResponse(200, "Mock")
        repository.saveMock(key1, response)
        repository.saveMock(key2, response)
        repository.saveMock(key3, response)

        // When
        val status = repository.getStatus()

        // Then
        assertEquals(3, status.mockCount)
        assertEquals(0, status.cacheCount)
        assertEquals(3, status.totalItems)
        assertFalse(status.isEmpty)
    }

    @Test
    fun `GIVEN repository with cache only WHEN getting status THEN returns correct cache count`() = runTest {
        // Given
        val key1 = createMockKey("GET", "/api/cache1")
        val key2 = createMockKey("POST", "/api/cache2")
        val response = createMockResponse(200, "Cache")
        repository.cacheRealResponse(key1, response)
        repository.cacheRealResponse(key2, response)

        // When
        val status = repository.getStatus()

        // Then
        assertEquals(0, status.mockCount)
        assertEquals(2, status.cacheCount)
        assertEquals(2, status.totalItems)
        assertFalse(status.isEmpty)
    }

    @Test
    fun `GIVEN repository with both mocks and cache WHEN getting status THEN returns correct counts`() = runTest {
        // Given
        val mockKey1 = createMockKey("GET", "/api/mock1")
        val mockKey2 = createMockKey("POST", "/api/mock2")
        val cacheKey1 = createMockKey("GET", "/api/cache1")
        val cacheKey2 = createMockKey("PUT", "/api/cache2")
        val cacheKey3 = createMockKey("DELETE", "/api/cache3")
        val response = createMockResponse(200, "Test")

        repository.saveMock(mockKey1, response)
        repository.saveMock(mockKey2, response)
        repository.cacheRealResponse(cacheKey1, response)
        repository.cacheRealResponse(cacheKey2, response)
        repository.cacheRealResponse(cacheKey3, response)

        // When
        val status = repository.getStatus()

        // Then
        assertEquals(2, status.mockCount)
        assertEquals(3, status.cacheCount)
        assertEquals(5, status.totalItems)
        assertFalse(status.isEmpty)
    }

    @Test
    fun `GIVEN repository with data WHEN clearing all data and getting status THEN returns empty status`() = runTest {
        // Given
        val key1 = createMockKey("GET", "/api/mock")
        val key2 = createMockKey("GET", "/api/cache")
        val response = createMockResponse(200, "Test")
        repository.saveMock(key1, response)
        repository.cacheRealResponse(key2, response)

        // When
        repository.clearAllMocks()
        repository.clearAllCache()
        val status = repository.getStatus()

        // Then
        assertEquals(0, status.mockCount)
        assertEquals(0, status.cacheCount)
        assertEquals(0, status.totalItems)
        assertTrue(status.isEmpty)
    }

    @Test
    fun `GIVEN repository WHEN getting status multiple times THEN returns consistent results`() = runTest {
        // Given
        val key = createMockKey("GET", "/api/test")
        val response = createMockResponse(200, "Test")
        repository.saveMock(key, response)

        // When
        val status1 = repository.getStatus()
        val status2 = repository.getStatus()

        // Then
        assertEquals(status1.mockCount, status2.mockCount)
        assertEquals(status1.cacheCount, status2.cacheCount)
        assertEquals(status1.totalItems, status2.totalItems)
        assertEquals(status1.isEmpty, status2.isEmpty)
    }

    // ================================
    // RepositoryStatus 데이터 클래스 테스트
    // ================================

    @Test
    fun `GIVEN RepositoryStatus with mock and cache counts WHEN checking computed properties THEN returns correct values`() {
        // Given
        val status = MemoryMockRepository.RepositoryStatus(mockCount = 3, cacheCount = 2)

        // When & Then
        assertEquals(3, status.mockCount)
        assertEquals(2, status.cacheCount)
        assertEquals(5, status.totalItems)
        assertFalse(status.isEmpty)
    }

    @Test
    fun `GIVEN RepositoryStatus with zero counts WHEN checking computed properties THEN returns empty status`() {
        // Given
        val status = MemoryMockRepository.RepositoryStatus(mockCount = 0, cacheCount = 0)

        // When & Then
        assertEquals(0, status.mockCount)
        assertEquals(0, status.cacheCount)
        assertEquals(0, status.totalItems)
        assertTrue(status.isEmpty)
    }

    @Test
    fun `GIVEN RepositoryStatus with only mocks WHEN checking computed properties THEN returns correct values`() {
        // Given
        val status = MemoryMockRepository.RepositoryStatus(mockCount = 5, cacheCount = 0)

        // When & Then
        assertEquals(5, status.mockCount)
        assertEquals(0, status.cacheCount)
        assertEquals(5, status.totalItems)
        assertFalse(status.isEmpty)
    }

    @Test
    fun `GIVEN RepositoryStatus with only cache WHEN checking computed properties THEN returns correct values`() {
        // Given
        val status = MemoryMockRepository.RepositoryStatus(mockCount = 0, cacheCount = 7)

        // When & Then
        assertEquals(0, status.mockCount)
        assertEquals(7, status.cacheCount)
        assertEquals(7, status.totalItems)
        assertFalse(status.isEmpty)
    }

    // ================================
    // 동시성 및 스레드 안전성 테스트
    // ================================

    @Test
    fun `GIVEN multiple concurrent save operations WHEN executing simultaneously THEN all saves complete successfully`() = runTest {
        // Given
        val numberOfOperations = 100
        val response = createMockResponse(200, "Concurrent test")

        // When
        val jobs = (1..numberOfOperations).map { index ->
            async {
                val key = createMockKey("GET", "/api/concurrent/$index")
                repository.saveMock(key, response)
            }
        }
        jobs.awaitAll()

        // Then
        val status = repository.getStatus()
        assertEquals(numberOfOperations, status.mockCount)

        // 각 키가 실제로 저장되었는지 확인
        repeat(numberOfOperations) { index ->
            val key = createMockKey("GET", "/api/concurrent/${index + 1}")
            assertTrue(repository.hasMock(key))
            assertNotNull(repository.getMock(key))
        }
    }

    @Test
    fun `GIVEN multiple concurrent cache operations WHEN executing simultaneously THEN all caches complete successfully`() = runTest {
        // Given
        val numberOfOperations = 100
        val response = createMockResponse(200, "Cache concurrent test")

        // When
        val jobs = (1..numberOfOperations).map { index ->
            async {
                val key = createMockKey("GET", "/api/cache/concurrent/$index")
                repository.cacheRealResponse(key, response)
            }
        }
        jobs.awaitAll()

        // Then
        val status = repository.getStatus()
        assertEquals(numberOfOperations, status.cacheCount)

        // 각 키가 실제로 캐시되었는지 확인
        repeat(numberOfOperations) { index ->
            val key = createMockKey("GET", "/api/cache/concurrent/${index + 1}")
            assertTrue(repository.hasCachedResponse(key))
            assertNotNull(repository.getCachedResponse(key))
        }
    }

    @Test
    fun `GIVEN concurrent read and write operations WHEN executing simultaneously THEN operations complete without data corruption`() = runTest {
        // Given
        val key1 = createMockKey("GET", "/api/concurrent/read-write")
        val key2 = createMockKey("POST", "/api/concurrent/read-write")
        val response1 = createMockResponse(200, "Response 1")
        val response2 = createMockResponse(201, "Response 2")

        // 초기 데이터 설정
        repository.saveMock(key1, response1)

        // When - 동시 읽기/쓰기 작업
        val jobs = listOf(
            // 읽기 작업들
            async { repository.getMock(key1) },
            async { repository.hasMock(key1) },
            async { repository.getAllMockKeys() },
            async { repository.getStatus() },
            // 쓰기 작업들
            async { repository.saveMock(key2, response2) },
            async { repository.cacheRealResponse(key1, response1) },
            async { repository.saveMock(key1, createMockResponse(200, "Updated")) }
        )
        val results = jobs.awaitAll()

        // Then - 모든 작업이 완료되고 데이터 무결성 유지
        assertTrue(repository.hasMock(key1))
        assertTrue(repository.hasMock(key2))
        assertTrue(repository.hasCachedResponse(key1))
        assertNotNull(repository.getMock(key1))
        assertNotNull(repository.getMock(key2))
        assertEquals("Updated", repository.getMock(key1)!!.body)
        assertEquals("Response 2", repository.getMock(key2)!!.body)
    }

    @Test
    fun `GIVEN concurrent remove operations WHEN executing simultaneously THEN removes complete safely`() = runTest {
        // Given
        val keys = (1..50).map { createMockKey("DELETE", "/api/remove/$it") }
        val response = createMockResponse(200, "To be removed")

        // 초기 데이터 설정
        keys.forEach { repository.saveMock(it, response) }

        // When - 동시 삭제 작업
        val jobs = keys.map { key ->
            async { repository.removeMock(key) }
        }
        val results = jobs.awaitAll()

        // Then
        assertTrue(results.all { it }) // 모든 삭제가 성공
        keys.forEach { key ->
            assertFalse(repository.hasMock(key))
            assertNull(repository.getMock(key))
        }
        assertEquals(0, repository.getStatus().mockCount)
    }

    @Test
    fun `GIVEN concurrent clear operations WHEN executing simultaneously THEN clears complete safely`() = runTest {
        // Given
        val mockKeys = (1..20).map { createMockKey("GET", "/api/mock/$it") }
        val cacheKeys = (1..20).map { createMockKey("GET", "/api/cache/$it") }
        val response = createMockResponse(200, "Test")

        // 초기 데이터 설정
        mockKeys.forEach { repository.saveMock(it, response) }
        cacheKeys.forEach { repository.cacheRealResponse(it, response) }

        // When - 동시 전체 삭제 작업
        val jobs = listOf(
            async { repository.clearAllMocks() },
            async { repository.clearAllCache() },
            async { repository.clearAllMocks() }, // 중복 실행
            async { repository.clearAllCache() }  // 중복 실행
        )
        jobs.awaitAll()

        // Then
        assertEquals(0, repository.getStatus().mockCount)
        assertEquals(0, repository.getStatus().cacheCount)
        assertTrue(repository.getAllMockKeys().isEmpty())
        assertTrue(repository.getAllCachedKeys().isEmpty())
    }

    @Test
    fun `GIVEN concurrent mixed operations with delays WHEN executing THEN all operations complete correctly`() = runTest {
        // Given
        val baseKey = createMockKey("GET", "/api/mixed")
        val response = createMockResponse(200, "Mixed operations")

        // When - 지연이 있는 복합 동시 작업
        val jobs = listOf(
            async {
                repository.saveMock(baseKey, response)
                delay(10)
                repository.hasMock(baseKey)
            },
            async {
                delay(5)
                repository.cacheRealResponse(baseKey, response)
                repository.hasCachedResponse(baseKey)
            },
            async {
                delay(15)
                repository.getMock(baseKey)
            },
            async {
                delay(8)
                repository.getCachedResponse(baseKey)
            },
            async {
                delay(20)
                repository.getStatus()
            }
        )
        val results = jobs.awaitAll()

        // Then
        assertTrue(repository.hasMock(baseKey))
        assertTrue(repository.hasCachedResponse(baseKey))
        assertNotNull(repository.getMock(baseKey))
        assertNotNull(repository.getCachedResponse(baseKey))
        val status = repository.getStatus()
        assertEquals(1, status.mockCount)
        assertEquals(1, status.cacheCount)
        assertEquals(2, status.totalItems)
        assertFalse(status.isEmpty)
    }

    // ================================
    // Mutex 동기화 테스트
    // ================================

    @Test
    fun `GIVEN repository WHEN performing mutex-protected operations THEN operations are properly synchronized`() = runTest {
        // Given
        val key = createMockKey("GET", "/api/mutex-test")
        val response = createMockResponse(200, "Mutex test")
        val numberOfOperations = 50

        // When - Mutex로 보호되는 작업들 동시 실행
        val jobs = (1..numberOfOperations).map { index ->
            async {
                when (index % 4) {
                    0 -> repository.saveMock(key, createMockResponse(200, "Version $index"))
                    1 -> repository.cacheRealResponse(key, createMockResponse(200, "Cache $index"))
                    2 -> repository.removeMock(key)
                    3 -> repository.clearAllMocks()
                }
            }
        }
        jobs.awaitAll()

        // Then - 데이터 무결성이 유지됨 (정확한 최종 상태는 예측할 수 없지만 오류는 없어야 함)
        val status = repository.getStatus()
        assertTrue(status.mockCount >= 0)
        assertTrue(status.cacheCount >= 0)

        // 저장소 상태가 일관성을 유지하는지 확인
        val mockKeys = repository.getAllMockKeys()
        val cacheKeys = repository.getAllCachedKeys()
        assertEquals(status.mockCount, mockKeys.size)
        assertEquals(status.cacheCount, cacheKeys.size)
    }

    // ================================
    // 엣지 케이스 테스트
    // ================================

    @Test
    fun `GIVEN repository with large number of entries WHEN performing operations THEN handles large scale correctly`() = runTest {
        // Given
        val largeScale = 1000
        val mockResponse = createMockResponse(200, "Large scale test")

        // When - 대규모 데이터 저장
        repeat(largeScale) { index ->
            val mockKey = createMockKey("GET", "/api/large/mock/$index")
            val cacheKey = createMockKey("GET", "/api/large/cache/$index")
            repository.saveMock(mockKey, mockResponse)
            repository.cacheRealResponse(cacheKey, mockResponse)
        }

        // Then
        val status = repository.getStatus()
        assertEquals(largeScale, status.mockCount)
        assertEquals(largeScale, status.cacheCount)
        assertEquals(largeScale * 2, status.totalItems)
        assertFalse(status.isEmpty)

        // 일부 키들이 올바르게 저장되었는지 확인
        val firstMockKey = createMockKey("GET", "/api/large/mock/0")
        val lastMockKey = createMockKey("GET", "/api/large/mock/${largeScale - 1}")
        val firstCacheKey = createMockKey("GET", "/api/large/cache/0")
        val lastCacheKey = createMockKey("GET", "/api/large/cache/${largeScale - 1}")

        assertTrue(repository.hasMock(firstMockKey))
        assertTrue(repository.hasMock(lastMockKey))
        assertTrue(repository.hasCachedResponse(firstCacheKey))
        assertTrue(repository.hasCachedResponse(lastCacheKey))
    }

    @Test
    fun `GIVEN repository with complex MockKey patterns WHEN storing and retrieving THEN handles all patterns correctly`() = runTest {
        // Given
        val complexKeys = listOf(
            createMockKey("GET", "/"),
            createMockKey("POST", "/api/v1/users/123/posts/456/comments"),
            createMockKey("PUT", "/api/search?q=test&sort=date&page=1&limit=100"),
            createMockKey("DELETE", "/api/users/{userId}/posts/{postId}"),
            createMockKey("PATCH", "/api/very/deeply/nested/resource/path/structure"),
            createMockKey("HEAD", "/api/users?filter=active&include=profile,posts&sort=-created_at"),
            createMockKey("OPTIONS", "/api/webhooks/callback?token=abc123&signature=xyz789")
        )
        val response = createMockResponse(200, "Complex pattern test")

        // When
        complexKeys.forEach { key ->
            repository.saveMock(key, response)
            repository.cacheRealResponse(key, response)
        }

        // Then
        complexKeys.forEach { key ->
            assertTrue(repository.hasMock(key))
            assertTrue(repository.hasCachedResponse(key))
            assertNotNull(repository.getMock(key))
            assertNotNull(repository.getCachedResponse(key))
        }

        val status = repository.getStatus()
        assertEquals(complexKeys.size, status.mockCount)
        assertEquals(complexKeys.size, status.cacheCount)
    }

    @Test
    fun `GIVEN repository with complex MockResponse variations WHEN storing and retrieving THEN handles all variations correctly`() = runTest {
        // Given
        val key = createMockKey("GET", "/api/complex-responses")
        val complexResponses = listOf(
            createMockResponse(100, "Continue", 0L, emptyMap()),
            createMockResponse(200, "Success with delay", 5000L, mapOf("Content-Type" to "application/json")),
            createMockResponse(404, "Not Found", 100L, mapOf("X-Error" to "Resource not found", "X-Trace-ID" to "12345")),
            createMockResponse(500, "Internal Server Error", 2000L, mapOf(
                "Content-Type" to "application/json",
                "X-Request-ID" to "req-123",
                "X-Server" to "backend-1",
                "Cache-Control" to "no-cache"
            )),
            createMockResponse(599, "Network Connect Timeout Error", 10000L, mapOf("X-Timeout" to "true"))
        )

        // When & Then
        complexResponses.forEachIndexed { index, response ->
            val indexedKey = createMockKey("GET", "/api/complex-responses/$index")
            repository.saveMock(indexedKey, response)
            repository.cacheRealResponse(indexedKey, response)

            val retrievedMock = repository.getMock(indexedKey)
            val retrievedCache = repository.getCachedResponse(indexedKey)

            assertNotNull(retrievedMock)
            assertNotNull(retrievedCache)
            assertEquals(response.code, retrievedMock!!.code)
            assertEquals(response.body, retrievedMock.body)
            assertEquals(response.delay, retrievedMock.delay)
            assertEquals(response.headers, retrievedMock.headers)
            assertEquals(response.code, retrievedCache!!.code)
            assertEquals(response.body, retrievedCache.body)
            assertEquals(response.delay, retrievedCache.delay)
            assertEquals(response.headers, retrievedCache.headers)
        }
    }
}