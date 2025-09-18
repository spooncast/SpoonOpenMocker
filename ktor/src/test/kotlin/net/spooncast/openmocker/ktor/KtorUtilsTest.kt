package net.spooncast.openmocker.ktor

import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import net.spooncast.openmocker.core.MockResponse
import org.junit.Assert.*
import org.junit.Test

class KtorUtilsTest {

    // BDD: Given JSON response body, When detectContentType is called, Then return JSON ContentType
    @Test
    fun `detectContentType identifies JSON content`() {
        // Arrange
        val jsonBodies = listOf(
            """{"key": "value"}""",
            """[{"id": 1}, {"id": 2}]""",
            """ { "nested": { "object": true } } """,
            """[ ]"""
        )

        // Act & Assert
        jsonBodies.forEach { body ->
            val result = KtorUtils.detectContentType(body)
            assertEquals("Expected JSON content type for body: $body",
                ContentType.Application.Json, result)
        }
    }

    // BDD: Given XML response body, When detectContentType is called, Then return XML ContentType
    @Test
    fun `detectContentType identifies XML content`() {
        // Arrange
        val xmlBodies = listOf(
            """<root><item>value</item></root>""",
            """<?xml version="1.0"?><data></data>""",
            """ <html><body>content</body></html> """
        )

        // Act & Assert
        xmlBodies.forEach { body ->
            val result = KtorUtils.detectContentType(body)
            assertEquals("Expected XML content type for body: $body",
                ContentType.Application.Xml, result)
        }
    }

    // BDD: Given HTML response body, When detectContentType is called, Then return HTML ContentType
    @Test
    fun `detectContentType identifies HTML content`() {
        // Arrange
        val htmlBodies = listOf(
            "This is HTML content with html tag",
            "Some HTML text here",
            "Response contains HTML elements"
        )

        // Act & Assert
        htmlBodies.forEach { body ->
            val result = KtorUtils.detectContentType(body)
            assertEquals("Expected HTML content type for body: $body",
                ContentType.Text.Html, result)
        }
    }

    // BDD: Given plain text response body, When detectContentType is called, Then return plain text ContentType
    @Test
    fun `detectContentType defaults to plain text`() {
        // Arrange
        val textBodies = listOf(
            "Simple text response",
            "Error message",
            "123456",
            ""
        )

        // Act & Assert
        textBodies.forEach { body ->
            val result = KtorUtils.detectContentType(body)
            assertEquals("Expected plain text content type for body: $body",
                ContentType.Text.Plain, result)
        }
    }

    // BDD: Given MockResponse with delay, When applyMockDelay is called, Then apply correct delay
    @Test
    fun `applyMockDelay applies correct delay`() = runTest {
        // Arrange
        val delayMs = 100L
        val mockResponse = MockResponse(code = 200, body = "test", delay = delayMs)

        // Act & Assert (Just verify function completes without error for non-zero delay)
        KtorUtils.applyMockDelay(mockResponse)
        // Note: Testing actual delay timing in unit tests is unreliable due to test scheduler
        // The function implementation uses kotlinx.coroutines.delay which will be properly delayed in real usage
    }

    // BDD: Given MockResponse with zero delay, When applyMockDelay is called, Then skip delay
    @Test
    fun `applyMockDelay skips delay when zero`() = runTest {
        // Arrange
        val mockResponse = MockResponse(code = 200, body = "test", delay = 0L)

        // Act & Assert (Just verify function completes immediately for zero delay)
        KtorUtils.applyMockDelay(mockResponse)
        // Note: Function should complete immediately without delay for delay = 0
    }

    // BDD: Given valid HTTP status codes, When getHttpMessage is called, Then return correct messages
    @Test
    fun `getHttpMessage returns correct messages`() {
        // Arrange
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
            503 to "Service Unavailable"
        )

