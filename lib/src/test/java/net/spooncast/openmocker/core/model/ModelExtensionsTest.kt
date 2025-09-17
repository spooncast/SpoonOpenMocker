package net.spooncast.openmocker.core.model

import net.spooncast.openmocker.lib.model.CachedKey
import net.spooncast.openmocker.lib.model.CachedResponse
import org.junit.Test
import org.junit.Assert.*

/**
 * ModelExtensions [모델 간 변환 확장 함수들] - BDD 스타일 종합 테스트
 * 100% 코드 커버리지와 양방향 변환 무손실성 검증을 포함한 포괄적 테스트 스위트
 */
class ModelExtensionsTest {

    // ================================
    // CachedKey ↔ MockKey 변환 테스트
    // ================================

    @Test
    fun `GIVEN valid CachedKey WHEN converting to MockKey THEN returns correct MockKey`() {
        // Given
        val cachedKey = CachedKey("GET", "/api/users")

        // When
        val mockKey = cachedKey.toMockKey()

        // Then
        assertEquals("GET", mockKey.method)
        assertEquals("/api/users", mockKey.path)
    }

    @Test
    fun `GIVEN valid MockKey WHEN converting to CachedKey THEN returns correct CachedKey`() {
        // Given
        val mockKey = MockKey("POST", "/api/posts")

        // When
        val cachedKey = mockKey.toCachedKey()

        // Then
        assertEquals("POST", cachedKey.method)
        assertEquals("/api/posts", cachedKey.path)
    }

    @Test
    fun `GIVEN CachedKeys with various HTTP methods WHEN converting to MockKey THEN all methods convert correctly`() {
        // Given
        val httpMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS")

        // When & Then
        httpMethods.forEach { method ->
            val cachedKey = CachedKey(method, "/api/test")
            val mockKey = cachedKey.toMockKey()

            assertEquals(method, mockKey.method)
            assertEquals("/api/test", mockKey.path)
        }
    }

    @Test
    fun `GIVEN MockKeys with various HTTP methods WHEN converting to CachedKey THEN all methods convert correctly`() {
        // Given
        val httpMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS")

        // When & Then
        httpMethods.forEach { method ->
            val mockKey = MockKey(method, "/api/test")
            val cachedKey = mockKey.toCachedKey()

            assertEquals(method, cachedKey.method)
            assertEquals("/api/test", cachedKey.path)
        }
    }

    @Test
    fun `GIVEN CachedKey with complex path WHEN converting to MockKey THEN path is preserved exactly`() {
        // Given
        val complexPaths = listOf(
            "/api/v1/users/123",
            "/search?q=test&sort=date",
            "/api/users?page=1&limit=10",
            "/api/users/{id}/posts/{postId}"
        )

        // When & Then
        complexPaths.forEach { path ->
            val cachedKey = CachedKey("GET", path)
            val mockKey = cachedKey.toMockKey()

            assertEquals("GET", mockKey.method)
            assertEquals(path, mockKey.path)
        }
    }

    @Test
    fun `GIVEN MockKey with complex path WHEN converting to CachedKey THEN path is preserved exactly`() {
        // Given
        val complexPaths = listOf(
            "/api/v1/users/123",
            "/search?q=test&sort=date",
            "/api/users?page=1&limit=10",
            "/api/users/{id}/posts/{postId}"
        )

        // When & Then
        complexPaths.forEach { path ->
            val mockKey = MockKey("GET", path)
            val cachedKey = mockKey.toCachedKey()

            assertEquals("GET", cachedKey.method)
            assertEquals(path, cachedKey.path)
        }
    }

    // ================================
    // CachedResponse ↔ MockResponse 변환 테스트
    // ================================

    @Test
    fun `GIVEN valid CachedResponse WHEN converting to MockResponse THEN returns correct MockResponse`() {
        // Given
        val cachedResponse = CachedResponse(200, "Hello World", 1000L)

        // When
        val mockResponse = cachedResponse.toMockResponse()

        // Then
        assertEquals(200, mockResponse.code)
        assertEquals("Hello World", mockResponse.body)
        assertEquals(1000L, mockResponse.delay)
        assertEquals(emptyMap<String, String>(), mockResponse.headers)
    }

    @Test
    fun `GIVEN MockResponse with headers WHEN converting to CachedResponse THEN converts correctly excluding headers`() {
        // Given
        val headers = mapOf("Content-Type" to "application/json", "X-Custom" to "value")
        val mockResponse = MockResponse(404, "Not Found", 500L, headers)

        // When
        val cachedResponse = mockResponse.toCachedResponse()

        // Then
        assertEquals(404, cachedResponse.code)
        assertEquals("Not Found", cachedResponse.body)
        assertEquals(500L, cachedResponse.duration)
        // headers는 CachedResponse에서 지원하지 않으므로 테스트하지 않음
    }

