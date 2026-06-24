package net.spooncast.openmocker.demo.repo

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [DemoChatSocketClient] 단위 테스트.
 *
 * 수신 이력 버퍼링은 lib 의 BufferedEventInjector 로 이전됐으므로, 이 클라이언트는 연결 상태 토글과
 * [DemoChatSocketClient.emit] → [DemoChatSocketClient.incoming] forwarding 만 책임진다.
 */
class DemoChatSocketClientTest {

    @Test
    fun `connect disconnect 가 connected 상태를 토글한다`() {
        val client = DemoChatSocketClient()
        assertFalse(client.connected.value)

        client.connect()
        assertTrue(client.connected.value)

        client.disconnect()
        assertFalse(client.connected.value)
    }

    @Test
    fun `emit 한 payload 가 incoming 으로 전달된다`() = runBlocking {
        val client = DemoChatSocketClient()

        // 구독이 emit 전에 활성화되도록 UNDISPATCHED 로 즉시 시작한다(SharedFlow replay=0).
        val firstIncoming = async(start = CoroutineStart.UNDISPATCHED) {
            client.incoming.first()
        }

        client.emit("hello")

        assertEquals("hello", firstIncoming.await())
    }
}
