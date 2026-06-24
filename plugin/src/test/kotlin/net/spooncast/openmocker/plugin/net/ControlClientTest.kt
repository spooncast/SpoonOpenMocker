package net.spooncast.openmocker.plugin.net

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * [ControlClient] 의 5 종 라우트 round-trip 과 에러 경로를, JDK 내장 [HttpServer] stub 상대로 검증.
 * 외부 의존성 없이 ephemeral 포트에 stub 을 띄워 contract 와이어 형태를 그대로 주고받는다.
 */
class ControlClientTest {

    private lateinit var server: HttpServer
    private lateinit var client: ControlClient

    /** 마지막으로 stub 이 수신한 요청. 라우트별 핸들러가 갱신한다. */
    private data class CapturedRequest(
        val method: String,
        val path: String,
        val rawQuery: String?,
        val body: String,
    )

    private val captured = HashMap<String, CapturedRequest>()

    @Before
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("localhost", 0), 0)
        server.start()
        client = ControlClient(host = "localhost", port = server.address.port)
    }

    @After
    fun tearDown() {
        server.stop(0)
    }

    /** 지정 경로에 (status, body) 를 응답하고 수신 요청을 [captured] 에 기록하는 핸들러를 등록한다. */
    private fun stub(path: String, status: Int, responseBody: String) {
        server.createContext(path, RecordingHandler(path, status, responseBody))
    }

    private inner class RecordingHandler(
        private val key: String,
        private val status: Int,
        private val responseBody: String,
    ) : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            val body = exchange.requestBody.readBytes().toString(StandardCharsets.UTF_8)
            captured[key] = CapturedRequest(
                method = exchange.requestMethod,
                path = exchange.requestURI.path,
                rawQuery = exchange.requestURI.rawQuery,
                body = body,
            )
            val bytes = responseBody.toByteArray(StandardCharsets.UTF_8)
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
    }

    private fun decode(value: String): String = URLDecoder.decode(value, StandardCharsets.UTF_8)

    @Test
    fun `getRecorded parses entries with and without mock`() {
        val json = """
            [
              {"method":"GET","path":"/weather","response":{"code":200,"body":"sunny"},
               "mock":{"code":500,"body":"err","duration":1000}},
              {"method":"POST","path":"/login","response":{"code":201,"body":"ok"}}
            ]
        """.trimIndent()
        stub("/rest/recorded", 200, json)

        val result = client.getRecorded()

        assertTrue(result.isSuccess)
        val entries = result.getOrThrow()
        assertEquals(2, entries.size)
        assertEquals("GET", captured["/rest/recorded"]?.method)
        // 첫 항목: mock 동반
        val first = entries[0]
        assertEquals("/weather", first.path)
        assertEquals(200, first.response.code)
        assertEquals("sunny", first.response.body)
        assertNotNull(first.mock)
        assertEquals(500, first.mock?.code)
        assertEquals(1000L, first.mock?.duration)
        // 둘째 항목: mock 부재 → null
        assertNull(entries[1].mock)
        assertEquals("/login", entries[1].path)
    }

    @Test
    fun `upsertMock sends MockRequest as json body`() {
        stub("/rest/mock", 200, """{"ok":true}""")

        val result = client.upsertMock(
            MockRequest(method = "GET", path = "/weather", code = 503, body = "down", duration = 250L)
        )

        assertTrue(result.isSuccess)
        val req = captured["/rest/mock"]!!
        assertEquals("POST", req.method)
        // 와이어 바디는 contract 필드명을 그대로 가진 JSON
        val sent = com.google.gson.Gson().fromJson(req.body, MockRequest::class.java)
        assertEquals("GET", sent.method)
        assertEquals("/weather", sent.path)
        assertEquals(503, sent.code)
        assertEquals("down", sent.body)
        assertEquals(250L, sent.duration)
    }

    @Test
    fun `clearMock sends DELETE with url-encoded method and path query`() {
        stub("/rest/mock", 200, """{"ok":true}""")

        val result = client.clearMock(method = "GET", path = "/a b/weather")

        assertTrue(result.isSuccess)
        val req = captured["/rest/mock"]!!
        assertEquals("DELETE", req.method)
        val query = req.rawQuery!!.split("&").associate {
            val (k, v) = it.split("=", limit = 2)
            decode(k) to decode(v)
        }
        assertEquals("GET", query["method"])
        assertEquals("/a b/weather", query["path"])
    }

    @Test
    fun `clearAll sends DELETE with all=true`() {
        stub("/rest/mock", 200, """{"ok":true}""")

        val result = client.clearAll()

        assertTrue(result.isSuccess)
        val req = captured["/rest/mock"]!!
        assertEquals("DELETE", req.method)
        assertEquals("all=true", req.rawQuery)
    }

    @Test
    fun `getSinks parses sinks with presets`() {
        val json = """
            [{"id":"demo","name":"Demo Sink","presets":[
               {"name":"p1","payload":"{\"a\":1}"},
               {"name":"p2","payload":"raw"}]}]
        """.trimIndent()
        stub("/inject/sinks", 200, json)

        val result = client.getSinks()

        assertTrue(result.isSuccess)
        val sinks = result.getOrThrow()
        assertEquals(1, sinks.size)
        assertEquals("demo", sinks[0].id)
        assertEquals("Demo Sink", sinks[0].name)
        assertEquals(2, sinks[0].presets.size)
        assertEquals("p1", sinks[0].presets[0].name)
        assertEquals("""{"a":1}""", sinks[0].presets[0].payload)
        assertEquals("GET", captured["/inject/sinks"]?.method)
    }

    @Test
    fun `getReceived parses received frames newest-first`() {
        val json = """
            [{"seq":3,"payload":"{\"event\":\"chat\"}"},
             {"seq":2,"payload":"{\"event\":\"tick\"}"},
             {"seq":1,"payload":"raw"}]
        """.trimIndent()
        // /inject/sinks 와 분리되도록 더 긴 prefix 컨텍스트로 등록(HttpServer 최장 prefix 매칭).
        stub("/inject/demo/received", 200, json)

        val result = client.getReceived("demo")

        assertTrue(result.isSuccess)
        val received = result.getOrThrow()
        assertEquals(3, received.size)
        assertEquals(3L, received[0].seq)
        assertEquals("""{"event":"chat"}""", received[0].payload)
        assertEquals(1L, received[2].seq)
        val req = captured["/inject/demo/received"]!!
        assertEquals("GET", req.method)
        assertEquals("/inject/demo/received", req.path)
    }

    @Test
    fun `getReceived returns failure for unknown sink`() {
        stub("/inject/ghost/received", 404, """{"ok":false,"error":"unknown sink: ghost"}""")

        val result = client.getReceived("ghost")

        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue(ex is ControlClientException)
        assertEquals(404, (ex as ControlClientException).statusCode)
    }

    @Test
    fun `inject posts raw payload unchanged to sink id`() {
        // /inject/sinks 보다 짧은 prefix 컨텍스트 — HttpServer 는 최장 prefix 매칭이므로 분리됨
        stub("/inject/", 200, """{"ok":true}""")

        val raw = """{"temp":-5,"unit":"C"}"""
        val result = client.inject(id = "demo", payload = raw)

        assertTrue(result.isSuccess)
        val req = captured["/inject/"]!!
        assertEquals("POST", req.method)
        assertEquals("/inject/demo", req.path)
        // body 는 파싱·재직렬화 없이 그대로 전달되어야 한다
        assertEquals(raw, req.body)
    }

    @Test
    fun `non-2xx status becomes failure with ControlClientException`() {
        stub("/inject/", 404, """{"ok":false,"error":"unknown sink: ghost"}""")

        val result = client.inject(id = "ghost", payload = "{}")

        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue(ex is ControlClientException)
        assertEquals(404, (ex as ControlClientException).statusCode)
        assertTrue(ex.responseBody.contains("unknown sink"))
    }

    @Test
    fun `connection failure becomes failure result`() {
        // 서버가 떠 있지 않은 포트로 향하는 클라이언트
        val deadClient = ControlClient(host = "localhost", port = findFreePort())

        val result = deadClient.getRecorded()

        assertTrue(result.isFailure)
        assertFalse(result.exceptionOrNull() is ControlClientException) // 연결 자체 실패(IOException 계열)
    }

    /** 사용 직후 닫아 비어 있는(연결 거부될) 포트를 얻는다. */
    private fun findFreePort(): Int {
        val tmp = HttpServer.create(InetSocketAddress("localhost", 0), 0)
        val port = tmp.address.port
        tmp.stop(0)
        return port
    }
}
