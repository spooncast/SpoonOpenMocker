package net.spooncast.openmocker.lib.control

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SinkRegistryTest {

    // SinkRegistry 는 object 싱글톤이므로 각 테스트 전에 상태를 비운다.
    @BeforeEach
    fun setUp() {
        SinkRegistry.clear()
    }

    private fun fakeSink(id: String, name: String = id) = object : OpenMockerEventSink {
        override val id: String = id
        override val name: String = name
        override fun inject(payload: String) = Unit
        override fun presets(): List<Preset> = emptyList()
    }

    @Test
    fun `register 후 get 으로 동일 sink 를 조회한다`() {
        val sink = fakeSink("wala")

        SinkRegistry.register(sink)

        assertSame(sink, SinkRegistry.get("wala"))
    }

    @Test
    fun `register 한 sink 는 all 에 포함된다`() {
        val a = fakeSink("a")
        val b = fakeSink("b")

        SinkRegistry.register(a)
        SinkRegistry.register(b)

        val all = SinkRegistry.all()
        assertEquals(2, all.size)
        assertTrue(all.containsAll(listOf(a, b)))
    }

    @Test
    fun `unregister 후 get 은 null 이고 all 에서 빠진다`() {
        val sink = fakeSink("wala")
        SinkRegistry.register(sink)

        SinkRegistry.unregister("wala")

        assertNull(SinkRegistry.get("wala"))
        assertFalse(SinkRegistry.all().contains(sink))
    }

    @Test
    fun `같은 id 재등록 시 마지막 sink 가 이긴다 (last-wins)`() {
        val first = fakeSink("wala", name = "first")
        val second = fakeSink("wala", name = "second")

        SinkRegistry.register(first)
        SinkRegistry.register(second)

        assertSame(second, SinkRegistry.get("wala"))
        assertEquals(1, SinkRegistry.all().size)
    }

    @Test
    fun `clear 후 all 은 비어있다`() {
        SinkRegistry.register(fakeSink("a"))
        SinkRegistry.register(fakeSink("b"))

        SinkRegistry.clear()

        assertTrue(SinkRegistry.all().isEmpty())
    }
}
