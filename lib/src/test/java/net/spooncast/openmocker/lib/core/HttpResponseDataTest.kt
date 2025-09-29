package net.spooncast.openmocker.lib.core

import net.spooncast.openmocker.lib.model.HttpResponseData
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Assertions.*

@DisplayName("HttpResponseData 테스트")
class HttpResponseDataTest {

    @Nested
    @DisplayName("주어진 조건: CachedResponse로 변환하기 위한 HttpResponseData")
    inner class ToCachedResponseTests {

        @Test
        @DisplayName("""
        [주어진 조건: 코드와 바디를 포함한 HttpResponseData]
        [실행: 지속 시간과 함께 CachedResponse로 변환]
        [예상 결과: 올바른 속성을 가진 CachedResponse 생성]
        """)
        fun `should create correct CachedResponse with duration`() {
            // Given
            val responseData = HttpResponseData(
                code = 200,
                body = """{"message": "success"}"""
            )
            val duration = 500L

            // When
            val cachedResponse = responseData.toCachedResponse(duration)

            // Then
            assertEquals(200, cachedResponse.code)
            assertEquals("""{"message": "success"}""", cachedResponse.body)
            assertEquals(500L, cachedResponse.duration)
        }

        @Test
        @DisplayName("""
        [주어진 조건: 코드와 바디를 포함한 HttpResponseData]
        [실행: 기본 지속 시간으로 CachedResponse로 변환]
        [예상 결과: 0 지속 시간 사용]
        """)
        fun `should use zero duration when not specified`() {
            // Given
            val responseData = HttpResponseData(
                code = 404,
                body = """{"error": "not found"}"""
            )

            // When
            val cachedResponse = responseData.toCachedResponse()

            // Then
            assertEquals(404, cachedResponse.code)
            assertEquals("""{"error": "not found"}""", cachedResponse.body)
            assertEquals(0L, cachedResponse.duration)
        }
    }

    @Nested
    @DisplayName("주어진 조건: 다양한 상태 코드를 가진 HttpResponseData")
    inner class SuccessfulStatusTests {

        @Test
        @DisplayName("""
        [주어진 조건: 2xx 상태 코드를 가진 HttpResponseData]
        [실행: isSuccessful 속성 확인]
        [예상 결과: 2xx 코드에 대해 true, 다른 코드에 대해 false 반환]
        """)
        fun `should return correct status for different codes`() {
            // Given & When & Then
            assertTrue(HttpResponseData(200, "").isSuccessful)
            assertTrue(HttpResponseData(201, "").isSuccessful)
            assertTrue(HttpResponseData(204, "").isSuccessful)
            assertTrue(HttpResponseData(299, "").isSuccessful)

            assertFalse(HttpResponseData(199, "").isSuccessful)
            assertFalse(HttpResponseData(300, "").isSuccessful)
            assertFalse(HttpResponseData(404, "").isSuccessful)
            assertFalse(HttpResponseData(500, "").isSuccessful)
        }
    }

    @Nested
    @DisplayName("주어진 조건: 헤더를 포함한 HttpResponseData")
    inner class HeaderAccessTests {

        @Test
        @DisplayName("""
        [주어진 조건: 다중 헤더를 포함한 HttpResponseData]
        [실행: 이름으로 헤더 조회]
        [예상 결과: 대소문자 구분 없이 첫 번째 값 반환]
        """)
        fun `should return first header value case insensitive`() {
            // Given
            val headers = mapOf(
                "Content-Type" to listOf("application/json", "charset=utf-8"),
                "Server" to listOf("nginx/1.18")
            )
            val responseData = HttpResponseData(
                code = 200,
                body = "{}",
                headers = headers
            )

            // When & Then
            assertEquals("application/json", responseData.getHeader("Content-Type"))
            assertEquals("application/json", responseData.getHeader("content-type"))
            assertEquals("nginx/1.18", responseData.getHeader("SERVER"))
            assertNull(responseData.getHeader("Non-Existent"))
        }

        @Test
        @DisplayName("""
        [주어진 조건: Content-Type 헤더를 포함한 HttpResponseData]
        [실행: 콘텐츠 타입 조회]
        [예상 결과: 콘텐츠 타입 헤더 값 반환]
        """)
        fun `should return content type header`() {
            // Given
            val headers = mapOf(
                "Content-Type" to listOf("application/json; charset=utf-8")
            )
            val responseData = HttpResponseData(
                code = 200,
                body = "{}",
                headers = headers
            )

            // When & Then
            assertEquals("application/json; charset=utf-8", responseData.getContentType())
        }
    }

    @Nested
    @DisplayName("주어진 조건: 다양한 콘텐츠 타입을 가진 HttpResponseData")
    inner class JsonDetectionTests {

        @Test
        @DisplayName("""
        [주어진 조건: 다양한 콘텐츠 타입을 가진 HttpResponseData]
        [실행: 응답이 JSON인지 확인]
        [예상 결과: JSON 콘텐츠 타입을 올바르게 감지]
        """)
        fun `should detect JSON content type correctly`() {
            // Given
            val jsonResponse = HttpResponseData(
                code = 200,
                body = "{}",
                headers = mapOf("Content-Type" to listOf("application/json"))
            )

            val jsonWithCharsetResponse = HttpResponseData(
                code = 200,
                body = "{}",
                headers = mapOf("Content-Type" to listOf("application/json; charset=utf-8"))
            )

            val textResponse = HttpResponseData(
                code = 200,
                body = "Hello",
                headers = mapOf("Content-Type" to listOf("text/plain"))
            )

            val noContentTypeResponse = HttpResponseData(
                code = 200,
                body = "{}"
            )

            // When & Then
            assertTrue(jsonResponse.isJsonResponse())
            assertTrue(jsonWithCharsetResponse.isJsonResponse())
            assertFalse(textResponse.isJsonResponse())
            assertFalse(noContentTypeResponse.isJsonResponse())
        }
    }

    @Nested
    @DisplayName("주어진 조건: HttpResponseData 생성자 매개변수")
    inner class ConstructorTests {

        @Test
        @DisplayName("""
        [주어진 조건: 상태 코드를 가진 HttpResponseData 생성자]
        [실행: 다른 코드로 인스턴스 생성]
        [예상 결과: 코드에 기반하여 isSuccessful 설정]
        """)
        fun `should set isSuccessful based on code`() {
            // Given & When
            val successResponse = HttpResponseData(200, "success")
            val notFoundResponse = HttpResponseData(404, "not found")
            val serverErrorResponse = HttpResponseData(500, "server error")

            // Then
            assertTrue(successResponse.isSuccessful)
            assertFalse(notFoundResponse.isSuccessful)
            assertFalse(serverErrorResponse.isSuccessful)
        }

        @Test
        @DisplayName("""
        [주어진 조건: isSuccessful 오버라이드를 가진 HttpResponseData 생성자]
        [실행: 명시적 isSuccessful 값으로 인스턴스 생성]
        [예상 결과: 코드 기반 기본값 대신 제공된 값 사용]
        """)
        fun `should allow overriding isSuccessful`() {
            // Given & When
            val response = HttpResponseData(
                code = 404,
                body = "not found",
                isSuccessful = true // Override default behavior
            )

            // Then
            assertEquals(404, response.code)
            assertTrue(response.isSuccessful) // Should use provided value
        }
    }
}