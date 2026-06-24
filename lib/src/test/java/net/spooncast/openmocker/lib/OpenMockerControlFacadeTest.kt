package net.spooncast.openmocker.lib

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URL

/**
 * [OpenMocker] facade 의 제어 서버 배선 통합 테스트(티켓 #119 검증 항목).
 *
 * 실제 loopback 소켓을 띄워 검증한다: `control.start` 후 `GET /rest/recorded` 가 200 + JSON
 * 배열로 응답하고, `control.stop` 후에는 같은 요청이 연결에 실패한다.
 */
@Tag("integration")
class OpenMockerControlFacadeTest {

    @AfterEach
    fun tearDown() {
        // 테스트 격리: 서버가 떠 있으면 반드시 내린다.
        OpenMocker.control.stop()
    }

    /** OS 가 비어 있는 포트를 고르게 한 뒤 즉시 닫아 그 번호를 반환한다(고정 포트 충돌 회피). */
    private fun freePort(): Int = ServerSocket(0).use { it.localPort }

    private fun get(port: Int): HttpURLConnection =
        (URL("http://127.0.0.1:$port/rest/recorded").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 2_000
            readTimeout = 2_000
        }

    @Test
    fun `control_start 후 GET rest_recorded 가 200 JSON 배열로 응답한다`() {
        val port = freePort()
        OpenMocker.control.start(port)

        val conn = get(port)
        try {
            assertEquals(200, conn.responseCode)
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            assertTrue(body.trimStart().startsWith("["), "recorded 응답은 JSON 배열이어야 한다: $body")
        } finally {
            conn.disconnect()
        }
    }

    @Test
    fun `control_stop 후에는 연결에 실패한다`() {
        val port = freePort()
        OpenMocker.control.start(port)
        // 떠 있는 동안에는 응답한다.
        get(port).also { assertEquals(200, it.responseCode) }.disconnect()

        OpenMocker.control.stop()

        // 내린 뒤에는 연결 자체가 실패한다(connection refused).
        assertThrows(IOException::class.java) {
            get(port).responseCode
        }
    }
}
