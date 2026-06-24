package net.spooncast.openmocker.plugin.net

/**
 * 제어 서버(localhost:8099) 의 동결된 API contract 를 플러그인 쪽에서 미러한 모델.
 *
 * :lib 의 `control/dto/ControlDtos.kt` 와 동일한 와이어(JSON) 형태를 표현하되, 플러그인은
 * 외부 런타임 의존성 0 을 유지하기 위해 kotlinx.serialization 대신 플랫폼 번들 Gson 으로
 * (역)직렬화한다. Gson 은 프로퍼티명을 그대로 키로 쓰므로 필드명이 contract 와 1:1 로 일치해야
 * 한다(임의 변경 금지). 서버는 `encodeDefaults = true` / `ignoreUnknownKeys = true` 로 동작한다.
 */

/**
 * `GET /rest/recorded` 응답 한 항목. 기록된 원본 응답([response]) 과, 설정되어 있으면
 * 현재 mock([mock]) 을 함께 담는다. mock 이 없으면 null(키 부재 또는 `mock:null`).
 */
data class RecordedEntry(
    val method: String,
    val path: String,
    val response: ResponseData,
    val mock: MockData? = null,
)

/** 기록된 원본 응답(관측값). */
data class ResponseData(
    val code: Int,
    val body: String,
)

/** 현재 설정된 mock 응답. */
data class MockData(
    val code: Int,
    val body: String,
    val duration: Long,
)

/** `POST /rest/mock` 요청 바디. create-or-update 로 mock 을 설정한다. */
data class MockRequest(
    val method: String,
    val path: String,
    val code: Int,
    val body: String,
    val duration: Long = 0L,
)

/** `GET /inject/sinks` 응답 한 항목. 등록된 sink 의 노출 정보. */
data class Sink(
    val id: String,
    val name: String,
    val presets: List<Preset>,
)

/** sink 가 제공하는 미리 정의된 주입 payload. */
data class Preset(
    val name: String,
    val payload: String,
)

/**
 * `GET /inject/{id}/received` 응답 한 항목. sink 가 수신한 프레임(일련번호 + 원문).
 * :lib 의 `ReceivedMessageDto` 와 필드명이 1:1 로 일치해야 한다(Gson 키 매칭).
 */
data class ReceivedMessage(
    val seq: Long,
    val payload: String,
)
