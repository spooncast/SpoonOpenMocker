package net.spooncast.openmocker.lib.control

import net.spooncast.openmocker.lib.control.dto.MockRequestDto
import net.spooncast.openmocker.lib.control.dto.MockDto
import net.spooncast.openmocker.lib.control.dto.PresetDto
import net.spooncast.openmocker.lib.control.dto.ReceivedMessageDto
import net.spooncast.openmocker.lib.control.dto.RecordedEntryDto
import net.spooncast.openmocker.lib.control.dto.ResponseDto
import net.spooncast.openmocker.lib.control.dto.SinkDto
import net.spooncast.openmocker.lib.data.repo.CacheRepo
import net.spooncast.openmocker.lib.model.CachedKey

/**
 * 제어 contract 의 도메인 동작을 담당한다. HTTP/소켓/직렬화는 모르고([ControlServer] 책임),
 * [CacheRepo] 와 [SinkRegistry] 호출로 각 라우트를 매핑하며 내부 모델 ↔ DTO 변환만 수행한다.
 *
 * HTTP 와 분리되어 있어 순수 단위 테스트가 가능하다.
 */
internal class ControlService(
    private val cacheRepo: CacheRepo,
    private val sinkRegistry: SinkRegistry = SinkRegistry,
) {

    /**
     * `GET /rest/recorded` — 기록된 모든 항목을 DTO 로 반환한다.
     *
     * `cachedMap` 은 Compose `SnapshotStateMap` 일 수 있어, 순회 중 변경되면 CME 가 날 수 있다.
     * `toMap()` 으로 방어 복사한 스냅샷을 매핑한다.
     */
    fun recorded(): List<RecordedEntryDto> {
        return cacheRepo.cachedMap.toMap().map { (key, value) ->
            RecordedEntryDto(
                method = key.method,
                path = key.path,
                response = ResponseDto(
                    code = value.response.code,
                    body = value.response.body,
                ),
                mock = value.mock?.let { mock ->
                    MockDto(
                        code = mock.code,
                        body = mock.body,
                        duration = mock.duration,
                    )
                },
            )
        }
    }

    /** `POST /rest/mock` — create-or-update 로 mock 을 설정한다. */
    fun upsertMock(req: MockRequestDto): Boolean {
        return cacheRepo.upsertMock(
            method = req.method,
            urlPath = req.path,
            code = req.code,
            body = req.body,
            duration = req.duration,
        )
    }

    /** `DELETE /rest/mock?method=&path=` — 해당 키의 mock 을 해제한다(키가 없으면 false). */
    fun unMock(method: String, path: String): Boolean {
        return cacheRepo.unMock(CachedKey(method, path))
    }

    /** `DELETE /rest/mock?all=true` — 기록/ mock 전체를 비운다. */
    fun clearAll() {
        cacheRepo.clearCache()
    }

    /** `GET /inject/sinks` — 등록된 sink 목록을 DTO 로 반환한다. */
    fun sinks(): List<SinkDto> {
        return sinkRegistry.all().map { sink ->
            SinkDto(
                id = sink.id,
                name = sink.name,
                presets = sink.presets().map { preset ->
                    PresetDto(name = preset.name, payload = preset.payload)
                },
            )
        }
    }

    /**
     * `GET /inject/{id}/received` — 해당 sink 가 수신한 프레임 목록을 DTO 로 반환한다.
     * 등록되지 않은 id 면 null(→ 라우터가 404 로 변환). sinks() 의 preset 매핑과 동일한 형태다.
     */
    fun received(id: String): List<ReceivedMessageDto>? {
        val sink = sinkRegistry.get(id) ?: return null
        return sink.received().map { msg ->
            ReceivedMessageDto(seq = msg.seq, payload = msg.payload)
        }
    }

    /**
     * `POST /inject/{id}` — 해당 sink 에 raw payload 를 주입한다.
     * 등록되지 않은 id 면 false 를 반환한다.
     */
    fun inject(id: String, payload: String): Boolean {
        val sink = sinkRegistry.get(id) ?: return false
        sink.inject(payload)
        return true
    }
}
