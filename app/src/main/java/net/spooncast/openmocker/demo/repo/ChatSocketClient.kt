package net.spooncast.openmocker.demo.repo

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 실시간(WebSocket) 수신 메시지를 앱에 공급하는 추상화(seam).
 *
 * 운영/디버그 구현이 이 계약 하나를 공유한다:
 * - 운영 구현은 실제 소켓을 열어 `onMessage` 프레임을 [incoming] 으로 forward 한다.
 * - 데모 구현([DemoChatSocketClient])은 OpenMocker sink 가 주입한 payload 를 [incoming] 으로 흘려보낸다.
 *
 * UI 는 어느 구현인지 모른 채 [incoming] 만 구독하므로, 같은 seam 으로 실제/모킹이 갈린다.
 */
interface ChatSocketClient {

    /** 수신 메시지 스트림. 구독 이후 도착한 메시지만 전달한다(replay 없음). */
    val incoming: SharedFlow<String>

    /** 연결 상태. 화면이 연결 표시에 사용한다. */
    val connected: StateFlow<Boolean>

    /** 연결을 시작한다(데모 구현은 상태만 토글). */
    fun connect()

    /** 연결을 종료한다. */
    fun disconnect()
}
