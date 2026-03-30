package net.spooncast.openmocker.lib.data.adapter

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import net.spooncast.openmocker.lib.model.CachedResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class KtorAdapterTest {

    private val sut = KtorAdapter()

    private fun captureRequestData(): HttpRequestData {
        var captured: HttpRequestData? = null
        val engine = MockEngine { request ->
            captured = request
            respond("", HttpStatusCode.OK, headersOf())
        }
        val client = HttpClient(engine)
        runBlocking {
            client.get("https://api.example.com/v1/test")
        }
        client.close()
        return captured!!
    }

    @Nested
    @DisplayName("createMockResponse()")
    inner class CreateMockResponseTest {

        @ParameterizedTest
        @ValueSource(ints = [200, 400, 404, 500, 503])
        @DisplayName("""
            [GIVEN: mock 응답의 상태 코드가 주어진 상태]
            [WHEN: createMockResponse 호출]
            [THEN: 예외 없이 해당 상태 코드의 HttpResponse를 반환한다]""")
        fun `returns response with matching status code without exception`(statusCode: Int) = runBlocking {
            val mockBody = """{"error": "test error message"}"""
            val cachedResponse = CachedResponse(
                code = statusCode,
                body = mockBody
            )
            val request = captureRequestData()

            val response = sut.createMockResponse(request, cachedResponse)

            assertEquals(statusCode, response.status.value)
        }

        @ParameterizedTest
        @ValueSource(ints = [200, 400, 404, 500, 503])
        @DisplayName("""
            [GIVEN: mock 응답의 body가 주어진 상태]
            [WHEN: createMockResponse 호출]
            [THEN: 반환된 응답의 body가 입력 mock body와 일치한다]""")
        fun `returns response with matching body`(statusCode: Int) = runBlocking {
            val mockBody = """{"message": "test"}"""
            val cachedResponse = CachedResponse(
                code = statusCode,
                body = mockBody
            )
            val request = captureRequestData()

            val response = sut.createMockResponse(request, cachedResponse)

            assertEquals(mockBody, response.bodyAsText())
        }
    }
}
