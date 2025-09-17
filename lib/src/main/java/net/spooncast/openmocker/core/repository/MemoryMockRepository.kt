package net.spooncast.openmocker.core.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.spooncast.openmocker.core.model.MockKey
import net.spooncast.openmocker.core.model.MockResponse
import java.util.concurrent.ConcurrentHashMap

/**
 * 메모리 기반 Mock 저장소 구현체
 * Thread-safe한 메모리 저장소를 제공합니다.
 *
 * 특징:
 * - ConcurrentHashMap을 사용한 동시성 안전 보장
 * - Mutex를 사용한 원자적 연산 지원
 * - 메모리 기반 고성능 저장소
 */
class MemoryMockRepository : MockRepository {

    private val mocks = ConcurrentHashMap<MockKey, MockResponse>()
    private val cachedResponses = ConcurrentHashMap<MockKey, MockResponse>()
    private val mutex = Mutex()

    override suspend fun saveMock(key: MockKey, mockResponse: MockResponse) = mutex.withLock {
        mocks[key] = mockResponse
    }

    override suspend fun getMock(key: MockKey): MockResponse? {
        return mocks[key]
    }

    override suspend fun removeMock(key: MockKey): Boolean = mutex.withLock {
        mocks.remove(key) != null
    }

    override suspend fun cacheRealResponse(key: MockKey, response: MockResponse) = mutex.withLock {
        cachedResponses[key] = response
    }

    override suspend fun getCachedResponse(key: MockKey): MockResponse? {
        return cachedResponses[key]
    }

    override suspend fun clearAllMocks() = mutex.withLock {
        mocks.clear()
    }

    override suspend fun clearAllCache() = mutex.withLock {
        cachedResponses.clear()
    }

    override suspend fun getAllMockKeys(): List<MockKey> {
        return mocks.keys.toList()
    }

    override suspend fun getAllCachedKeys(): List<MockKey> {
        return cachedResponses.keys.toList()
    }

    override suspend fun hasMock(key: MockKey): Boolean {
        return mocks.containsKey(key)
    }

    override suspend fun hasCachedResponse(key: MockKey): Boolean {
        return cachedResponses.containsKey(key)
    }

    /**
     * 저장소의 현재 상태 정보를 반환합니다.
     *
     * @return 저장소 상태 정보
     */
    fun getStatus(): RepositoryStatus {
        return RepositoryStatus(
            mockCount = mocks.size,
            cacheCount = cachedResponses.size
        )
    }

    /**
     * 저장소 상태 정보 데이터 클래스
     */
    data class RepositoryStatus(
        val mockCount: Int,
        val cacheCount: Int
    ) {
        val totalItems: Int get() = mockCount + cacheCount
        val isEmpty: Boolean get() = totalItems == 0
    }
}