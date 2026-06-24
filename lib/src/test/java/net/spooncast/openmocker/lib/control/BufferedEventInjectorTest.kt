package net.spooncast.openmocker.lib.control

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BufferedEventInjectorTest {

    private class RecordingInjector(
        id: String = "test",
        name: String = "Test",
        capacity: Int = 50,
    ) : BufferedEventInjector(id, name, capacity) {
        val delivered = mutableListOf<String>()
        override fun deliver(payload: String) { delivered += payload }
    }

    @Test
    fun `inject 마다 deliver 가 1회 호출된다`() {
        val injector = RecordingInjector()

        injector.inject("a")
        injector.inject("b")

        assertEquals(listOf("a", "b"), injector.delivered)
    }

    @Test
    fun `recorded 는 최신순이고 sequence 가 단조 증가한다`() {
        val injector = RecordingInjector()

        injector.inject("first")
        injector.inject("second")

        val recorded = injector.recorded()
        assertEquals(2, recorded.size)
        // 최신순(newest-first)
        assertEquals("second", recorded[0].payload)
        assertEquals("first", recorded[1].payload)
        // sequence 단조 증가(나중 프레임이 더 큼)
        assertTrue(recorded[0].sequence > recorded[1].sequence)
        assertEquals(2L, recorded[0].sequence)
        assertEquals(1L, recorded[1].sequence)
    }

    @Test
    fun `capacity 를 초과하면 오래된 프레임부터 버린다`() {
        val injector = RecordingInjector(capacity = 2)

        injector.inject("1")
        injector.inject("2")
        injector.inject("3")

        val recorded = injector.recorded()
        assertEquals(2, recorded.size)
        assertEquals(listOf("3", "2"), recorded.map { it.payload })
    }

    @Test
    fun `clearRecorded 후 목록은 비고 sequence 카운터는 유지된다`() {
        val injector = RecordingInjector()
        injector.inject("a")
        injector.inject("b")

        injector.clearRecorded()
        assertTrue(injector.recorded().isEmpty())

        // 카운터 유지: 다음 프레임의 sequence 는 이전 최대(2) 다음(3)이어야 한다.
        injector.inject("c")
        assertEquals(3L, injector.recorded().first().sequence)
    }
}