    @Test
    fun `GIVEN CachedResponse with default values WHEN converting to MockResponse THEN defaults convert correctly`() {
        // Given
        val cachedResponseWithDefaults = CachedResponse(200, "OK")

        // When
        val mockResponse = cachedResponseWithDefaults.toMockResponse()

        // Then
        assertEquals(200, mockResponse.code)
        assertEquals("OK", mockResponse.body)
        assertEquals(0L, mockResponse.delay)
        assertEquals(emptyMap<String, String>(), mockResponse.headers)
    }

    @Test
    fun `GIVEN CachedResponses with various status codes WHEN converting to MockResponse THEN all codes convert correctly`() {
        // Given
        val statusCodes = listOf(200, 201, 204, 400, 401, 404, 500, 502, 503)

        // When & Then
        statusCodes.forEach { code ->
            val cachedResponse = CachedResponse(code, "Test Response", 100L)
            val mockResponse = cachedResponse.toMockResponse()

            assertEquals(code, mockResponse.code)
            assertEquals("Test Response", mockResponse.body)
            assertEquals(100L, mockResponse.delay)
        }
    }

    @Test
    fun `GIVEN MockResponses with various status codes WHEN converting to CachedResponse THEN all codes convert correctly`() {
        // Given
        val statusCodes = listOf(200, 201, 204, 400, 401, 404, 500, 502, 503)

        // When & Then
        statusCodes.forEach { code ->
            val mockResponse = MockResponse(code, "Test Response", 100L)
            val cachedResponse = mockResponse.toCachedResponse()

            assertEquals(code, cachedResponse.code)
            assertEquals("Test Response", cachedResponse.body)
            assertEquals(100L, cachedResponse.duration)
        }
    }

    @Test
    fun `GIVEN CachedResponse with zero delay WHEN converting to MockResponse THEN delay converts correctly`() {
        // Given
        val cachedResponse = CachedResponse(200, "OK", 0L)

        // When
        val mockResponse = cachedResponse.toMockResponse()

        // Then
        assertEquals(0L, mockResponse.delay)
        assertFalse(mockResponse.hasDelay)
    }

    @Test
    fun `GIVEN CachedResponse with long body WHEN converting to MockResponse THEN body converts correctly`() {
        // Given
        val longBody = "A".repeat(1000)
        val cachedResponse = CachedResponse(200, longBody, 0L)

        // When
        val mockResponse = cachedResponse.toMockResponse()

        // Then
        assertEquals(longBody, mockResponse.body)
        assertEquals(1000, mockResponse.body.length)
    }

    @Test
    fun `GIVEN CachedResponse with empty body WHEN converting to MockResponse THEN empty body converts correctly`() {
        // Given
        val cachedResponse = CachedResponse(204, "", 0L)

        // When
        val mockResponse = cachedResponse.toMockResponse()

        // Then
        assertEquals("", mockResponse.body)
        assertEquals(204, mockResponse.code)
    }

    @Test
    fun `GIVEN MockResponse with very large delay WHEN converting to CachedResponse THEN delay converts correctly`() {
        // Given
        val largeDelay = Long.MAX_VALUE
        val mockResponse = MockResponse(200, "OK", largeDelay)

        // When
        val cachedResponse = mockResponse.toCachedResponse()

        // Then
        assertEquals(largeDelay, cachedResponse.duration)
    }

    // ================================
    // 양방향 변환 무손실성 테스트
    // ================================

    @Test
    fun `GIVEN MockKey WHEN converting to CachedKey and back to MockKey THEN equals original`() {
        // Given
        val originalMockKey = MockKey("PUT", "/api/items/123")

        // When
        val convertedBack = originalMockKey.toCachedKey().toMockKey()

        // Then
        assertEquals(originalMockKey, convertedBack)
        assertEquals(originalMockKey.method, convertedBack.method)
        assertEquals(originalMockKey.path, convertedBack.path)
    }

    @Test
    fun `GIVEN CachedKey WHEN converting to MockKey and back to CachedKey THEN equals original`() {
        // Given
        val originalCachedKey = CachedKey("DELETE", "/api/items/456")

        // When
        val convertedBackCached = originalCachedKey.toMockKey().toCachedKey()

        // Then
        assertEquals(originalCachedKey, convertedBackCached)
        assertEquals(originalCachedKey.method, convertedBackCached.method)
        assertEquals(originalCachedKey.path, convertedBackCached.path)
    }

