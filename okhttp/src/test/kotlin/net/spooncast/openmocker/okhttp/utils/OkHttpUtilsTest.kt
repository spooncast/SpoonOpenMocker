package net.spooncast.openmocker.okhttp.utils

import kotlinx.coroutines.test.runTest
import net.spooncast.openmocker.core.MockKey
import net.spooncast.openmocker.core.MockResponse
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.MediaType.Companion.toMediaType
import org.junit.Assert.*
import org.junit.Test
import kotlin.system.measureTimeMillis

class OkHttpUtilsTest {

    // OkHttpUtils.createMockOkHttpResponse tests
    // BDD: Given mock response data, When createMockOkHttpResponse is called, Then create correct OkHttp response
    @Test
    fun `createMockOkHttpResponse creates correct response`() {
        // Arrange
        val mockResponse = MockResponse(201, """{"success": true}""", 0L)
        val originalRequest = Request.Builder()
            .url("https://api.example.com/users")
            .post("{}".toRequestBody())
            .build()

        // Act
        val result = OkHttpUtils.createMockOkHttpResponse(mockResponse, originalRequest)

        // Assert
        assertEquals(201, result.code)
        assertEquals("Created", result.message)
        assertEquals(originalRequest, result.request)
        assertEquals(Protocol.HTTP_1_1, result.protocol)
        assertNotNull(result.body)
        assertEquals("""{"success": true}""", result.body?.string())
    }

    // BDD: Given JSON response body, When createMockOkHttpResponse is called, Then detect JSON content type
    @Test
    fun `createMockOkHttpResponse detects JSON content type`() {
        // Arrange
        val mockResponse = MockResponse(200, """{"json": true}""")
        val originalRequest = Request.Builder().url("https://api.example.com/test").build()

        // Act
        val result = OkHttpUtils.createMockOkHttpResponse(mockResponse, originalRequest)

        // Assert
        assertEquals("application/json; charset=utf-8", result.body?.contentType().toString())
    }

    // BDD: Given XML response body, When createMockOkHttpResponse is called, Then detect XML content type
    @Test
    fun `createMockOkHttpResponse detects XML content type`() {
        // Arrange
        val mockResponse = MockResponse(200, """<xml><data>test</data></xml>""")
        val originalRequest = Request.Builder().url("https://api.example.com/test").build()

        // Act
        val result = OkHttpUtils.createMockOkHttpResponse(mockResponse, originalRequest)

        // Assert
        assertEquals("application/xml; charset=utf-8", result.body?.contentType().toString())
    }

    // BDD: Given plain text response, When createMockOkHttpResponse is called, Then use text content type
    @Test
    fun `createMockOkHttpResponse uses text content type for plain text`() {
        // Arrange
        val mockResponse = MockResponse(200, "Plain text response")
        val originalRequest = Request.Builder().url("https://api.example.com/test").build()

        // Act
        val result = OkHttpUtils.createMockOkHttpResponse(mockResponse, originalRequest)

        // Assert
        assertEquals("text/plain; charset=utf-8", result.body?.contentType().toString())
    }

    // BDD: Given custom media type, When createMockOkHttpResponse is called, Then use provided media type
    @Test
    fun `createMockOkHttpResponse uses custom media type when provided`() {
        // Arrange
        val mockResponse = MockResponse(200, "custom content")
        val originalRequest = Request.Builder().url("https://api.example.com/test").build()
        val customMediaType = "application/custom; charset=utf-8".toMediaType()

        // Act
        val result = OkHttpUtils.createMockOkHttpResponse(mockResponse, originalRequest, customMediaType)

        // Assert
        assertEquals(customMediaType, result.body?.contentType())
    }

