package net.spooncast.openmocker.lib.control

import java.util.concurrent.ConcurrentHashMap

/**
 * [OpenMockerEventSink] 를 id 키로 보관하는 in-memory 싱글톤(thread-safe).
 *
 * 제어 서버가 `GET /inject/sinks` 로 목록을, `POST /inject/{id}` 로 특정 sink 를 조회한다.
 * 같은 id 로 재등록하면 마지막 등록이 이긴다(last-wins).
 */
internal object SinkRegistry {

    private val sinks = ConcurrentHashMap<String, OpenMockerEventSink>()

    /** sink 를 id 키로 등록한다. 같은 id 가 이미 있으면 덮어쓴다(last-wins). */
    fun register(sink: OpenMockerEventSink) {
        sinks[sink.id] = sink
    }

    /** 해당 id 의 sink 를 해제한다. */
    fun unregister(id: String) {
        sinks.remove(id)
    }

    /** 해당 id 의 sink 를 조회한다. 없으면 null. */
    fun get(id: String): OpenMockerEventSink? {
        return sinks[id]
    }

    /** 등록된 모든 sink 의 스냅샷. */
    fun all(): List<OpenMockerEventSink> {
        return sinks.values.toList()
    }

    /** 전체 비운다(주로 테스트용). */
    fun clear() {
        sinks.clear()
    }
}
