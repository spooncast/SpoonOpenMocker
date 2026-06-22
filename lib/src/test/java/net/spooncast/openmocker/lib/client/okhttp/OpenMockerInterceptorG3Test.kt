package net.spooncast.openmocker.lib.client.okhttp

import net.spooncast.openmocker.lib.data.repo.MemCacheRepoImpl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * G3 회귀 테스트 (OkHttp) — 티켓 #121.
 *
 * mock short-circuit 이 응답을 재기록(cache)해 mock 을 지우지 않음을 고정한다.
 * 시나리오: record → upsertMock → 동일 호출을 여러 번 반복했을 때
 * (1) 말단(가짜 서버)이 다시 호출되지 않고(short-circuit),
 * (2) 매 응답이 mock 값이며,
 * (3) `cachedMap[key].mock` 이 계속 유지된다.
 *
 * 의존: #115 T1(upsertMock).
 */
class OpenMockerInterceptorG3Test {

    private val repo = MemCacheRepoImpl.getInstance()

    /** 말단(가짜 서버) 호출 횟수 — short-circuit 검증용. */
    private val serverHits = AtomicInteger(0)

    private val serverCode = 200
    private val serverBody = """{"source":"server"}"""

    private val mockCode = 503
    private val mockBody = """{"source":"mock"}"""

    /** 체인 말단에서 실제 네트워크 대신 고정 응답을 돌려주며 호출 횟수를 센다. */
    private val fakeServer = Interceptor { chain ->
        serverHits.incrementAndGet()
        Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(serverCode)
            .message("OK")
            .body(serverBody.toResponseBody("application/json".toMediaType()))
            .build()
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(OpenMockerInterceptor.Builder().build())
        .addInterceptor(fakeServer)
        .build()

    @BeforeEach
    fun setUp() {
        // 싱글톤 레포 격리.
        repo.clearCache()
        serverHits.set(0)
    }

    private fun call(): Response =
        client.newCall(
            Request.Builder().url("https://api.example.com/v1/users").build()
        ).execute()

    @Test
    fun `mock 설정 후 반복 호출해도 mock 이 유지되고 서버를 다시 치지 않는다`() {
        // 1) 첫 호출: 실제 서버를 기록한다.
        call().use { assertEquals(serverCode, it.code) }
        assertEquals(1, serverHits.get(), "첫 호출은 서버를 한 번 친다")

        // 기록된 실제 CachedKey 로 mock 을 건다(path 포맷 추측 회피).
        val key = repo.cachedMap.keys.single()
        repo.upsertMock(key.method, key.path, mockCode, mockBody)

        // 2) 동일 호출을 여러 번 반복한다.
        repeat(5) {
            call().use { resp ->
                assertEquals(mockCode, resp.code, "반복 호출은 mock code 로 short-circuit 돼야 한다")
                assertEquals(mockBody, resp.body!!.string(), "반복 호출은 mock body 를 반환해야 한다")
            }
        }

        // 3) 서버는 첫 호출 이후 다시 불리지 않았다(mock short-circuit).
        assertEquals(1, serverHits.get(), "mock 적용 후에는 서버를 다시 치지 않아야 한다")

        // 4) 재기록으로 mock 이 지워지지 않고 유지된다.
        val mock = repo.cachedMap[key]?.mock
        assertNotNull(mock, "반복 호출 후에도 mock 이 유지돼야 한다")
        assertEquals(mockCode, mock!!.code)
        assertEquals(mockBody, mock.body)
    }
}
