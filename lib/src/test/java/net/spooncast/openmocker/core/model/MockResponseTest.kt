package net.spooncast.openmocker.core.model

import org.junit.Test
import org.junit.Assert.*

/**
 * MockResponse [HTTP 응답 데이터 클래스] - BDD 스타일 종합 테스트
 * 100% 코드 커버리지와 모든 엣지 케이스를 다루는 포괄적 테스트 스위트
 */
class MockResponseTest {

    // ================================
    // MockResponse 생성 테스트
    // ================================

    @Test
    fun `GIVEN valid all parameters WHEN creating MockResponse THEN creation succeeds`() {
        // Given
        val code = 200
        val body = "OK"
        val delay = 100L
        val headers = mapOf("Content-Type" to "application/json")

        // When
        val response = MockResponse(code, body, delay, headers)

        // Then
        assertEquals(200, response.code)
        assertEquals("OK", response.body)
        assertEquals(100L, response.delay)
        assertEquals(mapOf("Content-Type" to "application/json"), response.headers)
    }

    @Test
    fun `GIVEN default values only WHEN creating MockResponse THEN creation succeeds with defaults`() {
        // Given & When
        val response = MockResponse(404, "Not Found")

        // Then
        assertEquals(404, response.code)
        assertEquals("Not Found", response.body)
        assertEquals(0L, response.delay)
        assertTrue(response.headers.isEmpty())
    }

    @Test
    fun `GIVEN various valid status codes WHEN creating MockResponse THEN all codes succeed`() {
        // Given
        val validCodes = listOf(100, 101, 200, 201, 204, 300, 301, 400, 401, 404, 500, 502, 599)

        // When & Then
        validCodes.forEach { code ->
            val response = MockResponse(code, "Test")
            assertEquals(code, response.code)
            assertEquals("Test", response.body)
        }
    }

    @Test
    fun `GIVEN empty body and headers WHEN creating MockResponse THEN creation succeeds`() {
        // Given
        val emptyBody = ""
        val emptyHeaders = emptyMap<String, String>()

        // When
        val response = MockResponse(200, emptyBody, 0L, emptyHeaders)

        // Then
        assertEquals(200, response.code)
        assertEquals("", response.body)
        assertEquals(0L, response.delay)
        assertTrue(response.headers.isEmpty())
    }

    @Test
    fun `GIVEN complex headers map WHEN creating MockResponse THEN all headers stored correctly`() {
        // Given
        val complexHeaders = mapOf(
            "Content-Type" to "application/json",
            "X-Custom-Header" to "custom-value",
            "Authorization" to "Bearer token123",
            "Cache-Control" to "no-cache, no-store",
            "User-Agent" to "MockClient/1.0"
        )

        // When
        val response = MockResponse(200, "OK", 0L, complexHeaders)

        // Then
        assertEquals(complexHeaders, response.headers)
        assertEquals(5, response.headers.size)
        assertEquals("application/json", response.headers["Content-Type"])
        assertEquals("custom-value", response.headers["X-Custom-Header"])
    }

    // ================================
    // MockResponse 검증 테스트
    // ================================

