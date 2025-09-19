package net.spooncast.openmocker.lib.core

import net.spooncast.openmocker.lib.model.CachedKey
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Assertions.*

@DisplayName("HttpRequestData 테스트")
class HttpRequestDataTest {

    @Nested
    @DisplayName("주어진 조건: 메소드와 경로를 포함한 HttpRequestData")
    inner class ToCachedKeyTests {

        @Test
        @DisplayName("""
        [주어진 조건: 메소드, 경로, URL을 포함한 HttpRequestData]
        [실행: CachedKey로 변환]
        [예상 결과: 메소드와 경로가 올바른 CachedKey 생성]
        """)
        fun `should create correct CachedKey from request data`() {
            // Given
            val requestData = HttpRequestData(
                method = "GET",
                path = "/api/v1/users",
                url = "https://api.example.com/api/v1/users"
            )

            // When
            val cachedKey = requestData.toCachedKey()

            // Then
            assertEquals("GET", cachedKey.method)
            assertEquals("/api/v1/users", cachedKey.path)
        }
    }

    @Nested
    @DisplayName("주어진 조건: 헤더를 포함한 HttpRequestData")
    inner class HeaderAccessTests {

        @Test
        @DisplayName("""
        [주어진 조건: 다중 헤더를 포함한 HttpRequestData]
        [실행: 이름으로 헤더 조회]
        [예상 결과: 대소문자 구분 없이 첫 번째 값 반환]
        """)
        fun `should return first header value case insensitive`() {
            // Given
            val headers = mapOf(
                "Content-Type" to listOf("application/json", "charset=utf-8"),
                "Authorization" to listOf("Bearer token123")
            )
            val requestData = HttpRequestData(
                method = "POST",
                path = "/api/v1/data",
                url = "https://api.example.com/api/v1/data",
                headers = headers
            )

            // When & Then
            assertEquals("application/json", requestData.getHeader("Content-Type"))
            assertEquals("application/json", requestData.getHeader("content-type"))
            assertEquals("Bearer token123", requestData.getHeader("AUTHORIZATION"))
            assertNull(requestData.getHeader("Non-Existent"))
        }

        @Test
        @DisplayName("""
        [주어진 조건: 동일한 이름의 다중 헤더를 포함한 HttpRequestData]
        [실행: 이름으로 헤더들 조회]
        [예상 결과: 대소문자 구분 없이 모든 값 반환]
        """)
        fun `should return all header values case insensitive`() {
            // Given
            val headers = mapOf(
                "Accept" to listOf("application/json", "text/plain"),
                "Custom-Header" to listOf("value1")
            )
            val requestData = HttpRequestData(
                method = "GET",
                path = "/api/test",
                url = "https://api.example.com/api/test",
                headers = headers
            )

            // When & Then
            val acceptHeaders = requestData.getHeaders("accept")
            assertEquals(2, acceptHeaders.size)
            assertTrue(acceptHeaders.contains("application/json"))
            assertTrue(acceptHeaders.contains("text/plain"))

            val customHeaders = requestData.getHeaders("CUSTOM-HEADER")
            assertEquals(1, customHeaders.size)
            assertEquals("value1", customHeaders[0])

            val nonExistentHeaders = requestData.getHeaders("Non-Existent")
            assertTrue(nonExistentHeaders.isEmpty())
        }

        @Test
        @DisplayName("""
        [주어진 조건: 빈 헤더 맵을 가진 HttpRequestData]
        [실행: 이름으로 헤더 조회]
        [예상 결과: 안전하게 처리하고 null/빈 값 반환]
        """)
        fun `should handle empty headers map gracefully`() {
            // Given
            val requestData = HttpRequestData(
                method = "GET",
                path = "/api/test",
                url = "https://api.example.com/api/test",
                headers = emptyMap()
            )

            // When & Then
            assertNull(requestData.getHeader("Any-Header"))
            assertTrue(requestData.getHeaders("Any-Header").isEmpty())
        }
    }
}