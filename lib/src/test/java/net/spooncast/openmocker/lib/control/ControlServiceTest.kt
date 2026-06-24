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

    // SinkRegistry 는 object 싱글톤이므로 각 테스트 전에 상태를 비운다.
    private val service = ControlService(cacheRepo, SinkRegistry)

    @BeforeEach
    fun setUp() {
        SinkRegistry.clear()
    }

    private fun fakeSink(
        id: String,
        name: String = id,
        presets: List<Preset> = emptyList(),
    ) = object : OpenMockerEventSink {
        override val id: String = id
        override val name: String = name
        override fun inject(payload: String) = Unit
        override fun presets(): List<Preset> = presets
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
    fun `sinks 는 등록된 sink 와 preset 을 DTO 로 변환한다`() {
        SinkRegistry.register(
            fakeSink(
                id = "wala",
                name = "WALA",
                presets = listOf(Preset("room_close", "{\"type\":\"close\"}")),
            ),
        )

        val sinks = service.sinks()

        assertEquals(1, sinks.size)
        val dto = sinks.first()
        assertEquals("wala", dto.id)
        assertEquals("WALA", dto.name)
        assertEquals(1, dto.presets.size)
        assertEquals("room_close", dto.presets.first().name)
        assertEquals("{\"type\":\"close\"}", dto.presets.first().payload)
    }

    @Test
    fun `received 는 등록된 sink 의 수신 프레임을 DTO 로 변환한다`() {
        SinkRegistry.register(object : OpenMockerEventSink {
            override val id: String = "wala"
            override val name: String = "WALA"
            override fun inject(payload: String) = Unit
            override fun presets(): List<Preset> = emptyList()
            override fun received(): List<ReceivedMessage> = listOf(
                ReceivedMessage(seq = 2L, payload = "{\"event\":\"chat\"}"),
                ReceivedMessage(seq = 1L, payload = "{\"event\":\"tick\"}"),
            )
        })

        val received = service.received("wala")

        assertEquals(2, received?.size)
        assertEquals(2L, received?.first()?.seq)
        assertEquals("{\"event\":\"chat\"}", received?.first()?.payload)
        assertEquals(1L, received?.get(1)?.seq)
        assertEquals("{\"event\":\"tick\"}", received?.get(1)?.payload)
    }

    @Test
    fun `received 는 기록하지 않는 sink 면 빈 목록을 반환한다`() {
        SinkRegistry.register(fakeSink(id = "demo"))

        val received = service.received("demo")

        assertEquals(emptyList<Any>(), received)
    }

    @Test
    fun `received 는 미등록 id 면 null 을 반환한다`() {
        val received = service.received("unknown")

        assertNull(received)
    }

    @Test
    fun `inject 는 등록된 sink 에 payload 를 전달하고 true 를 반환한다`() {
        var injected: String? = null
        SinkRegistry.register(object : OpenMockerEventSink {
            override val id: String = "wala"
            override val name: String = "WALA"
            override fun inject(payload: String) { injected = payload }
            override fun presets(): List<Preset> = emptyList()
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
}
