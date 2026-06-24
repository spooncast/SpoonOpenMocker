package net.spooncast.openmocker.plugin.net

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

/**
 * OpenMocker 제어 서버 contract 를 의존성 0 으로 호출하는 통신 레이어.
 *
 * HTTP 는 Java 11 [HttpClient], JSON 은 플랫폼 번들 [Gson] 만 사용한다. 모든 호출은 예외/비2xx
 * 상태코드를 [Result] 의 실패로 흡수해, 호출부가 try/catch 없이 성공/실패만 다루게 한다.
 *
 * 운영 시 대상은 adb forward 로 노출된 기기 loopback 의 [DEFAULT_PORT]. 테스트는 ephemeral
 * 포트의 stub 서버를 주입한다.
 */
class ControlClient(
    private val host: String = "localhost",
    private val port: Int = DEFAULT_PORT,
    private val connectTimeout: Duration = Duration.ofSeconds(2),
    private val requestTimeout: Duration = Duration.ofSeconds(5),
    private val gson: Gson = Gson(),
) {
    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(connectTimeout)
        .build()

    private val baseUrl: String = "http://$host:$port"

    /** `GET /rest/recorded` — 기록된 항목 목록. */
    fun getRecorded(): Result<List<RecordedEntry>> = call {
        val res = send(newRequest("/rest/recorded").GET().build())
        ensureSuccess(res)
        val type = object : TypeToken<List<RecordedEntry>>() {}.type
        gson.fromJson<List<RecordedEntry>>(res.body(), type) ?: emptyList()
    }

    /** `POST /rest/mock` — mock create-or-update. */
    fun upsertMock(request: MockRequest): Result<Unit> = call {
        val res = send(
            newRequest("/rest/mock")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(request), StandardCharsets.UTF_8))
                .header("Content-Type", "application/json")
                .build()
        )
        ensureSuccess(res)
    }

    /** `DELETE /rest/mock?method=&path=` — 특정 항목의 mock 해제. */
    fun clearMock(method: String, path: String): Result<Unit> = call {
        val query = "method=${encode(method)}&path=${encode(path)}"
        val res = send(newRequest("/rest/mock?$query").DELETE().build())
        ensureSuccess(res)
    }

    /** `DELETE /rest/mock?all=true` — 전체 mock/기록 초기화. */
    fun clearAll(): Result<Unit> = call {
        val res = send(newRequest("/rest/mock?all=true").DELETE().build())
        ensureSuccess(res)
    }

    /** `GET /inject/injectors` — 등록된 injector 목록. */
    fun getInjectors(): Result<List<Injector>> = call {
        val res = send(newRequest("/inject/injectors").GET().build())
        ensureSuccess(res)
        val type = object : TypeToken<List<Injector>>() {}.type
        gson.fromJson<List<Injector>>(res.body(), type) ?: emptyList()
    }

    /** `GET /inject/{id}/recorded` — 해당 injector 가 수신한 프레임 목록(최신순). 미등록 injector 면 404 → 실패. */
    fun getRecorded(id: String): Result<List<RecordedMessage>> = call {
        val res = send(newRequest("/inject/${encode(id)}/recorded").GET().build())
        ensureSuccess(res)
        val type = object : TypeToken<List<RecordedMessage>>() {}.type
        gson.fromJson<List<RecordedMessage>>(res.body(), type) ?: emptyList()
    }

    /** `DELETE /inject/{id}/recorded` — 해당 injector 의 수신 이력 버퍼를 비운다. 미등록 injector 면 404 → 실패. */
    fun clearRecorded(id: String): Result<Unit> = call {
        val res = send(newRequest("/inject/${encode(id)}/recorded").DELETE().build())
        ensureSuccess(res)
    }

    /** `POST /inject/{id}` — raw payload 를 파싱 없이 그대로 전달. 미등록 injector 면 404 → 실패. */
    fun inject(id: String, payload: String): Result<Unit> = call {
        val res = send(
            newRequest("/inject/${encode(id)}")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build()
        )
        ensureSuccess(res)
    }

    private fun newRequest(pathAndQuery: String): HttpRequest.Builder =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + pathAndQuery))
            .timeout(requestTimeout)

    private fun send(request: HttpRequest): HttpResponse<String> =
        client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))

    private fun ensureSuccess(res: HttpResponse<String>) {
        if (res.statusCode() !in 200..299) {
            throw ControlClientException(res.statusCode(), res.body())
        }
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8)

    /** 블록을 실행하고 던져진 예외를 [Result.failure] 로 흡수한다. */
    private inline fun <T> call(block: () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: Exception) {
            Result.failure(e)
        }

    companion object {
        const val DEFAULT_PORT = 8099
    }
}

/** 제어 서버가 비2xx 상태를 반환했을 때의 실패 사유. */
class ControlClientException(
    val statusCode: Int,
    val responseBody: String,
) : RuntimeException("control server returned $statusCode: $responseBody")
