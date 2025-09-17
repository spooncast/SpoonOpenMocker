package net.spooncast.openmocker.core.repository

import net.spooncast.openmocker.core.model.MockKey
import net.spooncast.openmocker.core.model.MockResponse

/**
 * Mock 저장소 인터페이스
 * Mock 데이터의 저장, 조회, 관리를 담당합니다.
 *
 * 주요 기능:
 * - Mock 데이터 저장 및 조회
 * - 실제 응답 캐싱
 * - 동시성 안전 보장
 * - 메모리 관리
 */
interface MockRepository {

    /**
     * Mock 응답을 저장합니다.
     *
     * @param key Mock 요청 식별 키
     * @param mockResponse Mock 응답 데이터
     */
    suspend fun saveMock(key: MockKey, mockResponse: MockResponse)

    /**
     * Mock 응답을 조회합니다.
     *
     * @param key Mock 요청 식별 키
     * @return Mock 응답 또는 null
     */
    suspend fun getMock(key: MockKey): MockResponse?

    /**
     * Mock 응답을 삭제합니다.
     *
     * @param key Mock 요청 식별 키
     * @return 삭제 성공 여부
     */
    suspend fun removeMock(key: MockKey): Boolean

    /**
     * 실제 응답을 캐시합니다.
     *
     * @param key Mock 요청 식별 키
     * @param response 실제 응답 데이터
     */
    suspend fun cacheRealResponse(key: MockKey, response: MockResponse)

    /**
     * 캐시된 실제 응답을 조회합니다.
     *
     * @param key Mock 요청 식별 키
     * @return 캐시된 실제 응답 또는 null
     */
    suspend fun getCachedResponse(key: MockKey): MockResponse?

    /**
     * 모든 Mock을 삭제합니다.
     */
    suspend fun clearAllMocks()

    /**
     * 모든 캐시를 삭제합니다.
     */
    suspend fun clearAllCache()

    /**
     * 저장된 모든 Mock 키 목록을 조회합니다.
     *
     * @return Mock 키 목록
     */
    suspend fun getAllMockKeys(): List<MockKey>

    /**
     * 캐시된 모든 응답 키 목록을 조회합니다.
     *
     * @return 캐시된 응답 키 목록
     */
    suspend fun getAllCachedKeys(): List<MockKey>

    /**
     * Mock 존재 여부를 확인합니다.
     *
     * @param key Mock 요청 식별 키
     * @return Mock 존재 여부
     */
    suspend fun hasMock(key: MockKey): Boolean

    /**
     * 캐시된 응답 존재 여부를 확인합니다.
     *
     * @param key Mock 요청 식별 키
     * @return 캐시된 응답 존재 여부
     */
    suspend fun hasCachedResponse(key: MockKey): Boolean
}