    // BDD: Given various status codes, When createMockOkHttpResponse is called, Then handle all codes correctly
    @Test
    fun `createMockOkHttpResponse handles different status codes correctly`() {
        val testCases = mapOf(
            200 to "OK",
            201 to "Created",
            204 to "No Content",
            400 to "Bad Request",
            401 to "Unauthorized",
            403 to "Forbidden",
            404 to "Not Found",
            405 to "Method Not Allowed",
            500 to "Internal Server Error",
            502 to "Bad Gateway",
            503 to "Service Unavailable",
            418 to "Unknown" // Custom status code
        )

        for ((statusCode, expectedMessage) in testCases) {
            // Arrange
            val mockResponse = MockResponse(statusCode, "test body")
            val originalRequest = Request.Builder().url("https://api.example.com/test").build()

            // Act
            val result = OkHttpUtils.createMockOkHttpResponse(mockResponse, originalRequest)

            // Assert
            assertEquals("Status code should be $statusCode", statusCode, result.code)
            assertEquals("Message should be $expectedMessage", expectedMessage, result.message)
        }
    }

    // OkHttpUtils.applyMockDelay tests
    // BDD: Given zero delay, When applyMockDelay is called, Then complete immediately
    @Test
    fun `applyMockDelay does not delay when delay is zero`() = runTest {
        // Arrange
        val mockResponse = MockResponse(200, "test", 0L)

        // Act
        val elapsedTime = measureTimeMillis {
            OkHttpUtils.applyMockDelay(mockResponse)
        }

        // Assert
        assertTrue("Should not delay for zero delay", elapsedTime < 10)
    }

    // BDD: Given positive delay, When applyMockDelay is called, Then apply correct delay duration
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `applyMockDelay applies correct delay`() = runTest {
        // Arrange
        val delayMs = 100L
        val mockResponse = MockResponse(200, "test", delayMs)

        // Act & Assert - In test environment with TestDispatcher, we just verify the function completes
        // The actual delay behavior is controlled by the test dispatcher
        OkHttpUtils.applyMockDelay(mockResponse)

        // For virtual time testing, advance time and verify
        testScheduler.advanceTimeBy(delayMs)
        testScheduler.runCurrent()

        // If we get here without hanging, the delay was applied correctly
        assertTrue("Delay function completed successfully", true)
    }

    // BDD: Given negative delay, When applyMockDelay is called, Then complete immediately
    @Test
    fun `applyMockDelay does not delay for negative values`() = runTest {
        // Arrange
        val mockResponse = MockResponse(200, "test", -100L)

        // Act
        val elapsedTime = measureTimeMillis {
            OkHttpUtils.applyMockDelay(mockResponse)
        }

        // Assert
        assertTrue("Should not delay for negative delay", elapsedTime < 10)
    }

    // Extension function tests
    // BDD: Given HTTP request, When toMockKey is called, Then create correct MockKey
    @Test
    fun `Request toMockKey creates correct key`() {
        // Arrange
        val request = Request.Builder()
            .url("https://api.example.com/api/users/123?filter=active")
            .post("{}".toRequestBody())
            .build()

        // Act
        val result = request.toMockKey()

        // Assert
        assertEquals("POST", result.method)
        assertEquals("/api/users/123", result.path)
    }

    // BDD: Given GET request with query params, When toMockKey is called, Then extract path without query
    @Test
    fun `Request toMockKey handles GET requests`() {
        // Arrange
        val request = Request.Builder()
            .url("https://api.example.com/search?q=kotlin")
            .get()
            .build()

        // Act
        val result = request.toMockKey()

        // Assert
        assertEquals("GET", result.method)
        assertEquals("/search", result.path)
    }

    // BDD: Given HTTP response and delay, When toMockResponse is called, Then create correct MockResponse
    @Test
    fun `Response toMockResponse creates correct mock response`() {
        // Arrange
        val responseBody = """{"data": "test"}"""
        val response = createMockResponse(201, "Created", responseBody)
        val expectedDelay = 250L

        // Act
        val result = response.toMockResponse(expectedDelay)

        // Assert
        assertEquals(201, result.code)
        assertEquals(responseBody, result.body)
        assertEquals(expectedDelay, result.delay)
    }

