package net.spooncast.openmocker.ktor

import io.ktor.client.request.HttpRequestData
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import kotlinx.coroutines.delay
import net.spooncast.openmocker.core.MockKey
import net.spooncast.openmocker.core.MockResponse

/**
 * Utility functions and extension methods for Ktor integration with OpenMocker.
 *
 * This object provides convenient extension functions and utility methods to bridge
 * the gap between Ktor types and core OpenMocker types, following the same patterns
 * established in the OkHttp implementation.
 *
 * Key Features:
 * - Extension functions for easy conversion between Ktor and core types
 * - Mock response creation and management utilities
 * - Content type detection and handling
 * - Error handling for resource management
 * - Support for custom delays and response customization
 * - Coroutine-native implementation for async operations
 */
object KtorUtils {

    /**
     * Creates mock response information from a MockResponse and the original HttpRequestData.
     *
     * This method validates and prepares mock response data that can be used
     * by the plugin for mock response processing.
     *
     * @param mockResponse The mock response configuration
     * @param originalRequest The original HTTP request data
     * @return MockResponseInfo containing validated mock response data
     */
    suspend fun createMockKtorResponse(
        mockResponse: MockResponse,
        originalRequest: HttpRequestData
    ): MockResponseInfo {
        return createMockHttpResponse(mockResponse, originalRequest)
    }

    /**
     * Applies the delay specified in a MockResponse.
     *
     * This suspend function introduces an artificial delay before returning,
     * simulating network latency or slow server responses. Uses Kotlin coroutines
     * for non-blocking delay implementation.
     *
     * @param mockResponse The mock response containing delay information
     */
    suspend fun applyMockDelay(mockResponse: MockResponse) {
        if (mockResponse.delay > 0) {
            delay(mockResponse.delay)
        }
    }

    /**
     * Detects the appropriate ContentType based on response body content.
     *
     * This method performs simple content inspection to determine the most
     * appropriate content type for the response, supporting common formats.
     *
     * @param body The response body content
     * @return The detected ContentType
     */
    fun detectContentType(body: String): ContentType {
        val trimmed = body.trim()
        return when {
            trimmed.startsWith("{") || trimmed.startsWith("[") -> ContentType.Application.Json
            trimmed.startsWith("<") -> ContentType.Application.Xml
            body.contains("html", ignoreCase = true) -> ContentType.Text.Html
            else -> ContentType.Text.Plain
        }
    }

    /**
     * Gets the standard HTTP message for a given status code.
     *
     * @param code The HTTP status code
     * @return The corresponding HTTP message
     */
    fun getHttpMessage(code: Int): String {
        return when (code) {
            200 -> "OK"
            201 -> "Created"
            204 -> "No Content"
            400 -> "Bad Request"
            401 -> "Unauthorized"
            403 -> "Forbidden"
            404 -> "Not Found"
            405 -> "Method Not Allowed"
            500 -> "Internal Server Error"
            502 -> "Bad Gateway"
            503 -> "Service Unavailable"
            else -> "Unknown"
        }
    }

    /**
     * Validates a MockResponse configuration for correctness.
     *
     * @param mockResponse The mock response to validate
     * @throws IllegalArgumentException if validation fails
     */
    fun validateMockResponse(mockResponse: MockResponse) {
        require(mockResponse.code in 100..599) {
            "Response code must be valid HTTP status code (100-599), got: ${mockResponse.code}"
        }
        require(mockResponse.delay >= 0) {
            "Delay must be non-negative, got: ${mockResponse.delay}"
        }
    }

    /**
     * Creates a MockResponse from HTTP parameters.
     *
     * This is useful for creating mock responses programmatically without
     * having an actual Ktor HttpResponse object.
     *
     * @param code HTTP status code
     * @param body Response body content
     * @param delay Optional delay in milliseconds
     * @return A MockResponse with the specified parameters
     */
    fun createMockResponse(
        code: Int,
        body: String,
        delay: Long = 0L
    ): MockResponse {
        return MockResponse(
            code = code,
            body = body,
            delay = delay
        ).also { validateMockResponse(it) }
    }
}


/**
 * Extension function to convert a Ktor HttpResponse to a MockResponse.
 *
 * This extension safely extracts the response data and converts it to
 * a MockResponse for storage in the repository.
 *
 * Note: This uses the safe body reading function to avoid consuming the response stream.
 *
 * @param delay Optional delay to include in the mock response
 * @return A MockResponse representing this response
 */
suspend fun HttpResponse.toMockResponse(delay: Long = 0L): MockResponse {
    val bodyString = readBodySafely()

    return MockResponse(
        code = this.status.value,
        body = bodyString,
        delay = delay
    )
}

/**
 * Extension function to create a MockResponse from HttpRequestData parameters.
 *
 * This is useful for creating mock responses programmatically when you have
 * a request but want to specify custom response parameters.
 *
 * @param code HTTP status code
 * @param body Response body content
 * @param delay Optional delay in milliseconds
 * @return A MockResponse with the specified parameters
 */
fun HttpRequestData.createMockResponse(
    code: Int,
    body: String,
    delay: Long = 0L
): MockResponse {
    return KtorUtils.createMockResponse(code, body, delay)
}

/**
 * Extension function to check if a MockResponse represents a successful HTTP status.
 *
 * @return true if the response code is in the 200-299 range
 */
fun MockResponse.isSuccessful(): Boolean {
    return this.code in 200..299
}

/**
 * Extension function to check if a MockResponse represents a client error.
 *
 * @return true if the response code is in the 400-499 range
 */
fun MockResponse.isClientError(): Boolean {
    return this.code in 400..499
}

/**
 * Extension function to check if a MockResponse represents a server error.
 *
 * @return true if the response code is in the 500-599 range
 */
fun MockResponse.isServerError(): Boolean {
    return this.code in 500..599
}

/**
 * Extension function to check if a HttpStatusCode represents a successful response.
 *
 * @return true if the status code is in the 200-299 range
 */
fun HttpStatusCode.isSuccessful(): Boolean {
    return this.value in 200..299
}

/**
 * Extension function to extract the path from a Url object.
 *
 * This ensures consistent path extraction for mock key generation,
 * handling edge cases and URL formats properly.
 *
 * @return The path portion without query parameters or fragment
 */
fun Url.extractPath(): String {
    return encodedPath.takeIf { it.isNotEmpty() } ?: "/"
}

/**
 * Extension function to safely build headers for mock responses.
 *
 * @param contentType The content type to set
 * @param additionalHeaders Additional headers to include
 * @return Headers object with the specified headers
 */
fun buildMockHeaders(
    contentType: ContentType = ContentType.Application.Json,
    additionalHeaders: Map<String, String> = emptyMap()
): Headers {
    return HeadersBuilder().apply {
        append(HttpHeaders.ContentType, contentType.toString())
        additionalHeaders.forEach { (key, value) ->
            append(key, value)
        }
    }.build()
}