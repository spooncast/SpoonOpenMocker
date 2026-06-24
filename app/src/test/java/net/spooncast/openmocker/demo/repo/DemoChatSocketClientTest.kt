package net.spooncast.openmocker.demo.repo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [DemoChatSocketClient] 수신 히스토리 버퍼 단위 테스트.
 *
 * 검증: emit 이 최신순 노출·capacity(50) 제한·seq 단조 증가를 지킨다.
 */
class DemoChatSocketClientTest {

    @Test
    fun `recentReceived 는 빈 상태에서 빈 목록을 반환한다`() {
        val client = DemoChatSocketClient()

        assertEquals(emptyList<ReceivedFrame>(), client.recentReceived())
    }

    @Test
    fun `emit 한 프레임을 최신순으로 반환하고 seq 가 단조 증가한다`() {
        val client = DemoChatSocketClient()

        client.emit("a")
        client.emit("b")
        client.emit("c")

        val received = client.recentReceived()
        assertEquals(listOf("c", "b", "a"), received.map { it.payload })
        // seq 는 1,2,3 순으로 매겨지고, 최신순 노출이므로 3,2,1
        assertEquals(listOf(3L, 2L, 1L), received.map { it.seq })
    }

    @Test
    fun `버퍼는 최근 50건만 유지한다`() {
        val client = DemoChatSocketClient()

        repeat(60) { client.emit("msg-$it") }

        val received = client.recentReceived()
        assertEquals(50, received.size)
        // 가장 최신(msg-59)이 맨 앞, 가장 오래된 유지분(msg-10)이 맨 뒤
        assertEquals("msg-59", received.first().payload)
        assertEquals("msg-10", received.last().payload)
        // seq 는 끊김 없이 단조 감소(최신순)
        assertEquals(60L, received.first().seq)
        assertEquals(11L, received.last().seq)
        assertTrue(received.zipWithNext().all { (a, b) -> a.seq > b.seq })
    }
}
