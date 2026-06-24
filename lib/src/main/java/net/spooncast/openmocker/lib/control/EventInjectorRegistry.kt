package net.spooncast.openmocker.lib.control

import java.util.concurrent.ConcurrentHashMap

/**
 * [OpenMockerEventInjector] 를 id 키로 보관하는 in-memory 싱글톤(thread-safe).
 *
 * 제어 서버가 `GET /inject/injectors` 로 목록을, `POST /inject/{id}` 로 특정 injector 를 조회한다.
 * 같은 id 로 재등록하면 마지막 등록이 이긴다(last-wins).
 */
internal object EventInjectorRegistry {

    /** id 는 URL 경로 세그먼트(`/inject/{id}`)로 쓰이므로 안전한 문자만 허용한다. */
    private val ID_PATTERN = Regex("^[A-Za-z0-9._-]+$")

    private val injectors = ConcurrentHashMap<String, OpenMockerEventInjector>()

    /**
     * injector 를 id 키로 등록한다. 같은 id 가 이미 있으면 덮어쓴다(last-wins).
     *
     * @throws IllegalArgumentException id 가 비었거나 `[A-Za-z0-9._-]` 외 문자를 포함하면(URL 경로 안전성).
     */
    fun register(injector: OpenMockerEventInjector) {
        require(ID_PATTERN.matches(injector.id)) {
            "injector id must match ${ID_PATTERN.pattern} (URL path-safe), but was '${injector.id}'"
        }
        injectors[injector.id] = injector
    }

    /** 해당 id 의 injector 를 해제한다. */
    fun unregister(id: String) {
        injectors.remove(id)
    }

    /** 해당 id 의 injector 를 조회한다. 없으면 null. */
    fun get(id: String): OpenMockerEventInjector? {
        return injectors[id]
    }

    /** 등록된 모든 injector 의 스냅샷. */
    fun all(): List<OpenMockerEventInjector> {
        return injectors.values.toList()
    }

    /** 전체 비운다(주로 테스트용). */
    fun clear() {
        injectors.clear()
    }
}
