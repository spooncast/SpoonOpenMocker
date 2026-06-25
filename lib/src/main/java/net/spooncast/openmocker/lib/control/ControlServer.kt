package net.spooncast.openmocker.lib.control

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.spooncast.openmocker.lib.control.dto.MockRequestDto
import net.spooncast.openmocker.lib.control.dto.OkDto
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URLDecoder

/**
 * 임베디드 raw-socket HTTP/1.1 제어 서버. 외부 의존(임베디드 서버 라이브러리) 없이 `ServerSocket`
 * accept 루프만으로 제어 contract 를 수신한다. HTTP/소켓/직렬화는 이 클래스가 전담하고, 도메인
 * 동작은 [ControlService] 에 위임한다.
 *
 * - loopback 전용 bind (데스크톱은 `adb forward` 로 접속) → 외부 노출 없음.
 * - bind 실패는 throw 하지 않고 [Log] 로 남긴 뒤 noop — 디버그용 서버 실패가 앱 기동을 깨면 안 된다.
 * - 커넥션당 요청 1건, 응답 후 `Connection: close`.
 */
internal class ControlServer(
    private val service: ControlService,
) {

    private val lock = Any()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private var serverSocket: ServerSocket? = null
    private var scope: CoroutineScope? = null

    @Volatile
    private var running = false

    /**
     * 제어 서버를 시작한다. 이미 실행 중이면 멱등하게 무시한다.
     * bind 실패 시 예외를 던지지 않고 로깅 후 비활성 상태로 둔다.
     */
    fun start(port: Int = DEFAULT_PORT) {
        synchronized(lock) {
            if (running) return

            val server = try {
                ServerSocket().apply {
                    bind(InetSocketAddress(InetAddress.getLoopbackAddress(), port))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to bind control server on port $port; server disabled", e)
                return
            }

            val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            serverSocket = server
            scope = serverScope
            running = true

            serverScope.launch {
                while (isActive) {
                    val socket = try {
                        server.accept()
                    } catch (e: Exception) {
                        // stop() 이 소켓을 닫으면 accept() 가 SocketException → 루프 정상 종료.
                        break
                    }
                    launch { handle(socket) }
                }
            }
        }
    }

    /** 제어 서버를 멈춘다. 실행 중이 아니면 멱등하게 무시한다. */
    fun stop() {
        synchronized(lock) {
            if (!running) return
            running = false
            try {
                serverSocket?.close()
            } catch (_: Exception) {
                // 닫는 중 발생한 예외는 무시한다.
            }
            serverSocket = null
            scope?.cancel()
            scope = null
        }
    }

    private fun handle(socket: Socket) {
        socket.use { s ->
            // 소켓 read 타임아웃 — 아무것도(또는 일부만) 보내지 않는 half-open 커넥션이 readHttpRequest 의
            // 블로킹 read 에서 IO 스레드를 무한 점유하는 것을 막는다. 타임아웃 시 read 가
            // SocketTimeoutException 을 던지고, 아래 catch 가 응답/종료로 스레드를 반환한다.
            // 루프백+adb 경유라 정상 요청은 즉시 도착하므로 짧은 타임아웃으로 충분하다.
            s.soTimeout = SOCKET_READ_TIMEOUT_MS
            val output = s.getOutputStream()
            try {
                val request = readHttpRequest(s.getInputStream())
                val (status, body) = route(request)
                writeResponse(output, status, body)
            } catch (e: SocketTimeoutException) {
                // half-open/지연 커넥션 — 응답을 쓸 대상이 없으므로 조용히 닫는다(use 가 닫음).
            } catch (e: Exception) {
                Log.w(TAG, "Failed to handle control request", e)
                try {
                    writeResponse(output, 500, errorBody(e.message ?: "internal error"))
                } catch (_: Exception) {
                    // 응답 쓰기 실패는 무시한다(커넥션은 use 로 닫힘).
                }
            }
        }
    }

    /** 요청라인 + path/query 를 보고 [ControlService] 로 분기한다. 결과는 (status, json body). */
    private fun route(request: HttpRequest): Pair<Int, String> {
        val questionIdx = request.target.indexOf('?')
        val path = if (questionIdx >= 0) request.target.substring(0, questionIdx) else request.target
        val query = if (questionIdx >= 0) parseQuery(request.target.substring(questionIdx + 1)) else emptyMap()

        return when {
            request.method == "GET" && path == "/rest/recorded" ->
                200 to json.encodeToString(service.recorded())

            request.method == "POST" && path == "/rest/mock" -> {
                val dto = try {
                    json.decodeFromString<MockRequestDto>(request.body)
                } catch (e: Exception) {
                    return 400 to errorBody("invalid body: ${e.message}")
                }
                service.upsertMock(dto)
                200 to json.encodeToString(OkDto())
            }

            request.method == "DELETE" && path == "/rest/mock" -> {
                if (query["all"] == "true") {
                    service.clearAll()
                } else {
                    service.unMock(query["method"].orEmpty(), query["path"].orEmpty())
                }
                200 to json.encodeToString(OkDto())
            }

            request.method == "GET" && path == "/inject/injectors" ->
                200 to json.encodeToString(service.injectors())

            request.method == "GET" && path.startsWith("/inject/") && path.endsWith("/recorded") -> {
                val id = path.removePrefix("/inject/").removeSuffix("/recorded")
                val recorded = service.recorded(id)
                if (recorded != null) {
                    200 to json.encodeToString(recorded)
                } else {
                    404 to errorBody("unknown injector: $id")
                }
            }

            request.method == "DELETE" && path.startsWith("/inject/") && path.endsWith("/recorded") -> {
                val id = path.removePrefix("/inject/").removeSuffix("/recorded")
                if (service.clearRecorded(id)) {
                    200 to json.encodeToString(OkDto())
                } else {
                    404 to errorBody("unknown injector: $id")
                }
            }

            request.method == "POST" && path.startsWith("/inject/") -> {
                val id = path.removePrefix("/inject/")
                // body 는 파싱 없이 통째 전달한다.
                if (service.inject(id, request.body)) {
                    200 to json.encodeToString(OkDto())
                } else {
                    404 to errorBody("unknown injector: $id")
                }
            }

            else -> 404 to errorBody("not found: ${request.method} $path")
        }
    }

    private fun writeResponse(output: OutputStream, status: Int, body: String) {
        val bodyBytes = body.toByteArray(Charsets.UTF_8)
        val head = buildString {
            append("HTTP/1.1 ").append(status).append(' ').append(reasonPhrase(status)).append("\r\n")
            append("Content-Type: application/json; charset=utf-8\r\n")
            append("Content-Length: ").append(bodyBytes.size).append("\r\n")
            append("Connection: close\r\n")
            append("\r\n")
        }
        output.write(head.toByteArray(Charsets.ISO_8859_1))
        output.write(bodyBytes)
        output.flush()
    }

    private fun errorBody(message: String): String =
        json.encodeToString(buildJsonObject {
            put("ok", false)
            put("error", message)
        })

    private fun parseQuery(raw: String): Map<String, String> {
        if (raw.isBlank()) return emptyMap()
        val result = HashMap<String, String>()
        for (pair in raw.split("&")) {
            if (pair.isEmpty()) continue
            val idx = pair.indexOf('=')
            val key = if (idx < 0) pair else pair.substring(0, idx)
            val value = if (idx < 0) "" else pair.substring(idx + 1)
            result[urlDecode(key)] = urlDecode(value)
        }
        return result
    }

    private fun urlDecode(value: String): String =
        try {
            URLDecoder.decode(value, "UTF-8")
        } catch (e: Exception) {
            value
        }

    private fun reasonPhrase(status: Int): String = when (status) {
        200 -> "OK"
        400 -> "Bad Request"
        404 -> "Not Found"
        else -> "Internal Server Error"
    }

    companion object {
        private const val TAG = "OpenMockerControlServer"
        const val DEFAULT_PORT = 8099

        // 커넥션당 read 타임아웃(ms). 루프백 경유 제어 요청은 즉시 도착하므로 넉넉하면서도 유한한 값.
        private const val SOCKET_READ_TIMEOUT_MS = 10_000
    }
}

/** [readHttpRequest] 가 파싱한 요청. [headers] 키는 소문자로 정규화된다. */
internal data class HttpRequest(
    val method: String,
    val target: String,
    val headers: Map<String, String>,
    val body: String,
)

/**
 * raw [InputStream] 에서 HTTP/1.1 요청 1건을 읽어 [HttpRequest] 로 파싱한다(순수 함수, 단위 테스트 대상).
 *
 * head(요청라인 + 헤더)는 CRLF 단위로 직접 읽고, body 는 `Content-Length` 바이트만큼 정확히 읽어
 * UTF-8 로 디코드한다. `BufferedReader` 를 쓰면 내부 버퍼가 body 바이트까지 미리 삼켜버리는 고전적인
 * 버그가 생기므로, 바이트 단위로 직접 읽는다.
 */
internal fun readHttpRequest(input: InputStream): HttpRequest {
    // head 끝(\r\n\r\n) 까지 바이트 단위로 읽는다.
    val headBytes = ByteArrayOutputStream()
    var matched = 0 // \r\n\r\n 중 현재까지 매칭된 바이트 수
    while (true) {
        val b = input.read()
        if (b == -1) break
        headBytes.write(b)
        matched = when {
            b == CR && (matched == 0 || matched == 2) -> matched + 1
            b == LF && matched == 1 -> 2
            b == LF && matched == 3 -> 4
            else -> 0
        }
        if (matched == 4) break
    }

    // head 는 ASCII/ISO-8859-1 범위 → 라인 분해. (body 만 UTF-8)
    val head = headBytes.toByteArray().toString(Charsets.ISO_8859_1)
    val lines = head.split("\r\n")

    val requestLine = lines.firstOrNull()?.trim().orEmpty()
    val requestParts = requestLine.split(" ")
    val method = requestParts.getOrNull(0).orEmpty()
    val target = requestParts.getOrNull(1).orEmpty()

    val headers = HashMap<String, String>()
    for (i in 1 until lines.size) {
        val line = lines[i]
        if (line.isEmpty()) continue
        val colon = line.indexOf(':')
        if (colon <= 0) continue
        val name = line.substring(0, colon).trim().lowercase()
        val value = line.substring(colon + 1).trim()
        headers[name] = value
    }

    val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
    val body = if (contentLength > 0) {
        val buffer = ByteArray(contentLength)
        var read = 0
        while (read < contentLength) {
            val n = input.read(buffer, read, contentLength - read)
            if (n == -1) break
            read += n
        }
        String(buffer, 0, read, Charsets.UTF_8)
    } else {
        ""
    }

    return HttpRequest(method, target, headers, body)
}

private const val CR = '\r'.code
private const val LF = '\n'.code
