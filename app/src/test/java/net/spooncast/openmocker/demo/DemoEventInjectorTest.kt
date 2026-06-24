package net.spooncast.openmocker.demo

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.spooncast.openmocker.demo.repo.DemoChatSocketClient
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [DemoEventInjector] 단위 테스트.
 *
 * 데모-특정 배선만 검증한다: inject 가 (1) client.emit 으로 incoming 에 도달하고,
 * (2) BufferedEventInjector 베이스 버퍼에 기록돼 recorded 로 노출된다.
 * (버퍼 용량·sequence 채번 규칙 자체는 lib 의 BufferedEventInjectorTest 가 검증한다.)
 */
class DemoEventInjectorTest {

    @Test
    fun `id 와 name 은 demo 계약을 유지한다`() {
        val injector = DemoEventInjector(DemoChatSocketClient())

        assertEquals("demo", injector.id)
        assertEquals("Realtime (WebSocket) Demo", injector.name)
    }

    @Test
    fun `inject 한 payload 가 client incoming 으로 전달된다`() = runBlocking {
        val client = DemoChatSocketClient()
        val injector = DemoEventInjector(client)

        val firstIncoming = async(start = CoroutineStart.UNDISPATCHED) {
            client.incoming.first()
        }

        injector.inject("""{"event":"chat"}""")

        assertEquals("""{"event":"chat"}""", firstIncoming.await())
    }

    @Test
    fun `inject 한 payload 가 recorded 에 최신순으로 기록된다`() {
        val injector = DemoEventInjector(DemoChatSocketClient())

        injector.inject("first")
        injector.inject("second")

        val recorded = injector.recorded()
        assertEquals(listOf("second", "first"), recorded.map { it.payload })
    }
}
