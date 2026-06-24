package net.spooncast.openmocker.demo

import net.spooncast.openmocker.demo.repo.DemoChatSocketClient
import net.spooncast.openmocker.lib.control.BufferedEventInjector

/**
 * 제어 서버의 `POST /inject/demo` 로 주입된 payload 를, 데모의 실시간 메시지 스트림으로 흘려보내는 injector.
 *
 * 주입된 raw payload 를 [DemoChatSocketClient.emit] 로 그대로 전달해 "수신 WebSocket 메시지"처럼
 * 화면(Realtime 탭)에 표시되게 한다. 해석은 하지 않는다 — injector 는 통로일 뿐이다.
 *
 * 수신 이력 버퍼링(플러그인 "수신 메시지" 폴링용)은 [BufferedEventInjector] 가 자동 처리하므로,
 * 여기서는 실제 전달([deliver])만 구현한다.
 *
 * `id` 는 `"demo"` 로 유지해 기존 plugin/curl E2E(`/inject/demo`) 와 호환된다.
 */
class DemoEventInjector(
    private val client: DemoChatSocketClient,
) : BufferedEventInjector(
    id = "demo",
    name = "Realtime (WebSocket) Demo",
) {

    override fun deliver(payload: String) {
        client.emit(payload)
    }
}