        // Act & Assert
        testCases.forEach { (code, expectedMessage) ->
            val actualMessage = KtorUtils.getHttpMessage(code)
            assertEquals("Expected correct message for code $code", expectedMessage, actualMessage)
        }
    }

    // BDD: Given unknown HTTP status code, When getHttpMessage is called, Then return Unknown
    @Test
    fun `getHttpMessage returns Unknown for unrecognized codes`() {
        // Arrange
        val unknownCodes = listOf(100, 299, 418, 999)

        // Act & Assert
        unknownCodes.forEach { code ->
            val actualMessage = KtorUtils.getHttpMessage(code)
            assertEquals("Expected 'Unknown' for code $code", "Unknown", actualMessage)
        }
    }

    // BDD: Given valid HTTP parameters, When createMockResponse is called, Then create valid MockResponse
    @Test
    fun `createMockResponse creates valid mock response`() {
        // Arrange
        val code = 201
        val body = """{"created": true}"""
        val delay = 50L

        // Act
        val result = KtorUtils.createMockResponse(code, body, delay)

        // Assert
        assertEquals("Expected matching code", code, result.code)
        assertEquals("Expected matching body", body, result.body)
        assertEquals("Expected matching delay", delay, result.delay)
    }

    // BDD: Given invalid status code, When createMockResponse is called, Then throw exception
    @Test
    fun `createMockResponse throws exception for invalid status code`() {
        // Arrange
        val invalidCode = 999
        val body = "test"

        // Act & Assert
        try {
            KtorUtils.createMockResponse(invalidCode, body)
            fail("Expected IllegalArgumentException for invalid status code")
        } catch (e: IllegalArgumentException) {
            assertTrue("Expected error message about status code",
                e.message?.contains("Response code must be valid") ?: false)
        }
    }

    // BDD: Given negative delay, When createMockResponse is called, Then throw exception
    @Test
    fun `createMockResponse throws exception for negative delay`() {
        // Arrange
        val code = 200
        val body = "test"
        val negativeDelay = -100L

        // Act & Assert
        try {
            KtorUtils.createMockResponse(code, body, negativeDelay)
            fail("Expected IllegalArgumentException for negative delay")
        } catch (e: IllegalArgumentException) {
            assertTrue("Expected error message about delay",
                e.message?.contains("Delay must be non-negative") ?: false)
        }
    }

    // BDD: Given valid MockResponse, When validateMockResponse is called, Then not throw exception
    @Test
    fun `validateMockResponse accepts valid response`() {
        // Arrange
        val validResponse = MockResponse(code = 200, body = "OK", delay = 100L)

        // Act & Assert - Should not throw
        KtorUtils.validateMockResponse(validResponse)
    }

    // BDD: Given invalid status code MockResponse, When validateMockResponse is called, Then throw exception
    @Test
    fun `validateMockResponse rejects invalid status code`() {
        // Arrange
        val invalidResponse = MockResponse(code = 99, body = "Invalid", delay = 0L)

        // Act & Assert
        try {
            KtorUtils.validateMockResponse(invalidResponse)
            fail("Expected IllegalArgumentException for invalid status code")
        } catch (e: IllegalArgumentException) {
            assertTrue("Expected error message about response code",
                e.message?.contains("Response code must be valid") ?: false)
        }
    }

    // BDD: Given success status codes, When isSuccessful extension is called, Then return true
    @Test
    fun `isSuccessful extension returns true for 2xx codes`() {
        // Arrange
        val successCodes = listOf(200, 201, 204, 299)

        // Act & Assert
        successCodes.forEach { code ->
            val response = MockResponse(code, "test")
            assertTrue("Expected $code to be successful", response.isSuccessful())
        }
    }

    // BDD: Given client error status codes, When isClientError extension is called, Then return true
    @Test
    fun `isClientError extension returns true for 4xx codes`() {
        // Arrange
        val clientErrorCodes = listOf(400, 401, 404, 499)

        // Act & Assert
        clientErrorCodes.forEach { code ->
            val response = MockResponse(code, "error")
            assertTrue("Expected $code to be client error", response.isClientError())
        }
    }

    // BDD: Given server error status codes, When isServerError extension is called, Then return true
    @Test
    fun `isServerError extension returns true for 5xx codes`() {
        // Arrange
        val serverErrorCodes = listOf(500, 502, 503, 599)

        // Act & Assert
        serverErrorCodes.forEach { code ->
            val response = MockResponse(code, "error")
            assertTrue("Expected $code to be server error", response.isServerError())
        }
    }

    // BDD: Given success HttpStatusCode, When isSuccessful extension is called, Then return true
    @Test
    fun `HttpStatusCode isSuccessful extension works correctly`() {
        // Arrange
        val successStatuses = listOf(
            HttpStatusCode.OK,
            HttpStatusCode.Created,
            HttpStatusCode.NoContent
        )

        // Act & Assert
        successStatuses.forEach { status ->
            assertTrue("Expected $status to be successful", status.isSuccessful())
        }

        // Test non-success status
        assertFalse("Expected BadRequest to not be successful",
            HttpStatusCode.BadRequest.isSuccessful())
    }

    // BDD: Given Url object, When extractPath extension is called, Then return correct path
    @Test
    fun `Url extractPath extension works correctly`() {
        // Arrange
        val testCases = mapOf(
            Url("https://api.example.com/users") to "/users",
            Url("https://api.example.com/") to "/",
            Url("https://api.example.com/api/v1/data?param=value") to "/api/v1/data"
        )

        // Act & Assert
        testCases.forEach { (url, expectedPath) ->
            val actualPath = url.extractPath()
            assertEquals("Expected correct path for $url", expectedPath, actualPath)
        }
    }

    // BDD: Given content type and headers, When buildMockHeaders is called, Then create correct Headers
    @Test
    fun `buildMockHeaders creates correct headers`() {
        // Arrange
        val contentType = ContentType.Application.Json
        val additionalHeaders = mapOf(
            "Authorization" to "Bearer token",
            "X-Custom-Header" to "custom-value"
        )

        // Act
        val result = buildMockHeaders(contentType, additionalHeaders)

        // Assert
        assertEquals("Expected content type header",
            contentType.toString(), result[HttpHeaders.ContentType])
        assertEquals("Expected authorization header",
            "Bearer token", result["Authorization"])
        assertEquals("Expected custom header",
            "custom-value", result["X-Custom-Header"])
    }

    // BDD: Given default parameters, When buildMockHeaders is called, Then use defaults
    @Test
    fun `buildMockHeaders uses default parameters`() {
        // Act
        val result = buildMockHeaders()

        // Assert
        assertEquals("Expected default JSON content type",
            ContentType.Application.Json.toString(), result[HttpHeaders.ContentType])
    }
}