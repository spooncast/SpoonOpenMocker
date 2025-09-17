package net.spooncast.openmocker.core.model

/**
 * Mock 응답 데이터를 표현하는 클래스
 * HTTP 응답의 상태 코드, 본문, 지연 시간, 헤더를 포함합니다.
 *
 * @param code HTTP 상태 코드 (100-599)
 * @param body 응답 본문 내용
 * @param delay 응답 지연 시간 (밀리초, 0 이상)
 * @param headers HTTP 헤더 맵
 */
data class MockResponse(
    val code: Int,
    val body: String,
    val delay: Long = 0L,
    val headers: Map<String, String> = emptyMap()
) {
    init {
        require(code in 100..599) { "HTTP status code must be between 100 and 599, but was $code" }
        require(delay >= 0) { "Delay cannot be negative, but was $delay" }
    }

    /**
     * 성공적인 응답인지 확인합니다.
     * HTTP 상태 코드가 200-299 범위에 있으면 성공으로 간주합니다.
     */
    val isSuccess: Boolean
        get() = code in 200..299

    /**
     * 클라이언트 오류 응답인지 확인합니다.
     * HTTP 상태 코드가 400-499 범위에 있으면 클라이언트 오류로 간주합니다.
     */
    val isClientError: Boolean
        get() = code in 400..499

    /**
     * 서버 오류 응답인지 확인합니다.
     * HTTP 상태 코드가 500-599 범위에 있으면 서버 오류로 간주합니다.
     */
    val isServerError: Boolean
        get() = code in 500..599

    /**
     * 지연 시간이 설정되어 있는지 확인합니다.
     */
    val hasDelay: Boolean
        get() = delay > 0

    /**
     * 커스텀 헤더가 설정되어 있는지 확인합니다.
     */
    val hasHeaders: Boolean
        get() = headers.isNotEmpty()

    /**
     * MockResponse를 문자열로 표현합니다.
     * 형식: "HTTP $code ($body 길이: ${body.length})"
     */
    override fun toString(): String {
        val delayStr = if (hasDelay) " delay: ${delay}ms" else ""
        val headersStr = if (hasHeaders) " headers: ${headers.size}" else ""
        return "HTTP $code (body: ${body.length} chars)$delayStr$headersStr"
    }
}