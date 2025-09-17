package net.spooncast.openmocker.core.engine

import net.spooncast.openmocker.core.model.MockKey
import net.spooncast.openmocker.core.model.MockResponse

/**
 * Mock 엔진 인터페이스
 * HTTP 요청 모킹의 핵심 기능을 정의합니다.
 *
 * 주요 기능:
 * - Mock 응답 조회 및 반환
 * - 실제 응답 캐싱
 * - Mock 설정/해제
 * - 지연 시뮬레이션
 */
interface MockerEngine {

    /**
     * Mock 응답을 조회합니다.
     * Mock이 존재하면 해당 응답을, 없으면 null을 반환합니다.
     *
     * @param key Mock 요청 식별 키
     * @return Mock 응답 또는 null
     */
    suspend fun getMockResponse(key: MockKey): MockResponse?

    /**
     * 실제 응답을 캐시합니다.
     * 나중에 모킹할 수 있도록 실제 응답 데이터를 저장합니다.
     *
     * @param key Mock 요청 식별 키
     * @param response 실제 응답 데이터
     */
    suspend fun cacheResponse(key: MockKey, response: MockResponse)

    /**
     * Mock을 설정합니다.
     * 지정된 키에 대해 특정 응답을 Mock으로 설정합니다.
     *
     * @param key Mock 요청 식별 키
     * @param mockResponse Mock 응답 데이터
     */
    suspend fun setMock(key: MockKey, mockResponse: MockResponse)

    /**
     * Mock을 해제합니다.
     * 지정된 키에 대한 Mock을 제거하여 실제 응답이 반환되도록 합니다.
     *
     * @param key Mock 요청 식별 키
     */
    suspend fun removeMock(key: MockKey)

    /**
     * 모든 Mock을 해제합니다.
     * 설정된 모든 Mock을 제거합니다.
     */
    suspend fun clearAllMocks()

    /**
     * 캐시된 응답 목록을 조회합니다.
     * 현재 캐시된 모든 응답의 키 목록을 반환합니다.
     *
     * @return 캐시된 응답 키 목록
     */
    suspend fun getCachedKeys(): List<MockKey>

    /**
     * Mock 상태를 확인합니다.
     * 지정된 키에 대해 Mock이 설정되어 있는지 확인합니다.
     *
     * @param key Mock 요청 식별 키
     * @return Mock 설정 여부
     */
    suspend fun isMocked(key: MockKey): Boolean
}