    @Test
    fun `GIVEN MockResponse without headers WHEN converting to CachedResponse and back THEN equals original`() {
        // Given
        val originalMockResponse = MockResponse(201, "Created", 200L)

        // When
        val convertedBackMock = originalMockResponse.toCachedResponse().toMockResponse()

        // Then
        assertEquals(originalMockResponse, convertedBackMock)
        assertEquals(originalMockResponse.code, convertedBackMock.code)
        assertEquals(originalMockResponse.body, convertedBackMock.body)
        assertEquals(originalMockResponse.delay, convertedBackMock.delay)
    }

    @Test
    fun `GIVEN CachedResponse WHEN converting to MockResponse and back to CachedResponse THEN equals original`() {
        // Given
        val originalCachedResponse = CachedResponse(500, "Internal Server Error", 0L)

        // When
        val convertedBackCachedResp = originalCachedResponse.toMockResponse().toCachedResponse()

        // Then
        assertEquals(originalCachedResponse, convertedBackCachedResp)
        assertEquals(originalCachedResponse.code, convertedBackCachedResp.code)
        assertEquals(originalCachedResponse.body, convertedBackCachedResp.body)
        assertEquals(originalCachedResponse.duration, convertedBackCachedResp.duration)
    }

    @Test
    fun `GIVEN complex data models WHEN bidirectional conversion THEN all data is preserved`() {
        // Given
        val complexTestCases = listOf(
            Triple("OPTIONS", "/api/v2/complex/path/with/many/segments", "Complex Path"),
            Triple("PATCH", "/api/users/{userId}/posts/{postId}?include=comments", "Template Path"),
            Triple("HEAD", "/search?q=test query&sort=relevance&page=5", "Query Parameters")
        )

        // When & Then
        complexTestCases.forEach { (method, path, description) ->
            // Key conversion test
            val originalMockKey = MockKey(method, path)
            val convertedKey = originalMockKey.toCachedKey().toMockKey()
            assertEquals("$description - Key conversion failed", originalMockKey, convertedKey)

            // Response conversion test (using different status codes for variety)
            val statusCode = when (method) {
                "OPTIONS" -> 200
                "PATCH" -> 204
                "HEAD" -> 404
                else -> 200
            }
            val originalCachedResponse = CachedResponse(statusCode, description, 100L)
            val convertedResponse = originalCachedResponse.toMockResponse().toCachedResponse()
            assertEquals("$description - Response conversion failed", originalCachedResponse, convertedResponse)
        }
    }

    @Test
    fun `GIVEN boundary value data models WHEN bidirectional conversion THEN boundary values are preserved`() {
        // Given
        val boundaryTestCases = listOf(
            // 최소값들
            Triple("A", "/", 100),
            // 최대값들
            Triple("VERYLONGMETHODNAME", "/api/very/long/path/with/many/segments", 599),
            // 특수 케이스
            Triple("GET", "/api/users?page=1&limit=100&sort=created_at&order=desc", 204)
        )

        // When & Then
        boundaryTestCases.forEach { (method, path, statusCode) ->
            // Key boundary test
            val originalMockKey = MockKey(method, path)
            val convertedKey = originalMockKey.toCachedKey().toMockKey()
            assertEquals("Boundary key conversion failed for $method $path", originalMockKey, convertedKey)

            // Response boundary test
            val originalCachedResponse = CachedResponse(statusCode, "Boundary Test", Long.MAX_VALUE)
            val convertedResponse = originalCachedResponse.toMockResponse().toCachedResponse()
            assertEquals("Boundary response conversion failed for status $statusCode", originalCachedResponse, convertedResponse)
        }
    }

    // ================================
    // 변환 과정에서 헤더 처리 테스트
    // ================================

    @Test
    fun `GIVEN MockResponse with headers WHEN converting to CachedResponse THEN headers are ignored and other fields preserved`() {
        // Given
        val complexHeaders = mapOf(
            "Content-Type" to "application/json",
            "Authorization" to "Bearer token123",
            "X-Custom-Header" to "custom-value",
            "Cache-Control" to "no-cache"
        )
        val mockResponse = MockResponse(200, "Response with headers", 500L, complexHeaders)

        // When
        val cachedResponse = mockResponse.toCachedResponse()

        // Then
        assertEquals(200, cachedResponse.code)
        assertEquals("Response with headers", cachedResponse.body)
        assertEquals(500L, cachedResponse.duration)
        // CachedResponse에는 헤더 필드가 없으므로 검증할 수 없음
    }

    @Test
    fun `GIVEN CachedResponse WHEN converting to MockResponse THEN headers are always empty map`() {
        // Given
        val cachedResponse = CachedResponse(404, "Not Found", 1000L)

        // When
        val mockResponse = cachedResponse.toMockResponse()

        // Then
        assertEquals(404, mockResponse.code)
        assertEquals("Not Found", mockResponse.body)
        assertEquals(1000L, mockResponse.delay)
        assertTrue("Headers should be empty", mockResponse.headers.isEmpty())
        assertFalse("Should not have headers", mockResponse.hasHeaders)
    }

