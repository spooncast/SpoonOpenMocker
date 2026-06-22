package net.spooncast.openmocker.lib.control

/**
 * HTTP 범위 밖의 이벤트(예: WebSocket/WALA)를 앱이 OpenMocker 제어 통로에 연결하기 위한 범용 추상화.
 *
 * 앱이 이 인터페이스를 구현해 [net.spooncast.openmocker.lib.OpenMocker.registerSink] 로 등록하면,
 * 제어 서버가 `POST /inject/{id}` 로 받은 raw payload 를 파싱 없이 [inject] 로 그대로 전달한다.
 */
interface OpenMockerEventSink {
    /** 레지스트리 키. `POST /inject/{id}` 의 `{id}` 와 매칭된다. */
    val id: String

    /** 사람이 읽는 표시 이름(플러그인 UI 용). */
    val name: String

    /**
     * raw payload 를 sink 로 주입한다. 제어 서버는 본문을 해석하지 않고 통째로 전달하며,
     * 해석은 전적으로 sink 구현(앱)의 책임이다.
     */
    fun inject(payload: String)

    /** 플러그인 UI 가 버튼으로 노출할 미리 정의된 payload 목록(편의 기능). 없으면 빈 목록. */
    fun presets(): List<Preset>
}

/** 미리 정의된 주입 payload. [OpenMockerEventSink.presets] 가 반환한다. */
data class Preset(
    val name: String,
    val payload: String,
)
