package net.spooncast.openmocker.lib.control

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * [readHttpRequest] 순수 파서 단위 테스트.
 *
 * 핵심 검증: head 는 CRLF 로, body 는 `Content-Length` 바이트만큼 정확히 읽고, 멀티바이트 UTF-8
 * body 가 잘리지 않는다(BufferedReader 삼킴 버그 회피).
 */
class ControlServerTest {

    /** head(ISO-8859-1) + body(UTF-8) 를 raw 바이트로 이어붙여 InputStream 으로 만든다. */
    private fun streamOf(head: String, body: String = ""): InputStream {
        val bytes = head.toByteArray(Charsets.ISO_8859_1) + body.toByteArray(Charsets.UTF_8)
        return ByteArrayInputStream(bytes)
    }

    @Test
    fun `GET 요청 - 헤더만 있고 body 가 없다`() {
        val request = readHttpRequest(
            streamOf("GET /rest/recorded HTTP/1.1\r\nHost: localhost\r\n\r\n")
        )

        assertEquals("GET", request.method)
        assertEquals("/rest/recorded", request.target)
        assertEquals("localhost", request.headers["host"])
        assertEquals("", request.body)
    }

    @Test
    fun `헤더 이름은 소문자로 정규화된다`() {
        val request = readHttpRequest(
            streamOf("GET /inject/sinks HTTP/1.1\r\nContent-Type: application/json\r\n\r\n")
        )

        assertEquals("application/json", request.headers["content-type"])
    }

    @Test
    fun `POST 요청 - Content-Length 만큼 body 를 정확히 읽는다`() {
        val body = """{"method":"GET","path":"/a","code":200,"body":"ok"}"""
        val head = "POST /rest/mock HTTP/1.1\r\nContent-Length: ${body.toByteArray(Charsets.UTF_8).size}\r\n\r\n"

        val request = readHttpRequest(streamOf(head, body))

        assertEquals("POST", request.method)
        assertEquals("/rest/mock", request.target)
        assertEquals(body, request.body)
    }

    @Test
    fun `멀티바이트 UTF-8 body 가 잘리지 않는다 - Content-Length 는 바이트 수`() {
        val body = """{"payload":"한글 페이로드 😀"}"""
        val byteLength = body.toByteArray(Charsets.UTF_8).size
        // 바이트 길이와 문자 길이가 다름을 전제로 한다(파서가 바이트로 읽어야 함).
        assertTrue(byteLength > body.length)

        val head = "POST /inject/wala HTTP/1.1\r\nContent-Length: $byteLength\r\n\r\n"
        val request = readHttpRequest(streamOf(head, body))

        assertEquals(body, request.body)
    }

    @Test
    fun `Content-Length 를 넘는 후속 바이트는 body 에 포함되지 않는다`() {
        val body = "12345"
        val head = "POST /inject/x HTTP/1.1\r\nContent-Length: 5\r\n\r\n"
        // body 뒤에 잉여 바이트를 붙여도 5바이트만 읽어야 한다.
        val request = readHttpRequest(streamOf(head, body + "TRAILING_GARBAGE"))

        assertEquals("12345", request.body)
    }

    @Test
    fun `target 의 path 와 query 가 그대로 보존된다`() {
        val request = readHttpRequest(
            streamOf("DELETE /rest/mock?method=GET&path=%2Fa HTTP/1.1\r\n\r\n")
        )

        assertEquals("DELETE", request.method)
        assertEquals("/rest/mock?method=GET&path=%2Fa", request.target)
    }
}
