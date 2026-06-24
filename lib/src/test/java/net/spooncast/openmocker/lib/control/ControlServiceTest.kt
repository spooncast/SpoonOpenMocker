package net.spooncast.openmocker.lib.control

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.spooncast.openmocker.lib.control.dto.MockRequestDto
import net.spooncast.openmocker.lib.data.repo.CacheRepo
import net.spooncast.openmocker.lib.model.CachedKey
import net.spooncast.openmocker.lib.model.CachedResponse
import net.spooncast.openmocker.lib.model.CachedValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ControlServiceTest {

    private val cacheRepo = mockk<CacheRepo>(relaxed = true)

    // EventInjectorRegistry 는 object 싱글톤이므로 각 테스트 전에 상태를 비운다.
    private val service = ControlService(cacheRepo, EventInjectorRegistry)

    @BeforeEach
    fun setUp() {
        EventInjectorRegistry.clear()
    }

    private fun fakeInjector(
        id: String,
        name: String = id,
    ) = object : OpenMockerEventInjector {
        override val id: String = id
        override val name: String = name
        override fun inject(payload: String) = Unit
    }

    @Test
    fun `recorded 는 mock 이 없으면 mock 을 null 로 변환한다`() {
        every { cacheRepo.cachedMap } returns mapOf(
            CachedKey("GET", "/users") to CachedValue(
                response = CachedResponse(200, "{\"id\":1}"),
            ),
        )

        val result = service.recorded()

        assertEquals(1, result.size)
        val entry = result.first()
        assertEquals("GET", entry.method)
        assertEquals("/users", entry.path)
        assertEquals(200, entry.response.code)
        assertEquals("{\"id\":1}", entry.response.body)
        assertNull(entry.mock)
    }

    @Test
    fun `recorded 는 mock 이 있으면 MockDto 로 변환한다`() {
        every { cacheRepo.cachedMap } returns mapOf(
            CachedKey("POST", "/live") to CachedValue(
                response = CachedResponse(200, "original"),
                mock = CachedResponse(500, "mocked", duration = 1000L),
            ),
        )

        val entry = service.recorded().first()

        assertEquals(200, entry.response.code)
        assertEquals("original", entry.response.body)
        assertEquals(500, entry.mock?.code)
        assertEquals("mocked", entry.mock?.body)
        assertEquals(1000L, entry.mock?.duration)
    }

    @Test
    fun `upsertMock 은 cacheRepo 에 위임하고 결과를 반환한다`() {
        every {
            cacheRepo.upsertMock("GET", "/live", 500, "{}", 0L)
        } returns true

        val result = service.upsertMock(
            MockRequestDto(method = "GET", path = "/live", code = 500, body = "{}"),
        )

        assertTrue(result)
        verify { cacheRepo.upsertMock("GET", "/live", 500, "{}", 0L) }
    }

    @Test
    fun `unMock 은 CachedKey 를 만들어 cacheRepo 에 위임한다`() {
        every { cacheRepo.unMock(CachedKey("GET", "/live")) } returns true

        val result = service.unMock("GET", "/live")

        assertTrue(result)
        verify { cacheRepo.unMock(CachedKey("GET", "/live")) }
    }

    @Test
    fun `clearAll 은 cacheRepo clearCache 에 위임한다`() {
        service.clearAll()

        verify { cacheRepo.clearCache() }
    }

    @Test
    fun `injectors 는 등록된 injector 를 DTO 로 변환한다`() {
        EventInjectorRegistry.register(fakeInjector(id = "wala", name = "WALA"))

        val injectors = service.injectors()

        assertEquals(1, injectors.size)
        val dto = injectors.first()
        assertEquals("wala", dto.id)
        assertEquals("WALA", dto.name)
    }

    @Test
    fun `recorded(id) 는 등록된 injector 의 수신 프레임을 DTO 로 변환한다`() {
        EventInjectorRegistry.register(object : OpenMockerEventInjector {
            override val id: String = "wala"
            override val name: String = "WALA"
            override fun inject(payload: String) = Unit
            override fun recorded(): List<RecordedMessage> = listOf(
                RecordedMessage(sequence = 2L, payload = "{\"event\":\"chat\"}"),
                RecordedMessage(sequence = 1L, payload = "{\"event\":\"tick\"}"),
            )
        })

        val recorded = service.recorded("wala")

        assertEquals(2, recorded?.size)
        assertEquals(2L, recorded?.first()?.sequence)
        assertEquals("{\"event\":\"chat\"}", recorded?.first()?.payload)
        assertEquals(1L, recorded?.get(1)?.sequence)
        assertEquals("{\"event\":\"tick\"}", recorded?.get(1)?.payload)
    }

    @Test
    fun `recorded(id) 는 기록하지 않는 injector 면 빈 목록을 반환한다`() {
        EventInjectorRegistry.register(fakeInjector(id = "demo"))

        val recorded = service.recorded("demo")

        assertEquals(emptyList<Any>(), recorded)
    }

    @Test
    fun `recorded(id) 는 미등록 id 면 null 을 반환한다`() {
        val recorded = service.recorded("unknown")

        assertNull(recorded)
    }

    @Test
    fun `inject 는 등록된 injector 에 payload 를 전달하고 true 를 반환한다`() {
        var injected: String? = null
        EventInjectorRegistry.register(object : OpenMockerEventInjector {
            override val id: String = "wala"
            override val name: String = "WALA"
            override fun inject(payload: String) { injected = payload }
        })

        val result = service.inject("wala", "raw-payload")

        assertTrue(result)
        assertEquals("raw-payload", injected)
    }

    @Test
    fun `inject 는 미등록 id 면 false 를 반환한다`() {
        val result = service.inject("unknown", "raw-payload")

        assertFalse(result)
    }

    @Test
    fun `clearRecorded 는 등록된 injector 의 clearRecorded 를 호출하고 true 를 반환한다`() {
        var cleared = false
        EventInjectorRegistry.register(object : OpenMockerEventInjector {
            override val id: String = "wala"
            override val name: String = "WALA"
            override fun inject(payload: String) = Unit
            override fun clearRecorded() { cleared = true }
        })

        val result = service.clearRecorded("wala")

        assertTrue(result)
        assertTrue(cleared)
    }

    @Test
    fun `clearRecorded 는 미등록 id 면 false 를 반환한다`() {
        val result = service.clearRecorded("unknown")

        assertFalse(result)
    }
}
