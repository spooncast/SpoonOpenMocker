package net.spooncast.openmocker.core.engine

import kotlinx.coroutines.delay
import net.spooncast.openmocker.core.model.MockKey
import net.spooncast.openmocker.core.model.MockResponse
import net.spooncast.openmocker.core.repository.MockRepository

/**
 * MockerEngine 기본 구현체
 * Mock 엔진의 핵심 로직을 구현합니다.
 *
 * @param repository Mock 데이터를 관리하는 저장소
 */
class MockerEngineImpl(
    private val repository: MockRepository
) : MockerEngine {

    override suspend fun getMockResponse(key: MockKey): MockResponse? {
        val mockResponse = repository.getMock(key) ?: return null

        // 지연 시간이 설정되어 있으면 대기
        if (mockResponse.hasDelay) {
            delay(mockResponse.delay)
        }

        return mockResponse
    }

    override suspend fun cacheResponse(key: MockKey, response: MockResponse) {
        repository.cacheRealResponse(key, response)
    }

    override suspend fun setMock(key: MockKey, mockResponse: MockResponse) {
        repository.saveMock(key, mockResponse)
    }

    override suspend fun removeMock(key: MockKey) {
        repository.removeMock(key)
    }

    override suspend fun clearAllMocks() {
        repository.clearAllMocks()
    }

    override suspend fun getCachedKeys(): List<MockKey> {
        return repository.getAllCachedKeys()
    }

    override suspend fun isMocked(key: MockKey): Boolean {
        return repository.hasMock(key)
    }
}