    @Test
    fun `GIVEN MockResponse with headers WHEN converting via CachedResponse THEN headers are lost but other data preserved`() {
        // Given
        val originalHeaders = mapOf("Content-Type" to "application/json", "X-Test" to "value")
        val originalMockResponse = MockResponse(200, "Test", 300L, originalHeaders)

        // When
        val convertedResponse = originalMockResponse.toCachedResponse().toMockResponse()

        // Then
        assertEquals(originalMockResponse.code, convertedResponse.code)
        assertEquals(originalMockResponse.body, convertedResponse.body)
        assertEquals(originalMockResponse.delay, convertedResponse.delay)

        // 헤더는 손실되어야 함
        assertNotEquals(originalMockResponse.headers, convertedResponse.headers)
        assertTrue("Converted response should have no headers", convertedResponse.headers.isEmpty())
        assertFalse("Converted response should not have headers", convertedResponse.hasHeaders)

        // 원본은 헤더를 가져야 함
        assertTrue("Original response should have headers", originalMockResponse.hasHeaders)
        assertEquals(2, originalMockResponse.headers.size)
    }

    @Test
    fun `GIVEN MockResponse with empty headers WHEN testing conversion THEN empty header state is consistently maintained`() {
        // Given
        val mockResponseExplicitEmpty = MockResponse(200, "Test", 0L, emptyMap())
        val mockResponseDefaultEmpty = MockResponse(200, "Test")

        // When
        val convertedExplicit = mockResponseExplicitEmpty.toCachedResponse().toMockResponse()
        val convertedDefault = mockResponseDefaultEmpty.toCachedResponse().toMockResponse()

        // Then
        assertTrue("Explicitly empty headers should remain empty", convertedExplicit.headers.isEmpty())
        assertTrue("Default empty headers should remain empty", convertedDefault.headers.isEmpty())
        assertFalse("Explicitly empty should not have headers", convertedExplicit.hasHeaders)
        assertFalse("Default empty should not have headers", convertedDefault.hasHeaders)
    }

    // ================================
    // 변환 성능 및 안정성 테스트
    // ================================

    @Test
    fun `GIVEN bulk conversion operations WHEN performing repeated conversions THEN all conversions provide consistent results`() {
        // Given
        val iterations = 100
        val originalMockKey = MockKey("GET", "/api/test")
        val originalCachedResponse = CachedResponse(200, "Test Response", 100L)

        // When & Then
        repeat(iterations) { index ->
            // Key conversion consistency
            val convertedKey = originalMockKey.toCachedKey().toMockKey()
            assertEquals("Key conversion should be consistent at iteration $index", originalMockKey, convertedKey)

            // Response conversion consistency
            val convertedResponse = originalCachedResponse.toMockResponse().toCachedResponse()
            assertEquals("Response conversion should be consistent at iteration $index", originalCachedResponse, convertedResponse)
        }
    }

    @Test
    fun `GIVEN data with various character encodings WHEN converting THEN encoding is preserved`() {
        // Given
        val unicodeTestCases = listOf(
            "Hello World", // 기본 ASCII
            "안녕하세요", // 한글
            "こんにちは", // 일본어
            "🚀🌟💫", // 이모지
            "Test with \"quotes\" and special chars: @#$%^&*()",
            "{\"json\": \"data\", \"number\": 123, \"boolean\": true}"
        )

        // When & Then
        unicodeTestCases.forEach { testBody ->
            val cachedResponse = CachedResponse(200, testBody, 0L)
            val convertedResponse = cachedResponse.toMockResponse().toCachedResponse()

            assertEquals("Body encoding should be preserved for: $testBody",
                testBody, convertedResponse.body)
        }
    }

    @Test
    fun `GIVEN extreme case data WHEN converting THEN handled stably`() {
        // Given
        val extremeCases = listOf(
            // 매우 긴 본문
            CachedResponse(200, "A".repeat(50000), 0L),
            // 매우 큰 지연 시간
            CachedResponse(200, "Test", Long.MAX_VALUE - 1),
            // 경계값 상태 코드
            CachedResponse(100, "Continue", 0L),
            CachedResponse(599, "Network Error", 0L),
            // 빈 본문
            CachedResponse(204, "", 0L)
        )

        // When & Then
        extremeCases.forEach { original ->
            val converted = original.toMockResponse().toCachedResponse()
            assertEquals("Extreme case conversion should preserve data", original, converted)
        }
    }
}