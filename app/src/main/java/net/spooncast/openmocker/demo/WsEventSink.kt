package net.spooncast.openmocker.demo

import net.spooncast.openmocker.demo.repo.DemoChatSocketClient
import net.spooncast.openmocker.lib.control.OpenMockerEventSink
import net.spooncast.openmocker.lib.control.Preset

/**
 * 제어 서버의 `POST /inject/demo` 로 주입된 payload 를, 데모의 실시간 메시지 스트림으로 흘려보내는 sink.
 *
 * 주입된 raw payload 를 [DemoChatSocketClient.emit] 로 그대로 전달해 "수신 WebSocket 메시지"처럼
 * 화면(Realtime 탭)에 표시되게 한다. 해석은 하지 않는다 — sink 는 통로일 뿐이다.
 *
 * `id` 는 `"demo"` 로 유지해 기존 plugin/curl E2E(`/inject/demo`) 와 호환된다.
 */
class WsEventSink(
    private val client: DemoChatSocketClient,
) : OpenMockerEventSink {

    override val id: String = "demo"

    override val name: String = "Realtime (WebSocket) Demo"

    override fun inject(payload: String) {
        client.emit(payload)
    }

    override fun presets(): List<Preset> = listOf(
        Preset(name = "chat", payload = """{"event":"chat","from":"plugin","text":"hello from plugin"}"""),
        Preset(name = "tick", payload = """{"event":"tick","price":1234}"""),
        Preset(name = "notice", payload = """{"event":"notice","message":"서버 점검 예정"}"""),
    )
}
