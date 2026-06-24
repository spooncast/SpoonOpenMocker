package net.spooncast.openmocker.lib.control.dto

import kotlinx.serialization.Serializable

/**
 * 제어 서버(localhost:8099) 의 동결된 API contract 를 표현하는 직렬화 DTO 모음.
 *
 * 내부 모델([net.spooncast.openmocker.lib.model])과 분리해, 외부에 노출되는 JSON 형태를
 * contract 에 고정한다. 직렬화/역직렬화는 ControlServer 가 담당하고, 이 DTO 들은 그 경계의
 * 데이터 형태만 정의한다.
 */

/**
 * `GET /rest/recorded` 응답 한 항목. 기록된 원본 응답([response])과, 설정되어 있으면
 * 현재 mock([mock])을 함께 담는다.
 */
@Serializable
internal data class RecordedEntryDto(
    val method: String,
    val path: String,
    val response: ResponseDto,
    val mock: MockDto? = null,
)

/** 기록된 원본 응답(관측값). */
@Serializable
internal data class ResponseDto(
    val code: Int,
    val body: String,
)

/** 현재 설정된 mock 응답. */
@Serializable
internal data class MockDto(
    val code: Int,
    val body: String,
    val duration: Long,
)

/** `POST /rest/mock` 요청 바디. create-or-update 로 mock 을 설정한다. */
@Serializable
internal data class MockRequestDto(
    val method: String,
    val path: String,
    val code: Int,
    val body: String,
    val duration: Long = 0L,
)

/** `GET /inject/injectors` 응답 한 항목. 등록된 [net.spooncast.openmocker.lib.control.OpenMockerEventInjector] 의 노출 정보. */
@Serializable
internal data class InjectorDto(
    val id: String,
    val name: String,
)

/** `GET /inject/{id}/recorded` 응답 한 항목. injector 가 수신한 프레임(일련번호 + 원문). */
@Serializable
internal data class RecordedMessageDto(
    val sequence: Long,
    val payload: String,
)

/** 성공 응답 공용 바디(`{"ok":true}`). */
@Serializable
internal data class OkDto(
    val ok: Boolean = true,
)
