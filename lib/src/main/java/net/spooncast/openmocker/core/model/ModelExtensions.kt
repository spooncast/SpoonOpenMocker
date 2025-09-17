package net.spooncast.openmocker.core.model

import net.spooncast.openmocker.lib.model.CachedKey
import net.spooncast.openmocker.lib.model.CachedResponse

/**
 * 기존 모델과 Core 모델 간의 변환 확장 함수들
 * 기존 코드와의 호환성을 위해 제공됩니다.
 */

/**
 * CachedKey를 MockKey로 변환합니다.
 */
internal fun CachedKey.toMockKey(): MockKey = MockKey(
    method = method,
    path = path
)

/**
 * CachedResponse를 MockResponse로 변환합니다.
 */
internal fun CachedResponse.toMockResponse(): MockResponse = MockResponse(
    code = code,
    body = body,
    delay = duration,
    headers = emptyMap() // CachedResponse에는 headers가 없으므로 빈 맵 사용
)

/**
 * MockKey를 CachedKey로 변환합니다.
 */
internal fun MockKey.toCachedKey(): CachedKey = CachedKey(
    method = method,
    path = path
)

/**
 * MockResponse를 CachedResponse로 변환합니다.
 * headers 정보는 CachedResponse에서 지원하지 않으므로 무시됩니다.
 */
internal fun MockResponse.toCachedResponse(): CachedResponse = CachedResponse(
    code = code,
    body = body,
    duration = delay
)