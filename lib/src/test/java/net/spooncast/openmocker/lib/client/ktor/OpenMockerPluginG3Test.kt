package net.spooncast.openmocker.lib.client.ktor

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import net.spooncast.openmocker.lib.data.repo.MemCacheRepoImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * G3 회귀 테스트 (Ktor) — 티켓 #121.
 *
 * mock short-circuit 이 응답을 재기록(cache)해 mock 을 지우지 않음을 고정한다.
 * 시나리오: record → upsertMock → 동일 호출을 여러 번 반복했을 때
 * (1) 말단(MockEngine = 가짜 서버)이 다시 호출되지 않고(short-circuit),
 * (2) 매 응답이 mock 값이며,
 * (3) `cachedMap[key].mock` 이 계속 유지된다.
 *
 * 의존: #115 T1(upsertMock).
 */
class OpenMockerPluginG3Test {

    private val repo = MemCacheRepoImpl.getInstance()

    /** 말단(가짜 서버) 호출 횟수 — short-circuit 검증용. */
    private val serverHits = AtomicInteger(0)

    private val serverCode = HttpStatusCode.OK
    private val serverBody = """{"source":"server"}"""

    // 에러 코드 mock 도 재캐싱에 지워지지 않음을 함께 고정한다(KA-B 픽스 전제: 4xx/5xx mock 무크래시).
    private val mockCode = 503
    private val mockBody = """{"source":"mock"}"""

    private val url = "https://api.example.com/v1/users"

    private val client = HttpClient(
        MockEngine { _ ->
            serverHits.incrementAndGet()
            respond(
                content = ByteReadChannel(serverBody),
                status = serverCode,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
    ) {
        install(OpenMockerPlugin)
    }

    @BeforeEach
    fun setUp() {
        // 싱글톤 레포 격리.
        repo.clearCache()
        serverHits.set(0)
    }

    private fun call(): HttpResponse = runBlocking { client.get(url) }

    private fun bodyOf(response: HttpResponse): String = runBlocking { response.bodyAsText() }

    @Test
    fun `mock 설정 후 반복 호출해도 mock 이 유지되고 서버를 다시 치지 않는다`() {
        // 1) 첫 호출: 실제 서버를 기록한다.
        assertEquals(serverCode, call().status, "첫 호출은 실제 서버 응답을 받는다")
        assertEquals(1, serverHits.get(), "첫 호출은 서버를 한 번 친다")

        // 기록된 실제 CachedKey 로 mock 을 건다(path 포맷 추측 회피).
        val key = repo.cachedMap.keys.single()
        repo.upsertMock(key.method, key.path, mockCode, mockBody)

        // 2) 동일 호출을 여러 번 반복한다.
        repeat(5) {
            val resp = call()
            assertEquals(mockCode, resp.status.value, "반복 호출은 mock code 로 short-circuit 돼야 한다")
            assertEquals(mockBody, bodyOf(resp), "반복 호출은 mock body 를 반환해야 한다")
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