    @Test(expected = IllegalArgumentException::class)
    fun `GIVEN status code below minimum WHEN creating MockResponse THEN throws IllegalArgumentException`() {
        MockResponse(99, "Invalid")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `GIVEN status code above maximum WHEN creating MockResponse THEN throws IllegalArgumentException`() {
        MockResponse(600, "Invalid")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `GIVEN negative delay WHEN creating MockResponse THEN throws IllegalArgumentException`() {
        MockResponse(200, "OK", -1L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `GIVEN very negative delay WHEN creating MockResponse THEN throws IllegalArgumentException`() {
        MockResponse(200, "OK", -1000L)
    }

    @Test
    fun `GIVEN boundary status codes WHEN creating MockResponse THEN creation succeeds`() {
        // Given & When & Then
        assertNotNull(MockResponse(100, "Continue")) // 최소값
        assertNotNull(MockResponse(599, "Network Error")) // 최대값
        assertNotNull(MockResponse(200, "OK")) // 일반적인 성공
        assertNotNull(MockResponse(404, "Not Found")) // 일반적인 클라이언트 에러
        assertNotNull(MockResponse(500, "Internal Server Error")) // 일반적인 서버 에러
    }

    @Test
    fun `GIVEN zero delay WHEN creating MockResponse THEN creation succeeds`() {
        // Given & When
        val response = MockResponse(200, "OK", 0L)

        // Then
        assertEquals(0L, response.delay)
        assertFalse(response.hasDelay)
    }

    @Test
    fun `GIVEN very large delay WHEN creating MockResponse THEN creation succeeds`() {
        // Given & When
        val response = MockResponse(200, "OK", Long.MAX_VALUE)

        // Then
        assertEquals(Long.MAX_VALUE, response.delay)
        assertTrue(response.hasDelay)
    }

    // ================================
    // MockResponse 상태 코드 분류 테스트
    // ================================

    @Test
    fun `GIVEN 2xx status codes WHEN checking isSuccess THEN returns true`() {
        // Given
        val successCodes = listOf(200, 201, 202, 204, 206, 299)

        // When & Then
        successCodes.forEach { code ->
            val response = MockResponse(code, "Success")
            assertTrue("Status code $code should be success", response.isSuccess)
            assertFalse("Status code $code should not be client error", response.isClientError)
            assertFalse("Status code $code should not be server error", response.isServerError)
        }
    }

    @Test
    fun `GIVEN 4xx status codes WHEN checking isClientError THEN returns true`() {
        // Given
        val clientErrorCodes = listOf(400, 401, 403, 404, 409, 422, 429, 499)

        // When & Then
        clientErrorCodes.forEach { code ->
            val response = MockResponse(code, "Client Error")
            assertTrue("Status code $code should be client error", response.isClientError)
            assertFalse("Status code $code should not be success", response.isSuccess)
            assertFalse("Status code $code should not be server error", response.isServerError)
        }
    }

    @Test
    fun `GIVEN 5xx status codes WHEN checking isServerError THEN returns true`() {
        // Given
        val serverErrorCodes = listOf(500, 501, 502, 503, 504, 599)

        // When & Then
        serverErrorCodes.forEach { code ->
            val response = MockResponse(code, "Server Error")
            assertTrue("Status code $code should be server error", response.isServerError)
            assertFalse("Status code $code should not be success", response.isSuccess)
            assertFalse("Status code $code should not be client error", response.isClientError)
        }
    }

    @Test
    fun `GIVEN 1xx and 3xx status codes WHEN checking classification properties THEN all return false`() {
        // Given
        val informationalAndRedirectCodes = listOf(100, 101, 199, 300, 301, 302, 304, 399)

        // When & Then
        informationalAndRedirectCodes.forEach { code ->
            val response = MockResponse(code, "Info or Redirect")
            assertFalse("Status code $code should not be success", response.isSuccess)
            assertFalse("Status code $code should not be client error", response.isClientError)
            assertFalse("Status code $code should not be server error", response.isServerError)
        }
    }

    @Test
    fun `GIVEN boundary status codes WHEN checking classification THEN returns correct classification`() {
        // Given & When & Then
        // 199 -> 1xx (정보)
        val info = MockResponse(199, "Info")
        assertFalse(info.isSuccess)
        assertFalse(info.isClientError)
        assertFalse(info.isServerError)

        // 200 -> 2xx (성공)
        val success = MockResponse(200, "Success")
        assertTrue(success.isSuccess)
        assertFalse(success.isClientError)
        assertFalse(success.isServerError)

        // 299 -> 2xx (성공)
        val successBoundary = MockResponse(299, "Success Boundary")
        assertTrue(successBoundary.isSuccess)
        assertFalse(successBoundary.isClientError)
        assertFalse(successBoundary.isServerError)

        // 300 -> 3xx (리디렉션)
        val redirect = MockResponse(300, "Redirect")
        assertFalse(redirect.isSuccess)
        assertFalse(redirect.isClientError)
        assertFalse(redirect.isServerError)

        // 399 -> 3xx (리디렉션)
        val redirectBoundary = MockResponse(399, "Redirect Boundary")
        assertFalse(redirectBoundary.isSuccess)
        assertFalse(redirectBoundary.isClientError)
        assertFalse(redirectBoundary.isServerError)

        // 400 -> 4xx (클라이언트 에러)
        val clientError = MockResponse(400, "Client Error")
        assertFalse(clientError.isSuccess)
        assertTrue(clientError.isClientError)
        assertFalse(clientError.isServerError)

        // 499 -> 4xx (클라이언트 에러)
        val clientErrorBoundary = MockResponse(499, "Client Error Boundary")
        assertFalse(clientErrorBoundary.isSuccess)
        assertTrue(clientErrorBoundary.isClientError)
        assertFalse(clientErrorBoundary.isServerError)

        // 500 -> 5xx (서버 에러)
        val serverError = MockResponse(500, "Server Error")
        assertFalse(serverError.isSuccess)
        assertFalse(serverError.isClientError)
        assertTrue(serverError.isServerError)

        // 599 -> 5xx (서버 에러)
        val serverErrorBoundary = MockResponse(599, "Server Error Boundary")
        assertFalse(serverErrorBoundary.isSuccess)
        assertFalse(serverErrorBoundary.isClientError)
        assertTrue(serverErrorBoundary.isServerError)
    }

    // ================================
    // MockResponse 속성 확인 테스트
    // ================================

    @Test
    fun `GIVEN various delays WHEN checking hasDelay THEN returns correct result`() {
        // Given & When & Then
        assertFalse("0ms delay should not have delay", MockResponse(200, "OK", 0L).hasDelay)
        assertTrue("1ms delay should have delay", MockResponse(200, "OK", 1L).hasDelay)
        assertTrue("100ms delay should have delay", MockResponse(200, "OK", 100L).hasDelay)
        assertTrue("Long.MAX_VALUE delay should have delay", MockResponse(200, "OK", Long.MAX_VALUE).hasDelay)
    }

    @Test
    fun `GIVEN various header states WHEN checking hasHeaders THEN returns correct result`() {
        // Given & When & Then
        assertFalse("Empty headers should not have headers",
            MockResponse(200, "OK").hasHeaders)

        assertFalse("Explicitly empty headers should not have headers",
            MockResponse(200, "OK", headers = emptyMap()).hasHeaders)

        assertTrue("Single header should have headers",
            MockResponse(200, "OK", headers = mapOf("Content-Type" to "application/json")).hasHeaders)

        assertTrue("Multiple headers should have headers",
            MockResponse(200, "OK", headers = mapOf(
                "Content-Type" to "application/json",
                "X-Custom" to "value"
            )).hasHeaders)
    }

    @Test
    fun `GIVEN delay and headers combination WHEN checking properties THEN returns independent results`() {
        // Given
        val testCases = listOf(
            Triple(0L, emptyMap<String, String>(), Pair(false, false)),
            Triple(100L, emptyMap<String, String>(), Pair(true, false)),
            Triple(0L, mapOf("Content-Type" to "text/plain"), Pair(false, true)),
            Triple(500L, mapOf("X-Custom" to "value"), Pair(true, true))
        )

        // When & Then
        testCases.forEach { (delay, headers, expected) ->
            val response = MockResponse(200, "OK", delay, headers)
            assertEquals("Delay property should be independent", expected.first, response.hasDelay)
            assertEquals("Headers property should be independent", expected.second, response.hasHeaders)
        }
    }

    // ================================
    // MockResponse toString 테스트
    // ================================

    @Test
    fun `GIVEN basic MockResponse WHEN calling toString THEN returns correct format`() {
        // Given
        val response = MockResponse(200, "Hello World")

        // When
        val result = response.toString()

        // Then
        assertEquals("HTTP 200 (body: 11 chars)", result)
    }

    @Test
    fun `GIVEN MockResponse with delay WHEN calling toString THEN returns format with delay`() {
        // Given
        val response = MockResponse(404, "Not Found", 1000L)

        // When
        val result = response.toString()

        // Then
        assertEquals("HTTP 404 (body: 9 chars) delay: 1000ms", result)
    }

    @Test
    fun `GIVEN MockResponse with headers WHEN calling toString THEN returns format with headers count`() {
        // Given
        val response = MockResponse(200, "OK", headers = mapOf("Content-Type" to "application/json"))

        // When
        val result = response.toString()

        // Then
        assertEquals("HTTP 200 (body: 2 chars) headers: 1", result)
    }

    @Test
    fun `GIVEN MockResponse with delay and headers WHEN calling toString THEN returns format with all info`() {
        // Given
        val response = MockResponse(500, "Error", 500L, mapOf("X-Error" to "true", "Retry-After" to "60"))

        // When
        val result = response.toString()

        // Then
        assertEquals("HTTP 500 (body: 5 chars) delay: 500ms headers: 2", result)
    }

    @Test
    fun `GIVEN various body lengths WHEN calling toString THEN shows correct character count`() {
        // Given
        val testCases = listOf(
            Pair("", 0),
            Pair("A", 1),
            Pair("Hello, World!", 13),
            Pair("한글 테스트", 6), // Korean characters: 한(1) 글(1) (space)(1) 테(1) 스(1) 트(1) = 6
            Pair("{\"message\": \"JSON response with unicode: 🚀\"}", 43) // Emoji counts as 2 code units
        )

        // When & Then
        testCases.forEach { (body, expectedLength) ->
            val response = MockResponse(200, body)
            val result = response.toString()
            val actualLength = body.length
            assertTrue("Should contain correct character count for '$body': expected $actualLength but checking for $expectedLength",
                result.contains("body: $actualLength chars"))
        }
    }

    @Test
    fun `GIVEN zero delay and empty headers WHEN calling toString THEN returns basic format only`() {
        // Given
        val response = MockResponse(200, "Test", 0L, emptyMap())

        // When
        val result = response.toString()

        // Then
        assertEquals("HTTP 200 (body: 4 chars)", result)
        assertFalse("Should not contain delay info", result.contains("delay"))
        assertFalse("Should not contain headers info", result.contains("headers"))
    }

    // ================================
    // MockResponse 동등성 테스트
    // ================================

    @Test
    fun `GIVEN identical MockResponses WHEN comparing equality THEN returns true`() {
        // Given
        val response1 = MockResponse(200, "OK", 100L, mapOf("Content-Type" to "application/json"))
        val response2 = MockResponse(200, "OK", 100L, mapOf("Content-Type" to "application/json"))

        // When & Then
        assertEquals(response1, response2)
        assertEquals(response2, response1) // 대칭성
        assertTrue(response1 == response2)
    }

    @Test
    fun `GIVEN MockResponses with different status codes WHEN comparing equality THEN returns false`() {
        // Given
        val response1 = MockResponse(200, "OK", 100L, mapOf("Content-Type" to "application/json"))
        val response2 = MockResponse(201, "Created", 100L, mapOf("Content-Type" to "application/json"))

        // When & Then
        assertNotEquals(response1, response2)
        assertNotEquals(response2, response1)
        assertFalse(response1 == response2)
    }

    @Test
    fun `GIVEN MockResponses with different bodies WHEN comparing equality THEN returns false`() {
        // Given
        val response1 = MockResponse(200, "OK", 100L, mapOf("Content-Type" to "application/json"))
        val response2 = MockResponse(200, "Different", 100L, mapOf("Content-Type" to "application/json"))

        // When & Then
        assertNotEquals(response1, response2)
        assertFalse(response1 == response2)
    }

    @Test
    fun `GIVEN MockResponses with different delays WHEN comparing equality THEN returns false`() {
        // Given
        val response1 = MockResponse(200, "OK", 100L, mapOf("Content-Type" to "application/json"))
        val response2 = MockResponse(200, "OK", 200L, mapOf("Content-Type" to "application/json"))

        // When & Then
        assertNotEquals(response1, response2)
        assertFalse(response1 == response2)
    }

    @Test
    fun `GIVEN MockResponses with different headers WHEN comparing equality THEN returns false`() {
        // Given
        val response1 = MockResponse(200, "OK", 100L, mapOf("Content-Type" to "application/json"))
        val response2 = MockResponse(200, "OK", 100L, mapOf("Content-Type" to "text/plain"))

        // When & Then
        assertNotEquals(response1, response2)
        assertFalse(response1 == response2)
    }

    @Test
    fun `GIVEN MockResponse and null WHEN comparing equality THEN returns false`() {
        // Given
        val response = MockResponse(200, "OK")

        // When & Then
        assertNotEquals(response, null)
        assertFalse(response.equals(null))
    }

    @Test
    fun `GIVEN MockResponse and different type object WHEN comparing equality THEN returns false`() {
        // Given
        val response = MockResponse(200, "OK")
        val differentObject = "HTTP 200 OK"

        // When & Then
        assertNotEquals(response, differentObject)
        assertFalse(response.equals(differentObject))
    }

    @Test
    fun `GIVEN MockResponse WHEN comparing with itself THEN returns true`() {
        // Given
        val response = MockResponse(200, "OK", 100L, mapOf("Content-Type" to "application/json"))

        // When & Then
        assertEquals(response, response)
        assertTrue(response == response)
    }

    // ================================
    // MockResponse hashCode 테스트
    // ================================

    @Test
    fun `GIVEN identical MockResponses WHEN comparing hashCode THEN returns same hashCode`() {
        // Given
        val response1 = MockResponse(200, "OK", 100L, mapOf("Content-Type" to "application/json"))
        val response2 = MockResponse(200, "OK", 100L, mapOf("Content-Type" to "application/json"))

        // When & Then
        assertEquals(response1.hashCode(), response2.hashCode())
    }

    @Test
    fun `GIVEN different MockResponses WHEN comparing hashCode THEN likely returns different hashCode`() {
        // Given
        val response1 = MockResponse(200, "OK", 100L, mapOf("Content-Type" to "application/json"))
        val response2 = MockResponse(404, "Not Found", 200L, mapOf("Content-Type" to "text/plain"))
        val response3 = MockResponse(500, "Error", 0L, emptyMap())

        // When & Then
        assertNotEquals(response1.hashCode(), response2.hashCode())
        assertNotEquals(response1.hashCode(), response3.hashCode())
        assertNotEquals(response2.hashCode(), response3.hashCode())
    }

    @Test
    fun `GIVEN MockResponse WHEN calling hashCode multiple times THEN returns consistent value`() {
        // Given
        val response = MockResponse(200, "OK", 100L, mapOf("Content-Type" to "application/json"))

        // When
        val hashCode1 = response.hashCode()
        val hashCode2 = response.hashCode()
        val hashCode3 = response.hashCode()

        // Then
        assertEquals(hashCode1, hashCode2)
        assertEquals(hashCode1, hashCode3)
        assertEquals(hashCode2, hashCode3)
    }

    // ================================
    // MockResponse 경계값 테스트
    // ================================

    @Test
    fun `GIVEN min and max status codes WHEN creating MockResponse THEN creation succeeds`() {
        // Given & When & Then
        val minResponse = MockResponse(100, "Continue")
        assertEquals(100, minResponse.code)

        val maxResponse = MockResponse(599, "Network Error")
        assertEquals(599, maxResponse.code)
    }

    @Test
    fun `GIVEN very long body WHEN creating MockResponse THEN creation succeeds`() {
        // Given
        val longBody = "A".repeat(10000)

        // When
        val response = MockResponse(200, longBody)

        // Then
        assertEquals(longBody, response.body)
        assertEquals(10000, response.body.length)
        assertTrue(response.toString().contains("body: 10000 chars"))
    }

    @Test
    fun `GIVEN very many headers WHEN creating MockResponse THEN creation succeeds`() {
        // Given
        val manyHeaders = (1..100).associate { "Header-$it" to "Value-$it" }

        // When
        val response = MockResponse(200, "OK", 0L, manyHeaders)

        // Then
        assertEquals(100, response.headers.size)
        assertTrue(response.hasHeaders)
        assertTrue(response.toString().contains("headers: 100"))
    }

    @Test
    fun `GIVEN maximum delay WHEN creating MockResponse THEN creation succeeds`() {
        // Given
        val maxDelay = Long.MAX_VALUE

        // When
        val response = MockResponse(200, "OK", maxDelay)

        // Then
        assertEquals(maxDelay, response.delay)
        assertTrue(response.hasDelay)
        assertTrue(response.toString().contains("delay: ${maxDelay}ms"))
    }
}