package net.spooncast.openmocker.lib.control

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EventInjectorRegistryTest {

    // EventInjectorRegistry 는 object 싱글톤이므로 각 테스트 전에 상태를 비운다.
    @BeforeEach
    fun setUp() {
        EventInjectorRegistry.clear()
    }

    private fun fakeInjector(id: String, name: String = id) = object : OpenMockerEventInjector {
        override val id: String = id
        override val name: String = name
        override fun inject(payload: String) = Unit
    }

    @Test
    fun `register 후 get 으로 동일 injector 를 조회한다`() {
        val injector = fakeInjector("wala")

        EventInjectorRegistry.register(injector)

        assertSame(injector, EventInjectorRegistry.get("wala"))
    }

    @Test
    fun `register 한 injector 는 all 에 포함된다`() {
        val a = fakeInjector("a")
        val b = fakeInjector("b")

        EventInjectorRegistry.register(a)
        EventInjectorRegistry.register(b)

        val all = EventInjectorRegistry.all()
        assertEquals(2, all.size)
        assertTrue(all.containsAll(listOf(a, b)))
    }

    @Test
    fun `unregister 후 get 은 null 이고 all 에서 빠진다`() {
        val injector = fakeInjector("wala")
        EventInjectorRegistry.register(injector)

        EventInjectorRegistry.unregister("wala")

        assertNull(EventInjectorRegistry.get("wala"))
        assertFalse(EventInjectorRegistry.all().contains(injector))
    }

    @Test
    fun `같은 id 재등록 시 마지막 injector 가 이긴다 (last-wins)`() {
        val first = fakeInjector("wala", name = "first")
        val second = fakeInjector("wala", name = "second")

        EventInjectorRegistry.register(first)
        EventInjectorRegistry.register(second)

        assertSame(second, EventInjectorRegistry.get("wala"))
        assertEquals(1, EventInjectorRegistry.all().size)
    }

    @Test
    fun `clear 후 all 은 비어있다`() {
        EventInjectorRegistry.register(fakeInjector("a"))
        EventInjectorRegistry.register(fakeInjector("b"))

        EventInjectorRegistry.clear()

        assertTrue(EventInjectorRegistry.all().isEmpty())
    }

    @Test
    fun `영숫자와 dot underscore hyphen 으로 된 id 는 등록된다`() {
        EventInjectorRegistry.register(fakeInjector("demo.room_1-A"))

        assertNotNull(EventInjectorRegistry.get("demo.room_1-A"))
    }

    @Test
    fun `URL 경로에 안전하지 않은 id 는 등록 시 예외를 던진다`() {
        assertThrows(IllegalArgumentException::class.java) {
            EventInjectorRegistry.register(fakeInjector("with space"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            EventInjectorRegistry.register(fakeInjector("a/b"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            EventInjectorRegistry.register(fakeInjector(""))
        }
    }
}
