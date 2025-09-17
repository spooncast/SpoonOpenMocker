package net.spooncast.openmocker.core

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class MemoryMockRepositoryTest {

    private lateinit var repository: MemoryMockRepository

    @Before
    fun setUp() {
        repository = MemoryMockRepository()
    }

    @Test
    fun `saveMock and getMock should work correctly`() = runTest {
        // Given: 빈 저장소와 테스트 데이터가 준비되어 있음
        val key = MockKey("GET", "/test")
        val response = MockResponse(200, "test response", 100L)

        // When: 모킹 응답을 저장하고 조회함
        repository.saveMock(key, response)
        val retrieved = repository.getMock(key)

        // Then: 저장된 응답이 정확히 반환되어야 함
        assertEquals(response, retrieved)
    }

    @Test
    fun `removeMock should return true when mock exists`() = runTest {
        // Given: 모킹 응답이 저장되어 있음
        val key = MockKey("POST", "/api/test")
        val response = MockResponse(201, "created", 0L)
        repository.saveMock(key, response)

        // When: 저장된 모킹 응답을 삭제함
        val removeResult = repository.removeMock(key)
        val retrievedAfterRemove = repository.getMock(key)

        // Then: 삭제가 성공하고 응답이 더 이상 조회되지 않아야 함
        assertTrue(removeResult)
        assertNull(retrievedAfterRemove)
    }

    @Test
    fun `removeMock should return false when mock does not exist`() = runTest {
        // Given: 빈 저장소 상태
        val key = MockKey("DELETE", "/nonexistent")

        // When: 존재하지 않는 모킹 응답을 삭제하려고 함
        val removeResult = repository.removeMock(key)

        // Then: 삭제 실패가 반환되어야 함
        assertFalse(removeResult)
    }

    @Test
    fun `getAllMocks should return all saved mocks`() = runTest {
        // Given: 두 개의 서로 다른 모킹 응답이 저장되어 있음
        val key1 = MockKey("GET", "/test1")
        val response1 = MockResponse(200, "response1")
        val key2 = MockKey("POST", "/test2")
        val response2 = MockResponse(201, "response2")
        repository.saveMock(key1, response1)
        repository.saveMock(key2, response2)

        // When: 저장된 모든 모킹 응답을 조회함
        val allMocks = repository.getAllMocks()

        // Then: 저장된 두 개의 응답이 모두 올바르게 반환되어야 함
        assertEquals(2, allMocks.size)
        assertEquals(response1, allMocks[key1])
        assertEquals(response2, allMocks[key2])
    }

    @Test
    fun `cacheRealResponse and getCachedResponse should work correctly`() = runTest {
        // Given: 빈 저장소와 캐싱할 응답 데이터가 준비되어 있음
        val key = MockKey("GET", "/cache-test")
        val response = MockResponse(200, "cached response")

        // When: 실제 응답을 캐싱하고 조회함
        repository.cacheRealResponse(key, response)
        val cached = repository.getCachedResponse(key)

        // Then: 캐싱된 응답이 정확히 반환되어야 함
        assertEquals(response, cached)
    }

    @Test
    fun `clearAll should remove all mocks and cached responses`() = runTest {
        // Given: 모킹 응답과 캐싱 응답이 저장되어 있음
        val key = MockKey("GET", "/test")
        val response = MockResponse(200, "test")
        repository.saveMock(key, response)
        repository.cacheRealResponse(key, response)

        // When: 전체 데이터를 삭제함
        repository.clearAll()

        // Then: 모든 모킹 응답과 캐싱 응답이 삭제되어야 함
        assertNull(repository.getMock(key))
        assertNull(repository.getCachedResponse(key))
        assertTrue(repository.getAllMocks().isEmpty())
        assertTrue(repository.getAllCachedResponses().isEmpty())
    }

    @Test
    fun `thread safety test for concurrent operations`() = runTest {
        // Given: 동시 작업을 위한 설정과 공유 키가 준비되어 있음
        val totalOperations = 1000
        val successCount = AtomicInteger(0)
        val key = MockKey("GET", "/concurrent-test")

        // When: 여러 코루틴에서 동시에 모킹 응답 저장 및 조회를 수행함
        val jobs = List(totalOperations) { index ->
            launch {
                try {
                    val response = MockResponse(200, "response-$index", index.toLong())
                    repository.saveMock(key, response)

                    // 저장 작업 검증
                    val retrieved = repository.getMock(key)
                    if (retrieved != null) {
                        successCount.incrementAndGet()
                    }
                } catch (e: Exception) {
                    // 스레드 안전 구현에서는 예외가 발생하지 않아야 함
                    fail("Thread safety violation: ${e.message}")
                }
            }
        }

        // 모든 작업이 완료될 때까지 대기
        jobs.joinAll()

        // Then: 최소한 하나의 작업이 성공해야 하고 저장소는 일관된 상태를 유지해야 함
        assertTrue("No successful operations detected", successCount.get() > 0)
        assertNotNull(repository.getMock(key))
    }

    @Test
    fun `concurrent cache operations should be thread safe`() = runTest {
        // Given: 동시 캐싱 작업을 위한 키와 응답 데이터가 준비되어 있음
        val totalOperations = 500
        val keys = (1..totalOperations).map { MockKey("GET", "/cache-$it") }
        val responses = (1..totalOperations).map { MockResponse(200, "cached-$it") }

        // When: 동시에 캐싱 저장 작업과 읽기 작업을 수행함
        val cacheJobs = keys.zip(responses).map { (key, response) ->
            launch {
                repository.cacheRealResponse(key, response)
            }
        }

        val readJobs = keys.map { key ->
            launch {
                repository.getCachedResponse(key)
            }
        }

        // 모든 작업이 완료될 때까지 대기
        (cacheJobs + readJobs).joinAll()

        // Then: 모든 응답이 성공적으로 캐싱되어야 함
        val allCached = repository.getAllCachedResponses()
        assertEquals(totalOperations, allCached.size)
    }

    @Test
    fun `memory management utility methods should work correctly`() = runTest {
        // Given: 모킹 응답과 캐싱 응답이 각각 저장되어 있음
        val key1 = MockKey("GET", "/test1")
        val key2 = MockKey("POST", "/test2")
        val response = MockResponse(200, "test")
        repository.saveMock(key1, response)
        repository.cacheRealResponse(key2, response)

        // When & Then: 각 유틸리티 메서드가 정상적으로 동작해야 함
        assertEquals(1, repository.getMocksCount())
        assertEquals(1, repository.getCachedResponsesCount())

        // When: 모킹 응답만 삭제함
        repository.clearMocks()

        // Then: 모킹 응답은 삭제되고 캐싱 응답은 유지되어야 함
        assertEquals(0, repository.getMocksCount())
        assertEquals(1, repository.getCachedResponsesCount())

        // When: 캐싱 응답을 삭제함
        repository.clearCache()

        // Then: 캐싱 응답도 삭제되어야 함
        assertEquals(0, repository.getCachedResponsesCount())
    }

    @Test
    fun `getMock should return null when key does not exist`() = runTest {
        // Given: 빈 저장소 상태
        val key = MockKey("GET", "/nonexistent")

        // When: 존재하지 않는 키로 모킹 응답을 조회함
        val result = repository.getMock(key)

        // Then: null이 반환되어야 함
        assertNull(result)
    }

    @Test
    fun `getCachedResponse should return null when key does not exist`() = runTest {
        // Given: 빈 저장소 상태
        val key = MockKey("GET", "/nonexistent")

        // When: 존재하지 않는 키로 캐싱 응답을 조회함
        val result = repository.getCachedResponse(key)

        // Then: null이 반환되어야 함
        assertNull(result)
    }

    @Test
    fun `getAllMocks should return empty map when no mocks exist`() = runTest {
        // Given: 빈 저장소 상태

        // When: 모든 모킹 응답을 조회함
        val allMocks = repository.getAllMocks()

        // Then: 빈 맵이 반환되어야 함
        assertTrue(allMocks.isEmpty())
        assertEquals(0, allMocks.size)
    }

    @Test
    fun `getAllCachedResponses should return empty map when no cached responses exist`() = runTest {
        // Given: 빈 저장소 상태

        // When: 모든 캐싱 응답을 조회함
        val allCached = repository.getAllCachedResponses()

        // Then: 빈 맵이 반환되어야 함
        assertTrue(allCached.isEmpty())
        assertEquals(0, allCached.size)
    }

    @Test
    fun `saveMock should overwrite existing mock with same key`() = runTest {
        // Given: 특정 키에 모킹 응답이 이미 저장되어 있음
        val key = MockKey("PUT", "/api/update")
        val originalResponse = MockResponse(200, "original", 100L)
        val updatedResponse = MockResponse(204, "updated", 200L)
        repository.saveMock(key, originalResponse)

        // When: 같은 키로 새로운 모킹 응답을 저장함
        repository.saveMock(key, updatedResponse)
        val retrieved = repository.getMock(key)

        // Then: 새로운 응답으로 덮어써져야 함
        assertEquals(updatedResponse, retrieved)
        assertEquals(1, repository.getMocksCount()) // 개수는 그대로
    }

    @Test
    fun `cacheRealResponse should overwrite existing cached response with same key`() = runTest {
        // Given: 특정 키에 캐싱 응답이 이미 저장되어 있음
        val key = MockKey("GET", "/api/data")
        val originalResponse = MockResponse(200, "original cached")
        val updatedResponse = MockResponse(200, "updated cached")
        repository.cacheRealResponse(key, originalResponse)

        // When: 같은 키로 새로운 캐싱 응답을 저장함
        repository.cacheRealResponse(key, updatedResponse)
        val retrieved = repository.getCachedResponse(key)

        // Then: 새로운 응답으로 덮어써져야 함
        assertEquals(updatedResponse, retrieved)
        assertEquals(1, repository.getCachedResponsesCount()) // 개수는 그대로
    }

    @Test
    fun `getAllMocks should return immutable copy of stored mocks`() = runTest {
        // Given: 두 개의 모킹 응답이 저장되어 있음
        val key1 = MockKey("GET", "/test1")
        val key2 = MockKey("POST", "/test2")
        val response1 = MockResponse(200, "response1")
        val response2 = MockResponse(201, "response2")
        repository.saveMock(key1, response1)
        repository.saveMock(key2, response2)

        // When: 모든 모킹 응답을 조회하고 수정을 시도함
        val allMocks = repository.getAllMocks()

        // Then: 불변 복사본이 반환되어야 하므로 원본 데이터는 영향받지 않음
        assertEquals(2, allMocks.size)
        assertEquals(response1, allMocks[key1])
        assertEquals(response2, allMocks[key2])

        // 추가 저장 후에도 이전 조회 결과는 변경되지 않아야 함
        val key3 = MockKey("DELETE", "/test3")
        val response3 = MockResponse(204, "response3")
        repository.saveMock(key3, response3)
        assertEquals(2, allMocks.size) // 이전 결과는 변경되지 않음
        assertEquals(3, repository.getAllMocks().size) // 새로운 조회는 3개
    }

    @Test
    fun `getAllCachedResponses should return immutable copy of cached responses`() = runTest {
        // Given: 두 개의 캐싱 응답이 저장되어 있음
        val key1 = MockKey("GET", "/cache1")
        val key2 = MockKey("POST", "/cache2")
        val response1 = MockResponse(200, "cached1")
        val response2 = MockResponse(201, "cached2")
        repository.cacheRealResponse(key1, response1)
        repository.cacheRealResponse(key2, response2)

        // When: 모든 캐싱 응답을 조회하고 수정을 시도함
        val allCached = repository.getAllCachedResponses()

        // Then: 불변 복사본이 반환되어야 하므로 원본 데이터는 영향받지 않음
        assertEquals(2, allCached.size)
        assertEquals(response1, allCached[key1])
        assertEquals(response2, allCached[key2])

        // 추가 저장 후에도 이전 조회 결과는 변경되지 않아야 함
        val key3 = MockKey("DELETE", "/cache3")
        val response3 = MockResponse(204, "cached3")
        repository.cacheRealResponse(key3, response3)
        assertEquals(2, allCached.size) // 이전 결과는 변경되지 않음
        assertEquals(3, repository.getAllCachedResponses().size) // 새로운 조회는 3개
    }

    @Test
    fun `count methods should return zero for empty repository`() = runTest {
        // Given: 빈 저장소 상태

        // When & Then: 카운터 메서드들이 0을 반환해야 함
        assertEquals(0, repository.getMocksCount())
        assertEquals(0, repository.getCachedResponsesCount())
    }

    @Test
    fun `count methods should reflect correct numbers after operations`() = runTest {
        // Given: 빈 저장소 상태
        assertEquals(0, repository.getMocksCount())
        assertEquals(0, repository.getCachedResponsesCount())

        // When: 모킹 응답 3개와 캐싱 응답 2개를 추가함
        repository.saveMock(MockKey("GET", "/mock1"), MockResponse(200, "mock1"))
        repository.saveMock(MockKey("POST", "/mock2"), MockResponse(201, "mock2"))
        repository.saveMock(MockKey("PUT", "/mock3"), MockResponse(200, "mock3"))
        repository.cacheRealResponse(MockKey("GET", "/cache1"), MockResponse(200, "cache1"))
        repository.cacheRealResponse(MockKey("DELETE", "/cache2"), MockResponse(204, "cache2"))

        // Then: 각각의 카운트가 정확해야 함
        assertEquals(3, repository.getMocksCount())
        assertEquals(2, repository.getCachedResponsesCount())

        // When: 모킹 응답 하나를 삭제함
        repository.removeMock(MockKey("GET", "/mock1"))

        // Then: 모킹 카운트만 감소해야 함
        assertEquals(2, repository.getMocksCount())
        assertEquals(2, repository.getCachedResponsesCount())
    }

    @Test
    fun `clearMocks should only clear mocks and preserve cached responses`() = runTest {
        // Given: 모킹 응답과 캐싱 응답이 모두 저장되어 있음
        val mockKey = MockKey("GET", "/mock")
        val cacheKey = MockKey("POST", "/cache")
        val mockResponse = MockResponse(200, "mock")
        val cacheResponse = MockResponse(201, "cache")
        repository.saveMock(mockKey, mockResponse)
        repository.cacheRealResponse(cacheKey, cacheResponse)

        // When: 모킹 응답만 삭제함
        repository.clearMocks()

        // Then: 모킹 응답은 삭제되고 캐싱 응답은 보존되어야 함
        assertNull(repository.getMock(mockKey))
        assertEquals(cacheResponse, repository.getCachedResponse(cacheKey))
        assertEquals(0, repository.getMocksCount())
        assertEquals(1, repository.getCachedResponsesCount())
        assertTrue(repository.getAllMocks().isEmpty())
        assertEquals(1, repository.getAllCachedResponses().size)
    }

    @Test
    fun `clearCache should only clear cached responses and preserve mocks`() = runTest {
        // Given: 모킹 응답과 캐싱 응답이 모두 저장되어 있음
        val mockKey = MockKey("GET", "/mock")
        val cacheKey = MockKey("POST", "/cache")
        val mockResponse = MockResponse(200, "mock")
        val cacheResponse = MockResponse(201, "cache")
        repository.saveMock(mockKey, mockResponse)
        repository.cacheRealResponse(cacheKey, cacheResponse)

        // When: 캐싱 응답만 삭제함
        repository.clearCache()

        // Then: 캐싱 응답은 삭제되고 모킹 응답은 보존되어야 함
        assertEquals(mockResponse, repository.getMock(mockKey))
        assertNull(repository.getCachedResponse(cacheKey))
        assertEquals(1, repository.getMocksCount())
        assertEquals(0, repository.getCachedResponsesCount())
        assertEquals(1, repository.getAllMocks().size)
        assertTrue(repository.getAllCachedResponses().isEmpty())
    }

    @Test
    fun `concurrent clearAll operations should be thread safe`() = runTest {
        // Given: 여러 개의 모킹 응답과 캐싱 응답이 저장되어 있음
        val totalItems = 100
        repeat(totalItems) { index ->
            repository.saveMock(MockKey("GET", "/mock$index"), MockResponse(200, "mock$index"))
            repository.cacheRealResponse(MockKey("POST", "/cache$index"), MockResponse(201, "cache$index"))
        }
        assertEquals(totalItems, repository.getMocksCount())
        assertEquals(totalItems, repository.getCachedResponsesCount())

        // When: 동시에 여러 번 clearAll을 호출함
        val clearJobs = List(10) {
            launch {
                repository.clearAll()
            }
        }
        clearJobs.joinAll()

        // Then: 모든 데이터가 안전하게 삭제되어야 함
        assertEquals(0, repository.getMocksCount())
        assertEquals(0, repository.getCachedResponsesCount())
        assertTrue(repository.getAllMocks().isEmpty())
        assertTrue(repository.getAllCachedResponses().isEmpty())
    }

    @Test
    fun `mixed concurrent operations should maintain thread safety`() = runTest {
        // Given: 동시 작업을 위한 설정
        val totalOperations = 200
        val results = mutableListOf<Boolean>()

        // When: 저장, 조회, 삭제를 동시에 수행함
        val mixedJobs = List(totalOperations) { index ->
            launch {
                try {
                    val key = MockKey("GET", "/mixed$index")
                    val response = MockResponse(200, "mixed$index")

                    when (index % 4) {
                        0 -> {
                            // 저장 작업
                            repository.saveMock(key, response)
                            results.add(true)
                        }
                        1 -> {
                            // 조회 작업
                            repository.getMock(key)
                            results.add(true)
                        }
                        2 -> {
                            // 캐싱 작업
                            repository.cacheRealResponse(key, response)
                            results.add(true)
                        }
                        3 -> {
                            // 삭제 작업
                            repository.removeMock(key)
                            results.add(true)
                        }
                    }
                } catch (e: Exception) {
                    fail("Thread safety violation in mixed operations: ${e.message}")
                }
            }
        }
        mixedJobs.joinAll()

        // Then: 모든 작업이 예외 없이 완료되어야 함
        assertEquals(totalOperations, results.size)
        assertTrue("All operations should complete successfully", results.all { it })
    }

    @Test
    fun `data integrity should be maintained across all operations`() = runTest {
        // Given: 특정 키와 응답 데이터가 준비되어 있음
        val key1 = MockKey("GET", "/integrity1")
        val key2 = MockKey("POST", "/integrity2")
        val mockResponse1 = MockResponse(200, "mock1", 100L)
        val mockResponse2 = MockResponse(201, "mock2", 200L)
        val cacheResponse1 = MockResponse(200, "cache1", 50L)
        val cacheResponse2 = MockResponse(202, "cache2", 150L)

        // When & Then: 순차적으로 모든 작업을 수행하며 데이터 무결성을 확인함

        // 모킹 응답 저장 및 확인
        repository.saveMock(key1, mockResponse1)
        repository.saveMock(key2, mockResponse2)
        assertEquals(mockResponse1, repository.getMock(key1))
        assertEquals(mockResponse2, repository.getMock(key2))
        assertEquals(2, repository.getMocksCount())

        // 캐싱 응답 저장 및 확인
        repository.cacheRealResponse(key1, cacheResponse1)
        repository.cacheRealResponse(key2, cacheResponse2)
        assertEquals(cacheResponse1, repository.getCachedResponse(key1))
        assertEquals(cacheResponse2, repository.getCachedResponse(key2))
        assertEquals(2, repository.getCachedResponsesCount())

        // getAllMocks와 getAllCachedResponses 확인
        val allMocks = repository.getAllMocks()
        val allCached = repository.getAllCachedResponses()
        assertEquals(2, allMocks.size)
        assertEquals(2, allCached.size)
        assertEquals(mockResponse1, allMocks[key1])
        assertEquals(mockResponse2, allMocks[key2])
        assertEquals(cacheResponse1, allCached[key1])
        assertEquals(cacheResponse2, allCached[key2])

        // 선택적 삭제 후 확인
        assertTrue(repository.removeMock(key1))
        assertNull(repository.getMock(key1))
        assertEquals(mockResponse2, repository.getMock(key2)) // 다른 키는 영향받지 않음
        assertEquals(cacheResponse1, repository.getCachedResponse(key1)) // 캐시는 영향받지 않음
        assertEquals(1, repository.getMocksCount())
        assertEquals(2, repository.getCachedResponsesCount())

        // 전체 삭제 후 확인
        repository.clearAll()
        assertEquals(0, repository.getMocksCount())
        assertEquals(0, repository.getCachedResponsesCount())
        assertNull(repository.getMock(key2))
        assertNull(repository.getCachedResponse(key1))
        assertTrue(repository.getAllMocks().isEmpty())
        assertTrue(repository.getAllCachedResponses().isEmpty())
    }
}