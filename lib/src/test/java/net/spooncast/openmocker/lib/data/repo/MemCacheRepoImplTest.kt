package net.spooncast.openmocker.lib.data.repo

import net.spooncast.openmocker.lib.model.CachedKey
import net.spooncast.openmocker.lib.model.CachedResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MemCacheRepoImplTest {

    // getInstance() 는 싱글톤을 반환하므로 각 테스트 전에 상태를 비운다.
    private val repo = MemCacheRepoImpl.getInstance()

    @BeforeEach
    fun setUp() {
        repo.clearCache()
    }

    @Test
    fun `getInstance 는 항상 동일한 인스턴스를 반환한다`() {
        assertEquals(MemCacheRepoImpl.getInstance(), MemCacheRepoImpl.getInstance())
    }

    @Test
    fun `cache 는 cachedMap 에 response 를 저장한다`() {
        repo.cache("GET", "/users", 200, "{\"id\":1}")

        val value = repo.cachedMap[CachedKey("GET", "/users")]
        assertEquals(200, value?.response?.code)
    }

    @Test
    fun `cache 는 유효한 JSON 을 pretty print 형태로 정규화한다`() {
        repo.cache("GET", "/users", 200, "{\"id\":1}")

        val body = repo.cachedMap[CachedKey("GET", "/users")]?.response?.body
        // GsonBuilder().setPrettyPrinting() 결과는 개행과 들여쓰기를 포함한다.
        assertTrue(body!!.contains("\n"))
        assertTrue(body.contains("\"id\""))
    }

    @Test
    fun `cache 는 잘못된 JSON 이면 빈 문자열로 저장한다`() {
        repo.cache("GET", "/broken", 200, "not a json")

        assertEquals("", repo.cachedMap[CachedKey("GET", "/broken")]?.response?.body)
    }

    @Test
    fun `cache 는 같은 키로 다시 호출하면 덮어쓴다`() {
        repo.cache("GET", "/users", 200, "{}")
        repo.cache("GET", "/users", 500, "{}")

        assertEquals(1, repo.cachedMap.size)
        assertEquals(500, repo.cachedMap[CachedKey("GET", "/users")]?.response?.code)
    }

    @Test
    fun `clearCache 는 모든 항목을 제거한다`() {
        repo.cache("GET", "/a", 200, "{}")
        repo.cache("POST", "/b", 200, "{}")

        repo.clearCache()

        assertTrue(repo.cachedMap.isEmpty())
    }

    @Test
    fun `getMock 은 mock 이 설정되기 전에는 null 을 반환한다`() {
        repo.cache("GET", "/users", 200, "{}")

        assertNull(repo.getMock("GET", "/users"))
    }

    @Test
    fun `getMock 은 캐시에 없는 키면 null 을 반환한다`() {
        assertNull(repo.getMock("GET", "/missing"))
    }

    @Test
    fun `mock 은 캐시된 항목에 mock 을 설정하고 true 를 반환한다`() {
        repo.cache("GET", "/users", 200, "{}")
        val key = CachedKey("GET", "/users")
        val mockResponse = CachedResponse(code = 503, body = "down", duration = 1000L)

        val result = repo.mock(key, mockResponse)

        assertTrue(result)
        assertEquals(mockResponse, repo.getMock("GET", "/users"))
    }

    @Test
    fun `mock 은 캐시에 없는 키면 false 를 반환한다`() {
        val result = repo.mock(CachedKey("GET", "/missing"), CachedResponse(500, "x"))

        assertFalse(result)
    }

    @Test
    fun `upsertMock 은 캐시에 없던 키면 새로 생성하고 mock 을 설정한다`() {
        val result = repo.upsertMock("GET", "/users", 500, "down", 1000L)

        assertTrue(result)
        assertEquals(1, repo.cachedMap.size)
        val mock = repo.getMock("GET", "/users")
        assertEquals(CachedResponse(500, "down", 1000L), mock)
    }

    @Test
    fun `upsertMock 은 신규 생성 시 baseline response 도 mock 과 동일하게 둔다`() {
        repo.upsertMock("GET", "/users", 503, "x", 0L)

        val value = repo.cachedMap[CachedKey("GET", "/users")]
        assertEquals(CachedResponse(503, "x", 0L), value?.response)
    }

    @Test
    fun `upsertMock 은 기존 항목이면 response 는 보존하고 mock 만 갱신한다`() {
        repo.cache("GET", "/users", 200, "{}")

        repo.upsertMock("GET", "/users", 503, "down", 1000L)

        val value = repo.cachedMap[CachedKey("GET", "/users")]
        assertEquals(200, value?.response?.code)
        assertEquals(CachedResponse(503, "down", 1000L), value?.mock)
        assertEquals(1, repo.cachedMap.size)
    }

    @Test
    fun `upsertMock 은 duration 을 생략하면 0 으로 설정한다`() {
        repo.upsertMock("GET", "/users", 500, "x")

        assertEquals(0L, repo.getMock("GET", "/users")?.duration)
    }

    @Test
    fun `unMock 은 설정된 mock 을 제거하고 true 를 반환한다`() {
        repo.cache("GET", "/users", 200, "{}")
        val key = CachedKey("GET", "/users")
        repo.mock(key, CachedResponse(500, "x"))

        val result = repo.unMock(key)

        assertTrue(result)
        assertNull(repo.getMock("GET", "/users"))
    }

    @Test
    fun `unMock 은 캐시에 없는 키면 false 를 반환한다`() {
        assertFalse(repo.unMock(CachedKey("GET", "/missing")))
    }
}
