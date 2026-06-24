package net.spooncast.openmocker.demo.ui.ws

/**
 * Realtime 화면에 표시하는 수신 메시지 한 건.
 *
 * @param seq 도착 순서(1부터). 리스트 key 와 표시용 번호로 쓴다.
 * @param text 주입된 raw payload(해석하지 않고 그대로 표시).
 */
data class WsMessage(
    val seq: Int,
    val text: String,
)