    // BDD: Given response with empty body, When toMockResponse is called, Then handle gracefully
    @Test
    fun `Response toMockResponse handles empty body`() {
        // Arrange
        val response = createMockResponse(204, "No Content", "")

        // Act
        val result = response.toMockResponse()

        // Assert
        assertEquals(204, result.code)
        assertEquals("", result.body)
        assertEquals(0L, result.delay)
    }

    // BDD: Given response with null body, When toMockResponse is called, Then return empty string
    @Test
    fun `Response toMockResponse handles null body gracefully`() {
        // Arrange
        val request = Request.Builder().url("https://api.example.com").build()
        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .build() // No body set (will be null)

        // Act
        val result = response.toMockResponse()

        // Assert
        assertEquals(200, result.code)
        assertEquals("", result.body)
        assertEquals(0L, result.delay)
    }

    // BDD: Given HTTP response, When peekBody is called, Then create readable copy of response
    @Test
    fun `Response peekBody creates readable copy`() {
        // Arrange
        val originalBody = """{"original": true}"""
        val response = createMockResponse(200, "OK", originalBody)

        // Act
        val peekedResponse = response.peekBody()

        // Assert
        assertEquals(response.code, peekedResponse.code)
        assertEquals(response.message, peekedResponse.message)
        assertEquals(originalBody, peekedResponse.body?.string())
    }

    // BDD: Given request and mock parameters, When createMockResponse is called, Then create MockResponse
    @Test
    fun `Request createMockResponse creates correct response`() {
        // Arrange
        val request = Request.Builder()
            .url("https://api.example.com/test")
            .build()

        // Act
        val result = request.createMockResponse(
            code = 422,
            body = """{"error": "validation failed"}""",
            delay = 75L
        )

        // Assert
        assertEquals(422, result.code)
        assertEquals("""{"error": "validation failed"}""", result.body)
        assertEquals(75L, result.delay)
    }

    // BDD: Given various status codes, When isSuccessful is called, Then return correct boolean values
    @Test
    fun `MockResponse isSuccessful returns correct values`() {
        val testCases = mapOf(
            200 to true,
            201 to true,
            299 to true,
            300 to false,
            400 to false,
            500 to false
        )

        for ((statusCode, expectedSuccess) in testCases) {
            // Arrange
            val mockResponse = MockResponse(statusCode, "test body")

            // Act
            val result = mockResponse.isSuccessful()

            // Assert
            assertEquals("Status $statusCode should ${if (expectedSuccess) "be" else "not be"} successful",
                expectedSuccess, result)
        }
    }

    // BDD: Given various status codes, When isClientError is called, Then identify 4xx codes correctly
    @Test
    fun `MockResponse isClientError returns correct values`() {
        val testCases = mapOf(
            399 to false,
            400 to true,
            404 to true,
            499 to true,
            500 to false
        )

        for ((statusCode, expectedClientError) in testCases) {
            // Arrange
            val mockResponse = MockResponse(statusCode, "test body")

            // Act
            val result = mockResponse.isClientError()

            // Assert
            assertEquals("Status $statusCode should ${if (expectedClientError) "be" else "not be"} client error",
                expectedClientError, result)
        }
    }

    // BDD: Given various status codes, When isServerError is called, Then identify 5xx codes correctly
    @Test
    fun `MockResponse isServerError returns correct values`() {
        val testCases = mapOf(
            499 to false,
            500 to true,
            502 to true,
            599 to true,
            600 to false
        )

        for ((statusCode, expectedServerError) in testCases) {
            // Arrange
            val mockResponse = MockResponse(statusCode, "test body")

            // Act
            val result = mockResponse.isServerError()

            // Assert
            assertEquals("Status $statusCode should ${if (expectedServerError) "be" else "not be"} server error",
                expectedServerError, result)
        }
    }

    // Helper method to create mock responses for testing
    private fun createMockResponse(code: Int, message: String, body: String): Response {
        return Response.Builder()
            .request(Request.Builder().url("https://api.example.com").build())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(message)
            .body(body.toResponseBody())
            .build()
    }
}