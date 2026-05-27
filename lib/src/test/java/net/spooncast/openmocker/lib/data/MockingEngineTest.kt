package net.spooncast.openmocker.lib.data

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.spooncast.openmocker.lib.data.adapter.HttpClientAdapter
import net.spooncast.openmocker.lib.data.repo.CacheRepo
import net.spooncast.openmocker.lib.model.CachedResponse
import net.spooncast.openmocker.lib.model.HttpReq
import net.spooncast.openmocker.lib.model.HttpResp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MockingEngineTest {

    private lateinit var cacheRepo: CacheRepo
    private lateinit var adapter: HttpClientAdapter<String, String>
    private lateinit var engine: MockingEngine<String, String>

    @BeforeEach
    fun setUp() {
        cacheRepo = mockk(relaxed = true)
        adapter = mockk(relaxed = true)
        engine = MockingEngine(cacheRepo, adapter)
    }

    @Test
    fun `cacheResponse 는 어댑터로 추출한 데이터를 cacheRepo 에 저장한다`() {
        every { adapter.extractRequestData("req") } returns
            HttpReq(method = "GET", path = "/users", url = "https://x/users")
        every { adapter.extractResponseData("resp") } returns
            HttpResp(code = 201, body = "{\"a\":1}")

        engine.cacheResponse("req", "resp")

        verify {
            cacheRepo.cache(
                method = "GET",
                urlPath = "/users",
                responseCode = 201,
                responseBody = "{\"a\":1}"
            )
        }
    }

    @Test
    fun `getMockData 는 요청의 method, path 로 cacheRepo 를 조회한다`() {
        val expected = CachedResponse(code = 500, body = "mocked")
        every { adapter.extractRequestData("req") } returns
            HttpReq(method = "POST", path = "/login", url = "https://x/login")
        every { cacheRepo.getMock("POST", "/login") } returns expected

        val result = engine.getMockData("req")

        assertSame(expected, result)
    }

    @Test
    fun `getMockData 는 캐시에 없으면 null 을 반환한다`() {
        every { adapter.extractRequestData("req") } returns
            HttpReq(method = "GET", path = "/none", url = "https://x/none")
        every { cacheRepo.getMock(any(), any()) } returns null

        assertNull(engine.getMockData("req"))
    }

    @Test
    fun `createMockResponse 는 어댑터에 위임한다`() {
        val cached = CachedResponse(code = 200, body = "ok")
        every { adapter.createMockResponse("req", cached) } returns "mockResponse"

        val result = engine.createMockResponse("req", cached)

        assertEquals("mockResponse", result)
        verify { adapter.createMockResponse("req", cached) }
    }
}
